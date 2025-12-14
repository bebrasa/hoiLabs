package pozhidaev.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpectedType {
    @XmlAttribute(name = "children")
    private Integer children;
    @XmlAttribute(name = "siblings")
    private Integer siblings;

    public Integer getChildren() { return children; }
    public void setChildren(Integer children) { this.children = children; }
    public Integer getSiblings() { return siblings; }
    public void setSiblings(Integer siblings) { this.siblings = siblings; }
}

