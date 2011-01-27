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
 * StructuredProperties is a Domain Specific Language for structured
 * configuration properties files.
 * 
 * <p>
 * A Structured Properties .conf file looks something like this:
 * <pre>
 *   identifier = value
 *   
 *   identifier {              # HashMap 
 *     identifier = value
 *     identifier = value
 *   }
 *   
 *   identifier {              # ArrayList
 *     value
 *     value
 *   }
 * </pre>
 * <p>
 * Where values are either quoted strings, numbers or a block. The only
 * restriction with blocks is that you need to omit the = after an
 * identifier in a HashMap.
 * <p>
 * Comments are simply # until the end of line.
 * <p>
 * The Lexer was built using JFlex, so should be fairly robust. I had a
 * stab at using Cup but it seemed beyond me to grok how to make it fly at
 * the time so I hand wrote the Parser. It seems to work well, though I
 * need more test cases.
 * <p>
 * <b>Why do we need another configuration file format?</b>
 * <p>
 * Back in the dawn of history, Java was invented and along with it XML
 * came into fashion for storing structured configuration information.
 * <p>
 * Property lists were never in fashion, as while you could structure data
 * with it by separating identifiers with a period, it was ugly and nobody
 * liked it. They preferred XML.
 * <p>
 * But the winds of change blew and people came to realise that using XML
 * is a really fucking stupid idea, because it quickly becomes too hard to
 * read or edit. Especially edit. Half the time, people that were trying to
 * edit it just wanted a simple webserver to work, or wanted to store some
 * simple data, yet XML demanded that they use attributes and keep tags
 * balanced, etc.
 * <p>
 * At some point in recent history, people that were realising that making
 * humans edit XML files, sought out a better file format they could abuse
 * into being a config file format. So, many turned to JSON, or worse, YAML.
 * <p>
 * "But YAML is so easy to edit! Look at JSON! It's a subset, and its even
 * easier!"
 * <p>
 * Easier yes, but ideal for humans? No. Look at a JSON string that encodes a Map:
 * <p>
 * <pre>
 * {
 *   "balance":1000.21,
 *   "num":100,
 *   "nickname":null,
 *   "is_vip":true,
 *   "name":"foo" 
 * }
 * </pre>
 * <p>
 * Now compare this to a StructuredProperties config string:
 * <p>
 * <pre>
 * { 
 *   balance = 1000.21 
 *   num = 100 
 *   nickname = 0 
 *   is_vip = true 
 *   name = foo 
 * }
 * </pre>
 * <p>
 * Note that the keys are not required to be quoted. If you need an exotic key, you
 * just surround it with double quotes. All values are strings; its up to you to
 * convert them when you read them into whatever objects you need.
 * <p>
 * Whitespace separates items, as there is no need for anything to separate
 * them. Likewise I could have chosen not to have an "=" between the key and value
 * but it was decided that it would be nice to have some kind of visual similarity
 * to Java Properties files.
 * <p>
 * If you want a good data exchange format, look at JSON or YAML or XML. Thats
 * what they are designed to do really well. Editing files in these formats by
 * hand is possible, sure, but it's just a byproduct of their goals. It's not goal
 * of any of these languages to be used as configuration files, nor is it their goal
 * to be easily edited by humans.
 * <p>
 * The core goals of StructuredProperties is to be
 * <p>
 *   <li>Easily readable by humans</li>
 *   <li>Easily understandable by humans</li>
 *   <li>Easily editable by humans</li>
 * <p>
 * Note that one of the core goals is not "to be able to represent every possible
 * data type in existence". This is because there are many languages out there
 * already that do a far better job at doing that. My suggestion is that if you
 * have a requirement for a configuration file that stores some exotic data, that
 * you store the exotic data in XML or JSON files with the rest of your configuration
 * in a Structured Properties configuration file.
 * <p>
 * <b>Structured Properties Grammar</b><p>
 * <p>
 * A Structured Properties Configuration file (*.conf) is a simple human
 * readable and human editible configuration file format with a syntax 
 * that can be understood easily by anyone by just looking at the file.
 *<p>
 * Because conf files only support primitive types, the syntax can be kept
 * clean, and syntax errors in the configuration file can be kept to a
 * minimum. If you need more complex types, those objects probably should
 * belong in other serialization formats, and they probably are not required
 * to be human editable.
 *<p>
 * This example file contains a detailed explanation of the syntax of this
 * file, but the structure and usage of a Structured Properties file
 * is intended to be natural and easy to pick up without reading
 * documentation.
 *<p>
 * <b>COMMENT</b>
 *<p>
 *   # This is a comment. The only way to start a comment is with #, C-style 
 *   comments are not supported.
 *<p>
 *   Comments may be anywhere you like and are ignored until end of line.
 *<p>
 * <b>WHITESPACE</b>
 *<p>
 *   Whitespace is unimportant to the lexer and parser.
 *<p>
 *   There is no particular level of indentation required nor does indentation
 *   affect the parser in any way.
 *<p>
 *   The only explicit exception to this is when you use unquoted strings,
 *   explained below.
 *<p>
 *   Whitespace is defined as being a space ' ', a tab '\t' or a line ending
 *   character '\r' & '\n'. Vertical tab '\v' and line feed '\f' are also
 *   treated as whitespace, but you should avoid using them anyway.
 *<p>
 * <b>CHARACTER SET</b>
 *<p>
 *   Structured Property Configuration files are UTF-8. UTF-8 is the only
 *   supported character set.
 *<p>
 * <b>KEY STRINGS</b>
 *<p>
 *   Key strings are used as unquoted string keys in HASHMAPS.
 *<p>
 *   Key strings may be a bare word alphanumeric string that must begin with 
 *   a letter, and can contain '-' and '_'.
 *<p>
 *   Syntax examples:
 *<pre>
 *     something-name some5thing6 something_else
 *</pre>
 *   You can also use a QUOTED STRING as a key, in which case all characters
 *   are valid. Be aware that using a QUOTED STRING with a '.' will cause the
 *   convenience method getProperty(String) to fail; you will need to use
 *   getProperty(...) and specify each component explicitly.
 *<p>
 * <b>UNQUOTED STRING</b>
 *<p>
 *   Unquoted strings start from the first valid non whitespace character and
 *   end at the last. Unquoted strings may be continued onto the next line
 *   using a backslash (\) character, in the same way as Property files.
 *   Whitespace before the backslash is preserved, so that you can concatenate
 *   multiple strings easily.
 *<p>
 *   When an unquoted string is continued, the whitespace leading up to the
 *   first non whitespace character on the new line is ignored, at which pint
 *   the string is then continued.
 *<p>
 *   Tab (\t) and newline (\r and \n) escape characters are supported.
 *   The closed brace '}' is unsupported as a part of an unquoted string. If 
 *   you need a closed brace, use a quoted string.
 * <p>
 *   EXAMPLE
 *<pre>
 *      some_key = This is an unquoted string. \
 *                 This part of the string continues on.
 *</pre>
 *<p>   
 * <b>QUOTED STRINGS</b>
 *<p>
 *   Quoted strings are started with a doublequote (") and ended with a
 *   doublequote. There is no way to continue a quoted string; either use an
 *   unquoted string, or let the line run on.
 *<p>
 *   Tab (\t) and newline (\r and \n) escape characters are supported.
 *<p>
 *   Quoted strings may be used in HASHMAPs and ARRAYLISTs as both keys and
 *   values.
 *<p>
 *   EXAMPLE
 *<pre> 
 *      "This is a quoted string"
 *</pre>
 *<p>
 * <b>BLOCKS</b>
 *<p>
 *   A block is designated with a '{' and ended with a '}'. If a block is used
 *   as the value in a HASHMAP, you may omit the '='.
 *<p>
 *   A block can designate the beginning of either a HASHMAP or an ARRAYLIST. 
 *   The type it is depends on the symbols after the open brace '{'. If the
 *   two symbols are "STRING =", then it's a HASHMAP.
 *<p>
 *   A "STRING STRING ..." would designate an ARRAYLIST, where as a single
 *   STRING would also designate an ARRAYLIST.
 *<p>
 *   It goes without saying that all items in a HASHMAP must be STRING = 
 *   VALUE items, while all items in an ARRAYLIST must just be STRINGs.
 *<p>
 * <b>HASHMAP</b>
 *<p>
 *   HashMaps are used to provide a simple way to define key/value information.
 *   HashMaps are converted to java.util.HashMap objects by the parser.
 *<p>
 *   Syntax:
 *<pre>
 *     { KEY = VALUE
 *       KEY = VALUE 
 *       KEY = VALUE 
 *       ... }
 *</pre>
 *   Whitespace between the '=' and the key and value is not required, but 
 *   should be included for readability. If you use quoted strings for the
 *   values, then you only need whitespace between each entry.
 *<p>
 *   If you want to use unquoted strings, as they go until the end of line, you
 *   will need each key/value pair on a new line.
 *<p>
 *   The entire configuration file has an implied { } around it, and is forced
 *   to being a HashMap. Therefore, all entries in the root of the file is
 *   required to be a KEY = VALUE entry.
 *<p>
 * <b>ARRAYLIST</b>
 *<p>
 *   ArrayLists are used to provide a way to define a list of values. ArrayLists
 *   are converted to java.util.ArrayList objects by the parser.
 *<p>
 *   Syntax:
 *<pre> 
 *     { VALUE 
 *       VALUE 
 *       VALUE 
 *       ... }
 *</pre> 
 *   As with HashMaps, using unquoted strings will require newlines between
 *   entries, while using quoted strings do not.
 *   <p>
 *   For complete examples of the syntax, examine the example.conf file on github.
 *<p> 
 *
 * 
 * @author Nathan Ollerenshaw
 *
 */
public class StructuredProperties {
    /* Class Variables and Methods */
    
    private static boolean debugging                        = false;

    /**
     * Determine whether or not the StructuredProperties parser will emit
     * debugging information to stderr.
     * 
     * @return boolean
     */
    
    public static boolean isDebugging() {
        return debugging;
    }

    /**
     * Enables or disables the debugging mode of the StructuredProperties
     * class. This is a static variable, so it will affect all instances
     * of the class.
     * 
     * Typically you wil do this if you want to verify the parser is parsing
     * files correctly. It has little use other than to someone working with
     * the internals of the parser.
     * 
     * @param debuging	
     */
    
    public static void setDebugging(boolean debuging) {
        StructuredProperties.debugging = debuging;
    }

    /* Instance Variables and Methods */
    
    private StructuredPropertiesLexer lexer                 = null;
    private ArrayList<StructuredPropertiesSymbol> symbols   = new ArrayList<StructuredPropertiesSymbol>();
    private HashMap<String, Object> root                    = null;
    private int currentSymbolIndex                          = 0;
    private StructuredPropertiesSymbol currentSymbol        = null;

    /**
     * Parses the File as a Structured Properties configuration file.
     * 
     * @param configFile
     * @throws Error
     */
    
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

    /**
     * Parses the Reader as a Structured Properties configuration file.
     * 
     * @param in
     * @throws Error
     */
    
    public StructuredProperties(java.io.Reader in) throws Error {
        this.lexer = new StructuredPropertiesLexer(in);
        parse();
    }

    /**
     * Parses the InputStream as a Structured Properties configuration file.
     * 
     * @param in
     * @throws Error
     */
    
    public StructuredProperties(java.io.InputStream in) throws Error {
        this.lexer = new StructuredPropertiesLexer(in);
        parse();
    }
    
    private static void usage(String [ ] args) {
    	String usageString = 
    		"\nUsage: structuredproperties <fiename> [<key>]\n" +
    		"\n" +
    		"This program will parse a Structured Properties Configuration file\n" +
    		"and then perform a toString() on the root HashMap, outputting the\n" +
    		"parsed configuration to stdout.\n" +
    		"\n" +
    		"Optionally if you provide a key path, it will instead attempt to\n" +
    		"dump the associated configuration item.";
    	
    	System.out.println(usageString);
    }
    
    /**
     * A simple test harness; provide the name of a config file as
     * an argument, and it will parse the file and output the root object.
     * <p>
     * Java will handily iterate through ArrayLists and HashMaps if it finds
     * them, as it calls toString() on everything it's listing, so it
     * prints a (badly formatted) representation of whats in your configuration.
     * <p>
     * java -jar StructuredProperties.jar <filename>
     * 
     * @param args
     */
    
    public static void main(String [ ] args) {
        if (args.length == 0) {
        	usage(args);        	
            System.exit(1);
        }
        
        File f = new File(args[0]);
       
        StructuredProperties.setDebugging(false);
        StructuredProperties c = new StructuredProperties(f);

        switch (args.length) {
        case 1:
            for (String key : c.getRoot().keySet()) {
            	System.out.println(key + " = " + c.getRoot().get(key));
            }
            break;
        case 2:
        	System.out.printf("%s = %s\n", args[1], c.getProperty("NOT FOUND", args[1]));
        }
        
        System.exit(0);
    }

    /**
     * Returns a given property at the path indicated by the string key.
     * <p>
     * Key is a path to a hashMap entry. If you want to get a specific
     * entry in an a array, get the ArrayList itself first.
     * <p>
     * example:
     * <p>
     * <pre>
     * c.getProperty("default.options.server.ip-address", "12.0.0.1");
     * </pre>
     * <p>
     * @param key
     * @param defaultValue
     * @return HashMap, ArrayList, String
     */
    
    public Object getProperty(String defaultValue, String key) {
    	String a[] = key.split("\\.");
   	
    	return getProperty(a);
    }
    
    /**
     * Extracts a given object/string at a path that is provided as separate
     * arguments to the method. This is the only way to get at a configuration
     * object if you've decided that you want to use "." in a quoted string
     * key.
     * <p>
     * example:
     * <p>
     * <pre>
     * c.getProperty("default", "options", "server", "ip-address");
     * </pre>
     * 
     * @param defaultValue
     * @param keyparts
     * @return HashMap, ArrayList, String
     */
    
    public Object getProperty(String... keyparts) {
    	
    	int i = 0;
    	boolean found = true;
    	Object def = null;
    	
    	if (keyparts.length == 1)
    		return root.get(keyparts[0]);
    	
    	if (keyparts.length == 0)
    		return null;

    	HashMap<?, ?> hmap = root;
 	
    	for (i = 0; i < keyparts.length - 1; i++) {
    		if (hmap.get(keyparts[i]) instanceof HashMap<?, ?>) {
    			hmap = (HashMap<?, ?>) hmap.get(keyparts[i]);
    		}
    		else
    			found = false;
    	}
    	
    	if (found)
    		def = hmap.get(keyparts[keyparts.length - 1]);
    	
    	return def;
    }
   
    /**
     * This method (as well as the constructors and the other load methods) loads
     * and parses a Structured Properties Configuration file.
 	 *
     * @param in
     * @throws Error
     */

    public void load(java.io.Reader reader) {
        this.lexer = new StructuredPropertiesLexer(reader);
        parse();

    }
    
    /**
     * Please see load(java.io.Reader reader);
     * 
     * @param in
     * @throws Error
     */

    public void load(java.io.InputStream in) throws Error {
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
    	if (currentSymbolIndex + 1 == symbols.size())
    		throw new Error("Unexpected EOF in configuration. Is the configuration file complete?");
    	
        currentSymbolIndex++;
        currentSymbol = symbols.get(currentSymbolIndex);
        
        if (debugging)
        	System.out.printf("%s :: %d : advanced Symbol to: %s\n",
        			Thread.currentThread().getStackTrace()[2].getMethodName(),
        			Thread.currentThread().getStackTrace()[2].getLineNumber(),
        			currentSymbol.toString());
    }

    private void prevSymbol() throws Error {
    	assert(currentSymbolIndex > 0);
    	
        currentSymbolIndex--;
        currentSymbol = symbols.get(currentSymbolIndex);
        
        if (debugging)
        	System.out.printf("%s :: %d : rewound Symbol to: %s\n",
        			Thread.currentThread().getStackTrace()[2].getMethodName(),
        			Thread.currentThread().getStackTrace()[2].getLineNumber(),
        			currentSymbol.toString());
    }

    private HashMap<String, Object> parseHashMap() throws Error {
        HashMap<String, Object> map = new HashMap<String, Object>(); 
        boolean isRoot = false;
        
        if (currentSymbolIndex == 0)
            isRoot = true;

        /* Iterate until we get to the end of the defined block */
        while (currentSymbol.type == Type.STRING) {
            SimpleEntry<String, Object> entry = parseKeyValue();
            map.put(entry.getKey(), entry.getValue());
        }
        
        switch (currentSymbol.type) {
        case EOF:
        	if (isRoot)
        		return map;
        	
        	throw expectedError(String.format("%s or %s", Type.STRING, Type.BLOCK_END));
        case BLOCK_END:
        	return map;
        default:
        	throw expectedError(Type.BLOCK_END.toString());
        }
    }

    private SimpleEntry<String, Object> parseKeyValue() throws Error {
        SimpleEntry<String, Object> entry;

        if (currentSymbol.type != Type.STRING)
            expectedError(Type.STRING.toString());
        
        /* Nasty little cast here, but we're not using that object at all. */
        entry = new SimpleEntry<String, Object>(currentSymbol.object, (Object) null);
        
        nextSymbol();
        
        switch (currentSymbol.type) {
        case BLOCK_START:
            entry.setValue(parseBlock());
            return entry;
        case EQUALS:
            nextSymbol();
            
            switch (currentSymbol.type) {
            case STRING:
                entry.setValue(currentSymbol.object);
                nextSymbol();
                return entry;
            case BLOCK_START:
                entry.setValue(parseBlock());
                return entry;
            default:
                throw expectedError(String.format(Type.STRING.toString()));
            }
        	
       	default:
       		throw  expectedError(String.format("%s [{] or %s [=]", Type.BLOCK_START.toString(), Type.EQUALS.toString()));
        }
    }

    private ArrayList<Object> parseArrayList() throws Error {
        ArrayList<Object> list = new ArrayList<Object>();

        while (currentSymbol.type != Type.BLOCK_END) {
            switch (currentSymbol.type) {
            case STRING:
                list.add(currentSymbol.object);
                nextSymbol();
                break;
            case BLOCK_START:
                list.add(parseBlock());
                break;
            default:
                throw expectedError(
                        String.format(
                                "%s, or %s",
                                Type.STRING.toString(),
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
    	case STRING:
    		/* Its either a hashmap or an arraylist. The only way to know for
    		 * sure is to advance the token an extra step then rewind when we're
    		 * done.
    		 */
    		nextSymbol();
    		switch(currentSymbol.type) {
    		case STRING:
                /* Must be an ArrayList */
    			prevSymbol();
            	ArrayList<Object> l1 = parseArrayList();
            	assert (currentSymbol.type == Type.BLOCK_END) : Type.BLOCK_END;
            	nextSymbol();
            	return l1;
    		case BLOCK_START:
    		case EQUALS:
                /* Its a HashMap */
    			prevSymbol();
            	HashMap<String, Object> map = parseHashMap();
            	assert (currentSymbol.type == Type.BLOCK_END) : Type.BLOCK_END;
            	nextSymbol();
                return map;
            default:
            	throw expectedError(String.format(
                            "%s, %s or %s",
                            Type.STRING.toString(),
                            Type.BLOCK_START.toString(),
                            Type.EQUALS.toString()
                        ));
    		}
        case BLOCK_END:
            /* There is no way to know what it could be, return null. */
        	nextSymbol();
            return null;
        case BLOCK_START:
            /* A block instead of a string means this is an array. */
        	ArrayList<Object> l2 = parseArrayList();
        	assert (currentSymbol.type == Type.BLOCK_END) : Type.BLOCK_END;
        	nextSymbol();
        	return l2;
        default:
            throw expectedError(
                    String.format(
                            "%s, %s or %s",
                            Type.STRING.toString(),
                            Type.BLOCK_START.toString(),
                            Type.BLOCK_END.toString()
                        )
                    );
        }
    }
}
