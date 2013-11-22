
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

program returns [Node code]
  : terminator?                        { $code = new Node(null); }
    ( expressions                      { $code = $expressions.node; }
    )?
    EOF!
  ;

expressions returns [Node node]
  :                                    { $node = new Node(null); }
    head=expression                    { $node.addChild($head.node); }
    (
      terminator
      tail=expression                  { $node.addChild($tail.node); }
    )*
    terminator
  ;

// TODO: Right now, all expressions return an Object because it must be cascaded up from
// literalExpression. The idea of using Objects seems kind of scary. When I get around to
// adding a Value class to the code base, consider using Value instead of Object.

expression returns [Object node]
  : assignmentExpression               { $node = $assignmentExpression.node; }
  ;

assignmentExpression returns [Object node]
  : n1=orExpression                    { $node = $n1.node; }
    ( op=ASSIGN
      n2=assignmentExpression          { $node = new Node(new Symbol($op.text), $n1.node, $n2.node); }
    )?
  ;
  
orExpression returns [Object node]
  : n1=andExpression                   { $node = $n1.node; }
    ( op=OR
      n2=orExpression                  { $node = new Node(new Symbol($op.text), $n1.node, $n2.node); }
    )?
  ;

andExpression returns [Object node]
  : n1=relationalExpression            { $node = $n1.node; }
    ( op=AND
      n2=andExpression                 { $node = new Node(new Symbol($op.text), $n1.node, $n2.node); }
    )?
  ;

relationalExpression returns [Object node]
  : n1=additiveExpression              { $node = $n1.node; }
    ( op=relationalOperator
      n2=relationalExpression          { $node = new Node(new Symbol($op.text), $n1.node, $n2.node); }
    )?
  ;

additiveExpression returns [Object node]
  : n1=multiplicativeExpression        { $node = $n1.node; }
    ( op=additiveOperator
      n2=additiveExpression            { $node = new Node(new Symbol($op.text), $n1.node, $n2.node); }
    )?
  ;

multiplicativeExpression returns [Object node]
  : n1=unaryExpresion                  { $node = $n1.value; }
    ( op=multiplicativeOperator
      n2=multiplicativeExpression      { $node = new Node(new Symbol($op.text), $n1.value, $n2.node); }
    )?
  ;

unaryExpresion returns [Object value]
  : op=NOT n1=unaryExpresion           { $value = new Node(new Symbol($op.text), $n1.value); }
  | primaryExpression                  { $value = $primaryExpression.object; }
  ;

primaryExpression returns [Object object]
  : nodeExpression                     { $object = $nodeExpression.node; }
  | literalExpression                  { $object = $literalExpression.object; }
  ;

nodeExpression returns [Node node]
  : ( literalExpression                { $node = new Node($literalExpression.object); }
    )?

    OPEN_PAREN
    ( head=expressions                 { $node.addChildren($head.node.getChildren()); }
    )?
    CLOSE_PAREN

    ( OPEN_PAREN                       { $node = new Node($node); }
      ( tail=expressions               { $node.addChildren($tail.node.getChildren()); }
      )?
      CLOSE_PAREN
    )*
  ;

literalExpression returns [Object object]
  : NULL                               { $object = null; }
  | TRUE                               { $object = Boolean.TRUE; }
  | FALSE                              { $object = Boolean.FALSE; }
  | SYMBOL                             { $object = new Symbol($SYMBOL.text); }
  | STRING                             { $object = $text; }
  | INTEGER                            { $object = Integer.parseInt($text); }
  | FLOAT                              { $object = Float.parseFloat($text); }
  | DOUBLE                             { $object = Double.parseDouble($text); }
  ;

// TODO: Once I decide on the standard library names for operators as well as if
// operators should be functions or traits, I should update these rules.

relationalOperator returns [Symbol symbol]
  : EQUAL
  | NOT_EQUAL
  | LESS_THAN_EQUAL
  | GREATER_THAN_EQUAL
  | LESS_THAN
  | GREATER_THAN
  ;

additiveOperator returns [Symbol symbol]
  : PLUS
  | MINUS
  ;

multiplicativeOperator returns [Symbol symbol]
  : MULTIPLY
  | DIVIDE
  | MODULO
  ;

terminator
  : (NEWLINE | COMMA | SEMICOLON)+
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

