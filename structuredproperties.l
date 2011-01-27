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

WS                  = [ \t\v\f]
NL                  = \r|\n|\r\n
EQ                  = "="
ID                  = [a-z][a-zA-Z0-9\-_]*
OB                  = "{"
CB                  = "}"
DQ                  = "\""

%xstates QSTRING PSTRING PSTRING_WS PSTRING_WS_IGNORE VALUE

%%

<YYINITIAL> {
    {WS}                                    { /* YYINITIAL: Eat Whitespace */ }
    {NL}                                    { /* YYINITIAL: Eat Newline */ line++; }
    #.*{NL}                                 { /* YYINITIAL: Eat Comment */ line++; }
    {CB}                                    { /* YYINITIAL: Close Brace */ return new StructuredPropertiesSymbol(Type.BLOCK_END, yytext(), line); }
    {ID}                                    { /* YYINITIAL: ID String */   return new StructuredPropertiesSymbol(Type.STRING, yytext(), line); }
    {DQ}                                    { /* Block: Double quote */ string = new StringBuffer(); yybegin(QSTRING); }
    {EQ}                                    { /* YYINITIAL: Equals sign */ yybegin(VALUE); return new StructuredPropertiesSymbol(Type.EQUALS, yytext(), line); }
    {OB}                                    { /* Block: Open Brace */ return new StructuredPropertiesSymbol(Type.BLOCK_START, yytext(), line); }
}

<VALUE> {
    {WS}                                    { /* Block: Eat Whitespace */ }
    {DQ}                                    { /* Block: Double quote */ string = new StringBuffer(); yybegin(QSTRING); }
    {OB}                                    { /* Block: Open Brace */ yybegin(YYINITIAL); return new StructuredPropertiesSymbol(Type.BLOCK_START, yytext(), line); }
    [^ \t\v\f]                              { /* Block: Non-whitespace */ string = new StringBuffer(); string.append(yytext()); yybegin(PSTRING); }
}

<PSTRING> {
    {CB}                                    { yypushback(1); yybegin(YYINITIAL); 
                                              /* There must be a better way than this. */
                                              return new StructuredPropertiesSymbol(Type.STRING, string.toString().replaceAll("\\s+$", ""), line); }
    #.*{NL}                                 { line++; yybegin(YYINITIAL); 
                                              /* There must be a better way than this. */
                                              return new StructuredPropertiesSymbol(Type.STRING, string.toString().replaceAll("\\s+$", ""), line); }
    {NL}                                    { line++; yybegin(YYINITIAL); 
                                              /* There must be a better way than this. */
                                              return new StructuredPropertiesSymbol(Type.STRING, string.toString().replaceAll("\\s+$", ""), line); }
    \\{WS}*#.*{NL}                          { line++; yybegin(PSTRING_WS_IGNORE); }
    \\{WS}*{NL}                             { line++; yybegin(PSTRING_WS_IGNORE); }
    [^\n\r\\#}]+                            { string.append( yytext() ); }
}


<PSTRING_WS_IGNORE> {
    {WS}                                    { /* Eat */}
    .                                       { string.append(yytext()); yybegin(PSTRING); /* back to parsing the string */ }
}

<QSTRING> {
    {DQ}                                    { yybegin(YYINITIAL); return new StructuredPropertiesSymbol(Type.STRING, string.toString(), line); }
    [^\n\r\"\\]+                            { string.append( yytext() ); }
    \\t                                     { string.append('\t'); }
    \\n                                     { string.append('\n'); }
    \\r                                     { string.append('\r'); }
    \\\"                                    { string.append('\"'); }
    \\                                      { string.append('\\'); }
}

