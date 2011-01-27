package net.stupendous.util;

public class StructuredPropertiesSymbol {
    public enum Type {
        STRING,
        BLOCK_START,
        BLOCK_END,
        EQUALS,
        EOF,
        ERROR,
        UNSET
    }

    public Type type = Type.UNSET;
    public String object = null;
    
    int line = 0;

    public StructuredPropertiesSymbol(Type type, String object, int line) {
        this.type = type;
        this.object = object;
        this.line = line;
    }
    
    public String toString() {
    	return String.format("%d : %s (%s) ", line, type.toString(), object);
    }
}
