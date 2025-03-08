package com.accounting.balancex;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private Context context;
    private List<Transaction> transactionList;

    public TransactionAdapter(Context context, List<Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        // Debugging Log to check what is being retrieved
        Log.d("TransactionDebug", "Transaction Type for " + transaction.getReceiverName() + ": " + transaction.getTransactionType());

        if (transaction.getTransactionType().equalsIgnoreCase("Credit")) {
            holder.textType.setText("Credit");
            holder.textType.setTextColor(context.getResources().getColor(R.color.green));
            holder.textType.setBackgroundResource(R.drawable.bg_credit);
        } else {
            holder.textType.setText("Debit");
            holder.textType.setTextColor(context.getResources().getColor(R.color.red));
            holder.textType.setBackgroundResource(R.drawable.bg_debit);
        }

        holder.textUTR.setText(transaction.getUtr());
        holder.textPaymentMethod.setText(transaction.getPaymentMethod());

        String receiverName = transaction.getReceiverName();
        if (receiverName.length() > 10) {
            receiverName = receiverName.substring(0, 10) + "...";
        }
        holder.textReceiver.setText(receiverName);
        holder.textAmount.setText("₹" + transaction.getAmount());
        holder.textDate.setText(transaction.getDate());

        holder.iconChat.setOnClickListener(v -> {
            ArrayList<Transaction> filteredTransactions = new ArrayList<>();
            for (Transaction t : transactionList) {
                if (t.getReceiverName().equals(transaction.getReceiverName())) {
                    filteredTransactions.add(t);
                }
            }

            if (!filteredTransactions.isEmpty()) {
                Intent intent = new Intent(context, TransactionDetailsActivity.class);
                Gson gson = new Gson();
                String transactionsJson = gson.toJson(filteredTransactions);

                intent.putExtra("transactions_json", transactionsJson);
                intent.putExtra("receiverName", transaction.getReceiverName());
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "No transactions found for this contact.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public void updateList(List<Transaction> newList) {
        transactionList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textUTR, textPaymentMethod, textType;
        TextView textReceiver, textAmount, textDate;
        ImageView iconChat;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textUTR = itemView.findViewById(R.id.textUTR);
            textPaymentMethod = itemView.findViewById(R.id.textPaymentMethod);
            textType = itemView.findViewById(R.id.textType);

            textReceiver = itemView.findViewById(R.id.textReceiverName);
            textAmount = itemView.findViewById(R.id.textAmount);
            textDate = itemView.findViewById(R.id.textDate);
            iconChat = itemView.findViewById(R.id.iconChat);
        }
    }
}
