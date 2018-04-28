/* File: StructuredPropertiesSymbol.java
 * 
 *    Copyright 2018 Nathan Ollerenshaw <chrome@stupendous.net>
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
