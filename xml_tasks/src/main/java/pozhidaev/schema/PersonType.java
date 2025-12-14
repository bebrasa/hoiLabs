package pozhidaev.schema;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class PersonType {
    @XmlAttribute(name = "id")
    @XmlID
    private String id;

    private NameType name;

    @XmlElement(required = true)
    private GenderType gender = GenderType.UNKNOWN;

    private SpousesType spouses;
    private ParentsType parents;
    private ChildrenType children;
    private SiblingsType siblings;
    private ExpectedType expected;
    private FamilyType family;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public NameType getName() { return name; }
    public void setName(NameType name) { this.name = name; }
    public GenderType getGender() { return gender; }
    public void setGender(GenderType gender) { this.gender = gender; }
    public SpousesType getSpouses() { return spouses; }
    public void setSpouses(SpousesType spouses) { this.spouses = spouses; }
    public ParentsType getParents() { return parents; }
    public void setParents(ParentsType parents) { this.parents = parents; }
    public ChildrenType getChildren() { return children; }
    public void setChildren(ChildrenType children) { this.children = children; }
    public SiblingsType getSiblings() { return siblings; }
    public void setSiblings(SiblingsType siblings) { this.siblings = siblings; }
    public ExpectedType getExpected() { return expected; }
    public void setExpected(ExpectedType expected) { this.expected = expected; }
    public FamilyType getFamily() { return family; }
    public void setFamily(FamilyType family) { this.family = family; }
}
