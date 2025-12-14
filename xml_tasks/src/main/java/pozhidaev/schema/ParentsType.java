package pozhidaev.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ParentsType {
    @XmlElement(name = "parentRef")
    private List<RefType> parentRef = new ArrayList<>();
    @XmlElement(name = "parentName")
    private List<String> parentName = new ArrayList<>();

    public List<RefType> getParentRef() { return parentRef; }
    public List<String> getParentName() { return parentName; }
}

