package com.drid.group_reasoning.engine.knowledgebase;

import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.engine.parser.ast.Connective;

public class Literal{

    public AtomicSentence atomicSentence;
    private boolean isPositive;

    public Literal(AtomicSentence atomicSentence) {
        this(atomicSentence, true);
    }

    public Literal(AtomicSentence atomicSentence, boolean isPositive) {
        this.atomicSentence = atomicSentence;
        this.isPositive = isPositive;
    }

    public boolean isPositiveLiteral() {
        return isPositive;
    }

    private boolean isNegativeLiteral() {
        return !isPositive;
    }

    public AtomicSentence getAtomicSentence() {
        return atomicSentence;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(this.isPositiveLiteral()){
            sb.append(getAtomicSentence().getSymbol());
        }else if (this.isNegativeLiteral()){
            sb.append(Connective.NOT.getSymbol());
            sb.append(getAtomicSentence().getSymbol());
        }
        return sb.toString();
    }
}
