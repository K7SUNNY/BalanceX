package com.accounting.balancex;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

//class for transactions management
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        // Handle Touch & Hover Effect
        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: // When user touches the card
                    holder.hoverMessage.setVisibility(View.VISIBLE);
                    holder.transactionCard.setBackground(ContextCompat.getDrawable(v.getContext(), R.drawable.rounded_rectangular_gray)); // Grey background
                    break;

                case MotionEvent.ACTION_UP:   // When user lifts their finger
                case MotionEvent.ACTION_CANCEL: // If the touch is canceled
                    holder.hoverMessage.setVisibility(View.GONE);
                    holder.transactionCard.setBackground(ContextCompat.getDrawable(v.getContext(), R.drawable.rounded_rectangle));
                    break;
            }
            return true;
        });
        holder.transactionCard.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler = new Handler();
            private boolean isLongPress = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isLongPress = false; // Reset flag
                        holder.hoverMessage.setVisibility(View.VISIBLE);
                        holder.transactionCard.setBackground(ContextCompat.getDrawable(v.getContext(), R.drawable.rounded_rectangular_gray));

                        // Start long press detection
                        handler.postDelayed(() -> {
                            isLongPress = true; // Mark long press as true
                            showTransactionDetails(transaction, v.getContext()); // Show popup after 1500ms
                        }, 500);
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        holder.hoverMessage.setVisibility(View.GONE);
                        holder.transactionCard.setBackground(ContextCompat.getDrawable(v.getContext(), R.drawable.rounded_rectangle));

                        // Cancel long press if user lifts the finger early
                        handler.removeCallbacksAndMessages(null);
                        break;
                }
                return true;
            }
        });
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
        TextView hoverMessage;
        FrameLayout transactionCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textUTR = itemView.findViewById(R.id.textUTR);
            textPaymentMethod = itemView.findViewById(R.id.textPaymentMethod);
            textType = itemView.findViewById(R.id.textType);

            textReceiver = itemView.findViewById(R.id.textReceiverName);
            textAmount = itemView.findViewById(R.id.textAmount);
            textDate = itemView.findViewById(R.id.textDate);
            iconChat = itemView.findViewById(R.id.iconChat);
            hoverMessage = itemView.findViewById(R.id.hoverMessage);
            transactionCard = itemView.findViewById(R.id.transactionCard);
        }
    }
    private void showTransactionDetails(Transaction transaction, Context context) {
        // Create a dialog
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_transaction_details); // Your XML layout

        // Set translucent background
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Find Views
        TextView detailDate = dialog.findViewById(R.id.detailDate);
        TextView detailReceiver = dialog.findViewById(R.id.detailReceiver);
        TextView detailAmount = dialog.findViewById(R.id.detailAmount);
        TextView detailTransactionID = dialog.findViewById(R.id.detailTransactionID);
        TextView detailPaymentMethod = dialog.findViewById(R.id.detailPaymentMethod);
        TextView detailUTR = dialog.findViewById(R.id.detailUTR);
        ImageView closeBtn = dialog.findViewById(R.id.closeButton);
        ImageView copyTransactionID = dialog.findViewById(R.id.detailTransactionID_copy);
        ImageView copyUTR = dialog.findViewById(R.id.detailUTR_copy);
        TextView detailComments = dialog.findViewById(R.id.detailComments);
        TextView detailDescription = dialog.findViewById(R.id.detailDescription);
        TextView detailCategory = dialog.findViewById(R.id.detailCategory);

        // Set Transaction Data
        detailDate.setText(transaction.getDate());
        detailReceiver.setText(transaction.getReceiverName());
        detailAmount.setText("₹" + transaction.getAmount());
        detailPaymentMethod.setText(transaction.getPaymentMethod());
        detailUTR.setText(transaction.getUtr());
        detailTransactionID.setText(transaction.getTransactionID());
        detailComments.setText(transaction.getComments());
        detailDescription.setText(transaction.getDescription());
        detailCategory.setText(transaction.getCategory());


        // Close button action
        closeBtn.setOnClickListener(v -> dialog.dismiss());

        // Copy Transaction ID
        copyTransactionID.setOnClickListener(v -> {
            copyToClipboard(context, "Transaction ID", transaction.getTransactionID());
        });

        // Copy UTR
        copyUTR.setOnClickListener(v -> {
            copyToClipboard(context, "UTR", transaction.getUtr());
        });

        // Show the dialog
        dialog.show();
    }
    private void copyToClipboard(Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, label + " copied!", Toast.LENGTH_SHORT).show();
    }

}
