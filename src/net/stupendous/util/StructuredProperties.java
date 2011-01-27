/* File: StructuredProperties.java
 * 
 *    Copyright 2011 Nathan Ollerenshaw <chrome@stupendous.net>
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.stupendous.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;

import net.stupendous.util.StructuredPropertiesSymbol.Type;

/**
 * @author chrome
 *
 */
public class StructuredProperties {
    /* Class Variables and Methods */
    
    private static boolean debugging                        = false;

    public static boolean isDebugging() {
        return debugging;
    }

    public static void setDebugging(boolean debuging) {
        StructuredProperties.debugging = debuging;
    }

    /* Instance Variables and Methods */
    
    private StructuredPropertiesLexer lexer                 = null;
    private ArrayList<StructuredPropertiesSymbol> symbols   = new ArrayList<StructuredPropertiesSymbol>();
    private HashMap<String, Object> root                    = null;
    private int currentSymbolIndex                          = 0;
    private StructuredPropertiesSymbol currentSymbol        = null;

    private static void usage(String [ ] args) {
    	String usageString = 
    		"\nUsage: structuredproperties <fiename>\n" +
    		"\n" +
    		"This program will parse a Structured Properties Configuration file\n" +
    		"and then perform a toString() on the root HashMap, outputting the\n" +
    		"parsed configuration to stdout.\n";
    	
    	System.out.println(usageString);
    }
    
    public static void main(String [ ] args) {
        if (args.length == 0) {
        	usage(args);        	
            System.exit(1);
        }
        
        File f = new File(args[0]);
       
        StructuredProperties.setDebugging(true);
        
        StructuredProperties c = new StructuredProperties(f);
        
        System.out.println("Completed parse.");

        System.out.println("Parsed map:\n");
        
        for (String key : c.getRoot().keySet()) {
        	System.out.println(key + " = " + c.getRoot().get(key));
        }
        
        System.exit(0);
    }

    public StructuredProperties (File configFile) throws Error {
        InputStream in;
        
        try {
            in = new FileInputStream(configFile);
            this.lexer = new StructuredPropertiesLexer(in);
            parse();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public StructuredProperties(java.io.Reader in) throws Error {
        this.lexer = new StructuredPropertiesLexer(in);
        parse();
    }

    public StructuredProperties(java.io.InputStream in) throws Error {
        this.lexer = new StructuredPropertiesLexer(in);
        parse();
    }

    public HashMap<String, Object> getRoot() {
        return root;
    }

    private void parse() throws Error {
        StructuredPropertiesSymbol symbol;

        if (debugging)
        	System.out.println("Scanner debugging:");
        
        try {
            symbol = lexer.scan();
        
            while (symbol != null && symbol.type != Type.EOF) {
            	if (debugging) 
            		System.out.printf(" %d %s\n", symbols.size(), symbol.toString());

            	
            	symbols.add(symbol);
                symbol = lexer.scan();
            }
            
            symbols.add(new StructuredPropertiesSymbol(Type.EOF, null, 0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (debugging) {
            System.out.println("Finished lexing the file.");
            System.out.println("Size of symbol array: " + symbols.size());
        }
        
        if (symbols.size() == 0)
            throw new Error("Empty configuration.");

        currentSymbol = symbols.get(currentSymbolIndex);
        root = parseHashMap();
    }
    
    private Error expectedError(String expected) throws Error {
        return new Error(String.format(
            "Parse error on line %d: Expected symbol %s, got %s [%s].", 
            currentSymbol.line, expected, currentSymbol.type, currentSymbol.object));
    }
    
    private void nextSymbol() throws Error {
        currentSymbolIndex++;
        currentSymbol = symbols.get(currentSymbolIndex);
        
        if (debugging)
        	System.out.printf("%s :: %d : advanced Symbol to: %s\n",
        			Thread.currentThread().getStackTrace()[2].getMethodName(),
        			Thread.currentThread().getStackTrace()[2].getLineNumber(),
        			currentSymbol.toString());
    }

    private void prevSymbol() throws Error {
        currentSymbolIndex--;
        currentSymbol = symbols.get(currentSymbolIndex);
    }

    private HashMap<String, Object> parseHashMap() throws Error {
        HashMap<String, Object> map = new HashMap<String, Object>(); 
        boolean isRoot = false;
        
        if (currentSymbolIndex == 0)
            isRoot = true;

        /* Iterate until we get to the end of the defined block */
        while (currentSymbol.type == Type.IDENTIFIER) {
            SimpleEntry<String, Object> entry = parseKeyValue();
            map.put(entry.getKey(), entry.getValue());
        }
        
        switch (currentSymbol.type) {
        case EOF:
        	if (isRoot)
        		return map;
        	
        	throw expectedError(String.format("%s or %s", Type.IDENTIFIER, Type.BLOCK_END));
        case BLOCK_END:
        	return map;
        default:
        	throw expectedError(Type.BLOCK_END.toString());
        }
    }

    private SimpleEntry<String, Object> parseKeyValue() throws Error {
        SimpleEntry<String, Object> entry;

        if (currentSymbol.type != Type.IDENTIFIER)
            expectedError(Type.IDENTIFIER.toString());
        
        /* Nasty little cast here, but we know Identifiers are Strings */
        entry = new SimpleEntry<String, Object>((String) currentSymbol.object, "");
        
        nextSymbol();
        
        switch (currentSymbol.type) {
        case BLOCK_START:
            entry.setValue(parseBlock());
            return entry;
        case EQUALS:
            nextSymbol();
            
            switch (currentSymbol.type) {
            case STRING:
            case INTEGER:
            case DOUBLE:
                entry.setValue(currentSymbol.object);
                nextSymbol();
                return entry;
            default:
                throw expectedError(
                        String.format(
                                "%s, %s, %s (or IDENTIFIER { BLOCK })",
                                Type.STRING.toString(),
                                Type.INTEGER.toString(),
                                Type.DOUBLE.toString()
                            )
                        );
            }
        	
       	default:
       		throw  expectedError(String.format("%s [(] or %s [=]", Type.BLOCK_START.toString(), Type.EQUALS.toString()));
        }
    }

    private ArrayList<Object> parseArrayList() throws Error {
        ArrayList<Object> list = new ArrayList<Object>();

        while (currentSymbol.type != Type.BLOCK_END) {
            switch (currentSymbol.type) {
            case STRING:
            case INTEGER:
            case DOUBLE:
                list.add(currentSymbol.object);
                nextSymbol();
                break;
            case BLOCK_START:
                list.add(parseBlock());
                break;
            default:
                throw expectedError(
                        String.format(
                                "%s, %s, %s or %s",
                                Type.STRING.toString(),
                                Type.INTEGER.toString(),
                                Type.DOUBLE.toString(),
                                Type.BLOCK_END.toString()
                            )
                        );
            }
        }
        
        return list;
    }

    private Object parseBlock() throws Error {
    	
    	/* Parses a block from { ..... }
    	 * 
    	 * The idea is that we are given the currentSymbol pointing to 
    	 * the open brace, and we eat tokens until we reach the close
    	 * brace.
    	 *  
    	 */
    	
    	assert (currentSymbol.type == Type.BLOCK_START) : Type.BLOCK_START;
    	
    	nextSymbol();
    	
    	switch (currentSymbol.type) {
        case IDENTIFIER:
            /* Its a HashMap */
        	HashMap<String, Object> map = parseHashMap();
        	assert (currentSymbol.type == Type.BLOCK_END) : Type.BLOCK_END;
        	nextSymbol();
            return map;
        case STRING:
        case INTEGER:
        case DOUBLE:
            /* Must be an ArrayList */
        	ArrayList<Object> l1 = parseArrayList();
        	assert (currentSymbol.type == Type.BLOCK_END) : Type.BLOCK_END;
        	nextSymbol();
        	return l1;
        case BLOCK_END:
            /* There is no way to know what it could be, return null. */
        	nextSymbol();
            return null;
        case BLOCK_START:
            /* A block instead of the identifier means this is an array. */
        	ArrayList<Object> l2 = parseArrayList();
        	assert (currentSymbol.type == Type.BLOCK_END) : Type.BLOCK_END;
        	nextSymbol();
        	return l2;
        default:
            throw expectedError(
                    String.format(
                            "%s, %s, %s, %s, %s or %s",
                            Type.IDENTIFIER.toString(),
                            Type.STRING.toString(),
                            Type.INTEGER.toString(),
                            Type.DOUBLE.toString(),
                            Type.BLOCK_START.toString(),
                            Type.BLOCK_END.toString()
                        )
                    );
        }
    }
}
