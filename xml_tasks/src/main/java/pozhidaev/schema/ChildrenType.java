package pozhidaev.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ChildrenType {
    @XmlElement(name = "sonRef")
    private List<RefType> sonRef = new ArrayList<>();
    @XmlElement(name = "daughterRef")
    private List<RefType> daughterRef = new ArrayList<>();
    @XmlElement(name = "childRef")
    private List<RefType> childRef = new ArrayList<>();

    @XmlElement(name = "sonName")
    private List<String> sonName = new ArrayList<>();
    @XmlElement(name = "daughterName")
    private List<String> daughterName = new ArrayList<>();
    @XmlElement(name = "childName")
    private List<String> childName = new ArrayList<>();

    public List<RefType> getSonRef() { return sonRef; }
    public List<RefType> getDaughterRef() { return daughterRef; }
    public List<RefType> getChildRef() { return childRef; }
    public List<String> getSonName() { return sonName; }
    public List<String> getDaughterName() { return daughterName; }
    public List<String> getChildName() { return childName; }
}

