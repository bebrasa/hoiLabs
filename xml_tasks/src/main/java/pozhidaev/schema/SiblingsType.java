package pozhidaev.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class SiblingsType {
    @XmlElement(name = "brotherRef")
    private List<RefType> brotherRef = new ArrayList<>();
    @XmlElement(name = "sisterRef")
    private List<RefType> sisterRef = new ArrayList<>();
    @XmlElement(name = "siblingRef")
    private List<RefType> siblingRef = new ArrayList<>();

    @XmlElement(name = "brotherName")
    private List<String> brotherName = new ArrayList<>();
    @XmlElement(name = "sisterName")
    private List<String> sisterName = new ArrayList<>();
    @XmlElement(name = "siblingName")
    private List<String> siblingName = new ArrayList<>();

    public List<RefType> getBrotherRef() { return brotherRef; }
    public List<RefType> getSisterRef() { return sisterRef; }
    public List<RefType> getSiblingRef() { return siblingRef; }
    public List<String> getBrotherName() { return brotherName; }
    public List<String> getSisterName() { return sisterName; }
    public List<String> getSiblingName() { return siblingName; }
}

