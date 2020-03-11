package com.drid.group_reasoning.engine.inference;

import android.util.Log;

import com.drid.group_reasoning.engine.knowledgebase.Clause;
import com.drid.group_reasoning.engine.knowledgebase.KnowledgeBase;
import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.engine.parser.ast.Sentence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


public class ForwardChaining {

    private Map<Sentence, Boolean> inferred;
    private Map<Sentence, Set<Sentence>> premises;

    private static String TAG = ForwardChaining.class.getName();

    private Map<Clause, Integer> initCount(KnowledgeBase kb) {
        Map<Clause, Integer> count = new HashMap<>();

        Set<Clause> clauses = new HashSet<>(kb.getConjunctionOfClauses());

        Log.i(ForwardChaining.class.getSimpleName(), "initCount: clauses = " + clauses);
        for (Clause c : clauses) {
            if (!c.isDefiniteClause()) {
                throw new IllegalArgumentException(
                        "Knowledge Base contains non-horn clauses:" + c);
            }
            count.put(c, c.totalNegativeLiterals());
        }
        return count;
    }

    private Map<Sentence, Boolean> initInferred(KnowledgeBase kb) {
        Map<Sentence, Boolean> inferred = new HashMap<>();
        for (Sentence p : kb.getSymbols()) {
            inferred.put(p, false);
        }
        return inferred;
    }

    private Queue<Sentence> initAgenda(Map<Clause, Integer> count) {
        Queue<Sentence> agenda = new LinkedList<>();
        for (Clause c : count.keySet()) {
            if (c.totalNegativeLiterals() == 0) {
                agenda.add(conclusion(c));
            }
        }

        return agenda;
    }

    private Sentence conclusion(Clause c) {
        return c.getPositiveLiterals().iterator().next();
    }

    private Map<Sentence, Set<Clause>> initAtomOccurrence(
            Map<Clause, Integer> count,
            Map<Sentence, Boolean> inferred) {

        Map<Sentence, Set<Clause>> atomOccurrence = new HashMap<>();
        for (Sentence p : inferred.keySet()) {
            Set<Clause> clausesWithPInPremise = new HashSet<>();
            for (Clause c : count.keySet()) {
                // Note: The negative symbols comprise the premise
                if (c.getNegativeLiterals().contains(p)) {
                    clausesWithPInPremise.add(c);
                }
            }
            atomOccurrence.put(p, clausesWithPInPremise);
        }

        return atomOccurrence;
    }


    private Map<Sentence, Set<Sentence>> initPremises(KnowledgeBase kb) {
        Map<Sentence, Set<Sentence>> premises = new HashMap<>();
        List<Clause> conjunctionOfClauses = kb.getConjunctionOfClauses();

        for (Clause c : conjunctionOfClauses) {

            if (!c.getNegativeLiterals().isEmpty()) {
                Set<Sentence> negativeLiterals = new HashSet<Sentence>(c.getNegativeLiterals());
                Sentence goal = c.getPositiveLiterals().get(0);
                premises.put(goal, negativeLiterals);
            }

        }
        return premises;
    }

    public boolean fcEntails(KnowledgeBase kb, Sentence q) {

        Map<Clause, Integer> count = initCount(kb);
        inferred = initInferred(kb);
        Queue<Sentence> agenda = initAgenda(count);
        Map<Sentence, Set<Clause>> atomOccurrenceInPremise = initAtomOccurrence(count, inferred);
        premises = initPremises(kb);


        Log.i(TAG, "fcEntails: symbols " + kb.getSymbols());
        while (!agenda.isEmpty()) {
            AtomicSentence p = (AtomicSentence) agenda.remove();
            if (p.equals(q)) {
                return true;
            }

            if (inferred.get(p).equals(Boolean.FALSE)) {

                inferred.put(p, true);
                for (Clause c : atomOccurrenceInPremise.get(p)) {
                    count.put(c, count.get(c) - 1);
                    if (count.get(c) == 0) {
                        agenda.add(conclusion(c));
                    }
                }
            }
        }

        return false;
    }


    public Set<AtomicSentence> getMissingFacts(Sentence sentence) {
        Set<AtomicSentence> result = new HashSet<>();
        Log.i(ForwardChaining.class.getSimpleName(),
                "getMissingFacts:  getting Missing Facts of" + sentence);
        Log.i(ForwardChaining.class.getSimpleName(),
                "getMissingFacts: premises " + premises);

        if (premises.containsKey(sentence)) {
            for (Sentence premise : premises.get(sentence)) {
                if (inferred.get(premise).equals(Boolean.FALSE)) {
                    result.add((AtomicSentence) premise);
                }
            }
        }
        return result;
    }
}
