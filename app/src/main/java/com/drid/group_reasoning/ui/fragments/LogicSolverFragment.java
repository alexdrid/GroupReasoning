package com.drid.group_reasoning.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.drid.group_reasoning.R;
import com.drid.group_reasoning.data.contracts.FactContract.FactEntry;
import com.drid.group_reasoning.engine.inference.InferenceEngine;
import com.drid.group_reasoning.engine.parser.ast.AtomicSentence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class LogicSolverFragment extends Fragment {

    public static final String TAG = LogicSolverFragment.class.getSimpleName();

    private OnFragmentInteractionListener callback;

    private TextView log;
    private Button resolve;

    private InferenceEngine engine;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            assert action != null;
            switch (action) {
                case "new_facts":
                    List<AtomicSentence> facts = intent.getParcelableArrayListExtra("facts");

                    if (!facts.isEmpty()) {
                        log.append("\nNew facts arrived\n");
                        for (AtomicSentence fact : facts) {
                            log.append("-" + fact.getProposition() + "\n");
                        }
                        addNewFacts(facts);
                    } else {
                        log.append("\nNo facts found in peers ");
                    }
                    break;
                case "no_connected_peers":
                    Log.i(TAG, "onReceive: No connected peers");
                    log.append("\nNo connected peers");
                    break;
                case "connected_peers":
                    Log.i(TAG, "onReceive: Connected peers");

                    List<String> connectedPeers = intent.getStringArrayListExtra("connected_peers_list");
                    log.append("\nConnected peers: " + connectedPeers);
                    break;
                case "log_message":
                    String log_message = intent.getStringExtra("message");
                    log.append(log_message + "\n");
            }

        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_logic_solver, container, false);


        log = view.findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());

        resolve = view.findViewById(R.id.solve);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        resolve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log.setText("");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initEngine();
                    }
                }, 200);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("new_facts");
        intentFilter.addAction("no_peer_left_ask");
        intentFilter.addAction("connected_peers");
        intentFilter.addAction("no_connected_peers");
        intentFilter.addAction("new_query");
        intentFilter.addAction("log_message");

        getActivity().registerReceiver(this.receiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(receiver);
    }

    private void initEngine() {

        engine = new InferenceEngine(getContext());

        engine.initKnowledgeBase();
        engine.solve();

        log.append("Resolved queries:\n");
        for (AtomicSentence query : engine.getResolvedQueries()) {
            log.append(" - " + query.getProposition() + "\n");
            Log.i(TAG, " " + query.getProposition());
            insertFact(query);
        }

        log.append("\nUnresolved queries:\n");
        for (AtomicSentence query : engine.getUnresolvedQueries()) {
            log.append(" - " + query.getProposition() + "\n");
        }
        log.append("\nMissing facts :\n");

        List<AtomicSentence> missingFacts = new ArrayList<>(engine.getMissingFacts());
        if (!missingFacts.isEmpty()) {

            for (AtomicSentence query : missingFacts) {
                log.append(" - " + query.getProposition() + "\n");
            }
            log.append("\nAsking peers for these facts:\n");
            for (AtomicSentence a : missingFacts) {
                log.append("- " + a.getProposition() + "\n");
            }
            askPeers(missingFacts);
        }

    }

    private void askPeers(List<AtomicSentence> missingFacts) {
        this.callback.askPeers(missingFacts);
    }

    private void addNewFacts(List<AtomicSentence> facts) {

        Log.i(TAG, "addNewFacts: Trying to solve again");


        for (Iterator<AtomicSentence> iterator = facts.iterator(); iterator.hasNext(); ) {
            AtomicSentence value = iterator.next();
            if (engine.getKnowledgeBase().getSentences().contains(value)) {
                Log.i(TAG, "addNewFacts: " + value + " already exists");
                iterator.remove();
            }
        }

        if (!facts.isEmpty()) {

            engine.addNewFactsToKb(facts);
            engine.solve();
            List<AtomicSentence> resolvedQueries = engine.getResolvedQueries();
            List<AtomicSentence> unResolvedQueries = engine.getUnresolvedQueries();
            Set<AtomicSentence> missingFacts = engine.getMissingFacts();

            Log.i(TAG, "addNewFacts: Resolving with new facts");
            Log.i(TAG, "addNewFacts: Resolved - " + resolvedQueries);
            log.append("\nResolving with new facts\n");
            log.append("\nResolved queries:\n");

            for (AtomicSentence query : resolvedQueries) {
                log.append("-" + query.getProposition() + "\n");
                insertFact(query);
            }

            log.append("\nUnresolved queries:\n ");
            Log.i(TAG, "addNewFacts: Unresolved queries - " + unResolvedQueries);
            for (AtomicSentence query : unResolvedQueries) {
                log.append("-" + query.getProposition() + "\n");
            }
            log.append("\nMissing facts:\n ");

            if (!missingFacts.isEmpty()) {
                Log.i(TAG, "addNewFacts: missing facts - " + missingFacts);
                for (AtomicSentence query : missingFacts) {
                    log.append("-" + query.getProposition() + "\n");
                }
            }
        }
    }

    private void insertFact(AtomicSentence fact) {

        ContentValues values = new ContentValues();
        values.put(FactEntry.COLUMN_FACT_PROPOSITION, fact.getSymbol());
        values.put(FactEntry.COLUMN_FACT_SYMBOL, fact.getProposition());
        getActivity().getContentResolver().insert(FactEntry.FACT_URI, values);
    }

    public void setOnFragmentCreatedListener(OnFragmentInteractionListener callback) {
        this.callback = callback;
    }

    public interface OnFragmentInteractionListener {
        void askPeers(List<AtomicSentence> missingFacts);
    }
}
