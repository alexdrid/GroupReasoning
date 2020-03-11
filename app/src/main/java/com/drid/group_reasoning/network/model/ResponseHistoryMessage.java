package com.drid.group_reasoning.network.model;

import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;

import java.util.List;
import java.util.Map;

public class ResponseHistoryMessage extends Message {
    private String sender;
    private Map<AtomicSentence, List<String>> queryHistory;

    public ResponseHistoryMessage(String sender, Map<AtomicSentence, List<String>> queryHistory) {
        this.sender = sender;
        this.queryHistory = queryHistory;
    }

    public String getSender() {
        return sender;
    }

    public Map<AtomicSentence, List<String>> getQueryHistory() {
        return queryHistory;
    }
}
