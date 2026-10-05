package com.accounting.balancex;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class TransactionDetailsAdapter extends RecyclerView.Adapter<TransactionDetailsAdapter.ViewHolder> {
    private Context context;
    private List<Transaction> transactionList;

    public TransactionDetailsAdapter(Context context, List<Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction_details, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        boolean isCredit = transaction.getTransactionType().equalsIgnoreCase("Credit");

        // Align bubbles like a chat app: Credit on the left (Received), Debit on the right (Sent)
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.cardBubble.getLayoutParams();
        if (isCredit) {
            params.gravity = Gravity.START;
            holder.textType.setText("Credit");
            holder.textType.setTextColor(ContextCompat.getColor(context, R.color.finance_income));
            holder.textType.setBackgroundResource(R.drawable.bg_credit);
        } else {
            params.gravity = Gravity.END;
            holder.textType.setText("Debit");
            holder.textType.setTextColor(ContextCompat.getColor(context, R.color.finance_expense));
            holder.textType.setBackgroundResource(R.drawable.bg_debit);
        }
        holder.cardBubble.setLayoutParams(params);

        String symbol = SettingsManager.getCurrencySymbol(context);
        holder.textAmount.setText(symbol + transaction.getAmount());
        holder.textDate.setText(transaction.getDate());
        holder.textPaymentMethod.setText(transaction.getPaymentMethod());
        
        if (transaction.getUtr() != null && !transaction.getUtr().isEmpty()) {
            holder.textUTR.setText("UTR: " + transaction.getUtr());
            holder.textUTR.setVisibility(View.VISIBLE);
        } else {
            holder.textUTR.setVisibility(View.GONE);
        }

        if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
            holder.textDescription.setText(transaction.getDescription());
            holder.textDescription.setVisibility(View.VISIBLE);
        } else {
            holder.textDescription.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return transactionList != null ? transactionList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardBubble;
        TextView textType, textAmount, textDate, textDescription, textPaymentMethod, textUTR;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBubble = itemView.findViewById(R.id.cardBubble);
            textType = itemView.findViewById(R.id.textType);
            textAmount = itemView.findViewById(R.id.textAmount);
            textDate = itemView.findViewById(R.id.textDate);
            textDescription = itemView.findViewById(R.id.textDescription);
            textPaymentMethod = itemView.findViewById(R.id.textPaymentMethod);
            textUTR = itemView.findViewById(R.id.textUTR);
        }
    }
}
