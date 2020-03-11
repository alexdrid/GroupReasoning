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
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.drid.group_reasoning.R;
import com.drid.group_reasoning.data.contracts.FactContract.FactEntry;
import com.drid.group_reasoning.data.contracts.RuleContract;

import static com.drid.group_reasoning.data.contracts.RuleContract.PATH_SYMBOL;
import static com.drid.group_reasoning.ui.fragments.FactFragment.ARG_FACT_URI;


public class EditFactDialog extends DialogFragment {

    public static final String TAG = EditFactDialog.class.getSimpleName();

    private Toolbar toolbar;

    private EditText symbolEdit;
    private EditText propositionEdit;


    private Bundle arguments;
    private Uri currentUri;

    private boolean factIsChanged = false;

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            factIsChanged = true;
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
        }
    }


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_editor_fact, container, false);

        toolbar = view.findViewById(R.id.toolbar);

        symbolEdit = view.findViewById(R.id.edit_text_fact_symbol);
        propositionEdit = view.findViewById(R.id.edit_text_fact_proposition);

        symbolEdit.setOnTouchListener(onTouchListener);
        propositionEdit.setOnTouchListener(onTouchListener);

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        arguments = this.getArguments();

        if (arguments != null) {
            currentUri = Uri.parse(arguments.getString(ARG_FACT_URI));
        }


        if (currentUri == null) {
            toolbar.setTitle("Add a Fact");
        } else {
            toolbar.setTitle("Edit a Fact");
            reloadData();
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!factIsChanged) {
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
                        saveFact();
                        dismiss();
                        return true;
                    case R.id.delete:
                        displayDeleteAlertDialog();
                        return true;
                }
                return false;
            }
        });


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

    private void saveFact() {

        String factVariable = symbolEdit.getText().toString().trim();
        String factProposition = propositionEdit.getText().toString();


        if (currentUri == null) {
            insertFact(factVariable, factProposition);

        } else {
            updateFact(factVariable, factProposition);
        }

    }

    private void insertFact(String factVariable, String factProposition) {
        ContentValues facts = new ContentValues();

        facts.put(FactEntry.COLUMN_FACT_PROPOSITION, factVariable);
        facts.put(FactEntry.COLUMN_FACT_SYMBOL, factProposition);

        getActivity().getContentResolver().insert(FactEntry.FACT_URI, facts);
    }

    private void updateFact(String factPl, String factNl) {
        ContentValues facts = new ContentValues();

        facts.put(FactEntry.COLUMN_FACT_PROPOSITION, factPl);
        facts.put(FactEntry.COLUMN_FACT_SYMBOL, factNl);

        getActivity().getContentResolver().update(
                this.currentUri, facts, null, null);
    }

    private void deleteFact() {
        getActivity().getContentResolver().delete(currentUri,
                null,
                null);
    }

    private void reloadData() {
        long id = ContentUris.parseId(currentUri);

        Cursor factCursor = getActivity().getContentResolver().query(
                currentUri,
                new String[]{FactEntry.COLUMN_FACT_PROPOSITION, FactEntry.COLUMN_FACT_SYMBOL},
                FactEntry._ID + "=?",
                new String[]{String.valueOf(id)},
                null
        );

        if (factCursor.moveToFirst()) {
            symbolEdit.setText(
                    factCursor.getString(factCursor.getColumnIndex(FactEntry.COLUMN_FACT_PROPOSITION)));
            propositionEdit.setText(
                    factCursor.getString(factCursor.getColumnIndex(FactEntry.COLUMN_FACT_SYMBOL)));
        }

        factCursor.close();

    }

    private String searchProposition(String symbol) {

        String result = null;
        Uri queryVarUri = RuleContract.SymbolEntry.SYMBOLS_URI.buildUpon().appendPath(PATH_SYMBOL).build();

        Cursor symbolCursor = getActivity().getContentResolver().query(
                queryVarUri,
                null,
                RuleContract.SymbolEntry.COLUMN_SYMBOL + "=?",
                new String[]{symbol},

                null
        );

        if (symbolCursor.moveToFirst()) {
            result = symbolCursor.getString(symbolCursor.getColumnIndex(RuleContract.SymbolEntry.COLUMN_PROPOSITION));
        }

        return result;
    }


    private void displayDiscardAlertDialog() {
        AlertDialog dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AlertDialog))
                .setTitle("Do you want to discard changes ?")
                .setPositiveButton("Keep Editing", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        deleteRule();
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
        AlertDialog dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AlertDialog))
                .setTitle("Are you sure you want to delete this fact ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteFact();
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

}
