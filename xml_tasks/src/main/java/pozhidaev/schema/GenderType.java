package pozhidaev.schema;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;

@XmlEnum(String.class)
public enum GenderType {
    @XmlEnumValue("male") MALE,
    @XmlEnumValue("female") FEMALE,
    @XmlEnumValue("unknown") UNKNOWN;
}

