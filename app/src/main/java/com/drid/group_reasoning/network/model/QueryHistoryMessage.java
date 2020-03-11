package com.drid.group_reasoning.network.model;

public class QueryHistoryMessage extends Message {
    private String sender;

    public QueryHistoryMessage(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }
}
