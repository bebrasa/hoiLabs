package pozhidaev.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class NameType {
    private String first;
    private String last;
    private String full;

    public String getFirst() { return first; }
    public void setFirst(String first) { this.first = first; }
    public String getLast() { return last; }
    public void setLast(String last) { this.last = last; }
    public String getFull() { return full; }
    public void setFull(String full) { this.full = full; }
}

