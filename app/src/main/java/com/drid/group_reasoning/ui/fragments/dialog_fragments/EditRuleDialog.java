package com.drid.group_reasoning.ui.fragments.dialog_fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.drid.group_reasoning.R;
import com.drid.group_reasoning.data.contracts.RuleContract.RuleEntry;
import com.drid.group_reasoning.data.contracts.RuleContract.RuleSymbolEntry;
import com.drid.group_reasoning.data.contracts.RuleContract.SymbolEntry;
import com.drid.group_reasoning.engine.knowledgebase.Clause;
import com.drid.group_reasoning.engine.knowledgebase.Literal;
import com.drid.group_reasoning.engine.parser.LogicParser;
import com.drid.group_reasoning.engine.parser.ast.Sentence;
import com.drid.group_reasoning.engine.parser.visitors.LiteralCollector;
import com.drid.group_reasoning.engine.parser.visitors.SetProposition;
import com.drid.group_reasoning.ui.adapters.ConnectiveAdapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.drid.group_reasoning.data.contracts.RuleContract.PATH_SYMBOL;
import static com.drid.group_reasoning.ui.fragments.RuleFragment.ARG_RULE_URI;


public class EditRuleDialog extends DialogFragment {

    public static final String TAG = EditRuleDialog.class.getSimpleName();
    private Toolbar toolbar;

    private EditText ruleEditText;
    private TextView resultTextView;

    private View resultLayout;

    private Button addPropositionButton;
    private Button translateButton;

    private ViewGroup viewGroup;

    private LogicParser logicParser;

    private Map<String, String> symbol_proposition;

    private Bundle arguments;
    private Uri currentUri;

    private EditText symbolEdit;
    private EditText propositionEdit;

    private RecyclerView connectives;
    private ConnectiveAdapter adapter;

    private boolean ruleIsChanged = false;

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            ruleIsChanged = true;
            return false;
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullscreenDialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setWindowAnimations(R.style.AppTheme_Slide);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_editor_rule, container, false);

        toolbar = view.findViewById(R.id.toolbar);

        ruleEditText = view.findViewById(R.id.edit_text_rule);
        resultLayout = view.findViewById(R.id.result_layout);
        resultTextView = view.findViewById(R.id.result);

        ruleEditText.setOnTouchListener(onTouchListener);

        viewGroup = view.findViewById(R.id.insertion_point);


        addPropositionButton = view.findViewById(R.id.add_propositions_button);

        translateButton = view.findViewById(R.id.translate);

        connectives = view.findViewById(R.id.connectives_recycler_view);

        connectives.setLayoutManager(new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false));

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ConnectiveAdapter(getContext());
        connectives.setAdapter(adapter);

        logicParser = new LogicParser();
        symbol_proposition = new LinkedHashMap<>();

        arguments = this.getArguments();

        if (arguments != null) {
            currentUri = Uri.parse(arguments.getString(ARG_RULE_URI));
        }


        if (currentUri == null) {
            toolbar.setTitle("Add a Rule");
        } else {
            toolbar.setTitle("Edit a Rule");
            reloadData();
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ruleIsChanged) {
                    dismiss();
                } else {
                    displayDiscardAlertDialog();
                }
            }
        });

        toolbar.inflateMenu(R.menu.editor_options_menu);

        if (currentUri == null) {
            MenuItem deleteMenuItem = toolbar.getMenu().findItem(R.id.delete);
            deleteMenuItem.setVisible(false);
        }

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.save:
                        checkRuleValidity();
                        return true;
                    case R.id.delete:
                        displayDeleteAlertDialog();
                        return true;
                }
                return false;
            }


        });

        addPropositionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addProposition(viewGroup.getChildCount());
            }
        });


        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translate();
            }
        });

        adapter.setOnItemClickListener(new ConnectiveAdapter.ClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                int start = ruleEditText.getSelectionStart(); //this is to get the the cursor position
                String s = adapter.getSymbol(position);
                ruleEditText.getText().insert(start, s);
            }
        });

    }

    private void displayDiscardAlertDialog() {
        AlertDialog dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AlertDialog))
                .setTitle("Do you want to discard changes ?")
                .setPositiveButton("Keep Editing", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                }).create();

        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
    }

    private void displayDeleteAlertDialog() {
        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AlertDialog))
                .setTitle("Are you sure you want to delete this rule ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteRule();
                        dismiss();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();

        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

    }


    private void addProposition(int position) {
        viewGroup.setVisibility(View.VISIBLE);
        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.layout_add_propositions, null);
        ImageButton removeProposition = view.findViewById(R.id.remove_proposition_button);
        removeProposition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewGroup.removeView(view);
            }
        });
        viewGroup.addView(view, position);

        symbolEdit = getSymbolEditText(view);
        propositionEdit = getPropositionEditText(view);

        symbolEdit.setOnTouchListener(onTouchListener);
        propositionEdit.setOnTouchListener(onTouchListener);

        symbolEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String symbol = symbolEdit.getText().toString();
                String proposition = searchProposition(symbol);
                if (!TextUtils.isEmpty(symbol) && !TextUtils.isEmpty(proposition)) {
                    propositionEdit.setText(proposition);
                } else {
                    propositionEdit.setText("");
                }
            }
        });

    }

    private void translate() {
        Sentence sentence = null;

        if (!TextUtils.isEmpty(ruleEditText.getText())) {
            sentence = logicParser.parse(ruleEditText.getText().toString());
        } else {
            Toast.makeText(getContext(),
                    "Please type in a rule",
                    Toast.LENGTH_SHORT)
                    .show();
        }

        for (Map.Entry<String, String> entry : getSymbolProposition().entrySet()) {
            sentence.accept(new SetProposition(entry.getKey(), entry.getValue()));
        }


        if (sentence != null && sentence.getProposition() != null) {
            String nlSentence = sentence.getProposition();
            if (!TextUtils.isEmpty(nlSentence)) {
                resultLayout.setVisibility(View.VISIBLE);
                resultTextView.setText(new StringBuilder()
                        .append(nlSentence.substring(0, 1).toUpperCase())
                        .append(nlSentence.substring(1))
                        .toString());
            }
        } else {
            Toast.makeText(getContext(),
                    "Something went wrong with translation",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }


    private void checkRuleValidity() {
        String rulePl = ruleEditText.getText().toString().trim();
        String ruleNl = resultTextView.getText().toString();

        Sentence sentence = logicParser.parse(rulePl);
        Log.i(TAG, "checkRuleValidity: sentence: " + sentence);

        Sentence cnf = Sentence.convertToCnf(sentence);

        Log.i(TAG, "checkRuleValidity: cnf sentence: " + cnf);

        List<Literal> literals = LiteralCollector.getLiterals(cnf);

        Log.i(TAG, "checkRuleValidity: literals: " + literals);

        Clause clause = new Clause(literals);

        Log.i(TAG, "checkRuleValidity: " + clause);


        saveRule(rulePl, ruleNl);
        dismiss();
    }


    private void saveRule(String rulePl, String ruleNl) {

        if (currentUri == null) {
            insertRule(rulePl, ruleNl, getSymbolProposition());
        } else {
            updateRule(currentUri, rulePl, ruleNl, getSymbolProposition());
        }

    }

    private void insertRule(String rulePl, String ruleNl, Map<String, String> symbolProposition) {
        ContentValues rules = new ContentValues();

        rules.put(RuleEntry.COLUMN_RULE_PL, rulePl);
        rules.put(RuleEntry.COLUMN_RULE_NL, ruleNl);

        Uri newRuleUri = getActivity().getContentResolver().insert(RuleEntry.RULES_URI, rules);

        insertSymbols(symbolProposition, newRuleUri);
    }

    private void insertSymbols(Map<String, String> symbolProposition, Uri newRuleUri) {

        for (Map.Entry<String, String> entry : symbolProposition.entrySet()) {
            ContentValues symbols = new ContentValues();
            symbols.put(SymbolEntry.COLUMN_SYMBOL, entry.getKey());
            symbols.put(SymbolEntry.COLUMN_PROPOSITION, entry.getValue());
            Uri newSymbolUri = getActivity().getContentResolver()
                    .insert(SymbolEntry.SYMBOLS_URI, symbols);
            if (newSymbolUri == null) {

                Uri symbolUri =
                        SymbolEntry.SYMBOLS_URI.buildUpon().appendPath(PATH_SYMBOL).build();


                Cursor symbolCursor = getActivity().getContentResolver().query(
                        symbolUri,
                        new String[]{SymbolEntry._ID},
                        SymbolEntry.COLUMN_SYMBOL + "=?",
                        new String[]{symbols.getAsString(SymbolEntry.COLUMN_SYMBOL)},

                        null
                );

                if (symbolCursor.moveToFirst()) {
                    int symbol_id =
                            symbolCursor.getInt(symbolCursor.getColumnIndex(SymbolEntry._ID));
                    newSymbolUri =
                            ContentUris.withAppendedId(SymbolEntry.SYMBOLS_URI, symbol_id);
                }
            }
            insertRuleSymbol(newRuleUri, newSymbolUri);
        }
    }

    private void insertRuleSymbol(Uri newRuleUri, Uri newSymbolUri) {
        ContentValues rules_symbols = new ContentValues();

        long rule_id = ContentUris.parseId(newRuleUri);
        long symbol_id = ContentUris.parseId(newSymbolUri);


        rules_symbols.put(RuleSymbolEntry.KEY_RULE_ID, rule_id);
        rules_symbols.put(RuleSymbolEntry.KEY_SYMBOL_ID, symbol_id);

        getActivity().getContentResolver()
                .insert(RuleSymbolEntry.RULE_SYMBOL_URI, rules_symbols);

    }

    private void updateRule(
            Uri currentUri, String rulePl, String ruleNl, Map<String, String> symbol_proposition) {


        ContentValues rules = new ContentValues();

        rules.put(RuleEntry.COLUMN_RULE_PL, rulePl);
        rules.put(RuleEntry.COLUMN_RULE_NL, ruleNl);

        getActivity().getContentResolver().update(
                this.currentUri, rules, null, null);

        getActivity().getContentResolver().delete(
                RuleSymbolEntry.RULE_SYMBOL_URI,
                RuleSymbolEntry.KEY_RULE_ID + "=?",
                new String[]{String.valueOf(ContentUris.parseId(currentUri))}
        );
        insertSymbols(symbol_proposition, currentUri);
    }

    private void deleteRule() {

        getActivity().getContentResolver().delete(
                RuleSymbolEntry.RULE_SYMBOL_URI,
                RuleSymbolEntry.KEY_RULE_ID + "=?",
                new String[]{String.valueOf(currentUri)}
        );

        getActivity().getContentResolver().delete(currentUri, null, null);
    }

    public Map<String, String> getSymbolProposition() {

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);

            String symbol = getSymbolEditText(child).getText().toString();
            String proposition = getPropositionEditText(child).getText().toString();
            symbol_proposition.put(symbol, proposition);
        }

        return symbol_proposition;
    }

    private String searchProposition(String symbol) {

        String result = null;
        Uri symbolUri = SymbolEntry.SYMBOLS_URI.buildUpon().appendPath(PATH_SYMBOL).build();

        Cursor symbolCursor = getActivity().getContentResolver().query(
                symbolUri,
                null,
                SymbolEntry.COLUMN_SYMBOL + "=?",
                new String[]{symbol},
                null
        );

        if (symbolCursor.moveToFirst()) {
            result = symbolCursor.getString(symbolCursor.getColumnIndex(SymbolEntry.COLUMN_PROPOSITION));
        }

        return result;
    }

    private void reloadData() {

        long id = ContentUris.parseId(currentUri);

        Cursor ruleCursor = getActivity().getContentResolver().query(
                currentUri,
                new String[]{
                        RuleEntry.COLUMN_RULE_PL, RuleEntry.COLUMN_RULE_NL
                },
                RuleEntry._ID + "=?",
                new String[]{String.valueOf(id)},
                null
        );

        if (ruleCursor.moveToFirst()) {
            ruleEditText.setText(
                    ruleCursor.getString(ruleCursor.getColumnIndex(RuleEntry.COLUMN_RULE_PL)));
            resultLayout.setVisibility(View.VISIBLE);
            resultTextView.setText(
                    ruleCursor.getString(ruleCursor.getColumnIndex(RuleEntry.COLUMN_RULE_NL)));
        }

        ruleCursor.close();


        Cursor joinedTablesCursor = getActivity().getContentResolver().query(
                RuleSymbolEntry.RULE_SYMBOL_URI,
                new String[]{SymbolEntry.COLUMN_SYMBOL, SymbolEntry.COLUMN_PROPOSITION},
                RuleSymbolEntry.KEY_RULE_ID + "=?",
                new String[]{String.valueOf(id)},
                null
        );

        try {
            while (joinedTablesCursor.moveToNext()) {
                int index = joinedTablesCursor.getPosition();
                addProposition(viewGroup.getChildCount());
                symbolEdit = viewGroup.getChildAt(index).findViewById(R.id.symbol_edit);
                propositionEdit = viewGroup.getChildAt(index).findViewById(R.id.proposition_edit);

                symbolEdit.setText(
                        joinedTablesCursor.getString(
                                joinedTablesCursor.getColumnIndex(SymbolEntry.COLUMN_SYMBOL)));
                propositionEdit.setText(
                        joinedTablesCursor.getString(
                                joinedTablesCursor.getColumnIndex(SymbolEntry.COLUMN_PROPOSITION)));
            }
        } finally {
            joinedTablesCursor.close();
        }
    }

    private EditText getSymbolEditText(View view) {
        return view.findViewById(R.id.symbol_edit);
    }

    private EditText getPropositionEditText(View view) {
        return view.findViewById(R.id.proposition_edit);
    }
}
