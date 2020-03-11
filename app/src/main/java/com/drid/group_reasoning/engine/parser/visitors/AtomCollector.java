package com.drid.group_reasoning.engine.parser.visitors;

import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.engine.parser.ast.ComplexSentence;
import com.drid.group_reasoning.engine.parser.ast.Sentence;

import java.util.ArrayList;
import java.util.List;

public class AtomCollector implements LogicVisitor<List<AtomicSentence>> {
    
    public static List<AtomicSentence> getAtomicSentences(Sentence sentence){
        AtomCollector collector = new AtomCollector();
        return sentence.accept(collector);
    }

    @Override
    public List<AtomicSentence> visitAtomicSentence(AtomicSentence atomicSentence) {
        List<AtomicSentence> result = new ArrayList<>();
        result.add(atomicSentence);
        return result;
    }

    @Override
    public List<AtomicSentence> visitUnarySentence(ComplexSentence unarySentence) {
        List<AtomicSentence> result = new ArrayList<>();
        result.add((AtomicSentence) unarySentence.getSentenceA());
        return result;
    }

    @Override
    public List<AtomicSentence> visitBinarySentence(ComplexSentence binarySentence) {
        List<AtomicSentence> result = new ArrayList<>();
        result.addAll(binarySentence.getSentenceA().accept(this));
        result.addAll(binarySentence.getSentenceB().accept(this));
        return result;
    }

}
