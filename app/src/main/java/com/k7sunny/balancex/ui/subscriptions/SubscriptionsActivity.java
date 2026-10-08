package com.k7sunny.balancex.ui.subscriptions;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.k7sunny.balancex.R;
import com.k7sunny.balancex.SettingsManager;
import com.k7sunny.balancex.data.entity.SubscriptionEntity;
import com.k7sunny.balancex.data.repository.SubscriptionRepository;
import com.k7sunny.balancex.worker.SubscriptionWorker;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SubscriptionsActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 201;

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

        checkAndRequestNotificationPermission();

        loadSubscriptions();
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SubscriptionWorker.scheduleDailyCheck(this);
            }
        }
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
                    TextView textStatus = itemView.findViewById(R.id.textSubManageStatus);
                    TextView textCycle = itemView.findViewById(R.id.textSubManageCycle);
                    ImageView btnEdit = itemView.findViewById(R.id.btnEditSubscription);
                    ImageView btnDelete = itemView.findViewById(R.id.btnDeleteSubscription);
                    View divider = itemView.findViewById(R.id.dividerSubManage);

                    if (textName != null) textName.setText(sub.name);
                    if (textAmount != null) {
                        textAmount.setText(symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount));
                    }
                    if (textDue != null) {
                        textDue.setText(formatRelativeDueDate(sub.nextDueDate));
                    }
                    if (textStatus != null) {
                        textStatus.setVisibility(sub.isActive ? View.GONE : View.VISIBLE);
                    }
                    itemView.setAlpha(sub.isActive ? 1.0f : 0.6f);

                    if (textCycle != null) {
                        String cycleText = "• " + sub.billingCycle;
                        if (sub.reminderDaysBefore == 0) {
                            cycleText += " • Same day alert";
                        } else {
                            cycleText += " • " + sub.reminderDaysBefore + "d alert";
                        }
                        textCycle.setText(cycleText);
                    }
                    if (divider != null && i == subscriptions.size() - 1) {
                        divider.setVisibility(View.GONE);
                    }

                    if (btnEdit != null) {
                        btnEdit.setOnClickListener(v -> showSubscriptionSheet(sub));
                    }
                    itemView.setOnClickListener(v -> showSubscriptionSheet(sub));

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
        showSubscriptionSheet(null);
    }

    private void showSubscriptionSheet(@Nullable SubscriptionEntity existingSub) {
        boolean isEditing = existingSub != null;
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_add_subscription, null);
        dialog.setContentView(view);

        TextView textTitle = view.findViewById(R.id.textSheetSubscriptionTitle);
        TextInputEditText editName = view.findViewById(R.id.editSubName);
        TextInputEditText editAmount = view.findViewById(R.id.editSubAmount);
        TextInputEditText editDueDate = view.findViewById(R.id.editSubDueDate);
        ChipGroup chipGroupCycle = view.findViewById(R.id.chipGroupCycle);
        ChipGroup chipGroupReminder = view.findViewById(R.id.chipGroupReminder);
        MaterialSwitch switchActive = view.findViewById(R.id.switchIsActive);
        MaterialSwitch switchAuto = view.findViewById(R.id.switchAutoAdd);
        View btnSave = view.findViewById(R.id.btnSaveSubscription);

        if (isEditing) {
            if (textTitle != null) textTitle.setText("Edit Subscription");
            if (btnSave instanceof TextView) ((TextView) btnSave).setText("Save Changes");
            if (editName != null) editName.setText(existingSub.name);
            if (editAmount != null) editAmount.setText(String.format(Locale.US, "%.2f", existingSub.amount));
            if (editDueDate != null) editDueDate.setText(existingSub.nextDueDate != null ? existingSub.nextDueDate : "");

            if (chipGroupCycle != null) {
                if ("Yearly".equalsIgnoreCase(existingSub.billingCycle)) {
                    chipGroupCycle.check(R.id.chipYearly);
                } else if ("Weekly".equalsIgnoreCase(existingSub.billingCycle)) {
                    chipGroupCycle.check(R.id.chipWeekly);
                } else {
                    chipGroupCycle.check(R.id.chipMonthly);
                }
            }

            if (chipGroupReminder != null) {
                if (existingSub.reminderDaysBefore == 0) {
                    chipGroupReminder.check(R.id.chipRemindSameDay);
                } else if (existingSub.reminderDaysBefore == 1) {
                    chipGroupReminder.check(R.id.chipRemind1Day);
                } else if (existingSub.reminderDaysBefore == 3) {
                    chipGroupReminder.check(R.id.chipRemind3Days);
                } else if (existingSub.reminderDaysBefore == 7) {
                    chipGroupReminder.check(R.id.chipRemind7Days);
                } else {
                    chipGroupReminder.check(R.id.chipRemind2Days);
                }
            }

            if (switchActive != null) switchActive.setChecked(existingSub.isActive);
            if (switchAuto != null) switchAuto.setChecked(existingSub.autoAddTransaction);
        } else {
            if (textTitle != null) textTitle.setText("Add Recurring Subscription");
            if (btnSave instanceof TextView) ((TextView) btnSave).setText("Save Subscription");
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 7);
            if (editDueDate != null) {
                editDueDate.setText(sdf.format(cal.getTime()));
            }
            if (chipGroupCycle != null) chipGroupCycle.check(R.id.chipMonthly);
            if (chipGroupReminder != null) chipGroupReminder.check(R.id.chipRemind2Days);
            if (switchActive != null) switchActive.setChecked(true);
            if (switchAuto != null) switchAuto.setChecked(false);
        }

        if (editDueDate != null) {
            editDueDate.setOnClickListener(v -> {
                Calendar c = Calendar.getInstance();
                try {
                    String current = editDueDate.getText() != null ? editDueDate.getText().toString().trim() : "";
                    if (!current.isEmpty()) {
                        Date d = sdf.parse(current);
                        if (d != null) c.setTime(d);
                    }
                } catch (Exception ignored) {}
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

                if (amount <= 0) {
                    Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dueDate.isEmpty()) {
                    Toast.makeText(this, "Please select due date", Toast.LENGTH_SHORT).show();
                    return;
                }

                String cycle = "Monthly";
                if (chipGroupCycle != null) {
                    int selectedChipId = chipGroupCycle.getCheckedChipId();
                    if (selectedChipId == R.id.chipYearly) {
                        cycle = "Yearly";
                    } else if (selectedChipId == R.id.chipWeekly) {
                        cycle = "Weekly";
                    }
                }

                int reminderDays = 2;
                if (chipGroupReminder != null) {
                    int remId = chipGroupReminder.getCheckedChipId();
                    if (remId == R.id.chipRemindSameDay) {
                        reminderDays = 0;
                    } else if (remId == R.id.chipRemind1Day) {
                        reminderDays = 1;
                    } else if (remId == R.id.chipRemind3Days) {
                        reminderDays = 3;
                    } else if (remId == R.id.chipRemind7Days) {
                        reminderDays = 7;
                    }
                }

                boolean isActive = switchActive == null || switchActive.isChecked();
                boolean autoAdd = switchAuto != null && switchAuto.isChecked();

                btnSave.setEnabled(false);

                if (isEditing) {
                    existingSub.name = name;
                    existingSub.amount = amount;
                    existingSub.billingCycle = cycle;
                    existingSub.nextDueDate = dueDate;
                    existingSub.reminderDaysBefore = reminderDays;
                    existingSub.isActive = isActive;
                    existingSub.autoAddTransaction = autoAdd;

                    repository.update(existingSub, () -> {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            loadSubscriptions();
                            Toast.makeText(this, "Subscription updated!", Toast.LENGTH_SHORT).show();
                            checkAndRequestNotificationPermission();
                        });
                    });
                } else {
                    SubscriptionEntity newSub = new SubscriptionEntity(name, amount, cycle, dueDate, "Subscription");
                    newSub.reminderDaysBefore = reminderDays;
                    newSub.isActive = isActive;
                    newSub.autoAddTransaction = autoAdd;

                    repository.insert(newSub, () -> {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            loadSubscriptions();
                            Toast.makeText(this, "Subscription saved!", Toast.LENGTH_SHORT).show();
                            checkAndRequestNotificationPermission();
                        });
                    });
                }
            });
        }

        dialog.show();
    }

    public static String formatRelativeDueDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return "Due soon";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr.trim());
            if (date == null) return "Due " + dateStr;

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar due = Calendar.getInstance();
            due.setTime(date);
            due.set(Calendar.HOUR_OF_DAY, 0);
            due.set(Calendar.MINUTE, 0);
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);

            long diffMillis = due.getTimeInMillis() - today.getTimeInMillis();
            long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);

            if (diffDays == 0) {
                return "Due today";
            } else if (diffDays == 1) {
                return "Due tomorrow";
            } else if (diffDays > 1 && diffDays <= 7) {
                return "Due in " + diffDays + " days";
            } else if (diffDays < 0) {
                return "Overdue by " + Math.abs(diffDays) + "d";
            } else {
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
                return "Due " + displayFormat.format(date);
            }
        } catch (Exception e) {
            return "Due " + dateStr;
        }
    }
}
