package pozhidaev;

import javax.xml.stream.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Single-file XML cleaner that parses the provided people.xml and writes a consolidated, structured XML.
 *
 * Usage:
 *   java pozhidaev.XmlCleaner [input.xml] [output.xml]
 * Defaults:
 *   input:  src/people.xml
 *   output: build/output/people_consolidated.xml
 */
public class XmlCleaner {

    // ------- Public entry -------
    public static void main(String[] args) throws Exception {
        String inputPath = args != null && args.length > 0 ? args[0] : "src/people.xml";
        String outputPath = args != null && args.length > 1 ? args[1] : "build/output/people_consolidated.xml";

        Path in = Path.of(inputPath);
        Path out = Path.of(outputPath);
        Files.createDirectories(out.getParent());

        Map<String, Person> people = parsePeople(in);
        // write via JAXB with schema validation
        writeConsolidatedJaxb(out, people);

        System.out.println("Parsed people (by id/name): " + people.size());
        System.out.println("Wrote: " + out.toAbsolutePath());
    }

    // ------- Model (inner to keep single-file) -------
    private enum Gender { MALE, FEMALE, UNKNOWN }

    private static class Person {
        String id; // canonical id if known (e.g., P123)

        // Names
        final Set<String> firstNames = new LinkedHashSet<>();
        final Set<String> lastNames = new LinkedHashSet<>();
        final Set<String> fullNames = new LinkedHashSet<>();

        final Set<Gender> genderEvidence = new LinkedHashSet<>();

        final Set<String> spouseIds = new LinkedHashSet<>();
        final Set<String> spouseNames = new LinkedHashSet<>();

        final Set<String> parentIds = new LinkedHashSet<>();
        final Set<String> parentNames = new LinkedHashSet<>();

        final Set<String> siblingIds = new LinkedHashSet<>();
        final Set<String> siblingNames = new LinkedHashSet<>();
        final Set<String> brotherNames = new LinkedHashSet<>();
        final Set<String> sisterNames = new LinkedHashSet<>();

        final Set<String> sonIds = new LinkedHashSet<>();
        final Set<String> daughterIds = new LinkedHashSet<>();
        final Set<String> childIds = new LinkedHashSet<>();
        final Set<String> sonNames = new LinkedHashSet<>();
        final Set<String> daughterNames = new LinkedHashSet<>();
        final Set<String> childNames = new LinkedHashSet<>();

        // explicit parent names by role (when present in source)
        final Set<String> fatherNames = new LinkedHashSet<>();
        final Set<String> motherNames = new LinkedHashSet<>();

        Integer expectedChildren;
        Integer expectedSiblings;

        void setId(String v) {
            if (v == null || v.isBlank()) return;
            String nv = v.trim();
            if (this.id == null) this.id = nv;
        }

        String bestFirst() { return pickBest(firstNames); }
        String bestLast()  { return pickBest(lastNames); }
        String bestFull()  { 
            String full = pickBest(fullNames);
            if (full != null) return full;
            String fn = bestFirst();
            String ln = bestLast();
            if (fn == null && ln == null) return null;
            if (fn == null) return ln;
            if (ln == null) return fn;
            return fn + " " + ln;
        }

        Gender resolvedGender() {
            if (genderEvidence.contains(Gender.MALE) && !genderEvidence.contains(Gender.FEMALE)) return Gender.MALE;
            if (genderEvidence.contains(Gender.FEMALE) && !genderEvidence.contains(Gender.MALE)) return Gender.FEMALE;
            if (genderEvidence.isEmpty()) return Gender.UNKNOWN;
            return Gender.UNKNOWN; // conflicting -> unknown
        }
    }

    // ------- Parse using StAX -------
    private static Map<String, Person> parsePeople(Path input) throws Exception {
        XMLInputFactory f = XMLInputFactory.newInstance();
        Map<String, Person> byId = new LinkedHashMap<>();
        Map<String, Person> byName = new LinkedHashMap<>(); // for entries without id

        try (InputStream is = new BufferedInputStream(Files.newInputStream(input))) {
            XMLStreamReader r = f.createXMLStreamReader(is);
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT && r.getLocalName().equals("person")) {
                    Person p = readPerson(r);
                    mergePerson(p, byId, byName);
                }
            }
            r.close();
        }

        // Merge name-indexed into id-indexed if later IDs appeared with same names
        for (Iterator<Map.Entry<String, Person>> it = byName.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Person> e = it.next();
            Person p = e.getValue();
            String keyName = e.getKey();
            // If there is now an id-person with same best name, merge into it
            Person target = findByBestName(byId, keyName);
            if (target != null) {
                mergeInto(target, p);
                it.remove();
            }
        }

        // Append remaining name-only people into byId under a synthetic key
        int anon = 1;
        for (Person p : byName.values()) {
            if (p.id == null) {
                p.id = "ANON_" + (anon++);
            }
            byId.put(p.id, p);
        }
        return byId;
    }

    private static Person readPerson(XMLStreamReader r) throws XMLStreamException {
        Person p = new Person();

        // person @id and @name
        String attrId = attr(r, "id");
        if (isMeaningful(attrId)) p.setId(attrId);
        String attrName = attr(r, "name");
        if (isMeaningful(attrName)) p.fullNames.add(clean(attrName));

        int depth = 1; // we are at START_ELEMENT person
        while (r.hasNext() && depth > 0) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String el = r.getLocalName();
                switch (el) {
                    case "id": {
                        String v = attr(r, "value");
                        if (isMeaningful(v)) p.setId(v);
                        String text = safeElementText(r); // consumes to END_ELEMENT id
                        if (isMeaningful(text)) p.setId(text);
                        break;
                    }
                    case "firstname": {
                        String v = attr(r, "value");
                        String text = v != null ? v : safeElementText(r);
                        if (isMeaningful(text)) p.firstNames.add(clean(text));
                        break;
                    }
                    case "surname": {
                        String v = attr(r, "value");
                        String text = v != null ? v : safeElementText(r);
                        if (isMeaningful(text)) p.lastNames.add(clean(text));
                        break;
                    }
                    case "fullname": {
                        readFullname(r, p);
                        break;
                    }
                    case "gender": {
                        String v = attr(r, "value");
                        String text = v != null ? v : safeElementText(r);
                        Gender g = parseGender(text);
                        if (g != null) p.genderEvidence.add(g);
                        break;
                    }
                    case "husband":
                    case "wife":
                    case "spouse":
                    case "spouce": { // typo in source
                        String v = attr(r, "value");
                        if (isMeaningful(v)) addIdOrName(v, p.spouseIds, p.spouseNames);
                        skipElement(r); // ensure we are after this tag
                        break;
                    }
                    case "father": {
                        String text = safeElementText(r);
                        if (isMeaningful(text) && !isUnknown(text)) {
                            String t = clean(text);
                            p.parentNames.add(t);
                            p.fatherNames.add(t);
                        }
                        break;
                    }
                    case "mother": {
                        String text = safeElementText(r);
                        if (isMeaningful(text) && !isUnknown(text)) {
                            String t = clean(text);
                            p.parentNames.add(t);
                            p.motherNames.add(t);
                        }
                        break;
                    }
                    case "parent": {
                        String v = attr(r, "value");
                        if (isMeaningful(v) && !isUnknown(v)) addIdOrName(v, p.parentIds, p.parentNames);
                        String text = safeElementText(r);
                        if (isMeaningful(text) && !isUnknown(text)) addIdOrName(text, p.parentIds, p.parentNames);
                        break;
                    }
                    case "siblings": {
                        String v = attr(r, "val");
                        if (isMeaningful(v)) {
                            for (String token : v.split("\\s+")) {
                                if (isMeaningful(token)) addIdOrName(token, p.siblingIds, p.siblingNames);
                            }
                            skipElement(r);
                        } else {
                            readSiblingsBlock(r, p);
                        }
                        break;
                    }
                    case "siblings-number": {
                        String v = attr(r, "value");
                        String text = v != null ? v : safeElementText(r);
                        Integer n = parseIntSafe(text);
                        if (n != null) p.expectedSiblings = n;
                        break;
                    }
                    case "children-number": {
                        String v = attr(r, "value");
                        String text = v != null ? v : safeElementText(r);
                        Integer n = parseIntSafe(text);
                        if (n != null) p.expectedChildren = n;
                        break;
                    }
                    case "children": {
                        readChildrenBlock(r, p);
                        break;
                    }
                    default: {
                        // Unknown or unhandled nested tag -> skip its subtree
                        skipElement(r);
                    }
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT) {
                if (r.getLocalName().equals("person")) depth--;
            }
        }
        return p;
    }

    private static void readFullname(XMLStreamReader r, Person p) throws XMLStreamException {
        // r is at START_ELEMENT fullname
        String first = null, last = null;
        int depth = 1;
        while (r.hasNext() && depth > 0) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String el = r.getLocalName();
                if (el.equals("first")) {
                    first = safeElementText(r);
                } else if (el.equals("family")) {
                    last = safeElementText(r);
                } else {
                    skipElement(r);
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("fullname")) {
                depth--;
            }
        }
        if (isMeaningful(first)) p.firstNames.add(clean(first));
        if (isMeaningful(last)) p.lastNames.add(clean(last));
        String full = buildFull(first, last);
        if (isMeaningful(full)) p.fullNames.add(full);
    }

    private static void readSiblingsBlock(XMLStreamReader r, Person p) throws XMLStreamException {
        // r is at START_ELEMENT siblings; read nested until END_ELEMENT siblings
        int depth = 1;
        while (r.hasNext() && depth > 0) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String el = r.getLocalName();
                if (el.equals("brother")) {
                    String text = safeElementText(r);
                    if (isMeaningful(text)) p.brotherNames.add(clean(text));
                } else if (el.equals("sister")) {
                    String text = safeElementText(r);
                    if (isMeaningful(text)) p.sisterNames.add(clean(text));
                } else if (el.equals("sibling")) {
                    String text = safeElementText(r);
                    if (isMeaningful(text)) p.siblingNames.add(clean(text));
                } else {
                    skipElement(r);
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("siblings")) {
                depth--;
            }
        }
    }

    private static void readChildrenBlock(XMLStreamReader r, Person p) throws XMLStreamException {
        int depth = 1;
        while (r.hasNext() && depth > 0) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String el = r.getLocalName();
                if (el.equals("son")) {
                    String id = attr(r, "id");
                    if (isMeaningful(id)) { p.sonIds.add(id.trim()); skipElement(r); }
                    else { String text = safeElementText(r); if (isMeaningful(text)) p.sonNames.add(clean(text)); }
                } else if (el.equals("daughter")) {
                    String id = attr(r, "id");
                    if (isMeaningful(id)) { p.daughterIds.add(id.trim()); skipElement(r); }
                    else { String text = safeElementText(r); if (isMeaningful(text)) p.daughterNames.add(clean(text)); }
                } else if (el.equals("child")) {
                    String id = attr(r, "id");
                    if (isMeaningful(id)) { p.childIds.add(id.trim()); skipElement(r); }
                    else { String text = safeElementText(r); if (isMeaningful(text)) p.childNames.add(clean(text)); }
                } else {
                    skipElement(r);
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("children")) {
                depth--;
            }
        }
    }

    // ------- Merge logic (id preferred; else best full name) -------
    private static void mergePerson(Person incoming, Map<String, Person> byId, Map<String, Person> byName) {
        String bestName = incoming.bestFull();
        if (isMeaningful(incoming.id)) {
            Person target = byId.get(incoming.id);
            if (target == null) {
                target = incoming;
                byId.put(target.id, target);
            } else {
                mergeInto(target, incoming);
            }
            if (isMeaningful(bestName)) {
                Person nameOnly = byName.remove(normalizeKey(bestName));
                if (nameOnly != null) mergeInto(target, nameOnly);
            }
        } else if (isMeaningful(bestName)) {
            String key = normalizeKey(bestName);
            Person target = byName.get(key);
            if (target == null) byName.put(key, incoming);
            else mergeInto(target, incoming);
        } else {
            // No id, no name -> keep as name-only with synthetic key
            String key = "__ANON__" + byName.size();
            byName.put(key, incoming);
        }
    }

    private static void mergeInto(Person target, Person src) {
        if (target.id == null && src.id != null) target.id = src.id;

        target.firstNames.addAll(src.firstNames);
        target.lastNames.addAll(src.lastNames);
        target.fullNames.addAll(src.fullNames);
        target.genderEvidence.addAll(src.genderEvidence);

        target.spouseIds.addAll(src.spouseIds);
        target.spouseNames.addAll(src.spouseNames);

        target.parentIds.addAll(src.parentIds);
        target.parentNames.addAll(src.parentNames);

        target.siblingIds.addAll(src.siblingIds);
        target.siblingNames.addAll(src.siblingNames);
        target.brotherNames.addAll(src.brotherNames);
        target.sisterNames.addAll(src.sisterNames);

        target.sonIds.addAll(src.sonIds);
        target.daughterIds.addAll(src.daughterIds);
        target.childIds.addAll(src.childIds);
        target.sonNames.addAll(src.sonNames);
        target.daughterNames.addAll(src.daughterNames);
        target.childNames.addAll(src.childNames);

        if (target.expectedChildren == null) target.expectedChildren = src.expectedChildren;
        if (target.expectedSiblings == null) target.expectedSiblings = src.expectedSiblings;
    }

    private static Person findByBestName(Map<String, Person> byId, String bestNameKey) {
        for (Person p : byId.values()) {
            String best = p.bestFull();
            if (isMeaningful(best) && normalizeKey(best).equals(bestNameKey)) return p;
        }
        return null;
    }

    // ------- Write structured, consolidated XML (StAX) -------
    private static void writeConsolidated(Path output, Map<String, Person> people) throws Exception {
        XMLOutputFactory of = XMLOutputFactory.newInstance();
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(output))) {
            XMLStreamWriter w = of.createXMLStreamWriter(os, "UTF-8");
            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement("people");
            w.writeAttribute("count", String.valueOf(people.size()));

            // Stable order by id
            List<Person> list = new ArrayList<>(people.values());
            list.sort(Comparator.comparing(p -> p.id == null ? "" : p.id));

            for (Person p : list) {
                w.writeStartElement("person");
                if (isMeaningful(p.id)) w.writeAttribute("id", p.id);

                // name block
                String full = p.bestFull();
                String first = p.bestFirst();
                String last = p.bestLast();
                if (isMeaningful(full) || isMeaningful(first) || isMeaningful(last)) {
                    w.writeStartElement("name");
                    if (isMeaningful(first)) writeSimple(w, "first", first);
                    if (isMeaningful(last)) writeSimple(w, "last", last);
                    if (isMeaningful(full)) writeSimple(w, "full", full);
                    w.writeEndElement();
                }

                // gender
                Gender g = p.resolvedGender();
                w.writeEmptyElement("gender");
                w.writeAttribute("value", genderToString(g));

                // spouses
                if (!p.spouseIds.isEmpty() || !p.spouseNames.isEmpty()) {
                    w.writeStartElement("spouses");
                    for (String sid : p.spouseIds) { w.writeEmptyElement("spouse"); w.writeAttribute("id", sid); }
                    for (String sn : p.spouseNames) writeSimple(w, "spouse", sn);
                    w.writeEndElement();
                }

                // parents
                if (!p.parentIds.isEmpty() || !p.parentNames.isEmpty()) {
                    w.writeStartElement("parents");
                    for (String pid : p.parentIds) { w.writeEmptyElement("parent"); w.writeAttribute("id", pid); }
                    for (String pn : p.parentNames) writeSimple(w, "parent", pn);
                    w.writeEndElement();
                }

                // children
                if (!p.sonIds.isEmpty() || !p.daughterIds.isEmpty() || !p.childIds.isEmpty() ||
                        !p.sonNames.isEmpty() || !p.daughterNames.isEmpty() || !p.childNames.isEmpty()) {
                    w.writeStartElement("children");
                    for (String id : p.sonIds) { w.writeEmptyElement("son"); w.writeAttribute("id", id); }
                    for (String id : p.daughterIds) { w.writeEmptyElement("daughter"); w.writeAttribute("id", id); }
                    for (String id : p.childIds) { w.writeEmptyElement("child"); w.writeAttribute("id", id); }
                    for (String nm : p.sonNames) writeSimple(w, "son", nm);
                    for (String nm : p.daughterNames) writeSimple(w, "daughter", nm);
                    for (String nm : p.childNames) writeSimple(w, "child", nm);
                    w.writeEndElement();
                }

                // siblings: convert to brother/sister when possible by looking up sibling id gender
                if (!p.siblingIds.isEmpty() || !p.brotherNames.isEmpty() || !p.sisterNames.isEmpty() || !p.siblingNames.isEmpty()) {
                    w.writeStartElement("siblings");
                    for (String sid : p.siblingIds) {
                        Person sib = people.get(sid);
                        Gender sg = sib != null ? sib.resolvedGender() : Gender.UNKNOWN;
                        String tag = (sg == Gender.MALE) ? "brother" : (sg == Gender.FEMALE ? "sister" : "sibling");
                        w.writeEmptyElement(tag);
                        w.writeAttribute("id", sid);
                    }
                    for (String nm : p.brotherNames) writeSimple(w, "brother", nm);
                    for (String nm : p.sisterNames) writeSimple(w, "sister", nm);
                    for (String nm : p.siblingNames) writeSimple(w, "sibling", nm);
                    w.writeEndElement();
                }

                // expected markers
                if (p.expectedChildren != null || p.expectedSiblings != null) {
                    w.writeEmptyElement("expected");
                    if (p.expectedChildren != null) w.writeAttribute("children", String.valueOf(p.expectedChildren));
                    if (p.expectedSiblings != null) w.writeAttribute("siblings", String.valueOf(p.expectedSiblings));
                }

                w.writeEndElement(); // person
            }

            w.writeEndElement(); // people
            w.writeEndDocument();
            w.flush();
            w.close();
        }
    }

    private static void writeSimple(XMLStreamWriter w, String tag, String text) throws XMLStreamException {
        w.writeStartElement(tag);
        w.writeCharacters(text);
        w.writeEndElement();
    }

    // ------- Helpers -------
    private static String attr(XMLStreamReader r, String name) {
        String v = r.getAttributeValue(null, name);
        if (v == null) {
            for (int i = 0; i < r.getAttributeCount(); i++) {
                if (name.equals(r.getAttributeLocalName(i))) return r.getAttributeValue(i);
            }
        }
        return v;
    }

    private static void addIdOrName(String raw, Set<String> ids, Set<String> names) {
        String v = raw.trim();
        if (v.matches("[Pp]\\d+")) ids.add(v.toUpperCase());
        else if (!isUnknown(v)) names.add(clean(v));
    }

    private static boolean isUnknown(String s) {
        String v = s == null ? null : s.trim();
        if (v == null || v.isEmpty()) return false;
        String t = v.toLowerCase();
        return t.equals("unknown") || t.equals("none");
    }

    private static String clean(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ");
    }

    private static boolean isMeaningful(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String normalizeKey(String name) {
        return clean(name).toLowerCase();
    }

    private static String pickBest(Set<String> variants) {
        if (variants.isEmpty()) return null;
        return variants.stream().max(Comparator.comparingInt(String::length)).orElse(null);
    }

    private static String buildFull(String first, String last) {
        first = clean(first);
        last = clean(last);
        if (!isMeaningful(first) && !isMeaningful(last)) return null;
        if (!isMeaningful(first)) return last;
        if (!isMeaningful(last)) return first;
        return first + " " + last;
    }

    private static Integer parseIntSafe(String s) {
        if (!isMeaningful(s)) return null;
        try { return Integer.parseInt(s.trim()); } catch (Exception ignored) { return null; }
    }

    private static Gender parseGender(String raw) {
        if (!isMeaningful(raw)) return null;
        String s = raw.trim().toLowerCase();
        if (s.equals("m") || s.equals("male")) return Gender.MALE;
        if (s.equals("f") || s.equals("female")) return Gender.FEMALE;
        return Gender.UNKNOWN;
    }

    private static String genderToString(Gender g) {
        if (g == null) return "unknown";
        switch (g) {
            case MALE: return "male";
            case FEMALE: return "female";
            default: return "unknown";
        }
    }

    private static String safeElementText(XMLStreamReader r) throws XMLStreamException {
        // Only call when the current element is simple (no nested start elements)
        try {
            return clean(r.getElementText());
        } catch (XMLStreamException ex) {
            // If not simple, consume subtree to keep the parser stable
            skipElement(r);
            return null;
        }
    }

    private static void skipElement(XMLStreamReader r) throws XMLStreamException {
        int depth = 1; // at START_ELEMENT
        while (depth > 0 && r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) depth++;
            else if (ev == XMLStreamConstants.END_ELEMENT) depth--;
        }
    }

    // ------- JAXB output with schema validation -------
    private static void writeConsolidatedJaxb(Path output, Map<String, Person> people) throws Exception {
        pozhidaev.schema.People root = new pozhidaev.schema.People();
        // Deterministic order by id
        List<Person> list = new ArrayList<>(people.values());
        list.sort(Comparator.comparing(p -> p.id == null ? "" : p.id));
        for (Person p : list) {
            pozhidaev.schema.PersonType jp = new pozhidaev.schema.PersonType();
            if (isMeaningful(p.id)) jp.setId(p.id);

            String first = p.bestFirst();
            String last = p.bestLast();
            String full = p.bestFull();
            if (isMeaningful(first) || isMeaningful(last) || isMeaningful(full)) {
                pozhidaev.schema.NameType nm = new pozhidaev.schema.NameType();
                if (isMeaningful(first)) nm.setFirst(first);
                if (isMeaningful(last)) nm.setLast(last);
                if (isMeaningful(full)) nm.setFull(full);
                jp.setName(nm);
            }

            // gender
            switch (p.resolvedGender()) {
                case MALE -> jp.setGender(pozhidaev.schema.GenderType.MALE);
                case FEMALE -> jp.setGender(pozhidaev.schema.GenderType.FEMALE);
                default -> jp.setGender(pozhidaev.schema.GenderType.UNKNOWN);
            }

            // spouses
            if (!p.spouseIds.isEmpty() || !p.spouseNames.isEmpty()) {
                pozhidaev.schema.SpousesType st = new pozhidaev.schema.SpousesType();
                for (String id : p.spouseIds) st.getSpouseRef().add(new pozhidaev.schema.RefType(id));
                for (String n : p.spouseNames) st.getSpouseName().add(n);
                jp.setSpouses(st);
            }

            // parents
            if (!p.parentIds.isEmpty() || !p.parentNames.isEmpty()) {
                pozhidaev.schema.ParentsType pt = new pozhidaev.schema.ParentsType();
                for (String id : p.parentIds) pt.getParentRef().add(new pozhidaev.schema.RefType(id));
                for (String n : p.parentNames) pt.getParentName().add(n);
                jp.setParents(pt);
            }

            // children
            if (!p.sonIds.isEmpty() || !p.daughterIds.isEmpty() || !p.childIds.isEmpty() ||
                    !p.sonNames.isEmpty() || !p.daughterNames.isEmpty() || !p.childNames.isEmpty()) {
                pozhidaev.schema.ChildrenType ct = new pozhidaev.schema.ChildrenType();
                for (String id : p.sonIds) ct.getSonRef().add(new pozhidaev.schema.RefType(id));
                for (String id : p.daughterIds) ct.getDaughterRef().add(new pozhidaev.schema.RefType(id));
                for (String id : p.childIds) ct.getChildRef().add(new pozhidaev.schema.RefType(id));
                ct.getSonName().addAll(p.sonNames);
                ct.getDaughterName().addAll(p.daughterNames);
                ct.getChildName().addAll(p.childNames);
                jp.setChildren(ct);
            }

            // siblings
            if (!p.siblingIds.isEmpty() || !p.brotherNames.isEmpty() || !p.sisterNames.isEmpty() || !p.siblingNames.isEmpty()) {
                pozhidaev.schema.SiblingsType sb = new pozhidaev.schema.SiblingsType();
                for (String sid : p.siblingIds) {
                    Person sib = people.get(sid);
                    Gender sg = sib != null ? sib.resolvedGender() : Gender.UNKNOWN;
                    switch (sg) {
                        case MALE -> sb.getBrotherRef().add(new pozhidaev.schema.RefType(sid));
                        case FEMALE -> sb.getSisterRef().add(new pozhidaev.schema.RefType(sid));
                        default -> sb.getSiblingRef().add(new pozhidaev.schema.RefType(sid));
                    }
                }
                sb.getBrotherName().addAll(p.brotherNames);
                sb.getSisterName().addAll(p.sisterNames);
                sb.getSiblingName().addAll(p.siblingNames);
                jp.setSiblings(sb);
            }

            if (p.expectedChildren != null || p.expectedSiblings != null) {
                pozhidaev.schema.ExpectedType ex = new pozhidaev.schema.ExpectedType();
                ex.setChildren(p.expectedChildren);
                ex.setSiblings(p.expectedSiblings);
                jp.setExpected(ex);
            }

            // Derived family section
            pozhidaev.schema.FamilyType fam = new pozhidaev.schema.FamilyType();
            // father/mother refs from parentIds by gender
            for (String pid : p.parentIds) {
                Person par = people.get(pid);
                Gender g = par != null ? par.resolvedGender() : Gender.UNKNOWN;
                if (g == Gender.MALE) fam.getFatherRef().add(new pozhidaev.schema.RefType(pid));
                else if (g == Gender.FEMALE) fam.getMotherRef().add(new pozhidaev.schema.RefType(pid));
            }
            // father/mother names if present
            fam.getFatherName().addAll(p.fatherNames);
            fam.getMotherName().addAll(p.motherNames);

            // siblings by role from siblingIds + names
            for (String sid : p.siblingIds) {
                Person sib = people.get(sid);
                Gender sg = sib != null ? sib.resolvedGender() : Gender.UNKNOWN;
                switch (sg) {
                    case MALE -> fam.getBrotherRef().add(new pozhidaev.schema.RefType(sid));
                    case FEMALE -> fam.getSisterRef().add(new pozhidaev.schema.RefType(sid));
                    default -> { /* ignore into untyped */ }
                }
            }
            fam.getBrotherName().addAll(p.brotherNames);
            fam.getSisterName().addAll(p.sisterNames);

            // children by role
            for (String cid : p.sonIds) fam.getSonRef().add(new pozhidaev.schema.RefType(cid));
            for (String cid : p.daughterIds) fam.getDaughterRef().add(new pozhidaev.schema.RefType(cid));
            fam.getSonName().addAll(p.sonNames);
            fam.getDaughterName().addAll(p.daughterNames);

            // grandparents by traversing parents -> their parents
            for (String pid : p.parentIds) {
                Person par = people.get(pid);
                if (par == null) continue;
                for (String gpid : par.parentIds) {
                    Person gp = people.get(gpid);
                    Gender gg = gp != null ? gp.resolvedGender() : Gender.UNKNOWN;
                    if (gg == Gender.MALE) fam.getGrandfatherRef().add(new pozhidaev.schema.RefType(gpid));
                    else if (gg == Gender.FEMALE) fam.getGrandmotherRef().add(new pozhidaev.schema.RefType(gpid));
                }
            }
            // We cannot reliably classify grandparent names by gender from names-only, so omit Name fallbacks here.

            // uncles/aunts: siblings of each parent
            for (String pid : p.parentIds) {
                Person par = people.get(pid);
                if (par == null) continue;
                for (String psid : par.siblingIds) {
                    // avoid adding the parent themself if listed as own sibling erroneously
                    if (psid.equals(pid)) continue;
                    Person sib = people.get(psid);
                    Gender sg = sib != null ? sib.resolvedGender() : Gender.UNKNOWN;
                    if (sg == Gender.MALE) fam.getUncleRef().add(new pozhidaev.schema.RefType(psid));
                    else if (sg == Gender.FEMALE) fam.getAuntRef().add(new pozhidaev.schema.RefType(psid));
                }
                // names fallbacks from parent's brother/sister names
                fam.getUncleName().addAll(par.brotherNames);
                fam.getAuntName().addAll(par.sisterNames);
            }

            // attach if anything present
            if (!fam.getFatherRef().isEmpty() || !fam.getMotherRef().isEmpty() ||
                !fam.getFatherName().isEmpty() || !fam.getMotherName().isEmpty() ||
                !fam.getBrotherRef().isEmpty() || !fam.getSisterRef().isEmpty() ||
                !fam.getBrotherName().isEmpty() || !fam.getSisterName().isEmpty() ||
                !fam.getSonRef().isEmpty() || !fam.getDaughterRef().isEmpty() ||
                !fam.getSonName().isEmpty() || !fam.getDaughterName().isEmpty() ||
                !fam.getGrandfatherRef().isEmpty() || !fam.getGrandmotherRef().isEmpty() ||
                !fam.getUncleRef().isEmpty() || !fam.getAuntRef().isEmpty() ||
                !fam.getUncleName().isEmpty() || !fam.getAuntName().isEmpty()) {
                jp.setFamily(fam);
            }

            root.getPerson().add(jp);
        }
        root.setCount(root.getPerson().size());

        // Marshal with schema validation
        jakarta.xml.bind.JAXBContext ctx = jakarta.xml.bind.JAXBContext.newInstance(pozhidaev.schema.People.class);
        jakarta.xml.bind.Marshaller m = ctx.createMarshaller();
        m.setProperty(jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        javax.xml.validation.SchemaFactory sf = javax.xml.validation.SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        java.net.URL xsdUrl = Thread.currentThread().getContextClassLoader().getResource("people_v1.xsd");
        if (xsdUrl == null) throw new IllegalStateException("people_v1.xsd not found on classpath");
        javax.xml.validation.Schema schema = sf.newSchema(xsdUrl);
        m.setSchema(schema);

        try (java.io.OutputStream os = new java.io.BufferedOutputStream(java.nio.file.Files.newOutputStream(output))) {
            m.marshal(root, os);
        }
    }
}
