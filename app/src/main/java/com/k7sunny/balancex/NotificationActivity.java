package com.k7sunny.balancex;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {
    
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList = new ArrayList<>();
    private List<NotificationModel> filteredList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.applyTheme(this);
        setContentView(R.layout.activity_notification);

        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        // Back button functionality via the Toolbar
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Load notifications (dummy data)
        loadNotifications();

        // Default: Show Unread (false means it is not read)
        filterNotifications(false);

        // Handle Tab Selection
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Assuming Tab 0 is Unread, Tab 1 is Read
                boolean isReadTab = tab.getPosition() == 1;
                filterNotifications(isReadTab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadNotifications() {
        String sym = SettingsManager.getCurrencySymbol(this);
        notificationList.add(new NotificationModel("New Transaction", "You added " + sym + "500 to your balance.", false));
        notificationList.add(new NotificationModel("Payment Received", sym + "250 received from John.", false));
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

        if (adapter == null) {
            adapter = new NotificationAdapter(filteredList);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        // Handle empty state visibility
        if (filteredList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
}
