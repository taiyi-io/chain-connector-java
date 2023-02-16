package io.taiyi.chain.sdk;

public class DocumentProperty {
    private String name;
    private String type;
    private boolean indexed;
    private boolean omissible;

    public DocumentProperty(String name, String type) {
        this.name = name;
        this.type = type;
    }
    public DocumentProperty(String name, PropertyType type) {
        this.name = name;
        this.type = type.toString();
    }

    public DocumentProperty(String name, PropertyType type, boolean enableIndex) {
        this.name = name;
        this.type = type.toString();
        this.indexed = enableIndex;
    }

    public DocumentProperty(String name, PropertyType type, boolean enableIndex, boolean propertyOmissible) {
        this.name = name;
        this.type = type.toString();
        this.indexed = enableIndex;
        this.omissible = propertyOmissible;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    public boolean isOmissible() {
        return omissible;
    }

    public void setOmissible(boolean omissible) {
        this.omissible = omissible;
    }
}

