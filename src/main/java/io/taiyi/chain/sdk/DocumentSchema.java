package io.taiyi.chain.sdk;

public class DocumentSchema {
    private String name;
    private DocumentProperty[] properties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DocumentProperty[] getProperties() {
        return properties;
    }

    public void setProperties(DocumentProperty[] properties) {
        this.properties = properties;
    }
}

