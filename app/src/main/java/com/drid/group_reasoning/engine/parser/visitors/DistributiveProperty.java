package com.drid.group_reasoning.engine.parser.visitors;

import com.drid.group_reasoning.engine.parser.ast.ComplexSentence;
import com.drid.group_reasoning.engine.parser.ast.Connective;
import com.drid.group_reasoning.engine.parser.ast.Sentence;

public class DistributiveProperty extends SimplificationVisitor {

    @Override
    public Sentence visitBinarySentence(ComplexSentence node) {
        Sentence result;

        if (node.isOrSentence()) {
            Sentence s1 = node.getSentenceA().accept(this);
            Sentence s2 = node.getSentenceB().accept(this);

            if (s1.isAndSentence() || s2.isAndSentence()) {
                Sentence alpha, betaAndGamma;
                if (s2.isAndSentence()) {
                    alpha = s1;
                    betaAndGamma = s2;
                } else {
                    alpha = s2;
                    betaAndGamma = s1;
                }
                Sentence beta = ((ComplexSentence) betaAndGamma).getSentenceA();
                Sentence gamma = ((ComplexSentence) betaAndGamma).getSentenceB();

                if (s2.isAndSentence()) {
                    Sentence alphaOrBeta = new ComplexSentence(
                            alpha, beta, Connective.OR)
                            .accept(this);

                    Sentence alphaOrGamma = new ComplexSentence(
                            alpha, gamma, Connective.OR)
                            .accept(this);

                    result = new ComplexSentence(alphaOrBeta, alphaOrGamma, Connective.AND);
                } else {
                    ComplexSentence bOrA
                            = (ComplexSentence) (new ComplexSentence(
                                    beta,
                                    alpha,
                                    Connective.OR))
                                    .accept(this);
                    ComplexSentence cOrA
                            = (ComplexSentence) (new ComplexSentence(
                                    gamma,
                                    alpha,
                                    Connective.OR))
                                    .accept(this);

                    result = new ComplexSentence(bOrA, cOrA, Connective.AND);
                }
            } else {
                result = new ComplexSentence(s1, s2, Connective.OR);
            }
        } else {
            result = super.visitBinarySentence(node);
        }

        return result;
    }

}
