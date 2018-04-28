# About

StructuredProperties Configuration File Format and Parser

StructuredProperties is a parser designed around the same general
style as the java Properties class, intended to provide a simple
configuration file format for Java programs.

For a definition of the syntax and for example usage, please look
at the 'example.conf' file.

To build StructuredProperties, you will need a recent JDK and the
JFlex tool from http://jflex.de/ installed in your system path as
"jflex", along with Apache Ant for building.

JavaDoc pages have been generated along with a complete explanation
of the syntax (in case it isn't obvious) at http://matjam.github.com/StructuredProperties/

The basic usage is simple however, just create yourself a new File 
object to represent your configuration file, and pass it to the 
StructuredProperties constructor. use StructuredProperties.getRoot()
to get the root hashmap, at which point you can read entries as you 
normally would.

Example:

```java
File f = new File(args[0]);
StructuredProperties c = new StructuredProperties(f);
System.out.println(c.getRoot().toString());
```
Alternatively, you can use the getProperty(String default, String path)
method, which will get a property at a given path, separated by '.'.

```java
String v = getProperty("UNSET", "options.server-ip");
```

This of course relies on you not using period in the key of any of
your HashMaps, if that is the case, you can use the other form of
getProperty():

```java
String v = getProperty("UNSET", "options", "server.ip");
```

Please note that I'm not a Java programmer by trade; I've spent more time
with C by now than anything else, so if any part of the implementation
is not correct or needs work, I'd be happy to take any pull requests
that fix them.

# Why?

Back in the dawn of history, Java was invented and along with it XML came into fashion for storing structured configuration information.

Property lists were never in fashion, as while you could structure data with it by separating identifiers with a period, it was ugly and nobody liked it. They preferred XML.

But the winds of change blew and people came to realise that using XML is a really fucking stupid idea, because it quickly becomes too hard to read or edit. Especially edit. Half the time, people that were trying to edit it just wanted a simple webserver to work, or wanted to store some simple data, yet XML demanded that they use attributes and keep tags balanced, etc.

At some point in recent history, people that were realising that making humans edit XML files, sought out a better file format they could abuse into being a config file format. So, many turned to JSON, or worse, YAML.

"But YAML is so easy to edit! Look at JSON! It's a subset, and its even easier!"

Easier yes, but ideal for humans? No. Look at a JSON string that encodes a Map:

```json
{
   "balance":1000.21,
   "num":100,
   "nickname":null,
   "is_vip":true,
   "name":"foo" 
}
```
Now compare this to a StructuredProperties config string:

```ini
{ 
   balance = 1000.21 
   num = 100 
   nickname = 0 
   is_vip = true 
   name = foo 
}
```

Note that the keys are not required to be quoted. If you need an exotic key, you just surround it with double quotes. All values are strings; its up to you to convert them when you read them into whatever objects you need.

Whitespace separates items, as there is no need for anything to separate them. Likewise I could have chosen not to have an "=" between the key and value but it was decided that it would be nice to have some kind of visual similarity to Java Properties files.

If you want a good data exchange format, look at JSON or YAML or XML. Thats what they are designed to do really well. Editing files in these formats by hand is possible, sure, but it's just a byproduct of their goals. It's not goal of any of these languages to be used as configuration files, nor is it their goal to be easily edited by humans.

The core goals of StructuredProperties is to be

* Easily readable by humans
* Easily understandable by humans
* Easily editable by humans

Note that one of the core goals is not "to be able to represent every possible data type in existence". This is because there are many languages out there already that do a far better job at doing that. My suggestion is that if you have a requirement for a configuration file that stores some exotic data, that you store the exotic data in XML or JSON files with the rest of your configuration in a Structured Properties configuration file.

# License

Copyright 2018 Nathan Ollerenshaw <chrome@stupendous.net>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
