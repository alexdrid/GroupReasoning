package com.drid.group_reasoning.ui.fragments;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.drid.group_reasoning.R;
import com.drid.group_reasoning.ui.adapters.RuleAdapter;
import com.drid.group_reasoning.data.contracts.RuleContract.RuleEntry;
import com.drid.group_reasoning.data.contracts.RuleContract.RuleSymbolEntry;
import com.drid.group_reasoning.data.contracts.RuleContract.SymbolEntry;
import com.drid.group_reasoning.ui.fragments.dialog_fragments.EditRuleDialog;

public class RuleFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = RuleFragment.class.getSimpleName();

    public static final String ARG_RULE_URI = "rule_uri";

    private static final int RULE_LOADER = 0;

    private LinearLayout emptyList;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;

    private RuleAdapter ruleAdapter;

    private EditRuleDialog dialog;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_rule, container, false);

        emptyList = view.findViewById(R.id.empty_rule_list);

        recyclerView = view.findViewById(R.id.rule_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        ruleAdapter = new RuleAdapter(getContext(), null);
        recyclerView.setAdapter(ruleAdapter);

        ruleAdapter.setOnItemClickListener(new RuleAdapter.ClickListener() {
            @Override
            public void onItemClick(View v, int id) {

                Uri selectedUri = ContentUris.withAppendedId(RuleEntry.RULES_URI, id);

                Bundle arguments = new Bundle();
                arguments.putString(ARG_RULE_URI, String.valueOf(selectedUri));

                openDialog(arguments);
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    fab.hide();
                } else {
                    fab.show();
                }
                super.onScrolled(recyclerView, dx, dy);

            }
        });
    }


    @Override
    public void setUserVisibleHint(boolean isVisible) {
        super.setUserVisibleHint(isVisible);

        if (isVisible && isResumed()) {
            onResume();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!getUserVisibleHint()) {
            return;
        }

        getLoaderManager().initLoader(RULE_LOADER, null, this);

        KnowledgeFragment knowledgeFragment = (KnowledgeFragment) getParentFragment();

        fab = knowledgeFragment.getFab();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog(null);
            }
        });


    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.rule_options_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all_rules:
                displayDeleteAllRulesDialog();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayDeleteAllRulesDialog() {
        AlertDialog dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AlertDialog))
                .setTitle("Are you sure you want to delete all rules ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAllRules();
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

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        String[] projection = {
                RuleEntry._ID,
                RuleEntry.COLUMN_RULE_NL,
                RuleEntry.COLUMN_RULE_PL};

        return new CursorLoader(
                getContext(),
                RuleEntry.RULES_URI,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        ruleAdapter.swapCursor(cursor);
        emptyList.setVisibility(cursor.getCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        ruleAdapter.swapCursor(null);
    }


    private void deleteAllRules() {
        getActivity().getContentResolver()
                .delete(RuleEntry.RULES_URI, null, null);
        getActivity().getContentResolver()
                .delete(SymbolEntry.SYMBOLS_URI, null, null);
        getActivity().getContentResolver()
                .delete(RuleSymbolEntry.RULE_SYMBOL_URI,
                        null, null);
    }

    private void openDialog(final Bundle arguments) {

        Thread openDialogThread = new Thread(new Runnable() {
            @Override
            public void run() {
                dialog = new EditRuleDialog();
                dialog.show(getChildFragmentManager(), EditRuleDialog.TAG);
                dialog.setArguments(arguments);
            }
        });

        openDialogThread.start();

    }
}
