package de.adEditor.routes.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Marker {

    @JacksonXmlProperty(isAttribute = true, localName = "i")
    private Long id;

    @JacksonXmlProperty(isAttribute = true, localName = "n")
    private String name;

    @JacksonXmlProperty(isAttribute = true, localName = "g")
    private String group;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
