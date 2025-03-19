package com.accounting.balancex;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

//
public class RecentTransactionsAdapter extends RecyclerView.Adapter<RecentTransactionsAdapter.RecentTransactionViewHolder> {

    private Context context;
    private List<com.accounting.balancex.RecentTransactionModel> recentTransactions;

    public RecentTransactionsAdapter(Context context, List<com.accounting.balancex.RecentTransactionModel> recentTransactions) {
        this.context = context;
        this.recentTransactions = recentTransactions;
    }

    public RecentTransactionsAdapter(List<com.accounting.balancex.RecentTransactionModel> recentTransactions) {
    }

    @NonNull
    @Override
    public RecentTransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_transaction_item, parent, false);
        return new RecentTransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentTransactionViewHolder holder, int position) {
        com.accounting.balancex.RecentTransactionModel transaction = recentTransactions.get(position);

        String textViewReceiver = transaction.getReceiver();
        if (textViewReceiver != null && textViewReceiver.length() > 6) {
            textViewReceiver = textViewReceiver.substring(0, 6) + " ";
        }
        holder.textViewReceiver.setText(textViewReceiver);

        // ✅ Format the date before displaying
        holder.textViewDate.setText(formatDate(transaction.getDate()));

        holder.textViewAmount.setText("₹" + transaction.getAmount());
        holder.textViewType.setText(transaction.getType());

        // Change text color based on transaction type
        if (transaction.getType().equalsIgnoreCase("Credit")) {
            holder.textViewType.setTextColor(Color.parseColor("#008000")); // Green for Credit
        } else if (transaction.getType().equalsIgnoreCase("Debit")) {
            holder.textViewType.setTextColor(Color.parseColor("#ff0000")); // Red for Debit
        }
    }

    private String formatDate(String dateStr) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM", Locale.getDefault()); // Changed "MMMM" to "MMM"

        try {
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateStr; // Return original if parsing fails
        }
    }

    @Override
    public int getItemCount() {
        return recentTransactions != null ? recentTransactions.size() : 0; // Prevent null crash
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

