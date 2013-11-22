/*
 *
 *  Copyright 2012 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.lang.compiler;

import silo.lang.nodes.*;
import silo.lang.compiler.grammar.GrammarLexer;
import silo.lang.compiler.grammar.GrammarParser;

import org.antlr.runtime.*;

public class Parser {

    public Nodes parse(String source) {
        Nodes nodes = null;

        ANTLRStringStream stream = new ANTLRStringStream(source);
        GrammarLexer lexer = new GrammarLexer(stream);

        /*
        Token token;
        while (null != (token = lexer.nextToken())) {
            int tokenType = token.getType();
            if (tokenType == -1) break;
            if (token.getChannel() == Token.DEFAULT_CHANNEL)
            System.out.println(GrammarParser.tokenNames[token.getType()]);
        }
        lexer.reset();
        */

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GrammarParser parser = new GrammarParser(tokens);

        try {
            nodes = parser.program().nodes;
        } catch(RecognitionException exception) {
            throw new RuntimeException("Parser error.");
        }

        return nodes;
    }

    public GrammarParser parser(String source) {
        ANTLRStringStream stream = new ANTLRStringStream(source);
        GrammarLexer lexer = new GrammarLexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GrammarParser parser = new GrammarParser(tokens);

        return parser;
    }

    public static String formatAST(Node node) {
        return formatAST(node, 0);
    }

    public static String formatAST(Node node, int indent) {
        StringBuffer buffer = new StringBuffer();
        
        String tab = "";
        for(int i = 0; i < indent; i++) {
            tab += "  ";
        }
        
        buffer.append(tab);

        if(node == null) {
            buffer.append("null\n");
        } else {
             buffer.append(node.toParseTreeString() + "\n");
             
             for(Node child : node.children()) {
                 buffer.append(formatAST(child, indent + 1));
             }
        }
        
        return buffer.toString();
    }

}


