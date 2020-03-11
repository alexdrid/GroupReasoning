package com.drid.group_reasoning.engine.parser.exceptions;


import com.drid.group_reasoning.engine.parser.LogicToken;

public class ParserException extends RuntimeException {

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, LogicToken token) {
        super(message + " " + token.sequence);
    }
}
