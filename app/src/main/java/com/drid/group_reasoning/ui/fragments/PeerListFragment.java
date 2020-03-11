package com.drid.group_reasoning.ui.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
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
import android.widget.ProgressBar;

import com.drid.group_reasoning.R;
import com.drid.group_reasoning.ui.adapters.PeerListAdapter;
import com.drid.group_reasoning.data.contracts.PeerContract.PeerEntry;
import com.drid.group_reasoning.network.model.Peer;


public class PeerListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = PeerListFragment.class.getSimpleName();

    private OnFragmentInteractionListener callback;

    private static final int PEER_LOADER = 0;

    private PeerListAdapter peerListAdapter;

    private FloatingActionButton searchFab;
    private RecyclerView recyclerView;

    private LinearLayout emptyList;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_peer_list, container, false);

        emptyList = view.findViewById(R.id.empty_peer_list);

        progressBar = view.findViewById(R.id.progress_bar);

        recyclerView = view.findViewById(R.id.peers_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        searchFab = view.findViewById(R.id.search_fab);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        peerListAdapter = new PeerListAdapter(getActivity(), null);
        recyclerView.setAdapter(peerListAdapter);

        peerListAdapter.setOnClickListener(new PeerListAdapter.ClickListener() {
            @Override
            public void onItemClick(View v, int id) {

                Cursor cursor = getActivity().getContentResolver().query(
                        PeerEntry.PEER_URI,
                        null,
                        PeerEntry._ID + "=?",
                        new String[]{String.valueOf(id)},
                        null
                );

                Peer peer = new Peer();

                if (cursor.moveToFirst()) {
                    String peer_id =
                            cursor.getString(cursor.getColumnIndex(PeerEntry.COLUMN_PEER_ID));
                    String name =
                            cursor.getString(cursor.getColumnIndex(PeerEntry.COLUMN_PEER_NAME));
                    String status = cursor.getString(
                            cursor.getColumnIndex(PeerEntry.COLUMN_PEER_STATUS));
                    peer.setPeerId(peer_id);
                    peer.setName(name);
                    peer.setStatus(status);
                }

                cursor.close();

                System.out.println(peer);

                if (peer.getStatus().equals(Peer.AVAILABLE)) {
                    callback.connectToPeer(peer);
                } else if (peer.getStatus().equals(Peer.CONNECTED)) {
                    displayDisconnectDialog(peer);
                }

            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    searchFab.hide();
                } else {
                    searchFab.show();
                }
                super.onScrolled(recyclerView, dx, dy);

            }
        });


        searchFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.discoverPeers();
                emptyList.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(PEER_LOADER, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.peer_options_menu, menu);
        return;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all_peers:
                displayDisconnectFromAllPeersDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        String[] projection = {
                PeerEntry._ID,
                PeerEntry.COLUMN_PEER_ID,
                PeerEntry.COLUMN_PEER_NAME,
                PeerEntry.COLUMN_PEER_STATUS};

        return new CursorLoader(
                getContext(),
                PeerEntry.PEER_URI,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        peerListAdapter.swapCursor(cursor);

        if(cursor.getCount() == 0){
            emptyList.setVisibility(View.VISIBLE);
        }else{
            progressBar.setVisibility(View.GONE);
            emptyList.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        peerListAdapter.swapCursor(null);
    }

    private void deletePeer(String peer_id) {
        getActivity().getContentResolver().delete(
                PeerEntry.PEER_URI,
                PeerEntry.COLUMN_PEER_ID + "=?",
                new String[]{peer_id});

    }

    private void deleteAllPeers() {
        getActivity().getContentResolver()
                .delete(PeerEntry.PEER_URI, null, null);

    }

    private void disconnectFromAllPeers(){
        callback.disconnectFromAllPeers();
    }

    private void disconnectFromPeer(Peer peer) {
        callback.disconnectFromPeer(peer);
    }

    private void displayDisconnectDialog(final Peer peer) {
        AlertDialog dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AlertDialog))
                .setTitle("Are you sure you want to disconnect from " + peer.getName() + "?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deletePeer(peer.getPeerId());
                        disconnectFromPeer(peer);
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

    private void displayDisconnectFromAllPeersDialog() {
        AlertDialog dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AlertDialog))
                .setTitle("Are you sure you want to disconnect from all peers ? ")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAllPeers();
                        disconnectFromAllPeers();
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


    public void setOnFragmentCreatedListener(OnFragmentInteractionListener callback) {
        this.callback = callback;
    }

    public interface OnFragmentInteractionListener {
        void discoverPeers();

        void connectToPeer(Peer peer);

        void disconnectFromAllPeers();

        void disconnectFromPeer(Peer selectedPeer);
    }
}
