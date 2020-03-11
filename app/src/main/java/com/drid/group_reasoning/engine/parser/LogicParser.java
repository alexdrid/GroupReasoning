package com.drid.group_reasoning.engine.parser;

import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.engine.parser.ast.ComplexSentence;
import com.drid.group_reasoning.engine.parser.ast.Connective;
import com.drid.group_reasoning.engine.parser.ast.Sentence;
import com.drid.group_reasoning.engine.parser.exceptions.ParserException;

import java.util.LinkedList;

public class LogicParser {

    private LinkedList<LogicToken> tokens;
    private LogicToken lookahead;

    public Sentence parse(String expression) {
        LogicTokenizer tokenizer = LogicTokenizer.getTokenizer();
        tokenizer.tokenize(expression);
        LinkedList<LogicToken> tokens = tokenizer.getTokens();
        return this.parse(tokens);
    }

    private Sentence parse(LinkedList<LogicToken> tokens) {
        this.tokens = new LinkedList<>(tokens);
        lookahead = this.tokens.getFirst();
        Sentence node = sentence();

        if (lookahead.token != LogicToken.END_OF_INPUT) {
            throw new ParserException(String.format(
                    "Unexpected symbol %s found", lookahead.getSequence()));

        }

        return node;
    }

    private void nextToken() {
        tokens.pop();
        // at the end of input we return an epsilon token
        if (tokens.isEmpty()) {
            lookahead = new LogicToken(LogicToken.END_OF_INPUT, "", -1);
        } else {
            lookahead = tokens.getFirst();
        }
    }

    private Sentence sentence() {
        Sentence node = atomicSentence();
        return complexSentence(node);
    }

    private Sentence atomicSentence() {
        Sentence sentenceNode;
        if (lookahead.token == LogicToken.SYMBOL) {
            sentenceNode = new AtomicSentence(lookahead.getSequence());
            nextToken();
            return sentenceNode;
        }
        return null;
    }

    private Sentence complexSentence(Sentence node) {
        if (lookahead.token == LogicToken.OPEN_BRACKET) {
            nextToken();
            Sentence expr = sentence();
            if (lookahead.token != LogicToken.CLOSE_BRACKET) {
                throw new ParserException("Closing brackets expected", lookahead);
            }
            nextToken();
            expr = complexSentence(expr);
            return expr;
        } else if (lookahead.token == LogicToken.NOT) {
            nextToken();
            return new ComplexSentence(sentence(), Connective.NOT);
        } else if (lookahead.token == LogicToken.AND) {
            nextToken();
            Sentence rightSentence = sentence();
            return new ComplexSentence(node, rightSentence, Connective.AND);
        } else if (lookahead.token == LogicToken.OR) {
            nextToken();
            Sentence rightSentence = sentence();
            return new ComplexSentence(node, rightSentence, Connective.OR);
        } else if (lookahead.token == LogicToken.IMPLICATION) {
            nextToken();
            Sentence rightSentence = sentence();
            return new ComplexSentence(node, rightSentence, Connective.IMPLICATION);
        } else if (lookahead.token == LogicToken.EQUIVALENCE) {
            nextToken();
            Sentence rightSentence = sentence();
            return new ComplexSentence(node, rightSentence, Connective.BICONDITIONAL);
        }

        return node;
    }

}
