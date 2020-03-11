package com.drid.group_reasoning.engine.parser.visitors;

import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.engine.parser.ast.ComplexSentence;
import com.drid.group_reasoning.engine.parser.ast.Sentence;

public class SetProposition implements LogicVisitor<Sentence> {

    private String symbol;
    private String proposition;

    public SetProposition(String symbol, String proposition) {
        this.symbol = symbol;
        this.proposition = proposition;
    }

    @Override
    public Sentence visitAtomicSentence(AtomicSentence atomicSentence) {
        if (atomicSentence.getSymbol().equals(this.symbol)) {
            atomicSentence.setProposition(this.proposition);
        }

        return atomicSentence;
    }

    @Override
    public Sentence visitUnarySentence(ComplexSentence unarySentence) {
        return new ComplexSentence(
                unarySentence.getSentenceA().accept(this),
                unarySentence.getConnective());
    }

    @Override
    public Sentence visitBinarySentence(ComplexSentence binarySentence) {
        return new ComplexSentence(
                binarySentence.getSentenceA().accept(this),
                binarySentence.getSentenceB().accept(this),
                binarySentence.getConnective());
    }

}
