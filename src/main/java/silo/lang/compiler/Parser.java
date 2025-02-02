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

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;

public class Parser {

    public static Node parse(String source) {
        return parse(null, source);
    }

    public static Node parse(String fileName, String source) {
        ANTLRInputStream stream = new ANTLRInputStream(source);
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
        GrammarParser parser = new GrammarParser(fileName, tokens);
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

        try {
            return parser.program().value;
        } catch(RecognitionException exception) {
            throw new RuntimeException("Parser error.");
        }
    }
}


