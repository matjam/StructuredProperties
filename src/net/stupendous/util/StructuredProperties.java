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
	
	private static boolean debugging 						= false;

	public static boolean isDebugging() {
		return debugging;
	}

	public static void setDebugging(boolean debuging) {
		StructuredProperties.debugging = debuging;
	}

	/* Instance Variables and Methods */
	
	private StructuredPropertiesLexer lexer 				= null;
    private ArrayList<StructuredPropertiesSymbol> symbols 	= new ArrayList<StructuredPropertiesSymbol>();
    private HashMap<String, Object> root 					= null;
    private int currentSymbolIndex 							= 0;
    private StructuredPropertiesSymbol currentSymbol 		= null;

    public static void main(String [ ] args) {
    	if (args.length == 0)
    		System.err.println("args: <filename>");
    	
    	File f = new File(args[0]);
       
    	StructuredProperties.setDebugging(true);
    	
    	StructuredProperties c = new StructuredProperties(f);
        
        System.out.println("Completed parse.");
        
        System.out.println(c.getRoot().toString());

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

    	try {
			symbol = lexer.scan();
		
			while (symbol != null && symbol.type != Type.EOF) {
				symbols.add(symbol);
				symbol = lexer.scan();
				
				if (debugging)
					System.out.println(symbol.type.toString());
	        }
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
			
			nextSymbol();
		}
		
		if (isRoot && currentSymbol.type == Type.EOF) {
			/* We expect to hit EOF if we're parsing the root map */
			return map;
		}
		
		if (currentSymbol.type != Type.BLOCK_END) {
			/* The only thing that should end a hashmap is a curly brace. */
			
			expectedError(Type.BLOCK_END.toString());
		}
		
		nextSymbol();
		
		return map;
    }

	private SimpleEntry<String, Object> parseKeyValue() throws Error {
    	SimpleEntry<String, Object> entry;

    	if (currentSymbol.type != Type.IDENTIFIER)
    		expectedError(Type.IDENTIFIER.toString());
    	
    	/* Nasty little cast here, but we know Identifiers are Strings */
    	entry = new SimpleEntry<String, Object>((String) currentSymbol.object, "");
    	
    	nextSymbol();
    	
    	/* Blocks don't need to have an = after the identifier */
    	
    	if (currentSymbol.type == Type.BLOCK_START) {
    		nextSymbol();
    		entry.setValue(parseBlock());
    		return entry;
    	}
    	
    	/* It's not a block, so it must have an = after it */
    	
    	if (currentSymbol.type != Type.EQUALS)
    		expectedError(String.format("%s (=)", Type.EQUALS.toString()));
    	
    	nextSymbol();
    	
    	switch (currentSymbol.type) {
    	case STRING:
    	case INTEGER:
    	case DOUBLE:
    		entry.setValue(parseValue());
    	}
    	
    	
		return entry;
    }

	private ArrayList<Object> parseArrayList() throws Error {
    	ArrayList<Object> list = new ArrayList<Object>();
    	
    	while (currentSymbol.type != Type.BLOCK_END) {
    		switch (currentSymbol.type) {
    		case STRING:
    		case INTEGER:
    		case DOUBLE:
    			list.add(currentSymbol.object);
    			break;
    		case BLOCK_START:
    			nextSymbol();
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
    		nextSymbol();
    	}
    	
    	return null;
    }

	private Object parseBlock() throws Error {
      	switch (currentSymbol.type) {
    	case IDENTIFIER:
    		/* Its a HashMap */
    		return parseHashMap();
    	case STRING:
    	case INTEGER:
    	case DOUBLE:
    		/* Must be an ArrayList */
    		return parseArrayList();
    	case BLOCK_END:
    		/* There is no way to know what it could be, return null. */
    		return null;
    	case BLOCK_START:
    		/* Nested blocks? Ok, we can handle that. */
    		nextSymbol();
    		return parseBlock();
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
	
    
    private Object parseValue() throws Error {
    	switch (currentSymbol.type) {
    	case STRING:
    	case INTEGER:
    	case DOUBLE:
    		return currentSymbol.object;
    	case BLOCK_START:
    		nextSymbol();
    		return parseBlock();
		default:
			throw expectedError(
					String.format(
							"%s, %s, %s or %s",
							Type.STRING.toString(),
							Type.INTEGER.toString(),
							Type.DOUBLE.toString(),
							Type.BLOCK_START.toString()
						)
					);
    	}
    }
}
