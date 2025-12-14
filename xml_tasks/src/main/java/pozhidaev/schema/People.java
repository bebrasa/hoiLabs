package pozhidaev.schema;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "people")
public class People {
    @XmlAttribute(name = "count")
    private Integer count;

    @XmlElement(name = "person")
    private List<PersonType> person = new ArrayList<>();

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public List<PersonType> getPerson() { return person; }
}

