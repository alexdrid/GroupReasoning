package com.drid.group_reasoning.engine.parser.visitors;

import android.util.Log;

import com.drid.group_reasoning.engine.knowledgebase.Literal;
import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.engine.parser.ast.ComplexSentence;
import com.drid.group_reasoning.engine.parser.ast.Sentence;

import java.util.ArrayList;
import java.util.List;

public class LiteralCollector implements LogicVisitor<List<Literal>> {
    
    public static List<Literal> getLiterals(Sentence sentence){
        LiteralCollector collector = new LiteralCollector();
        List<Literal> literals = sentence.accept(collector);
        return literals;
    }

    @Override
    public List<Literal> visitAtomicSentence(AtomicSentence atomicSentence) {
        List<Literal> result = new ArrayList<>();
        result.add(new Literal(atomicSentence));
        return result;
    }

    @Override
    public List<Literal> visitUnarySentence(ComplexSentence negated) {
        List<Literal> result = new ArrayList<>();
        result.add(new Literal((AtomicSentence) negated.getSentenceA(), false));
        return result;
    }

    @Override
    public List<Literal> visitBinarySentence(ComplexSentence complexSentence) {
        List<Literal> result = new ArrayList<>();
        result.addAll(
                complexSentence.getSentenceA().accept(this));
        result.addAll(
                complexSentence.getSentenceB().accept(this));

        return result;
    }

}
