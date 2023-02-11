package io.taiyi.chain.sdk;

public class DocumentProperty {
    private String name;
    private PropertyType type;
    private boolean indexed;
    private boolean omissible;

    public DocumentProperty(String name, PropertyType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PropertyType getType() {
        return type;
    }

    public void setType(PropertyType type) {
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

