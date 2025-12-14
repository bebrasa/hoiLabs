package pozhidaev.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class FamilyType {
    @XmlElement(name = "fatherRef") private List<RefType> fatherRef = new ArrayList<>();
    @XmlElement(name = "fatherName") private List<String> fatherName = new ArrayList<>();
    @XmlElement(name = "motherRef") private List<RefType> motherRef = new ArrayList<>();
    @XmlElement(name = "motherName") private List<String> motherName = new ArrayList<>();

    @XmlElement(name = "brotherRef") private List<RefType> brotherRef = new ArrayList<>();
    @XmlElement(name = "brotherName") private List<String> brotherName = new ArrayList<>();
    @XmlElement(name = "sisterRef") private List<RefType> sisterRef = new ArrayList<>();
    @XmlElement(name = "sisterName") private List<String> sisterName = new ArrayList<>();

    @XmlElement(name = "sonRef") private List<RefType> sonRef = new ArrayList<>();
    @XmlElement(name = "sonName") private List<String> sonName = new ArrayList<>();
    @XmlElement(name = "daughterRef") private List<RefType> daughterRef = new ArrayList<>();
    @XmlElement(name = "daughterName") private List<String> daughterName = new ArrayList<>();

    @XmlElement(name = "grandfatherRef") private List<RefType> grandfatherRef = new ArrayList<>();
    @XmlElement(name = "grandfatherName") private List<String> grandfatherName = new ArrayList<>();
    @XmlElement(name = "grandmotherRef") private List<RefType> grandmotherRef = new ArrayList<>();
    @XmlElement(name = "grandmotherName") private List<String> grandmotherName = new ArrayList<>();

    @XmlElement(name = "uncleRef") private List<RefType> uncleRef = new ArrayList<>();
    @XmlElement(name = "uncleName") private List<String> uncleName = new ArrayList<>();
    @XmlElement(name = "auntRef") private List<RefType> auntRef = new ArrayList<>();
    @XmlElement(name = "auntName") private List<String> auntName = new ArrayList<>();

    public List<RefType> getFatherRef() { return fatherRef; }
    public List<String> getFatherName() { return fatherName; }
    public List<RefType> getMotherRef() { return motherRef; }
    public List<String> getMotherName() { return motherName; }
    public List<RefType> getBrotherRef() { return brotherRef; }
    public List<String> getBrotherName() { return brotherName; }
    public List<RefType> getSisterRef() { return sisterRef; }
    public List<String> getSisterName() { return sisterName; }
    public List<RefType> getSonRef() { return sonRef; }
    public List<String> getSonName() { return sonName; }
    public List<RefType> getDaughterRef() { return daughterRef; }
    public List<String> getDaughterName() { return daughterName; }
    public List<RefType> getGrandfatherRef() { return grandfatherRef; }
    public List<String> getGrandfatherName() { return grandfatherName; }
    public List<RefType> getGrandmotherRef() { return grandmotherRef; }
    public List<String> getGrandmotherName() { return grandmotherName; }
    public List<RefType> getUncleRef() { return uncleRef; }
    public List<String> getUncleName() { return uncleName; }
    public List<RefType> getAuntRef() { return auntRef; }
    public List<String> getAuntName() { return auntName; }
}

