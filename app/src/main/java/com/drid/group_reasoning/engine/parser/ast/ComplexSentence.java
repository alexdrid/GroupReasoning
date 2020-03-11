package com.drid.group_reasoning.engine.parser.ast;

public class ComplexSentence extends Sentence {

    private Sentence a;
    private Sentence b;

    private Connective connective;

    public ComplexSentence(Sentence a, Sentence b, Connective connective) {
        this.a = a;
        this.b = b;
        this.connective = connective;
    }

    public ComplexSentence(Sentence negated, Connective connective) {
        this.a = negated;
        this.connective = connective;
    }

    public Sentence getSentenceA() {
        return a;
    }

    public Sentence getSentenceB() {
        return b;
    }

    @Override
    public Connective getConnective() {
        return connective;
    }

    public String getProposition() {
        StringBuilder sb = new StringBuilder();

        if (isNotSentence()) {
            if (getSentenceA().getProposition() != null) {
                sb.append("it is not the case that")
                        .append(" ")
                        .append(getSentenceA().getProposition());
            }

        } else if (isAndSentence()) {
            if (getSentenceA().getProposition() != null && getSentenceB() != null) {
                sb.append(getSentenceA().getProposition())
                        .append(" ")
                        .append("and")
                        .append(" ")
                        .append(getSentenceB().getProposition());
            }
        } else if (isOrSentence()) {
            if (getSentenceA().getProposition() != null && getSentenceB() != null) {
                sb.append(getSentenceA().getProposition())
                        .append(" ")
                        .append("or")
                        .append(" ")
                        .append(getSentenceB().getProposition());
            }
        } else if (isImplication()) {
            if (getSentenceA().getProposition() != null && getSentenceB() != null) {
                sb.append("if")
                        .append(" ")
                        .append(getSentenceA().getProposition())
                        .append(", ")
                        .append("then")
                        .append(" ")
                        .append(getSentenceB().getProposition());
            }
        } else {
            if (getSentenceA().getProposition() != null && getSentenceB() != null) {
                sb.append(getSentenceA().getProposition())
                        .append(" ")
                        .append("if and only if")
                        .append(" ")
                        .append(getSentenceB().getProposition());
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (isNotSentence()) {
            sb.append("(")
                    .append(this.connective.getSymbol())
                    .append(this.a)
                    .append(")");
        } else {
            sb.append("(");
            sb.append(this.a)
                    .append(" ")
                    .append(this.connective.getSymbol())
                    .append(" ")
                    .append(this.b);

            sb.append(")");
        }
        return sb.toString();
    }
}
