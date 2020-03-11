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
import com.drid.group_reasoning.ui.adapters.FactAdapter;
import com.drid.group_reasoning.data.contracts.FactContract.FactEntry;
import com.drid.group_reasoning.ui.fragments.dialog_fragments.EditFactDialog;


public class FactFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = FactFragment.class.getSimpleName();

    public static final String ARG_FACT_URI = "fact_uri";

    private static final int FACT_LOADER = 1;

    private LinearLayout emptyList;
    private FloatingActionButton fab;
    private RecyclerView recyclerView;

    private FactAdapter factAdapter;

    private EditFactDialog dialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fact, container, false);

        emptyList = view.findViewById(R.id.empty_facts_list);

        recyclerView = view.findViewById(R.id.facts_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        factAdapter = new FactAdapter(getContext(), null);
        recyclerView.setAdapter(factAdapter);

        KnowledgeFragment knowledgeFragment = (KnowledgeFragment) getParentFragment();

        fab = knowledgeFragment.getFab();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        factAdapter.setOnClickListener(new FactAdapter.ClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Uri selectedUri = ContentUris.withAppendedId(FactEntry.FACT_URI, position);

                System.out.println(selectedUri.toString());
                Bundle arguments = new Bundle();
                arguments.putString(ARG_FACT_URI, String.valueOf(selectedUri));

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

        getLoaderManager().initLoader(FACT_LOADER, null, this);

        KnowledgeFragment knowledgeFragment = (KnowledgeFragment) getParentFragment();

        fab = knowledgeFragment.getFab();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog(null);
            }
        });

    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        String[] projection = {
                FactEntry._ID,
                FactEntry.COLUMN_FACT_SYMBOL,
                FactEntry.COLUMN_FACT_PROPOSITION};

        return new CursorLoader(
                getContext(),
                FactEntry.FACT_URI,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        factAdapter.swapCursor(cursor);
        emptyList.setVisibility(cursor.getCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        factAdapter.swapCursor(null);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.fact_options_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all_facts:
                displayDeleteAllFactsDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayDeleteAllFactsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AlertDialog))
                .setTitle("Are you sure you want to delete all facts ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAllFacts();
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

    private void deleteAllFacts() {
        getActivity().getContentResolver().delete(
                FactEntry.FACT_URI, null, null);
    }

    private void openDialog(Bundle arguments) {
        dialog = new EditFactDialog();
        dialog.show(getChildFragmentManager(), EditFactDialog.TAG);
        dialog.setArguments(arguments);
    }
}
