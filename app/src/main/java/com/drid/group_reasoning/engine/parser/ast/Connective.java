package com.drid.group_reasoning.engine.parser.ast;

public enum Connective {
    NOT("¬"),
    AND("∧"),
    OR("∨"),
    IMPLICATION("→"),
    BICONDITIONAL("↔");

    private String symbol;

    Connective(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
