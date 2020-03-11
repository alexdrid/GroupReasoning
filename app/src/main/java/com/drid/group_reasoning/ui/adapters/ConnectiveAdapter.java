package com.drid.group_reasoning.ui.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.drid.group_reasoning.R;

import java.util.ArrayList;
import java.util.List;

public class ConnectiveAdapter extends RecyclerView.Adapter<ConnectiveAdapter.ViewHolder> {

    private List<String> connectives;
    private Context context;
    private static ClickListener listener;

    public ConnectiveAdapter(Context context) {
        this.context = context;
        connectives = new ArrayList<>();
        connectives.add("(");
        connectives.add(")");
        connectives.add("¬");
        connectives.add("∧");
        connectives.add("∨");
        connectives.add("→");
        connectives.add("↔");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view =
                LayoutInflater
                        .from(context)
                        .inflate(R.layout.list_item_connective, viewGroup, false);
        return new ViewHolder(view);
    }


    public void setOnItemClickListener(ClickListener listener) {
        ConnectiveAdapter.listener = listener;
    }

    public String getSymbol(int i){
        return connectives.get(i);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        viewHolder.connective.setText(connectives.get(i));
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(v, viewHolder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return connectives.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView connective;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            connective = itemView.findViewById(R.id.connective);

        }
    }

    public interface ClickListener {
        void onItemClick(View v, int position);
    }
}
