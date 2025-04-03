package com.accounting.balancex;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {
    private Button buttonRead, buttonUnread;
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList = new ArrayList<>();
    private List<NotificationModel> filteredList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        buttonRead = findViewById(R.id.buttonRead);
        buttonUnread = findViewById(R.id.buttonUnread);
        recyclerView = findViewById(R.id.recyclerViewNotifications);

        // Set default selection to Unread
        buttonUnread.setBackgroundResource(R.drawable.selected_title);
        buttonUnread.setBackgroundTintList(null); // This fixes the initial purple tint
        buttonRead.setBackgroundResource(0);
        buttonRead.setBackgroundTintList(null);

        // Back button functionality
        ImageView backButton = findViewById(R.id.imageViewbackbutton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Closes the notification activity and returns to the previous screen
            }
        });

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Load notifications (dummy data)
        loadNotifications();

        // Default: Show Unread
        filterNotifications(false);

        buttonUnread.setOnClickListener(v -> filterNotifications(false));
        buttonRead.setOnClickListener(v -> filterNotifications(true));
    }
    private void loadNotifications() {
        notificationList.add(new NotificationModel("New Transaction", "You added ₹500 to your balance.", false));
        notificationList.add(new NotificationModel("Payment Received", "₹250 received from John.", false));
        notificationList.add(new NotificationModel("Reminder", "Check your monthly report.", true));
        notificationList.add(new NotificationModel("Offer", "Get 10% cashback on your next transaction.", true));
    }

    private void filterNotifications(boolean showRead) {
        filteredList.clear();

        for (NotificationModel notification : notificationList) {
            if (notification.isRead() == showRead) {
                filteredList.add(notification);
            }
        }

        adapter = new NotificationAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        // 🔥 Update button background on selection
        if (showRead) {
            buttonRead.setBackgroundResource(R.drawable.selected_title);
            buttonUnread.setBackgroundResource(0);
        } else {
            buttonUnread.setBackgroundResource(R.drawable.selected_title);
            buttonRead.setBackgroundResource(0);
        }
    }
}
