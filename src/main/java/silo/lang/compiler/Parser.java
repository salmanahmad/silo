/*
 *
 *  Copyright 2013 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.lang.compiler;

import silo.lang.*;
import silo.lang.compiler.grammar.GrammarLexer;
import silo.lang.compiler.grammar.GrammarParser;

import org.antlr.runtime.*;
import java.util.Vector;

public class Parser {

    public Node parse(String source) {
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
            return parser.program().value;
        } catch(RecognitionException exception) {
            throw new RuntimeException("Parser error.");
        }
    }
}


