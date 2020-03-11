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
import com.drid.group_reasoning.data.contracts.RuleContract.RuleEntry;

public class RuleAdapter extends RecyclerView.Adapter<RuleAdapter.RuleViewHolder> {

    private Context context;
    private Cursor cursor;


    private static ClickListener listener;


    public RuleAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @NonNull
    @Override
    public RuleViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view =
                LayoutInflater
                        .from(context)
                        .inflate(R.layout.list_item_knowledge, viewGroup, false);
        return new RuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RuleViewHolder ruleViewHolder, int i) {
        if (!cursor.moveToPosition(i)) {
            return;
        }

        int id = cursor.getInt(cursor.getColumnIndex(RuleEntry._ID));
        String ruleNl = cursor.getString(cursor.getColumnIndex(RuleEntry.COLUMN_RULE_NL));
        String rulePL = cursor.getString(cursor.getColumnIndex(RuleEntry.COLUMN_RULE_PL));

        ruleViewHolder.id = id;
        ruleViewHolder.ruleNlTextView.setText(ruleNl);
        ruleViewHolder.rulePlTextView.setText(rulePL);

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

    public void setOnItemClickListener(ClickListener listener) {
        RuleAdapter.listener = listener;
    }

    public class RuleViewHolder extends RecyclerView.ViewHolder {

        private int id;
        private TextView ruleNlTextView;
        private TextView rulePlTextView;

        public RuleViewHolder(@NonNull final View itemView) {
            super(itemView);
            ruleNlTextView = itemView.findViewById(R.id.data_nl);
            rulePlTextView = itemView.findViewById(R.id.data_pl);

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
        void onItemClick(View v, int id);
    }
}
