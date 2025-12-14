package pozhidaev.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class SpousesType {
    @XmlElement(name = "spouseRef")
    private List<RefType> spouseRef = new ArrayList<>();
    @XmlElement(name = "spouseName")
    private List<String> spouseName = new ArrayList<>();

    public List<RefType> getSpouseRef() { return spouseRef; }
    public List<String> getSpouseName() { return spouseName; }
}

