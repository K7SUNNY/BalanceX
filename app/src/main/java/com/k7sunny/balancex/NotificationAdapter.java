package com.k7sunny.balancex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.k7sunny.balancex.data.entity.NotificationEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface NotificationActionListener {
        void onNotificationClick(NotificationEntity item);
        void onActionClick(NotificationEntity item);
        void onDeleteClick(NotificationEntity item);
    }

    private final List<NotificationEntity> items;
    private final NotificationActionListener listener;

    public NotificationAdapter(List<NotificationEntity> items, NotificationActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationEntity item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.textTitle.setText(item.title);
        holder.textMessage.setText(item.message);
        holder.textTime.setText(formatRelativeTime(item.timestamp));

        // Read vs Unread styling
        if (item.isRead) {
            holder.dotUnread.setVisibility(View.GONE);
            holder.card.setAlpha(0.75f);
            holder.card.setStrokeColor(ContextCompat.getColor(context, R.color.border_subtle));
        } else {
            holder.dotUnread.setVisibility(View.VISIBLE);
            holder.card.setAlpha(1.0f);
            holder.card.setStrokeColor(ContextCompat.getColor(context, R.color.color_primary_container));
        }

        // Configure type badge, icon, and button action text
        configureTypeVisuals(context, holder, item);

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(item);
        });

        holder.btnAction.setOnClickListener(v -> {
            if (listener != null) listener.onActionClick(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    private void configureTypeVisuals(Context context, ViewHolder holder, NotificationEntity item) {
        String type = item.type != null ? item.type : NotificationEntity.TYPE_SYSTEM;

        int iconRes;
        int iconTint;
        int bgBadgeColor;
        String tagText;
        String actionBtnText;

        switch (type) {
            case NotificationEntity.TYPE_BILL:
                iconRes = R.drawable.ic_subscriptions;
                iconTint = ContextCompat.getColor(context, R.color.action_sub_tint);
                bgBadgeColor = ContextCompat.getColor(context, R.color.action_sub_bg);
                tagText = "BILL REMINDER";
                actionBtnText = "View Bill →";
                break;

            case NotificationEntity.TYPE_BUDGET:
                iconRes = R.drawable.ic_pulse;
                iconTint = ContextCompat.getColor(context, R.color.action_goals_tint);
                bgBadgeColor = ContextCompat.getColor(context, R.color.action_goals_bg);
                tagText = "BUDGET ALERT";
                actionBtnText = "Check Budget →";
                break;

            case NotificationEntity.TYPE_GOAL:
                iconRes = R.drawable.ic_target;
                iconTint = ContextCompat.getColor(context, R.color.action_security_tint);
                bgBadgeColor = ContextCompat.getColor(context, R.color.action_security_bg);
                tagText = "SAVINGS GOAL";
                actionBtnText = "View Goal →";
                break;

            case NotificationEntity.TYPE_TRANSACTION:
                boolean isIncome = item.message != null && item.message.toLowerCase(Locale.ROOT).contains("received");
                if (isIncome) {
                    iconRes = R.drawable.ic_arrow_down_left;
                    iconTint = ContextCompat.getColor(context, R.color.finance_income);
                    bgBadgeColor = ContextCompat.getColor(context, R.color.finance_income_container);
                    tagText = "INCOME";
                } else {
                    iconRes = R.drawable.ic_arrow_up_right;
                    iconTint = ContextCompat.getColor(context, R.color.finance_expense);
                    bgBadgeColor = ContextCompat.getColor(context, R.color.finance_expense_container);
                    tagText = "EXPENSE";
                }
                actionBtnText = "View Ledger →";
                break;

            case NotificationEntity.TYPE_REPORT:
                iconRes = R.drawable.ic_report;
                iconTint = ContextCompat.getColor(context, R.color.color_primary);
                bgBadgeColor = ContextCompat.getColor(context, R.color.color_primary_container);
                tagText = "STATEMENT";
                actionBtnText = "Open Reports →";
                break;

            default:
                iconRes = R.drawable.ic_notification_bell;
                iconTint = ContextCompat.getColor(context, R.color.color_primary);
                bgBadgeColor = ContextCompat.getColor(context, R.color.surface_input);
                tagText = "SYSTEM";
                actionBtnText = "Open App →";
                break;
        }

        holder.imageIcon.setImageResource(iconRes);
        holder.imageIcon.setColorFilter(iconTint);

        GradientDrawable badgeDrawable = new GradientDrawable();
        badgeDrawable.setShape(GradientDrawable.OVAL);
        badgeDrawable.setColor(bgBadgeColor);
        holder.layoutIconBadge.setBackground(badgeDrawable);

        holder.textTag.setText(tagText);
        holder.textTag.setTextColor(iconTint);

        if (item.actionTarget != null && !item.actionTarget.isEmpty()) {
            holder.btnAction.setText(actionBtnText);
            holder.btnAction.setVisibility(View.VISIBLE);
        } else {
            holder.btnAction.setVisibility(View.GONE);
        }
    }

    private String formatRelativeTime(long timestamp) {
        if (timestamp <= 0) return "";
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60_000L) {
            return "Just now";
        } else if (diff < 3600_000L) {
            long mins = diff / 60_000L;
            return mins + "m ago";
        } else if (diff < 24 * 3600_000L) {
            long hours = diff / 3600_000L;
            return hours + "h ago";
        } else if (diff < 48 * 3600_000L) {
            return "Yesterday";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final FrameLayout layoutIconBadge;
        final ImageView imageIcon;
        final TextView textTag;
        final TextView textTime;
        final View dotUnread;
        final TextView textTitle;
        final TextView textMessage;
        final TextView btnAction;
        final ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardNotification);
            layoutIconBadge = itemView.findViewById(R.id.layoutIconBadge);
            imageIcon = itemView.findViewById(R.id.imageNotifIcon);
            textTag = itemView.findViewById(R.id.textNotifTag);
            textTime = itemView.findViewById(R.id.textNotifTime);
            dotUnread = itemView.findViewById(R.id.dotUnreadIndicator);
            textTitle = itemView.findViewById(R.id.textNotifTitle);
            textMessage = itemView.findViewById(R.id.textNotifMessage);
            btnAction = itemView.findViewById(R.id.btnNotifAction);
            btnDelete = itemView.findViewById(R.id.btnDeleteNotif);
        }
    }
}
