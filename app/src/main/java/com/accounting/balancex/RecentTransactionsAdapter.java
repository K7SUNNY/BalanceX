package com.accounting.balancex;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecentTransactionsAdapter extends RecyclerView.Adapter<RecentTransactionsAdapter.RecentTransactionViewHolder> {

    private Context context;
    private List<RecentTransactionModel> recentTransactions;

    public RecentTransactionsAdapter(Context context, List<RecentTransactionModel> recentTransactions) {
        this.context = context;
        this.recentTransactions = recentTransactions;
    }

    @NonNull
    @Override
    public RecentTransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_transaction_item, parent, false);
        return new RecentTransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentTransactionViewHolder holder, int position) {
        RecentTransactionModel transaction = recentTransactions.get(position);

        String textViewReceiver = transaction.getReceiver();
        if (textViewReceiver != null && textViewReceiver.length() > 5) {
            textViewReceiver = textViewReceiver.substring(0, 5) + "...";
        }
        holder.textViewReceiver.setText(textViewReceiver);
        holder.textViewDate.setText(transaction.getDate());
        holder.textViewAmount.setText("₹" + transaction.getAmount());
        holder.textViewType.setText(transaction.getType());

        // Change text color based on transaction type
        if (transaction.getType().equalsIgnoreCase("Credit")) {
            holder.textViewType.setTextColor(Color.parseColor("#008000")); // Green for Credit
        } else if (transaction.getType().equalsIgnoreCase("Debit")) {
            holder.textViewType.setTextColor(Color.parseColor("#ff0000")); // Red for Debit
        }
    }

    @Override
    public int getItemCount() {
        return recentTransactions.size();
    }

    // ✅ Define ViewHolder class inside the adapter
    public static class RecentTransactionViewHolder extends RecyclerView.ViewHolder {
        TextView textViewReceiver, textViewDate, textViewAmount, textViewType;

        public RecentTransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewReceiver = itemView.findViewById(R.id.textViewReceiver);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            textViewAmount = itemView.findViewById(R.id.textViewAmount);
            textViewType = itemView.findViewById(R.id.textViewType);
        }
    }
}

