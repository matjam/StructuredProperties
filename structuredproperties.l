/*  
    SimpleConf

    Simple human readable and editable configuration file format.
*/

package net.stupendous.util;
import net.stupendous.util.StructuredPropertiesSymbol;
import net.stupendous.util.StructuredPropertiesSymbol.Type;

@SuppressWarnings("unused")

%%
%class StructuredPropertiesLexer
%unicode
%function scan
%type StructuredPropertiesSymbol

%{
  StringBuffer string = null;

  int line = 1;
%}

%eofval{
    return new StructuredPropertiesSymbol(Type.EOF, null, line);
%eofval}

Whitespace              = [ \t\v\f]
Decimal                 = [0-9]
Integer                 = ([+|-]{0,1}{Decimal}+)
Double                  = ([+|-]{0,1}{Decimal}+\.{Decimal}+)
Alpha                   = [a-zA-Z]
Comment                 = (#.*\n)
BlockStart              = "{"
BlockEnd                = "}"
Newline                 = [\n\r]
Equals                  = "="
Identifier              = {Alpha}[a-zA-Z0-9\-_]*

%state STRING

%%

<YYINITIAL> {
  \"                                        { string = new StringBuffer(); yybegin(STRING); }

  {Identifier}                              { return new StructuredPropertiesSymbol(Type.IDENTIFIER, yytext(), line); }
  {Integer}                                 { return new StructuredPropertiesSymbol(Type.INTEGER, yytext(), line); }
  {Double}                                  { return new StructuredPropertiesSymbol(Type.DOUBLE, yytext(), line); }
  {BlockStart}                              { return new StructuredPropertiesSymbol(Type.BLOCK_START, yytext(), line); }
  {BlockEnd}                                { return new StructuredPropertiesSymbol(Type.BLOCK_END, yytext(), line); }
  {Equals}                                  { return new StructuredPropertiesSymbol(Type.EQUALS, yytext(), line); }
  {Comment}                                 { line++; /* Eat Comments */ }
  {Whitespace}                              { /* Eat WhiteSpace */ }
  {Newline}                                 { line++; /* Eat but count new lines */ }
}

<STRING> {
  \"{Whitespace}*\\{Whitespace}*{Newline}+{Whitespace}*\"   { line++; /* Continuation onto another line */ }
  [^\n\r\"\\]+                              { string.append( yytext() ); }
  \"                                        { yybegin(YYINITIAL); return new StructuredPropertiesSymbol(Type.STRING, new String(string), line); }
  \\t                                       { string.append('\t'); }
  \\n                                       { string.append('\n'); }
  \\r                                       { string.append('\r'); }
  \\f                                       { string.append('\f'); }
  \\\"                                      { string.append('\"'); }
  \\                                        { string.append('\\'); }
}

/* error fallback */
.|\n                             { throw new Error(String.format("Parse error on line: Illegal character %s", line , yytext())); }

