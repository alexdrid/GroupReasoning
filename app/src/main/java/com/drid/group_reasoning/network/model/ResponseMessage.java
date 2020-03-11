package com.drid.group_reasoning.network.model;

import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;

import java.util.List;

public class ResponseMessage extends Message {

    private String sender;
    private List<AtomicSentence> foundFacts;


    public ResponseMessage(String sender, List<AtomicSentence> foundFacts) {
        this.sender = sender;
        this.foundFacts = foundFacts;
    }

    public String getSender() {
        return sender;
    }

    public List<AtomicSentence> getFoundFacts() {
        return foundFacts;
    }

}
