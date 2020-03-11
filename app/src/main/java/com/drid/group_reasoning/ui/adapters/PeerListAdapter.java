package com.drid.group_reasoning.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.drid.group_reasoning.R;
import com.drid.group_reasoning.data.contracts.PeerContract.PeerEntry;

public class PeerListAdapter extends RecyclerView.Adapter<PeerListAdapter.PeerViewHolder> {

    private Context context;
    private Cursor cursor;


    private ClickListener listener;

    public PeerListAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @NonNull
    @Override
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.list_item_peer, viewGroup, false);
        return new PeerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PeerViewHolder peerViewHolder, int i) {
        if (!cursor.moveToPosition(i)) {
            return;
        }

        int id = cursor.getInt(cursor.getColumnIndex(PeerEntry._ID));
        String name = cursor.getString(cursor.getColumnIndex(PeerEntry.COLUMN_PEER_NAME));
        String status = cursor.getString(cursor.getColumnIndex(PeerEntry.COLUMN_PEER_STATUS));


        peerViewHolder.id = id;
        peerViewHolder.nameTextView.setText(name);
        peerViewHolder.statusTextView.setText(status);

    }

    @Override
    public int getItemCount() {
        return (cursor == null) ? 0 : cursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {

        cursor = newCursor;

        if (newCursor != null) {
            notifyDataSetChanged();
        }
    }


    public void setOnClickListener(ClickListener listener) {
        this.listener = listener;
    }

    public class PeerViewHolder extends RecyclerView.ViewHolder {

        public int id;
        TextView nameTextView;
        TextView statusTextView;


        public PeerViewHolder(@NonNull final View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.device_name);
            statusTextView = itemView.findViewById(R.id.status);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(v, id);
                }
            });
        }
    }


    public interface ClickListener {
        void onItemClick(View v, int id);
    }
}
