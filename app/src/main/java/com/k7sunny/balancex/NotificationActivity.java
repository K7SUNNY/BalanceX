package com.k7sunny.balancex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.k7sunny.balancex.data.entity.NotificationEntity;
import com.k7sunny.balancex.data.repository.NotificationRepository;
import com.k7sunny.balancex.notifications.AppNotificationManager;
import com.k7sunny.balancex.ui.goals.GoalsBudgetsActivity;
import com.k7sunny.balancex.ui.subscriptions.SubscriptionsActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.NotificationActionListener {

    private enum FilterMode {
        ALL, UNREAD, BILLS, BUDGETS, TRANSACTIONS
    }

    private FilterMode currentFilter = FilterMode.ALL;

    private TextView textUnreadBadge;
    private ImageView btnMarkAllRead;
    private ImageView btnClearAll;
    private TextView chipFilterAll, chipFilterUnread, chipFilterBills, chipFilterBudgets, chipFilterTransactions;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private TextView textEmptyTitle, textEmptySubtitle;
    private MaterialButton btnEmptyAction;

    private NotificationRepository repository;
    private NotificationAdapter adapter;
    private final List<NotificationEntity> displayedNotifications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.applyTheme(this);
        setContentView(R.layout.activity_notification);

        View coordinator = findViewById(R.id.coordinator);
        if (coordinator != null) {
            ViewCompat.setOnApplyWindowInsetsListener(coordinator, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, 0);
                return windowInsets;
            });
        }

        repository = new NotificationRepository(this);

        initViews();
        setupFilterChips();
        setupActions();
        setupRecyclerView();

        checkAndRequestNotificationPermission();

        // Perform initial sync of system alerts (e.g. bills due, thresholds)
        AppNotificationManager.syncSystemAndActiveAlerts(this, () -> runOnUiThread(this::loadNotifications));
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        textUnreadBadge = findViewById(R.id.textUnreadBadge);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        btnClearAll = findViewById(R.id.btnClearAll);

        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterUnread = findViewById(R.id.chipFilterUnread);
        chipFilterBills = findViewById(R.id.chipFilterBills);
        chipFilterBudgets = findViewById(R.id.chipFilterBudgets);
        chipFilterTransactions = findViewById(R.id.chipFilterTransactions);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        textEmptyTitle = findViewById(R.id.textEmptyTitle);
        textEmptySubtitle = findViewById(R.id.textEmptySubtitle);
        btnEmptyAction = findViewById(R.id.btnEmptyAction);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.color_primary);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                AppNotificationManager.syncSystemAndActiveAlerts(this, () -> runOnUiThread(() -> {
                    loadNotifications();
                    swipeRefreshLayout.setRefreshing(false);
                }));
            });
        }

        if (btnEmptyAction != null) {
            btnEmptyAction.setOnClickListener(v -> {
                AppNotificationManager.syncSystemAndActiveAlerts(this, () -> runOnUiThread(this::loadNotifications));
                Toast.makeText(this, "Alerts refreshed", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupFilterChips() {
        chipFilterAll.setOnClickListener(v -> setFilter(FilterMode.ALL));
        chipFilterUnread.setOnClickListener(v -> setFilter(FilterMode.UNREAD));
        chipFilterBills.setOnClickListener(v -> setFilter(FilterMode.BILLS));
        chipFilterBudgets.setOnClickListener(v -> setFilter(FilterMode.BUDGETS));
        chipFilterTransactions.setOnClickListener(v -> setFilter(FilterMode.TRANSACTIONS));

        updateFilterChipsUI();
    }

    private void setFilter(FilterMode mode) {
        if (currentFilter != mode) {
            currentFilter = mode;
            updateFilterChipsUI();
            loadNotifications();
        }
    }

    private void updateFilterChipsUI() {
        int activeBg = R.drawable.rounded_corner_container_fliter_section;
        int inactiveBg = R.drawable.rounded_corner_unselected;

        int activeColor = ContextCompat.getColor(this, R.color.text_primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        chipFilterAll.setBackgroundResource(currentFilter == FilterMode.ALL ? activeBg : inactiveBg);
        chipFilterAll.setTextColor(currentFilter == FilterMode.ALL ? activeColor : inactiveColor);
        chipFilterAll.setTypeface(null, currentFilter == FilterMode.ALL ? Typeface.BOLD : Typeface.NORMAL);

        chipFilterUnread.setBackgroundResource(currentFilter == FilterMode.UNREAD ? activeBg : inactiveBg);
        chipFilterUnread.setTextColor(currentFilter == FilterMode.UNREAD ? activeColor : inactiveColor);
        chipFilterUnread.setTypeface(null, currentFilter == FilterMode.UNREAD ? Typeface.BOLD : Typeface.NORMAL);

        chipFilterBills.setBackgroundResource(currentFilter == FilterMode.BILLS ? activeBg : inactiveBg);
        chipFilterBills.setTextColor(currentFilter == FilterMode.BILLS ? activeColor : inactiveColor);
        chipFilterBills.setTypeface(null, currentFilter == FilterMode.BILLS ? Typeface.BOLD : Typeface.NORMAL);

        chipFilterBudgets.setBackgroundResource(currentFilter == FilterMode.BUDGETS ? activeBg : inactiveBg);
        chipFilterBudgets.setTextColor(currentFilter == FilterMode.BUDGETS ? activeColor : inactiveColor);
        chipFilterBudgets.setTypeface(null, currentFilter == FilterMode.BUDGETS ? Typeface.BOLD : Typeface.NORMAL);

        chipFilterTransactions.setBackgroundResource(currentFilter == FilterMode.TRANSACTIONS ? activeBg : inactiveBg);
        chipFilterTransactions.setTextColor(currentFilter == FilterMode.TRANSACTIONS ? activeColor : inactiveColor);
        chipFilterTransactions.setTypeface(null, currentFilter == FilterMode.TRANSACTIONS ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void setupActions() {
        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> {
                repository.markAllAsRead(() -> runOnUiThread(() -> {
                    Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
                    loadNotifications();
                }));
            });
        }

        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Clear Notifications")
                        .setMessage("Are you sure you want to delete all notifications? This cannot be undone.")
                        .setPositiveButton("Clear All", (d, w) -> {
                            repository.deleteAll(() -> runOnUiThread(() -> {
                                Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show();
                                loadNotifications();
                            }));
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(displayedNotifications, this);
        recyclerView.setAdapter(adapter);

        // Swipe-to-delete item touch helper
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getAdapterPosition();
                if (pos >= 0 && pos < displayedNotifications.size()) {
                    NotificationEntity item = displayedNotifications.get(pos);
                    repository.delete(item.id, () -> runOnUiThread(NotificationActivity.this::loadNotifications));
                    Toast.makeText(NotificationActivity.this, "Notification deleted", Toast.LENGTH_SHORT).show();
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void loadNotifications() {
        // First load unread count badge
        repository.getUnreadCount(count -> runOnUiThread(() -> {
            if (count > 0) {
                textUnreadBadge.setVisibility(View.VISIBLE);
                textUnreadBadge.setText(count + " Unread");
            } else {
                textUnreadBadge.setVisibility(View.GONE);
            }
        }));

        // Load items based on active filter
        switch (currentFilter) {
            case ALL:
                repository.getAll(this::renderList);
                break;
            case UNREAD:
                repository.getUnread(this::renderList);
                break;
            case BILLS:
                repository.getByTypes(Arrays.asList(NotificationEntity.TYPE_BILL, NotificationEntity.TYPE_REMINDER), this::renderList);
                break;
            case BUDGETS:
                repository.getByTypes(Arrays.asList(NotificationEntity.TYPE_BUDGET, NotificationEntity.TYPE_GOAL), this::renderList);
                break;
            case TRANSACTIONS:
                repository.getByType(NotificationEntity.TYPE_TRANSACTION, this::renderList);
                break;
        }
    }

    private void renderList(List<NotificationEntity> items) {
        runOnUiThread(() -> {
            displayedNotifications.clear();
            if (items != null) {
                displayedNotifications.addAll(items);
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            if (displayedNotifications.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
                configureEmptyState();
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
            }
        });
    }

    private void configureEmptyState() {
        switch (currentFilter) {
            case UNREAD:
                textEmptyTitle.setText("No Unread Alerts");
                textEmptySubtitle.setText("You're completely up to date with all financial alerts and reminders.");
                break;
            case BILLS:
                textEmptyTitle.setText("No Bill Reminders");
                textEmptySubtitle.setText("All your recurring subscriptions and scheduled payments are clear.");
                break;
            case BUDGETS:
                textEmptyTitle.setText("No Budget Warnings");
                textEmptySubtitle.setText("All category budgets are within limits and your goals are progressing well.");
                break;
            case TRANSACTIONS:
                textEmptyTitle.setText("No Transaction Alerts");
                textEmptySubtitle.setText("Recent transaction alerts and spending milestones will appear here.");
                break;
            default:
                textEmptyTitle.setText("All Caught Up!");
                textEmptySubtitle.setText("No notifications yet. We'll alert you about upcoming bills, budget limits, and financial milestones.");
                break;
        }
    }

    @Override
    public void onNotificationClick(NotificationEntity item) {
        if (item == null) return;
        if (!item.isRead) {
            item.isRead = true;
            repository.markAsRead(item.id, null);
            adapter.notifyDataSetChanged();
            loadNotifications();
        }
        handleActionRouting(item);
    }

    @Override
    public void onActionClick(NotificationEntity item) {
        if (item == null) return;
        if (!item.isRead) {
            item.isRead = true;
            repository.markAsRead(item.id, null);
            adapter.notifyDataSetChanged();
            loadNotifications();
        }
        handleActionRouting(item);
    }

    @Override
    public void onDeleteClick(NotificationEntity item) {
        if (item == null) return;
        repository.delete(item.id, () -> runOnUiThread(this::loadNotifications));
    }

    private void handleActionRouting(NotificationEntity item) {
        if (item.actionTarget == null) return;

        Intent intent = null;
        switch (item.actionTarget) {
            case NotificationEntity.ACTION_SUBSCRIPTIONS:
                intent = new Intent(this, SubscriptionsActivity.class);
                break;
            case NotificationEntity.ACTION_GOALS:
                intent = new Intent(this, GoalsBudgetsActivity.class);
                break;
            case NotificationEntity.ACTION_HISTORY:
                intent = new Intent(this, HistoryActivity.class);
                break;
            case NotificationEntity.ACTION_REPORTS:
                intent = new Intent(this, ReportsActivity.class);
                break;
            case NotificationEntity.ACTION_ENTRY:
                intent = new Intent(this, EntryActivity.class);
                break;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }
}
