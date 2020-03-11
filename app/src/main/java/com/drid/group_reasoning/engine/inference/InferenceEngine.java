package com.drid.group_reasoning.engine.inference;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.drid.group_reasoning.data.contracts.FactContract;
import com.drid.group_reasoning.data.contracts.RuleContract;
import com.drid.group_reasoning.engine.knowledgebase.Clause;
import com.drid.group_reasoning.engine.knowledgebase.KnowledgeBase;
import com.drid.group_reasoning.engine.parser.LogicParser;
import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;
import com.drid.group_reasoning.engine.parser.ast.Sentence;
import com.drid.group_reasoning.engine.parser.visitors.AtomCollector;
import com.drid.group_reasoning.engine.parser.visitors.ClauseCollector;
import com.drid.group_reasoning.engine.parser.visitors.SetProposition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.drid.group_reasoning.data.contracts.RuleContract.PATH_SYMBOL;

public class InferenceEngine {

    private static final String TAG = "InferenceEngine";
    private Context context;

    private KnowledgeBase kb;
    private List<Sentence> rules = new ArrayList<>();
    private List<Sentence> facts = new ArrayList<>();

    private LogicParser parser = new LogicParser();

    private List<AtomicSentence> queries = new ArrayList<>();
    private List<AtomicSentence> resolvedQueries = new ArrayList<>();
    private List<AtomicSentence> unresolvedQueries = new ArrayList<>();

    private Set<AtomicSentence> missingFacts = new HashSet<>();

    public InferenceEngine(Context context) {
        this.context = context;
    }

    public void initKnowledgeBase() {

        kb = new KnowledgeBase();

        rules = getRulesFromDatabase();
        facts = getFactsFromDatabase();


        for (Sentence rule : rules) {

            kb.tell(rule);
        }

        for (Sentence fact : facts) {
            kb.tell(fact);
        }
    }


    private List<Sentence> getRulesFromDatabase(){
        List<Sentence> rulesList = new ArrayList<>();

        Cursor ruleCursor = context.getContentResolver().query(
                RuleContract.RuleEntry.RULES_URI,
                new String[]{RuleContract.RuleEntry.COLUMN_RULE_PL},
                null,
                null,
                null);
        if (ruleCursor.moveToFirst()) {
            do {
                String rulePl = ruleCursor.getString(
                        ruleCursor.getColumnIndex(RuleContract.RuleEntry.COLUMN_RULE_PL));

                Sentence sentence = parser.parse(rulePl);

                List<AtomicSentence> symbols = AtomCollector.getAtomicSentences(sentence);
                for(AtomicSentence atom : symbols){
                    String proposition = getPropositionFromDatabase(atom);
                    atom.accept(new SetProposition(atom.getSymbol(),proposition));
                }

                rulesList.add(sentence);
            } while (ruleCursor.moveToNext());
        }

        return rulesList;
    }

    private List<Sentence> getFactsFromDatabase() {
        List<Sentence> factList = new ArrayList<>();

        Cursor factCursor = context.getContentResolver().query(
                FactContract.FactEntry.FACT_URI,
                null,
                null,
                null,
                null);

        if (factCursor.moveToFirst()) {
            do {
                String symbol = factCursor.getString(
                        factCursor.getColumnIndex(FactContract.FactEntry.COLUMN_FACT_PROPOSITION));
                String proposition = factCursor.getString(
                        factCursor.getColumnIndex(FactContract.FactEntry.COLUMN_FACT_SYMBOL));

                Sentence sentence = parser.parse(symbol);

                sentence.accept(new SetProposition(symbol, proposition));

                factList.add(sentence);
            } while (factCursor.moveToNext());
        }

        return factList;
    }

    private String getPropositionFromDatabase(AtomicSentence sentence) {
        String proposition = "";
        Uri queryVarUri = RuleContract.SymbolEntry.SYMBOLS_URI.buildUpon()
                .appendPath(PATH_SYMBOL).build();

        Cursor symbolCursor = context.getContentResolver().query(
                queryVarUri,
                new String[]{RuleContract.SymbolEntry.COLUMN_PROPOSITION},
                RuleContract.SymbolEntry.COLUMN_SYMBOL + "=?",
                new String[]{sentence.getSymbol()},
                null
        );
        if (symbolCursor.moveToFirst()) {
            proposition = symbolCursor
                    .getString(symbolCursor.getColumnIndex(RuleContract.SymbolEntry.COLUMN_PROPOSITION));
        }

        return proposition;
    }

    private void conclusionOfRule(Sentence rule) {
        Sentence cnf = Sentence.convertToCnf(rule);
        List<Clause> clauses = ClauseCollector.getClauses(cnf);

        for(Clause clause : clauses){
            if(clause.isDefiniteClause()){
                List<AtomicSentence> positiveLiterals = clause.getPositiveLiterals();
                for(AtomicSentence positiveLiteral : positiveLiterals){
                    String proposition = getPropositionFromDatabase(positiveLiteral);

                    if (proposition != null) {
                        positiveLiteral.accept(new SetProposition(positiveLiteral.getSymbol(), proposition));
                    }
                }
                queries.addAll(positiveLiterals);
            }else{
                Log.i(TAG, "conclusionOfRule: Not a definite clause " + clause);
            }
        }

    }




    public void solve() {
        resolvedQueries.clear();
        unresolvedQueries.clear();
        missingFacts.clear();
        queries.clear();
        Log.i(TAG, "solve: Knowledge Base " + kb.getClauses());
        ForwardChaining fc = new ForwardChaining();


        for(Sentence rule : rules){
            conclusionOfRule(rule);
        }

        for(AtomicSentence query : queries){
            boolean isProved = fc.fcEntails(kb, query);

            if (isProved) {
                Log.i(TAG, "solve: Query " + query + " is resolved");
                resolvedQueries.add(query);
            } else {

                Log.i(TAG, "solve: Query " + query + " is not resolved");

                unresolvedQueries.add(query);
                missingFacts.addAll(fc.getMissingFacts(query));
            }
        }


        if (!resolvedQueries.isEmpty()) {
            Log.i(TAG, "solve: Resolved queries:\n " + resolvedQueries);
        }

        if (!unresolvedQueries.isEmpty()) {
            Log.i(TAG, "solve: Unresolved queries:\n " + unresolvedQueries);
            Log.i(TAG, "solve: Missing facts: " + missingFacts);
        }

    }

    public void solve(List<AtomicSentence> queries) {

        Log.i(TAG, "solve' : Knowledge Base " + kb.getClauses());
        Log.i(TAG, "solve' : Queries  " + queries);
        ForwardChaining fc = new ForwardChaining();


        for (AtomicSentence query : queries) {
            Log.i(TAG, "solve: Does " + query.getSymbol() + " entail the knowledge base?");
            boolean isProved = fc.fcEntails(kb, query);

            if (isProved) {
                resolvedQueries.add(query);
            } else {
                unresolvedQueries.add(query);
                Set<AtomicSentence> notInferred = fc.getMissingFacts(query);

                if (!notInferred.isEmpty()) {
                    missingFacts.addAll(notInferred);
                }
            }

        }


        Log.i(TAG, "solve' : Resolved queries: " + resolvedQueries);
        Log.i(TAG, "solve' : Unresolved queries: " + unresolvedQueries);
        Log.i(TAG, "solve' : Missing facts: " + missingFacts);

    }

    public List<AtomicSentence> getResolvedQueries() {
        return resolvedQueries;
    }

    public List<AtomicSentence> getUnresolvedQueries() {
        return unresolvedQueries;
    }

    public Set<AtomicSentence> getMissingFacts() {
        return missingFacts;
    }

    public void addNewFactsToKb(List<AtomicSentence> facts) {

        for (AtomicSentence fact : facts) {

            if (!kb.getSentences().contains(fact)) {
                kb.tell(fact);
                Log.i(TAG, "addNewFactsToKb: Knowledge Base " + kb.getClauses());
            } else {
                Log.i(TAG, "addNewFactsToKb: " + fact.toString() + " already exists");
                Log.i(TAG, "addNewFactsToKb: Knowledge Base " + kb.getClauses());
            }
        }


    }

    public KnowledgeBase getKnowledgeBase() {
        return kb;
    }
}

