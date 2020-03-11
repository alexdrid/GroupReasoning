package com.drid.group_reasoning.engine.parser;

import com.drid.group_reasoning.engine.parser.exceptions.ParserException;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogicTokenizer {

    private LinkedList<TokenInfo> tokenInfoList;
    private LinkedList<LogicToken> tokens;

    public LogicTokenizer() {
        tokenInfoList = new LinkedList<>();
        tokens = new LinkedList<>();
    }

    public static LogicTokenizer getTokenizer() {
        LogicTokenizer tokenizer = new LogicTokenizer();
        tokenizer.add("[a-zA-Z][a-zA-Z0-9_]*", 1); // symbol
        tokenizer.add("\\(", 2); // open bracket
        tokenizer.add("\\)", 3); // close bracket
        tokenizer.add("\\¬", 4); // negation
        tokenizer.add("\\∧", 5); // and
        tokenizer.add("\\∨", 6); // or
        tokenizer.add("\\→", 7); // implication
        tokenizer.add("\\↔", 8); // equivalence

        return tokenizer;
    }

    public void add(String regex, int token) {
        tokenInfoList.add(
                new TokenInfo(Pattern.compile("^(" + regex + ")"), token));
    }

    public void tokenize(String str) {
        String s = str.trim();
        tokens.clear();

        while (!s.equals("")) {
            boolean match = false;

            for (TokenInfo info : tokenInfoList) {
                Matcher m = info.regex.matcher(s);

                if (m.find()) {
                    match = true;

                    String tok = m.group().trim();
                    s = m.replaceFirst("").trim();
                    tokens.add(new LogicToken(info.token, tok));

                    break;
                }
            }
            if (!match) {
                throw new ParserException(
                        "Unexpected character in input: " + s);
            }
        }
    }

    public LinkedList<LogicToken> getTokens() {
        return tokens;
    }

    private class TokenInfo {

        public final Pattern regex;
        public final int token;

        public TokenInfo(Pattern regex, int token) {
            this.regex = regex;
            this.token = token;
        }
    }
}
