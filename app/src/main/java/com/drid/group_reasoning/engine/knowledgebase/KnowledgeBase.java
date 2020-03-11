package com.drid.group_reasoning.engine.knowledgebase;

import android.database.Cursor;
import android.hardware.SensorEvent;
import android.net.Uri;

import com.drid.group_reasoning.data.contracts.RuleContract;
import com.drid.group_reasoning.engine.parser.LogicParser;
import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.engine.parser.ast.Sentence;
import com.drid.group_reasoning.engine.parser.visitors.AtomCollector;
import com.drid.group_reasoning.engine.parser.visitors.ClauseCollector;
import com.drid.group_reasoning.engine.parser.visitors.LiteralCollector;
import com.drid.group_reasoning.engine.parser.visitors.SetProposition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.drid.group_reasoning.data.contracts.RuleContract.PATH_SYMBOL;


public class KnowledgeBase {

    private List<Sentence> sentences;
    private Set<AtomicSentence> symbols;
    private List<Clause> clauses;
    private LogicParser parser;

    public KnowledgeBase() {
        this.clauses = new ArrayList<>();
        this.sentences = new ArrayList<>();
        this.symbols = new HashSet<>();
        this.parser = new LogicParser();
    }

    public void tell(Sentence sentence) {
        addSentence(sentence);
    }

    private void addSentence(Sentence sentence) {
        this.sentences.add(sentence);
        Sentence cnfSentence = Sentence.convertToCnf(sentence);
        List<Literal> literals = LiteralCollector.getLiterals(cnfSentence);
        this.clauses.add(new Clause(literals));
    }

    public List<Clause> getClauses() {
        return clauses;
    }

    public List<Clause> getConjunctionOfClauses() {
        List<Clause> result = new ArrayList<>();
        if(!sentences.isEmpty()){
            Sentence kbSentence = Sentence.conjunctionOfSentences(sentences);
            Sentence kbCnfSentence = Sentence.convertToCnf(kbSentence);
            result.addAll(ClauseCollector.getClauses(kbCnfSentence));
        }

        return result;
    }

    public List<Sentence> getSentences() {
        return sentences;
    }

    public Set<AtomicSentence> getSymbols() {
        for (Sentence sentence : sentences) {
            this.symbols.addAll(AtomCollector.getAtomicSentences(sentence));
        }
        return symbols;
    }
}
