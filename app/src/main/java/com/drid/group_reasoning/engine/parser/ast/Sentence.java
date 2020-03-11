package com.drid.group_reasoning.engine.parser.ast;

import com.drid.group_reasoning.engine.parser.visitors.DeMorgansLaw;
import com.drid.group_reasoning.engine.parser.visitors.DistributiveProperty;
import com.drid.group_reasoning.engine.parser.visitors.EquivalenceElimination;
import com.drid.group_reasoning.engine.parser.visitors.ImplicationElimination;
import com.drid.group_reasoning.engine.parser.visitors.LogicVisitor;

import java.util.List;

public abstract class Sentence implements SentenceNode{

    public abstract String getProposition();
    
    public abstract Connective getConnective();

    public boolean isBiconditional() {
        return getConnective() == Connective.BICONDITIONAL;
    }

    public boolean isImplication() {
        return getConnective() == Connective.IMPLICATION;
    }

    public boolean isAndSentence() {
        return getConnective() == Connective.AND;
    }

    public boolean isOrSentence() {
        return getConnective() == Connective.OR;
    }

    public boolean isNotSentence() {
        return getConnective() == Connective.NOT;
    }

    public boolean isUnarySentence() {
        return isNotSentence();
    }

    public boolean isBinarySentence() {
        return getConnective() != null && !(getConnective() == Connective.NOT);
    }

    public boolean isAtomicSentence() {
        return getConnective() == null;
    }

    public static Sentence convertToCnf(Sentence sentence){
        Sentence result;
               
        result = sentence.accept(new EquivalenceElimination());
        
        result = result.accept(new ImplicationElimination());
        
        result = result.accept(new DeMorgansLaw());
        
        result = result.accept(new DistributiveProperty());
        
        return result;        
    }

    public static Sentence conjunctionOfSentences(List<Sentence> sentences){


        if (sentences.size() == 1) {
            return sentences.get(0);
        }

        return new ComplexSentence(
                sentences.get(0),
                conjunctionOfSentences(sentences.subList(1,sentences.size())),
                Connective.AND);
    }

    public <T> T accept(LogicVisitor<T> visitor) {
        T result = null;
        if (isAtomicSentence()) {
            result = visitor.visitAtomicSentence((AtomicSentence) this);
        } else if (isUnarySentence()) {
            result = visitor.visitUnarySentence((ComplexSentence) this);
        } else if (isBinarySentence()) {
            result = visitor.visitBinarySentence((ComplexSentence) this);
        }
        return result;
    }
}
