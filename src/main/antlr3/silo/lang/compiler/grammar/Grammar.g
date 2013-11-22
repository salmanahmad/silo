
grammar Grammar;

options {
  output = AST;
  backtrack=true;
  ASTLabelType=CommonTree;
}

@parser::header { 
package silo.lang.compiler.grammar;

import silo.lang.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

}

@lexer::header {
package silo.lang.compiler.grammar;
import silo.lang.*;
}

@parser::members {
    // TODO - Better error reporting here --- http://www.antlr.org/wiki/display/ANTLR3/Error+reporting+and+recovery
    public void emitErrorMessage(String message) {
        throw new RuntimeException(message);
    }
}


@lexer::members {
    // TODO - Better error reporting here --- http://www.antlr.org/wiki/display/ANTLR3/Error+reporting+and+recovery
    public void emitErrorMessage(String message) {
        throw new RuntimeException(message);
    }
}

program
  : terminator? expressions? EOF!
  ;

expressions
  : head=expression 
    (
      terminator 
      tail=expression
    )*
    terminator
  ;

expression
  : assignmentExpression
  ;

assignmentExpression
  : n1=orExpression
    ( ASSIGN
      n2=assignmentExpression
    )?
  ;
  
orExpression
  : n1=andExpression
    ( OR
      n2=orExpression
    )?
  ;

andExpression
  : n1=relationalExpression
    ( AND
      n2=andExpression
    )?
  ;

relationalExpression
  : n1=additiveExpression
    ( relationalOperator
      n2=relationalExpression
    )?
  ;

additiveExpression
  : n1=multiplicativeExpression
    ( additiveOperator
      n2=additiveExpression
    )?
  ;

multiplicativeExpression
  : n1=unaryExpresion 
    ( multiplicativeOperator
      n2=multiplicativeExpression
    )?
  ;

unaryExpresion
  : NOT node1=unaryExpresion
  | primaryExpression
  ;

primaryExpression
  : nodeExpression
  | literalExpression 
  ;

nodeExpression
  : ( literalExpression
    )?
    nodeTailExpression
  ;

nodeTailExpression
  : OPEN_PAREN
    ( head=expression      
      ( (COMMA | NEWLINE) (COMMA | NEWLINE)*
        tail=expression
      )*
    )?
    CLOSE_PAREN

    ( nodeTailExpression
    )?
  ;

literalExpression
  : TRUE
  | FALSE
  | NULL
  | SYMBOL
  | STRING
  | NUMBER
  ;

relationalOperator 
  : EQUAL
  | NOT_EQUAL
  | LESS_THAN_EQUAL
  | GREATER_THAN_EQUAL
  | LESS_THAN
  | GREATER_THAN
  ;

additiveOperator
  : PLUS
  | MINUS
  ;

multiplicativeOperator
  : MULTIPLY
  | DIVIDE
  | MODULO
  ;

terminator
  : (NEWLINE | SEMICOLON)+
  | EOF
  ;

STRING
@init{ StringBuilder buf = new StringBuilder(); }
  : '"' 
    ( escape=ESC                       {buf.append(getText());} 
    | normal=~('"'|'\\'|'\n'|'\r')     {buf.appendCodePoint($normal);} 
    )*
    '"'                                {setText(buf.toString());}
  | '\''
    ( normal=~('\'')                   {buf.appendCodePoint($normal);} 
    )*
    '\''                               {setText(buf.toString());}
  ;

INTEGER:            '-'? DIGIT+;
DOUBLE:             '-'? DIGIT+ ('.' DIGIT+);
FLOAT:              '-'? DIGIT+ ('.' DIGIT+) 'f';

TRUE:               'true';
FALSE:              'false';

NULL:               'null';

SYMBOL:             LETTER SYMBOL_CHAR+;

SEMICOLON:          ';';
DOT:                '.';
COMMA:              ',';

OPEN_BRACKET:       '[';
CLOSE_BRACKET:      ']';
OPEN_PAREN:         '(';
CLOSE_PAREN:        ')';
OPEN_BRACE:         '{';
CLOSE_BRACE:        '}';

ASSIGN:             '=';

EQUAL:              '==';
NOT_EQUAL:          '!=';
LESS_THAN_EQUAL:    '<=';
GREATER_THAN_EQUAL: '>=';
LESS_THAN:          '<';
GREATER_THAN:       '>';

ARROW:              '=>';

PLUS:               '+';
MINUS:              '-';

MULTIPLY:           '*';
DIVIDE:             '/';
MODULO:             '%';

AND:                '&&' | 'and';
OR:                 '||' | 'or';
NOT:                '!' | 'not';

COMMENT:            '//' ~('\r' | '\n')* (NEWLINE | EOF) { $type = NEWLINE; };

NEWLINE:            '\r'? '\n';
WHITESPACE:         SPACE+ { $channel = HIDDEN; };

fragment ESC
  : '\\'
    ( 'n'    {setText("\n");}
    | 'r'    {setText("\r");}
    | 't'    {setText("\t");}
    | 'b'    {setText("\b");}
    | 'f'    {setText("\f");}
    | '"'    {setText("\"");}
    | '\''   {setText("\'");}
    | '/'    {setText("/");}
    | '\\'   {setText("\\");}
    | ('u')+ i=HEX_DIGIT j=HEX_DIGIT k=HEX_DIGIT l=HEX_DIGIT { setText(Character.toString((char)Integer.parseInt("" + $i.getText() + $j.getText() + $k.getText() + $l.getText(), 16))); }
    )
  ;

fragment LETTER:      LOWER | UPPER;
fragment NUMBER:      INTEGER | FLOAT | DOUBLE;
fragment SYMBOL_CHAR: LETTER | DIGIT | UNDERSCORE;
fragment COLON:       ':';
fragment UNDERSCORE:  '_';
fragment LOWER:       'a'..'z';
fragment UPPER:       'A'..'Z';
fragment DIGIT:       '0'..'9';
fragment HEX_DIGIT:   ('0'..'9'|'a'..'f'|'A'..'F');
fragment SPACE:       ' ' | '\t';

