package com.accounting.balancex;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private final Context context;
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

        boolean isCredit = transaction.getTransactionType() != null &&
                transaction.getTransactionType().equalsIgnoreCase("Credit");

        // 1. Icon Squircle and Type Styling
        if (isCredit) {
            holder.cardIconContainer.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.finance_income_container));
            holder.iconTransactionType.setImageResource(R.drawable.ic_arrow_down_left);
            holder.iconTransactionType.setColorFilter(
                    ContextCompat.getColor(context, R.color.finance_income));

            holder.textType.setText("Credit");
            holder.textType.setTextColor(ContextCompat.getColor(context, R.color.finance_income));
            holder.textType.setBackground(null);

            String symbol = SettingsManager.getCurrencySymbol(context);
            holder.textAmount.setText("+" + symbol + formatAmount(transaction.getAmount()));
            holder.textAmount.setTextColor(ContextCompat.getColor(context, R.color.finance_income));
        } else {
            holder.cardIconContainer.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.finance_expense_container));
            holder.iconTransactionType.setImageResource(R.drawable.ic_arrow_up_right);
            holder.iconTransactionType.setColorFilter(
                    ContextCompat.getColor(context, R.color.finance_expense));

            holder.textType.setText("Debit");
            holder.textType.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.textType.setBackground(null);

            String symbol = SettingsManager.getCurrencySymbol(context);
            holder.textAmount.setText("-" + symbol + formatAmount(transaction.getAmount()));
            holder.textAmount.setTextColor(ContextCompat.getColor(context, R.color.finance_expense));
        }

        // 2. Receiver & Metadata
        String receiverName = transaction.getReceiverName();
        if (receiverName == null || receiverName.trim().isEmpty()) {
            receiverName = "Unknown";
        }
        holder.textReceiverName.setText(receiverName);

        // 3. Category & Date
        String category = transaction.getCategory();
        if (category == null || category.trim().isEmpty()) {
            category = "General";
        }
        holder.textCategory.setText(category);

        String date = transaction.getDate();
        if (date == null || date.trim().isEmpty()) {
            date = "N/A";
        }
        holder.textDate.setText(date);

        // 4. Divider Visibility (hide for last item)
        if (holder.itemDivider != null) {
            holder.itemDivider.setVisibility(position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
        }

        // 5. Click Listeners
        holder.transactionCard.setOnClickListener(v -> showTransactionDetails(transaction, v.getContext()));

        holder.transactionCard.setOnLongClickListener(v -> {
            String utr = transaction.getUtr();
            String toCopy = (utr != null && !utr.trim().isEmpty() && !utr.equalsIgnoreCase("Unknown"))
                    ? utr : transaction.getTransactionID();
            if (toCopy != null && !toCopy.trim().isEmpty() && !toCopy.equalsIgnoreCase("N/A")) {
                copyToClipboard(context, "Reference", toCopy);
            } else {
                String symbol = SettingsManager.getCurrencySymbol(context);
                copyToClipboard(context, "Transaction", transaction.getReceiverName() + " - " + symbol + transaction.getAmount());
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return transactionList != null ? transactionList.size() : 0;
    }

    public void updateList(List<Transaction> newList) {
        this.transactionList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View transactionCard, itemDivider;
        MaterialCardView cardIconContainer;
        ImageView iconTransactionType;
        TextView textReceiverName, textCategory, textDate;
        TextView textAmount, textType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionCard = itemView.findViewById(R.id.transactionCard);
            cardIconContainer = itemView.findViewById(R.id.cardIconContainer);
            iconTransactionType = itemView.findViewById(R.id.iconTransactionType);
            textReceiverName = itemView.findViewById(R.id.textReceiverName);
            textCategory = itemView.findViewById(R.id.textCategory);
            textDate = itemView.findViewById(R.id.textDate);
            textAmount = itemView.findViewById(R.id.textAmount);
            textType = itemView.findViewById(R.id.textType);
            itemDivider = itemView.findViewById(R.id.itemDivider);
        }
    }

    private void showTransactionDetails(Transaction transaction, Context ctx) {
        BottomSheetDialog dialog = new BottomSheetDialog(ctx);
        View view = LayoutInflater.from(ctx).inflate(R.layout.bottom_sheet_transaction_details, null);
        dialog.setContentView(view);

        TextView bsTextType = view.findViewById(R.id.bsTextType);
        TextView bsTextAmount = view.findViewById(R.id.bsTextAmount);
        TextView bsTextReceiver = view.findViewById(R.id.bsTextReceiver);
        TextView bsTextDate = view.findViewById(R.id.bsTextDate);
        TextView bsTextPaymentMethod = view.findViewById(R.id.bsTextPaymentMethod);
        TextView bsTextCategory = view.findViewById(R.id.bsTextCategory);
        TextView bsTextUTR = view.findViewById(R.id.bsTextUTR);
        TextView bsTextTxnId = view.findViewById(R.id.bsTextTxnId);
        TextView bsTextComments = view.findViewById(R.id.bsTextComments);
        LinearLayout bsLayoutComments = view.findViewById(R.id.bsLayoutComments);

        ImageView bsCloseButton = view.findViewById(R.id.bsCloseButton);
        ImageView bsCopyUTR = view.findViewById(R.id.bsCopyUTR);
        ImageView bsCopyTxnId = view.findViewById(R.id.bsCopyTxnId);

        MaterialButton bsBtnContactHistory = view.findViewById(R.id.bsBtnContactHistory);
        MaterialButton bsBtnShare = view.findViewById(R.id.bsBtnShare);

        boolean isCredit = transaction.getTransactionType() != null &&
                transaction.getTransactionType().equalsIgnoreCase("Credit");

        String symbol = SettingsManager.getCurrencySymbol(ctx);
        if (isCredit) {
            bsTextType.setText("CREDIT / RECEIVED");
            bsTextType.setTextColor(ContextCompat.getColor(ctx, R.color.finance_income));
            bsTextType.setBackgroundResource(R.drawable.bg_credit);
            bsTextAmount.setText("+" + symbol + formatAmount(transaction.getAmount()));
            bsTextAmount.setTextColor(ContextCompat.getColor(ctx, R.color.finance_income));
        } else {
            bsTextType.setText("DEBIT / SENT");
            bsTextType.setTextColor(ContextCompat.getColor(ctx, R.color.finance_expense));
            bsTextType.setBackgroundResource(R.drawable.bg_debit);
            bsTextAmount.setText("-" + symbol + formatAmount(transaction.getAmount()));
            bsTextAmount.setTextColor(ContextCompat.getColor(ctx, R.color.finance_expense));
        }

        bsTextReceiver.setText(transaction.getReceiverName());
        bsTextDate.setText(transaction.getDate());
        bsTextPaymentMethod.setText(transaction.getPaymentMethod());
        bsTextCategory.setText(transaction.getCategory() != null && !transaction.getCategory().isEmpty()
                ? transaction.getCategory() : "General");

        String utr = transaction.getUtr();
        bsTextUTR.setText(utr != null && !utr.isEmpty() ? utr : "N/A");

        String txnId = transaction.getTransactionID();
        bsTextTxnId.setText(txnId != null && !txnId.isEmpty() ? txnId : "N/A");

        String comments = transaction.getComments();
        if (comments != null && !comments.trim().isEmpty()) {
            bsLayoutComments.setVisibility(View.VISIBLE);
            bsTextComments.setText(comments);
        } else if (transaction.getDescription() != null && !transaction.getDescription().trim().isEmpty()) {
            bsLayoutComments.setVisibility(View.VISIBLE);
            bsTextComments.setText(transaction.getDescription());
        } else {
            bsLayoutComments.setVisibility(View.GONE);
        }

        bsCloseButton.setOnClickListener(v -> dialog.dismiss());

        bsCopyUTR.setOnClickListener(v -> copyToClipboard(ctx, "UTR", bsTextUTR.getText().toString()));
        bsCopyTxnId.setOnClickListener(v -> copyToClipboard(ctx, "Transaction ID", bsTextTxnId.getText().toString()));

        bsBtnContactHistory.setOnClickListener(v -> {
            dialog.dismiss();
            openPersonHistory(transaction);
        });

        bsBtnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareBody = "BalanceX Transaction Receipt:\n" +
                    "Party: " + transaction.getReceiverName() + "\n" +
                    "Type: " + transaction.getTransactionType() + "\n" +
                    "Amount: " + symbol + transaction.getAmount() + "\n" +
                    "Date: " + transaction.getDate() + "\n" +
                    "Ref / UTR: " + transaction.getUtr() + "\n" +
                    "Payment Method: " + transaction.getPaymentMethod();
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Transaction Receipt");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            ctx.startActivity(Intent.createChooser(shareIntent, "Share Transaction via"));
        });

        dialog.show();
    }

    private void openPersonHistory(Transaction transaction) {
        ArrayList<Transaction> filteredTransactions = new ArrayList<>();
        if (transactionList != null) {
            for (Transaction t : transactionList) {
                if (t.getReceiverName().equalsIgnoreCase(transaction.getReceiverName())) {
                    filteredTransactions.add(t);
                }
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
            Toast.makeText(context, "No other transactions found for this person.", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyToClipboard(Context ctx, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        vibrate(ctx);
        Toast.makeText(ctx, label + " copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void vibrate(Context ctx) {
        SettingsManager.vibrate(ctx);
    }

    private String formatAmount(String amountStr) {
        try {
            double parsed = Double.parseDouble(amountStr);
            return String.format("%,.2f", parsed);
        } catch (Exception e) {
            return amountStr;
        }
    }
}
