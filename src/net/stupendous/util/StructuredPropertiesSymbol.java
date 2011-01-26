package net.stupendous.util;

public class StructuredPropertiesSymbol {
    public enum Type {
        IDENTIFIER,
        STRING,
        INTEGER,
        DOUBLE,
        BLOCK_START,
        BLOCK_END,
        EQUALS,
        EOF,
        ERROR,
        UNSET
    }

    public Type type = Type.UNSET;
    public Object object = null;
    
    int line = 0;

    public StructuredPropertiesSymbol(Type type, Object object, int line) {
        this.type = type;
        this.object = object;
        this.line = line;
    }
}
