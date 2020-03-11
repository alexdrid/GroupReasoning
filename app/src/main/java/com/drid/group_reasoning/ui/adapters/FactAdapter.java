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
import com.drid.group_reasoning.data.contracts.FactContract.FactEntry;

public class FactAdapter extends RecyclerView.Adapter<FactAdapter.FactViewHolder> {

    private Context context;
    private Cursor cursor;

    private ClickListener listener;

    public FactAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @NonNull
    @Override
    public FactViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.list_item_knowledge, viewGroup, false);
        return new FactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FactViewHolder factViewHolder, int i) {
        if (!cursor.moveToPosition(i)) {
            return;
        }

        int id = cursor.getInt(cursor.getColumnIndex(FactEntry._ID));
        String factNl = cursor.getString(cursor.getColumnIndex(FactEntry.COLUMN_FACT_SYMBOL));
        String factPl = cursor.getString(cursor.getColumnIndex(FactEntry.COLUMN_FACT_PROPOSITION));

        factViewHolder.id = id;
        factViewHolder.factNlTextView.setText(factNl);
        factViewHolder.factPlTextView.setText(factPl);
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

    public class FactViewHolder extends RecyclerView.ViewHolder {

        private int id;
        private TextView factNlTextView;
        private TextView factPlTextView;

        public FactViewHolder(@NonNull final View itemView) {
            super(itemView);
            factNlTextView = itemView.findViewById(R.id.data_nl);
            factPlTextView = itemView.findViewById(R.id.data_pl);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(v, id);
                }
            });
        }

        public int getId() {
            return id;
        }
    }

    public interface ClickListener {
        void onItemClick(View v, int position);
    }
}
