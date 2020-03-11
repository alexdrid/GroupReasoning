package com.drid.group_reasoning.engine.parser;

public class LogicToken {

    public static final int END_OF_INPUT = 0;
    public static final int SYMBOL = 1;
    public static final int OPEN_BRACKET = 2;
    public static final int CLOSE_BRACKET = 3;
    public static final int NOT = 4;
    public static final int AND = 5;
    public static final int OR = 6;
    public static final int IMPLICATION = 7;
    public static final int EQUIVALENCE = 8;

    public final int token;
    public final String sequence;
    public  int position;

    public LogicToken(int token, String sequence) {
        super();
        this.token = token;
        this.sequence = sequence;
    }

   public LogicToken(int token, String sequence, int position) {
        super();
        this.token = token;
        this.sequence = sequence;
        this.position = position;
    }

    public String getSequence() {
        return sequence;
    }
}
