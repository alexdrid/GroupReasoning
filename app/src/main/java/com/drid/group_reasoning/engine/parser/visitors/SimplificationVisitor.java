package com.drid.group_reasoning.engine.parser.visitors;


import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.engine.parser.ast.ComplexSentence;
import com.drid.group_reasoning.engine.parser.ast.Sentence;


public abstract class SimplificationVisitor
        implements LogicVisitor<Sentence> {

    @Override
    public Sentence visitAtomicSentence(AtomicSentence node) {
        return node;
    }

    @Override
    public Sentence visitUnarySentence(ComplexSentence node){
        return new ComplexSentence(
                node.getSentenceA().accept(this),
                node.getConnective());
    }
    
    @Override
    public Sentence visitBinarySentence(ComplexSentence node){
        return new ComplexSentence(
                node.getSentenceA().accept(this),
                node.getSentenceB().accept(this), 
                node.getConnective());
    }
}
