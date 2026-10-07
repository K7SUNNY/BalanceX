package com.accounting.balancex.ui.subscriptions;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.accounting.balancex.R;
import com.accounting.balancex.SettingsManager;
import com.accounting.balancex.data.entity.SubscriptionEntity;
import com.accounting.balancex.data.repository.SubscriptionRepository;
import com.accounting.balancex.worker.SubscriptionWorker;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubscriptionsActivity extends AppCompatActivity {

    private SubscriptionRepository repository;
    private LinearLayout containerItems;
    private View layoutEmpty;
    private View cardSubscriptionsList;
    private TextView textTotalCommitment;
    private TextView textActiveCount;
    private TextView textDueThisWeek;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.applyTheme(this);
        setContentView(R.layout.activity_subscriptions);

        setupWindowInsets();

        repository = new SubscriptionRepository(this);

        containerItems = findViewById(R.id.containerSubscriptionItems);
        layoutEmpty = findViewById(R.id.layoutEmptySubscriptionsView);
        cardSubscriptionsList = findViewById(R.id.cardSubscriptionsList);
        textTotalCommitment = findViewById(R.id.textTotalCommitmentAmount);
        textActiveCount = findViewById(R.id.textActiveSubsCount);
        textDueThisWeek = findViewById(R.id.textDueThisWeekCount);

        findViewById(R.id.btnBackSubscriptions).setOnClickListener(v -> finish());

        View.OnClickListener addListener = v -> showAddSubscriptionSheet();
        findViewById(R.id.btnAddSubscriptionTop).setOnClickListener(addListener);
        findViewById(R.id.btnAddNewSubscription).setOnClickListener(addListener);

        // Schedule daily background alert check via WorkManager
        SubscriptionWorker.scheduleDailyCheck(this);

        loadSubscriptions();
    }

    private void setupWindowInsets() {
        View root = findViewById(R.id.subscriptionsRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(root);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubscriptions();
    }

    private void loadSubscriptions() {
        repository.getAllSubscriptions(subscriptions -> runOnUiThread(() -> {
            if (subscriptions == null || subscriptions.isEmpty()) {
                if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                if (cardSubscriptionsList != null) cardSubscriptionsList.setVisibility(View.GONE);
                if (containerItems != null) containerItems.removeAllViews();
                updateCommitments(0, 0, 0);
                return;
            }

            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            if (cardSubscriptionsList != null) cardSubscriptionsList.setVisibility(View.VISIBLE);
            if (containerItems != null) {
                containerItems.removeAllViews();
                double totalMonthly = 0;
                int activeCount = 0;
                int dueThisWeek = 0;

                Calendar calNow = Calendar.getInstance();
                long nowMillis = calNow.getTimeInMillis();
                long oneWeekMillis = 7L * 24 * 60 * 60 * 1000;

                String symbol = SettingsManager.getCurrencySymbol(this);

                for (int i = 0; i < subscriptions.size(); i++) {
                    SubscriptionEntity sub = subscriptions.get(i);
                    if (sub.isActive) {
                        activeCount++;
                        if ("Yearly".equalsIgnoreCase(sub.billingCycle)) {
                            totalMonthly += sub.amount / 12.0;
                        } else if ("Weekly".equalsIgnoreCase(sub.billingCycle)) {
                            totalMonthly += sub.amount * 4.33;
                        } else {
                            totalMonthly += sub.amount;
                        }

                        try {
                            Date dueDate = sdf.parse(sub.nextDueDate);
                            if (dueDate != null) {
                                long diff = dueDate.getTime() - nowMillis;
                                if (diff >= 0 && diff <= oneWeekMillis) {
                                    dueThisWeek++;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    View itemView = getLayoutInflater().inflate(R.layout.item_subscription_manage, containerItems, false);
                    TextView textName = itemView.findViewById(R.id.textSubManageName);
                    TextView textAmount = itemView.findViewById(R.id.textSubManageAmount);
                    TextView textDue = itemView.findViewById(R.id.textSubManageDue);
                    TextView textCycle = itemView.findViewById(R.id.textSubManageCycle);
                    ImageView btnDelete = itemView.findViewById(R.id.btnDeleteSubscription);
                    View divider = itemView.findViewById(R.id.dividerSubManage);

                    if (textName != null) textName.setText(sub.name);
                    if (textAmount != null) {
                        textAmount.setText(symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount));
                    }
                    if (textDue != null) {
                        textDue.setText("Due " + sub.nextDueDate);
                    }
                    if (textCycle != null) {
                        textCycle.setText("• " + sub.billingCycle);
                    }
                    if (divider != null && i == subscriptions.size() - 1) {
                        divider.setVisibility(View.GONE);
                    }

                    if (btnDelete != null) {
                        btnDelete.setOnClickListener(v -> {
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Delete Subscription")
                                    .setMessage("Stop tracking " + sub.name + "?")
                                    .setPositiveButton("Delete", (d, w) -> {
                                        repository.delete(sub.id, this::loadSubscriptions);
                                        Toast.makeText(this, "Subscription removed", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });
                    }

                    containerItems.addView(itemView);
                }

                updateCommitments(totalMonthly, activeCount, dueThisWeek);
            }
        }));
    }

    private void updateCommitments(double totalMonthly, int activeCount, int dueThisWeek) {
        String symbol = SettingsManager.getCurrencySymbol(this);
        if (textTotalCommitment != null) {
            textTotalCommitment.setText(symbol + String.format(Locale.getDefault(), "%,.2f", totalMonthly) + " / mo");
        }
        if (textActiveCount != null) {
            textActiveCount.setText(activeCount + " Active Recurring Services");
        }
        if (textDueThisWeek != null) {
            textDueThisWeek.setText(dueThisWeek + " Due This Week");
        }
    }

    private void showAddSubscriptionSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_add_subscription, null);
        dialog.setContentView(view);

        TextInputEditText editName = view.findViewById(R.id.editSubName);
        TextInputEditText editAmount = view.findViewById(R.id.editSubAmount);
        TextInputEditText editDueDate = view.findViewById(R.id.editSubDueDate);
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupCycle);
        com.google.android.material.materialswitch.MaterialSwitch switchAuto = view.findViewById(R.id.switchAutoAdd);
        View btnSave = view.findViewById(R.id.btnSaveSubscription);

        // Default due date = 1 week from today
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        if (editDueDate != null) {
            editDueDate.setText(sdf.format(cal.getTime()));
            editDueDate.setOnClickListener(v -> {
                Calendar c = Calendar.getInstance();
                new DatePickerDialog(this, (dp, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth);
                    editDueDate.setText(sdf.format(picked.getTime()));
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String name = editName != null && editName.getText() != null ? editName.getText().toString().trim() : "";
                String amountStr = editAmount != null && editAmount.getText() != null ? editAmount.getText().toString().trim() : "";
                String dueDate = editDueDate != null && editDueDate.getText() != null ? editDueDate.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter service name", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (Exception e) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                String cycle = "Monthly";
                if (chipGroup != null) {
                    int selectedChipId = chipGroup.getCheckedChipId();
                    if (selectedChipId == R.id.chipYearly) {
                        cycle = "Yearly";
                    } else if (selectedChipId == R.id.chipWeekly) {
                        cycle = "Weekly";
                    }
                }

                boolean autoAdd = switchAuto != null && switchAuto.isChecked();

                SubscriptionEntity newSub = new SubscriptionEntity(name, amount, cycle, dueDate, "Subscription");
                newSub.autoAddTransaction = autoAdd;

                repository.insert(newSub, () -> {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        loadSubscriptions();
                        Toast.makeText(this, "Subscription saved!", Toast.LENGTH_SHORT).show();
                    });
                });
            });
        }

        dialog.show();
    }
}
