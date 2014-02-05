
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

    String fileName;

    public GrammarParser(String fileName, CommonTokenStream tokens) {
        super(tokens);
        this.fileName = fileName;
    }

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

program returns [Node value]
  : terminator?                        { $value = new Node(null); }
    ( expressions                      { $value = $expressions.value; }
      terminator
    )?
    EOF!
  ;

expressions returns [Node value]
  :                                    { $value = new Node(null); }
    head=expression                    { $value.addChild($head.value); }
    (
      terminator?
      tail=expression                  { $value.addChild($tail.value); }
    )*
  ;

// TODO: Right now, all expressions return an Object because it must be cascaded up from
// literalExpression. The idea of using Objects seems kind of scary. When I get around to
// adding a Value class to the code base, consider using Value instead of Object.

expression returns [Object value]
  : assignmentExpression               { $value = $assignmentExpression.value; }
  ;

assignmentExpression returns [Object value]
  : n1=orExpression                    { $value = $n1.value; }
    ( op=ASSIGN
      n2=assignmentExpression          { $value = new Node(new Symbol($op.text), $n1.value, $n2.value); }
    )?
  ;
  
orExpression returns [Object value]
  : n1=andExpression                   { $value = $n1.value; }
    ( op=OR
      n2=orExpression                  { $value = new Node(new Symbol($op.text), $n1.value, $n2.value); }
    )?
  ;

andExpression returns [Object value]
  : n1=relationalExpression            { $value = $n1.value; }
    ( op=AND
      n2=andExpression                 { $value = new Node(new Symbol($op.text), $n1.value, $n2.value); }
    )?
  ;

relationalExpression returns [Object value]
  : n1=additiveExpression              { $value = $n1.value; }
    ( op=relationalOperator
      n2=relationalExpression          { $value = new Node(new Symbol($op.text), $n1.value, $n2.value); }
    )?
  ;

additiveExpression returns [Object value]
  : n1=multiplicativeExpression        { $value = $n1.value; }
    ( op=additiveOperator
      n2=additiveExpression            { $value = new Node(new Symbol($op.text), $n1.value, $n2.value); }
    )?
  ;

multiplicativeExpression returns [Object value]
  : n1=binaryExpression                { $value = $n1.value; }
    ( op=multiplicativeOperator
      n2=multiplicativeExpression      { $value = new Node(new Symbol($op.text), $n1.value, $n2.value); }
    )?
  ;

binaryExpression returns [Object value]
  : n1=consExpression                  { $value = $n1.value; }
    ( op=binaryOperator
      n2=binaryExpression              { $value = new Node(new Symbol($op.text), $n1.value, $n2.value); }
    )?
  ;

consExpression returns [Object value]
  : n1=unaryExpresion                    { $value = $n1.value; }
    ( op=consOperator
      n2=consExpression                  { $value = new Node(new Symbol($op.text), $n1.value, $n2.value); }
    )?
  ;

unaryExpresion returns [Object value]
  : op=unaryOperator n1=unaryExpresion { $value = new Node(new Symbol($op.text), $n1.value); }
  | chainExpression                    { $value = $chainExpression.value; }
  ;

chainExpression returns [Object value]
  : n1=primaryExpression               { $value = $n1.value; }
    ( op=chainOperator
      n2=chainExpression               { $value = new Node(new Symbol($op.text), $n1.value, $n2.value); }
    )?
  ;

// These rules are left associative, unlike the rest of the grammar
primaryExpression returns [Object value]
  : nodeExpression                     { $value = $nodeExpression.value; }
  | accessExpression                   { $value = $accessExpression.value; }
  ;

// - I allow parenExpression between accessOperators to make the grammar liberal. The default implementation of
//   the "dot" operator will reject this but I do not reject it here because it may be useful for macro-developers
nodeExpression returns [Node value]
  :                                    { $value = new Node(null); }
    ( access=accessExpression          { $value = new Node($access.value); }
    )?

    paren=parenExpression              { $value.addChildren($paren.value); }

    ( ( paren=parenExpression          { $value = new Node($value); $value.addChildren($paren.value); }
      | op=accessOperator
        ( paren=parenExpression        { $value = new Node(new Symbol($op.text), $value, $paren.value); }
        | literal=literalExpression    { $value = new Node(new Symbol($op.text), $value, $literal.value); }
        )
      )
    )*
  ;

accessExpression returns [Object value]
  : n1=literalExpression                 { $value = $n1.value; }
    ( op=accessOperator
      ( paren=parenExpression            { $value = new Node(new Symbol($op.text), $value, $paren.value); }
      | literal=literalExpression        { $value = new Node(new Symbol($op.text), $value, $literal.value); }
      )

      //n2=accessExpression              { $value = new Node(new Symbol($op.text), $n1.value, $n2.value); }
      //n2=literalExpression             { $value = new Node(new Symbol($op.text), $value, $n2.value); }
    )*
  ;

// TODO: Consider making brakets "[]" "<>" literals as well. Follow groovy and make a empty map "[:]"

literalExpression returns [Object value]
  : NULL                               { $value = null; }
  | TRUE                               { $value = Boolean.TRUE; }
  | FALSE                              { $value = Boolean.FALSE; }
  | STRING                             { $value = $text; }
  | INTEGER                            { $value = Integer.parseInt($text); }
  | FLOAT                              { $value = Float.parseFloat($text); }
  | DOUBLE                             { $value = Double.parseDouble($text); }
  | symbol                             { $value = new Symbol($symbol.text); }
  | blockExpression                    { $value = $blockExpression.value; }
  ;

parenExpression returns [Node value]
  : OPEN_PAREN                    { $value = new Node(null); }
    terminator?
    ( expressions                 { $value = $expressions.value; }
    )?
    terminator?
    CLOSE_PAREN
  ;

symbol returns [Symbol value]
  : IDENTIFIER
  | ASSIGN
  | AND
  | OR
  | NOT
  | EQUAL
  | NOT_EQUAL
  | LESS_THAN_EQUAL
  | GREATER_THAN_EQUAL
  | LESS_THAN
  | GREATER_THAN
  | PLUS
  | MINUS
  | MULTIPLY
  | DIVIDE
  | MODULO
  | ARROW
  | SCOPE
  | COLON
  | DOT
  | PIPE
  | HASH
  | OPERATOR
  ;

blockExpression returns [Node value]
// TODO: Should this return a node with a null label instead of do?
  : OPEN_BRACE                         { $value = new Node(new Symbol("do")); }
    terminator?
    ( expressions                      { $value = new Node(new Symbol("do"), $expressions.value.getChildren()); }
    )?
    terminator?
    CLOSE_BRACE
  ;

// TODO: Once I decide on the standard library names for operators as well as if
// operators should be functions or traits, I should update these rules to return
// the proper symbol representation...

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

unaryOperator returns [Symbol symbol]
  : NOT
  ;

binaryOperator returns [Symbol symbol]
  : ARROW
  ;

consOperator returns [Symbol symbol]
  : COLON
  ;

chainOperator returns [Symbol symbol]
  : PIPE
  | HASH
  ;

accessOperator returns [Symbol symbol]
  : SCOPE
  | DOT
  ;

terminator
  : (NEWLINE | COMMA | SEMICOLON)+
  | EOF
  ;

STRING
@init{ StringBuilder buf = new StringBuilder(); }
  : '"'
    ( escape=ESC                       {buf.append(getText());}
    | normal=~('"'|'\\')               {buf.appendCodePoint($normal);}
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

OPEN_BRACKET:       '[';
CLOSE_BRACKET:      ']';
OPEN_PAREN:         '(';
CLOSE_PAREN:        ')';
OPEN_BRACE:         '{';
CLOSE_BRACE:        '}';

ASSIGN: FRAGMENT_ASSIGN;
AND: FRAGMENT_AMP FRAGMENT_AMP;
OR: FRAGMENT_PIPE FRAGMENT_PIPE;
NOT: FRAGMENT_NOT;
EQUAL: FRAGMENT_ASSIGN FRAGMENT_ASSIGN;
NOT_EQUAL: FRAGMENT_NOT FRAGMENT_ASSIGN;
LESS_THAN_EQUAL: FRAGMENT_LESS_THAN FRAGMENT_ASSIGN;
GREATER_THAN_EQUAL: FRAGMENT_GREATER_THAN FRAGMENT_ASSIGN;
LESS_THAN: FRAGMENT_LESS_THAN;
GREATER_THAN: FRAGMENT_GREATER_THAN;
PLUS: FRAGMENT_PLUS;
MINUS: FRAGMENT_MINUS;
MULTIPLY: FRAGMENT_MULTIPLY;
DIVIDE: FRAGMENT_DIVIDE;
MODULO: FRAGMENT_MODULO;
ARROW: FRAGMENT_ASSIGN FRAGMENT_GREATER_THAN;
SCOPE: FRAGMENT_COLON FRAGMENT_COLON;
COLON: FRAGMENT_COLON;
DOT: FRAGMENT_DOT;
PIPE: FRAGMENT_PIPE;
HASH: FRAGMENT_HASH;
OPERATOR
  : ( FRAGMENT_ASSIGN
    | FRAGMENT_LESS_THAN
    | FRAGMENT_GREATER_THAN
    | FRAGMENT_PLUS
    | FRAGMENT_MINUS
    | FRAGMENT_MULTIPLY
    | FRAGMENT_DIVIDE
    | FRAGMENT_MODULO
    | FRAGMENT_NOT
    | FRAGMENT_PIPE
    | FRAGMENT_AMP
    | FRAGMENT_DOT
    | FRAGMENT_COLON
    | FRAGMENT_HASH
    )+
  ;

fragment FRAGMENT_ASSIGN:             '=';
fragment FRAGMENT_LESS_THAN:          '<';
fragment FRAGMENT_GREATER_THAN:       '>';
fragment FRAGMENT_PLUS:               '+';
fragment FRAGMENT_MINUS:              '-';
fragment FRAGMENT_MULTIPLY:           '*';
fragment FRAGMENT_DIVIDE:             '/';
fragment FRAGMENT_MODULO:             '%';
fragment FRAGMENT_NOT:                '!';
fragment FRAGMENT_PIPE:               '|';
fragment FRAGMENT_AMP:                '&';
fragment FRAGMENT_DOT:                '.';
fragment FRAGMENT_COLON:              ':';
fragment FRAGMENT_HASH:               '#';

IDENTIFIER:             ((LETTER | UNDERSCORE) SYMBOL_CHAR*);

SEMICOLON:          ';';
COMMA:              ',';
NEWLINE:            '\r'? '\n';
WHITESPACE:         SPACE+ { $channel = HIDDEN; };

COMMENT:            '//' ~('\r' | '\n')* (NEWLINE | EOF) { $type = NEWLINE; };

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
fragment SYMBOL_CHAR: LETTER | UNDERSCORE | DIGIT;
fragment UNDERSCORE:  '_';
fragment LOWER:       'a'..'z';
fragment UPPER:       'A'..'Z';
fragment DIGIT:       '0'..'9';
fragment HEX_DIGIT:   ('0'..'9'|'a'..'f'|'A'..'F');
fragment SPACE:       ' ' | '\t';

