package com.drid.group_reasoning.network.model;

import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;

import java.util.List;

public class QueryMessage extends Message {


    private String sender;
    private List<AtomicSentence> requestedFacts;


    public QueryMessage(String sender, List<AtomicSentence> requestedFacts) {
        this.sender = sender;
        this.requestedFacts = requestedFacts;
    }


    public String getSender() {
        return sender;
    }

    public List<AtomicSentence> getRequestedFacts() {
        return requestedFacts;
    }

}
