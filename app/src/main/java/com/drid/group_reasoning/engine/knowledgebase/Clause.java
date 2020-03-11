package com.drid.group_reasoning.engine.knowledgebase;

import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Clause {

    private List<Literal> literals;
    private List<AtomicSentence> positiveLiterals;
    private List<AtomicSentence> negativeLiterals;

    public Clause(Literal... literals) {
        this(Arrays.asList(literals));

    }

    public Clause(List<Literal> literals){
        this.literals = literals;
        this.positiveLiterals = new ArrayList<>();
        this.negativeLiterals = new ArrayList<>();

        addLiterals(literals);
    }

    private void addLiterals(List<Literal> literals) {
        for (Literal l : literals) {
            // Only add to caches if not already added
            if (l.isPositiveLiteral()) {
                this.positiveLiterals.add(l.getAtomicSentence());
            } else {
                this.negativeLiterals.add(l.getAtomicSentence());
            }

        }
    }

//    public boolean isFalse() {
//        return isEmpty();
//    }
//
//    private boolean isEmpty() {
//        return literals.size() == 0;
//    }

    public boolean isDefiniteClause() {
        return positiveLiterals.size() == 1;
    }

    public int totalPositiveLiterals() {
        return positiveLiterals.size();
    }

    public int totalNegativeLiterals() {
        return negativeLiterals.size();
    }

    public List<AtomicSentence> getPositiveLiterals() {
        return positiveLiterals;
    }

    public List<AtomicSentence> getNegativeLiterals() {
        return negativeLiterals;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Literal literal : literals) {
            sb.append(literal);
            if(!literal.equals(literals.get(literals.size() - 1))){
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }

}
