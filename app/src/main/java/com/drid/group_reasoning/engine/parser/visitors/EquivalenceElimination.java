package com.drid.group_reasoning.engine.parser.visitors;


import com.drid.group_reasoning.engine.parser.ast.ComplexSentence;
import com.drid.group_reasoning.engine.parser.ast.Connective;
import com.drid.group_reasoning.engine.parser.ast.Sentence;

public class EquivalenceElimination extends SimplificationVisitor {

    @Override
    public Sentence visitBinarySentence(ComplexSentence sentence) {
        Sentence result;

        if (sentence.isBiconditional()) {
            Sentence a = sentence.getSentenceA().accept(this);
            Sentence b = sentence.getSentenceB().accept(this);

            Sentence aImpliesB
                    = new ComplexSentence(a, b, Connective.IMPLICATION);

            Sentence bImpliesA
                    = new ComplexSentence(b, a, Connective.IMPLICATION);

            result = new ComplexSentence(aImpliesB, bImpliesA, Connective.AND);
        }else{
            result = super.visitBinarySentence(sentence);
        }

        return result;
    }

}
