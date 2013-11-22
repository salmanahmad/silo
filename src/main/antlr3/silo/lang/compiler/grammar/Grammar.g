
grammar Grammar;

options {
  output = AST;
  backtrack=true;
  ASTLabelType=CommonTree;
}

@parser::header { 
package silo.lang.compiler.grammar;

import silo.lang.nodes.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

}

@lexer::header {
package silo.lang.compiler.grammar;
import silo.lang.nodes.*;
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

program returns [Nodes nodes]
  : terminator? expressions? EOF! { $nodes = $expressions.nodes; }
  ;

expressions returns [Nodes nodes]
  :                      { $nodes = new Nodes(); }
    head=expression      { $nodes.add($head.node); }
    (
      terminator         
      tail=expression    { $nodes.add($tail.node); }
    )*
    terminator
  ;

expression returns [Node node]
  : assignmentExpression { $node = $assignmentExpression.node; }
  ;

assignmentExpression returns [Node node]
  : assignment             { $node = $assignment.node; }
  | e=orExpression         { $node = $e.node; }
  ;
  
orExpression returns [Node node]
  : n1=andExpression      { $node = $n1.node; }
    ( OR
      n2=orExpression     { $node = new LogicalOperation($start.getLine(), $n1.node, $n2.node, LogicalOperation.Kind.OR); }
    )?
  ;

andExpression returns [Node node]
  : n1=relationalExpression { $node = $n1.node; }
    ( AND
      n2=andExpression      { $node = new LogicalOperation($start.getLine(), $n1.node, $n2.node, LogicalOperation.Kind.AND); }
    )?
  ;

relationalExpression returns [Node node]
  : n1=additiveExpression     { $node = $n1.node; }
    ( relationalOperator
      n2=relationalExpression { $node = new RelationalOperation($start.getLine(), $n1.node, $n2.node, $relationalOperator.kind); }
    )?
  ;

additiveExpression returns [Node node]
  : n1=multiplicativeExpression   { $node = $n1.node; }
    ( additiveOperator
      n2=additiveExpression       { $node = new ArithmeticOperation($start.getLine(), $n1.node, $n2.node, $additiveOperator.kind); }
    )?
  ;

multiplicativeExpression returns [Node node]
  : n1=unaryExpresion             { $node = $n1.node; }
    ( multiplicativeOperator
      n2=multiplicativeExpression { $node = new ArithmeticOperation($start.getLine(), $n1.node, $n2.node, $multiplicativeOperator.kind); }
    )?
  ;

unaryExpresion returns [Node node]
  : NOT node1=unaryExpresion      { $node = new NotOperation($start.getLine(), $node1.node); }
  | primaryExpression             { $node = $primaryExpression.node; }
  ;

primaryExpression returns [Node node]
  : functionDefinition            { $node = $functionDefinition.node; }
  | typeDefinition                { $node = $typeDefinition.node; }
  | controlStructure              { $node = $controlStructure.node; }
  | packageDeclaration            { $node = $packageDeclaration.node; }
  | includeStatement              { $node = $includeStatement.node; }
  | atom                          { $node = $atom.node; }
  ;

parameter returns [Parameter node]
  : typeIdentifier        { $node = new Parameter($start.getLine()); $node.identifier = $typeIdentifier.node; }
    ( IDENTIFIER          { $node.name = $IDENTIFIER.text; }
    )?
  ;

parameterList returns [ParameterList node]
  : head=parameter      { $node = new ParameterList($start.getLine()); $node.parameters.add($head.node); }
    ( (COMMA | NEWLINE) (COMMA | NEWLINE)*
      tail=parameter    { $node.parameters.add($tail.node); }
    )*
  ;

expressionList returns [Nodes node]
  : head=expression      { $node = new Nodes(); $node.add($head.node); }
    ( (COMMA | NEWLINE) (COMMA | NEWLINE)*
      tail=expression    { $node.add($tail.node); }
    )*
  ;

typeIdentifier returns [TypeIdentifier node]
  : identifierPath       { $node = new TypeReference($start.getLine()); ((TypeReference)$node).identifier = $identifierPath.node; }
  | typeLiteral          { $node = $typeLiteral.node; }
  ;

identifierPath returns [IdentifierPath node]
  : head=IDENTIFIER      { $node = new IdentifierPath($start.getLine()); $node.path.add($head.text); }
    ( DOT
      tail=IDENTIFIER    { $node.path.add($tail.text); }
    )*
  ;

assignment returns [Node node]
  : declaration          { $node = $declaration.node; }
  | reassignment         { $node = $reassignment.node; }
  ;

declaration returns [Node node]
@init{ VariableDeclaration d = null; Nodes nodes = null; Assignment a = null; }
  : typeIdentifier       { d = new VariableDeclaration($start.getLine()); d.typeIdentifier = $typeIdentifier.node; }
                         { a = new Assignment($start.getLine()); }
                         { nodes = new Nodes(); }
    IDENTIFIER           { d.name = $IDENTIFIER.text; }
                         { nodes.add(d); }
                         { $node = d; }
    ( ASSIGN
      expression         { a.path = new ResolveSymbol($start.getLine(), $IDENTIFIER.text); }
                         { a.value = $expression.node; }
                         { nodes.add(a); $node = nodes; }
    )?
  ;

reassignment returns [Assignment node]
  : atom                 { $node = new Assignment($start.getLine()); $node.path = $atom.node; }
    ASSIGN
    expression           { $node.value = $expression.node; }
  ;

atom returns [Node node]
@init { AtomTail a = null; }
  : ( literal                               { $node = $literal.node; }
    | OPEN_PAREN expression CLOSE_PAREN     { $node = $expression.node; }
    | IDENTIFIER                            { $node = new ResolveSymbol($start.getLine(), $IDENTIFIER.text); }
    )
    ( atomTail                              { a = $atomTail.node; a.cascadeSource($node); $node = a; }
    )?
  ;

atomTail returns [AtomTail node]
  : access              { $node = $access.node; }
  | lookup              { $node = $lookup.node; }
  | block               { $node = $block.node; }
  | call                { $node = $call.node; }
  ;

access returns [AtomTail node]
@init { AtomTail a = null; }
  : DOT
    IDENTIFIER        { $node = new Access($start.getLine(), $IDENTIFIER.text); }
    ( atomTail        { a = $atomTail.node; a.cascadeSource($node); $node = a; }
    )?
  ;

lookup returns [AtomTail node]
@init { AtomTail a = null; }
  : OPEN_BRACKET
    expression         { $node = new Lookup($start.getLine(), $expression.node); }
    CLOSE_BRACKET
    ( atomTail         { a = $atomTail.node; a.cascadeSource($node); $node = a; }
    )?
  ;

// TODO - Named parameters?
call returns [AtomTail node]
@init { AtomTail a = null; }
  : OPEN_PAREN         { $node = new Call($start.getLine()); }
    ( expressionList   { ((Call)$node).arguments = $expressionList.node; }
    )?
    CLOSE_PAREN
    ( atomTail         { a = $atomTail.node; a.cascadeSource($node); $node = a; }
    )?
  ;

block returns [AtomTail node]
@init { AtomTail a = null; }
  :                             { $node = new FunctionLiteral($start.getLine()); }
    ( OPEN_PAREN
      ( inputs=parameterList    { ((FunctionLiteral)$node).functionInputs = $inputs.node; }
      )?
      ( ARROW
        outputs=parameterList   { ((FunctionLiteral)$node).functionOutputs = $outputs.node; }
      )?
      CLOSE_PAREN
    )?
    NEWLINE* OPEN_BRACE NEWLINE*
    ( expressions               { ((FunctionLiteral)$node).body = $expressions.nodes; }
    )?
    CLOSE_BRACE
    ( atomTail                  { a = $atomTail.node; a.cascadeSource($node); $node = a; }
    )?
  ;

functionDefinition returns [FunctionDefinition node]
  : DEFINE                   { $node = new FunctionDefinition($start.getLine()); }
    IDENTIFIER               { $node.name = $IDENTIFIER.text; }
    ASSIGN
    functionTypeLiteral      { $node.typeIdentifier = $functionTypeLiteral.node; }
    block                    { $node.functionLiteral = $block.node; }
  ;

typeDefinition returns [TypeDefinition node]
  : DEFINE            { $node = new TypeDefinition($start.getLine()); }
    IDENTIFIER        { $node.name = $IDENTIFIER.text; }
    ASSIGN
    typeIdentifier    { $node.typeIdentifier = $typeIdentifier.node; }
  ;

controlStructure returns [Node node]
  : ifStatement           { $node = $ifStatement.node; }
  | whileLoop             { $node = $whileLoop.node; }
  | foreverLoop           { $node = $foreverLoop.node; }
  | breakStatement        { $node = $breakStatement.node; }
  | returnStatement       { $node = $returnStatement.node; }
  ;

ifStatement returns [Branch node]
@init { Branch branchPointer = null; }
  : IF                               { $node = new Branch($start.getLine()); }
                                     { branchPointer = $node; }
    OPEN_PAREN
    condition=expression             { $node.condition = $condition.node; }
    CLOSE_PAREN
    OPEN_BRACE NEWLINE*
    ( trueBranch=expressions         { $node.trueBranch = $trueBranch.nodes; }
    )?
    CLOSE_BRACE

    ( NEWLINE*
      elseIfStatement                { branchPointer.falseBranch = $elseIfStatement.node; }
                                     { branchPointer = (Branch)branchPointer.falseBranch; }

    )*
    ( NEWLINE*
      elseStatement                  { branchPointer.falseBranch = $elseStatement.node;  }

    )?

  ;

elseIfStatement returns [Branch node]
  : ELSE IF               { $node = new Branch($start.getLine()); }
    OPEN_PAREN
    expression            { $node.condition = $expression.node; }
    CLOSE_PAREN
    OPEN_BRACE NEWLINE*
    ( expressions         { $node.trueBranch = $expressions.nodes; }
    )?
    CLOSE_BRACE
  ;

elseStatement returns [Nodes node]
  : ELSE
    OPEN_BRACE NEWLINE*
    ( expressions         { $node = $expressions.nodes; }
    )?
    CLOSE_BRACE
  ;

whileLoop returns [Loop node]
@init { Nodes body = new Nodes(); Branch branch = null; }

  : WHILE                 { $node = new Loop($start.getLine()); }
    OPEN_PAREN
    expression            { branch = new Branch($start.getLine()); branch.condition = $expression.node;  }
    CLOSE_PAREN
    OPEN_BRACE NEWLINE*
    ( expressions         { body = $expressions.nodes; }
    )?
    CLOSE_BRACE           { branch.trueBranch = body; branch.falseBranch = new Break($start.getLine(), null);}
                          { $node.body = branch; }
  ;

foreverLoop returns [Loop node]
  : LOOP                   { $node = new Loop($start.getLine()); }
    OPEN_BRACE NEWLINE*
    ( expressions          { $node.body = $expressions.nodes; }
    )?
    CLOSE_BRACE
  ;

breakStatement returns [Break node]
  : BREAK expression   { $node = new Break($start.getLine(), $expression.node); }
  | BREAK              { $node = new Break($start.getLine(), null); }
  ;

returnStatement returns [Return node]
  : RETURN expression  { $node = new Return($start.getLine(), $expression.node); }
  | RETURN             { $node = new Return($start.getLine(), null); }
  ;

packageDeclaration returns [Node node]
  : PACKAGE identifierPath
  ;

includeStatement returns [Node node]
  : INCLUDE identifierPath
  ;

literal returns [Node node]
  : INTEGER             { $node = new IntegerLiteral($start.getLine(), Integer.parseInt($text)); }
  | DOUBLE              { $node = new DoubleLiteral($start.getLine(), Double.parseDouble($text)); }
  | FLOAT               { $node = new FloatLiteral($start.getLine(), Float.parseFloat($text)); }
  | TRUE                { $node = new TrueLiteral($start.getLine()); }
  | FALSE               { $node = new FalseLiteral($start.getLine()); }
  | NULL                { $node = new NullLiteral($start.getLine()); }
  | STRING              { $node = new StringLiteral($start.getLine(), $text); }
  | typeLiteral         { $node = $typeLiteral.node; }
  | collectionLiteral   { $node = $collectionLiteral.node; }
  ;

typeLiteral returns [TypeLiteral node]
  : arrayTypeLiteral        { $node = $arrayTypeLiteral.node; }
  | functionTypeLiteral     { $node = $functionTypeLiteral.node; }
  | structTypeLiteral       { $node = $structTypeLiteral.node; }
  ;

arrayTypeLiteral returns [TypeLiteral node]
  : ( identifierPath        { $node = new TypeLiteral($start.getLine()); 
                              $node.arrayType = new TypeReference($start.getLine());
                              $node.arrayType.identifier = $identifierPath.node; }
    | functionTypeLiteral   { $node = $functionTypeLiteral.node; }
    | structTypeLiteral     { $node = $structTypeLiteral.node; }
    )
    ( OPEN_BRACKET
      CLOSE_BRACKET         { $node.arrayDepth++; }
    )+
  ;

functionTypeLiteral returns [TypeLiteral node]
  : FUNCTION             { $node = new TypeLiteral($start.getLine()); }
                         { $node.functionInputs = new ParameterList($start.getLine()); }
                         { $node.functionOutputs = new ParameterList($start.getLine()); }
    ( NEWLINE* OPEN_PAREN NEWLINE*
      ( inputs=parameterList    { $node.functionInputs = inputs.node; }
      )?
      ( ARROW
        outputs=parameterList   { $node.functionOutputs = outputs.node; }
      )?
      NEWLINE* CLOSE_PAREN
    )?
  ;

structTypeLiteral returns [TypeLiteral node]
  : STRUCT                { $node = new TypeLiteral($start.getLine()); }
                          { $node.structureFields = new ParameterList($start.getLine()); }
    ( NEWLINE* OPEN_PAREN NEWLINE*
      ( parameterList     { $node.structureFields = $parameterList.node; }
      )?
      NEWLINE* CLOSE_PAREN
    )?
  ;

collectionLiteral returns [Node node]
@init{ /* TODO - This needs to be syntactic sugar not an actual node. */ }
  : OPEN_BRACKET
    ( expressionList
    )?
    CLOSE_BRACKET
  ;

relationalOperator returns [RelationalOperation.Kind kind]
  : EQUAL                { $kind = RelationalOperation.Kind.EQUAL; }
  | NOT_EQUAL            { $kind = RelationalOperation.Kind.NOT_EQUAL; }
  | LESS_THAN_EQUAL      { $kind = RelationalOperation.Kind.LESS_THAN_EQUAL; }
  | GREATER_THAN_EQUAL   { $kind = RelationalOperation.Kind.GREATER_THAN_EQUAL; }
  | LESS_THAN            { $kind = RelationalOperation.Kind.LESS_THAN; }
  | GREATER_THAN         { $kind = RelationalOperation.Kind.GREATER_THAN; }
  ;

additiveOperator returns [ArithmeticOperation.Kind kind]
  : PLUS       { $kind = ArithmeticOperation.Kind.ADD; }
  | MINUS      { $kind = ArithmeticOperation.Kind.SUBTRACT; }
  ;

multiplicativeOperator returns [ArithmeticOperation.Kind kind]
  : MULTIPLY  { $kind = ArithmeticOperation.Kind.MULTIPLY; }
  | DIVIDE    { $kind = ArithmeticOperation.Kind.DIVIDE; }
  | MODULO    { $kind = ArithmeticOperation.Kind.REMAINDER; }
  ;

terminator
  : (NEWLINE | SEMICOLON)+
  | EOF
  ;

IF:                 'if';
ELSE:               'else';
LOOP:               'loop';
WHILE:              'while';

RETURN:             'return';
BREAK:              'break';

DEFINE:             'define';
PACKAGE:            'package';
INCLUDE:            'include';

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

FUNCTION:           'function';
STRUCT:             'struct';

TRUE:               'true';
FALSE:              'false';

NULL:               'null';

IDENTIFIER:         ID_CHAR+;

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
fragment ID_CHAR:     LETTER | DIGIT | UNDERSCORE | COLON;
fragment COLON:       ':';
fragment UNDERSCORE:  '_';
fragment LOWER:       'a'..'z';
fragment UPPER:       'A'..'Z';
fragment DIGIT:       '0'..'9';
fragment HEX_DIGIT:   ('0'..'9'|'a'..'f'|'A'..'F');
fragment SPACE:       ' ' | '\t';

