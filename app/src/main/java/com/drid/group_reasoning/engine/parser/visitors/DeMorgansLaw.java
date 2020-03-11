package com.drid.group_reasoning.engine.parser.visitors;

import com.drid.group_reasoning.engine.parser.ast.ComplexSentence;
import com.drid.group_reasoning.engine.parser.ast.Connective;
import com.drid.group_reasoning.engine.parser.ast.Sentence;

public class DeMorgansLaw extends SimplificationVisitor {

    @Override
    public Sentence visitUnarySentence(ComplexSentence node) {
        Sentence result = null;

        Sentence negated = node.getSentenceA();

        if (negated.isAtomicSentence()) {
            result = node;
        } else if (negated.isNotSentence()) {
            Sentence a = ((ComplexSentence) negated).getSentenceA();
            result = a.accept(this);
        } else if (negated.isAndSentence()
                || negated.isOrSentence()) {
            Sentence alpha = ((ComplexSentence) negated).getSentenceA();
            Sentence beta = ((ComplexSentence) negated).getSentenceB();

            Sentence notAlpha
                    = (new ComplexSentence(alpha, Connective.NOT)).accept(this);
            Sentence notBeta
                    = (new ComplexSentence(beta, Connective.NOT)).accept(this);
            if (negated.isAndSentence()) {
                result = new ComplexSentence(notAlpha, notBeta, Connective.OR);
            } else {
                result = new ComplexSentence(notAlpha, notBeta, Connective.AND);
            }
        }

        return result;
    }

}
