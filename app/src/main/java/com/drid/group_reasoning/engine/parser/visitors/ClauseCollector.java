package com.drid.group_reasoning.engine.parser.visitors;

import com.drid.group_reasoning.engine.knowledgebase.Clause;
import com.drid.group_reasoning.engine.knowledgebase.Literal;
import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.engine.parser.ast.ComplexSentence;
import com.drid.group_reasoning.engine.parser.ast.Sentence;

import java.util.ArrayList;
import java.util.List;


public class ClauseCollector implements LogicVisitor<List<Clause>> {
    
    public static List<Clause> getClauses(Sentence sentence){
        ClauseCollector collector = new ClauseCollector();
        List<Clause> clauses = sentence.accept(collector);
        return clauses;
    }

    @Override
    public List<Clause> visitAtomicSentence(AtomicSentence atomicSentence) {
        List<Clause> result = new ArrayList<>();
        Literal pLiteral = new Literal(atomicSentence);
        result.add(new Clause(pLiteral));
        return result;
    }

    @Override
    public List<Clause> visitUnarySentence(ComplexSentence negated) {
        List<Clause> result = new ArrayList<>();
        Literal nLiteral = new Literal((AtomicSentence) negated.getSentenceA(), false);
        result.add(new Clause(nLiteral));
        return result;
    }

    @Override
    public List<Clause> visitBinarySentence(ComplexSentence complexSentence) {
        List<Clause> result = new ArrayList<>();
        if(complexSentence.isAndSentence()){
            result.addAll(
                    complexSentence.getSentenceA().accept(this));
            result.addAll(
                    complexSentence.getSentenceB().accept(this));
        }else if (complexSentence.isOrSentence()){
            List<Literal> literals = new ArrayList<>(LiteralCollector.getLiterals(complexSentence));
            result.add(new Clause(literals));
        }

        return result;
    }
}
