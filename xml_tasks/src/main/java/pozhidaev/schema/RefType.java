package pozhidaev.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class RefType {
    @XmlAttribute(name = "ref", required = true)
    private String ref;

    public RefType() {}
    public RefType(String ref) { this.ref = ref; }
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }
}

