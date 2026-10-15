package com.k7sunny.balancex.ui.subscriptions;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.k7sunny.balancex.BaseSecureActivity;
import com.k7sunny.balancex.BuildConfig;
import com.k7sunny.balancex.R;
import com.k7sunny.balancex.SettingsManager;
import com.k7sunny.balancex.data.entity.SubscriptionEntity;
import com.k7sunny.balancex.data.repository.SubscriptionRepository;
import com.k7sunny.balancex.finance.RecurrenceCalculator;
import com.k7sunny.balancex.worker.SubscriptionWorker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SubscriptionsActivity extends BaseSecureActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 201;

    private static final String[] CATEGORIES = new String[]{
            "Subscription", "Entertainment", "Utilities", "Rent",
            "Insurance", "Internet", "Software", "Fitness", "General"
    };

    private static final String[] PAYMENT_METHODS = new String[]{
            "UPI", "Credit Card", "Debit Card", "Net Banking", "Cash", "Auto-Debit"
    };

    private SubscriptionRepository repository;
    private LinearLayout containerItems;
    private View layoutEmpty;
    private View cardSubscriptionsList;
    private TextView textTotalCommitment;
    private TextView textActiveCount;
    private TextView textDueThisWeek;
    private ImageView btnCheckNowDebug;

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

        // Schedule periodic morning reminder via WorkManager
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
                updateCommitments(0, 0, 0, 0);
                return;
            }

            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            if (cardSubscriptionsList != null) cardSubscriptionsList.setVisibility(View.VISIBLE);
            if (containerItems == null) return;

            containerItems.removeAllViews();
            LocalDate today = LocalDate.now();

            // Sort: Overdue -> Due soon -> Active -> Paused -> Ended
            subscriptions.sort((s1, s2) -> {
                int r1 = getSortRank(s1, today);
                int r2 = getSortRank(s2, today);
                if (r1 != r2) return Integer.compare(r1, r2);
                String d1 = s1.nextDueDate != null ? s1.nextDueDate : "";
                String d2 = s2.nextDueDate != null ? s2.nextDueDate : "";
                return d1.compareTo(d2);
            });

            double totalMonthly = 0;
            double next30DaysOutflow = 0;
            int activeCount = 0;
            int dueThisWeek = 0;
            String symbol = SettingsManager.getCurrencySymbol(this);

            for (int i = 0; i < subscriptions.size(); i++) {
                SubscriptionEntity sub = subscriptions.get(i);
                boolean isPaused = isSubscriptionPaused(sub, today);
                boolean isEnded = isSubscriptionEnded(sub, today);
                boolean isInTrial = isSubscriptionInTrial(sub, today);

                RecurrenceCalculator.Cycle cycle = RecurrenceCalculator.Cycle.fromString(sub.billingCycle);

                if (sub.isActive && !isPaused && !isEnded) {
                    activeCount++;
                    totalMonthly += RecurrenceCalculator.toMonthlyAmount(sub.amount, cycle, sub.customInterval);

                    if (sub.nextDueDate != null && !sub.nextDueDate.trim().isEmpty()) {
                        try {
                            LocalDate due = LocalDate.parse(sub.nextDueDate.trim());
                            long days = RecurrenceCalculator.daysUntil(due, today);
                            if (days >= 0 && days <= 7) {
                                dueThisWeek++;
                            }

                            // Next 30 days calculation
                            LocalDate anchor = parseSafeDate(sub.anchorDate != null ? sub.anchorDate : sub.nextDueDate, due);
                            List<LocalDate> nextOccurrences = RecurrenceCalculator.occurrencesBetween(
                                    anchor, cycle, sub.customInterval, today, today.plusDays(30), 10
                            );
                            next30DaysOutflow += (nextOccurrences.size() * sub.amount);
                        } catch (Exception ignored) {}
                    }
                }

                View itemView = getLayoutInflater().inflate(R.layout.item_subscription_manage, containerItems, false);
                TextView textName = itemView.findViewById(R.id.textSubManageName);
                TextView textAmount = itemView.findViewById(R.id.textSubManageAmount);
                TextView textDue = itemView.findViewById(R.id.textSubManageDue);
                TextView textStatus = itemView.findViewById(R.id.textSubManageStatus);
                TextView textCycle = itemView.findViewById(R.id.textSubManageCycle);
                ImageView btnMarkPaid = itemView.findViewById(R.id.btnMarkPaidSubscription);
                ImageView btnEdit = itemView.findViewById(R.id.btnEditSubscription);
                ImageView btnDelete = itemView.findViewById(R.id.btnDeleteSubscription);
                View divider = itemView.findViewById(R.id.dividerSubManage);

                if (textName != null) textName.setText(sub.name);
                if (textAmount != null) {
                    textAmount.setText(symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount));
                }

                // Render Relative Due Status Badge
                if (textDue != null) {
                    if (isEnded) {
                        textDue.setText("Ended");
                        textDue.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
                    } else if (isPaused) {
                        textDue.setText("Paused until " + sub.pausedUntil);
                        textDue.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    } else if (isInTrial) {
                        textDue.setText("Free Trial");
                        textDue.setTextColor(ContextCompat.getColor(this, R.color.finance_income));
                    } else {
                        textDue.setText(formatRelativeDueDate(sub.nextDueDate, today));
                        long days = getDaysUntil(sub.nextDueDate, today);
                        if (days <= 0) {
                            textDue.setTextColor(ContextCompat.getColor(this, R.color.finance_expense));
                        } else if (days <= 3) {
                            textDue.setTextColor(ContextCompat.getColor(this, R.color.color_primary));
                        } else {
                            textDue.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                        }
                    }
                }

                // Status tag (Inactive, Auto-Pay)
                if (textStatus != null) {
                    if (!sub.isActive) {
                        textStatus.setVisibility(View.VISIBLE);
                        textStatus.setText("Inactive");
                    } else if (sub.autoAddTransaction) {
                        textStatus.setVisibility(View.VISIBLE);
                        textStatus.setText("Auto-Pay");
                    } else {
                        textStatus.setVisibility(View.GONE);
                    }
                }

                itemView.setAlpha((sub.isActive && !isPaused && !isEnded) ? 1.0f : 0.6f);

                if (textCycle != null) {
                    String cycleText = "• " + sub.billingCycle;
                    if (sub.paymentMethod != null && !sub.paymentMethod.isEmpty()) {
                        cycleText += " (" + sub.paymentMethod + ")";
                    }
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

                // Mark as Paid
                if (btnMarkPaid != null) {
                    if (isEnded || !sub.isActive) {
                        btnMarkPaid.setVisibility(View.GONE);
                    } else {
                        btnMarkPaid.setVisibility(View.VISIBLE);
                        btnMarkPaid.setOnClickListener(v -> confirmMarkAsPaid(sub));
                    }
                }

                if (btnEdit != null) {
                    btnEdit.setOnClickListener(v -> showSubscriptionSheet(sub));
                }
                itemView.setOnClickListener(v -> showSubscriptionOptionsSheet(sub));

                // Delete with Undo Snackbar
                if (btnDelete != null) {
                    btnDelete.setOnClickListener(v -> confirmDeleteSubscription(sub));
                }

                containerItems.addView(itemView);
            }

            updateCommitments(totalMonthly, next30DaysOutflow, activeCount, dueThisWeek);
        }));
    }

    private int getSortRank(SubscriptionEntity sub, LocalDate today) {
        if (!sub.isActive) return 5;
        if (isSubscriptionEnded(sub, today)) return 5;
        if (isSubscriptionPaused(sub, today)) return 4;

        long days = getDaysUntil(sub.nextDueDate, today);
        if (days < 0) return 0; // Overdue
        if (days == 0) return 1; // Due today
        if (days <= 7) return 2; // Due soon
        return 3; // Active later
    }

    private boolean isSubscriptionPaused(SubscriptionEntity sub, LocalDate today) {
        if (sub.pausedUntil == null || sub.pausedUntil.trim().isEmpty()) return false;
        try {
            LocalDate pause = LocalDate.parse(sub.pausedUntil.trim());
            return pause.isAfter(today);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSubscriptionEnded(SubscriptionEntity sub, LocalDate today) {
        if (sub.endDate == null || sub.endDate.trim().isEmpty()) return false;
        try {
            LocalDate end = LocalDate.parse(sub.endDate.trim());
            return today.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSubscriptionInTrial(SubscriptionEntity sub, LocalDate today) {
        if (sub.trialEndsOn == null || sub.trialEndsOn.trim().isEmpty()) return false;
        try {
            LocalDate trial = LocalDate.parse(sub.trialEndsOn.trim());
            return !today.isAfter(trial);
        } catch (Exception e) {
            return false;
        }
    }

    private long getDaysUntil(String dateStr, LocalDate today) {
        if (dateStr == null || dateStr.trim().isEmpty()) return 999;
        try {
            LocalDate d = LocalDate.parse(dateStr.trim());
            return RecurrenceCalculator.daysUntil(d, today);
        } catch (Exception e) {
            return 999;
        }
    }

    private LocalDate parseSafeDate(String dateStr, LocalDate fallback) {
        if (dateStr == null || dateStr.trim().isEmpty()) return fallback;
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private void updateCommitments(double totalMonthly, double next30Days, int activeCount, int dueThisWeek) {
        String symbol = SettingsManager.getCurrencySymbol(this);
        if (textTotalCommitment != null) {
            textTotalCommitment.setText(symbol + String.format(Locale.getDefault(), "%,.2f", totalMonthly) + " / mo");
        }
        if (textActiveCount != null) {
            String activeStr = activeCount + " Active Subscriptions";
            if (next30Days > 0) {
                activeStr += " • Next 30d: " + symbol + String.format(Locale.getDefault(), "%,.2f", next30Days);
            }
            textActiveCount.setText(activeStr);
        }
        if (textDueThisWeek != null) {
            textDueThisWeek.setText(dueThisWeek + " Due This Week");
        }
    }

    private void confirmMarkAsPaid(SubscriptionEntity sub) {
        String symbol = SettingsManager.getCurrencySymbol(this);
        new AlertDialog.Builder(this)
                .setTitle("Mark as Paid")
                .setMessage("Record " + symbol + String.format(Locale.getDefault(), "%,.2f", sub.amount)
                        + " debit for " + sub.name + " (" + sub.nextDueDate + ") and advance to next cycle?")
                .setPositiveButton("Mark Paid", (d, w) -> {
                    repository.markAsPaid(sub.id, sub.nextDueDate, sub.paymentMethod, () -> {
                        runOnUiThread(() -> {
                            loadSubscriptions();
                            Toast.makeText(this, "Payment recorded for " + sub.name, Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteSubscription(SubscriptionEntity sub) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Subscription")
                .setMessage("Stop tracking " + sub.name + "? Existing transaction history will remain intact in your ledger.")
                .setPositiveButton("Delete", (d, w) -> {
                    repository.delete(sub.id, () -> {
                        runOnUiThread(() -> {
                            loadSubscriptions();
                            Snackbar.make(findViewById(R.id.subscriptionsRoot), "Subscription removed", Snackbar.LENGTH_LONG)
                                    .setAction("Undo", uv -> {
                                        sub.id = 0; // Re-insert with auto-generated ID
                                        repository.insert(sub, () -> runOnUiThread(this::loadSubscriptions));
                                    })
                                    .show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSubscriptionOptionsSheet(SubscriptionEntity sub) {
        String[] options;
        LocalDate today = LocalDate.now();
        boolean isPaused = isSubscriptionPaused(sub, today);

        if (isPaused) {
            options = new String[]{"Mark as Paid", "Resume Subscription", "Skip this Cycle", "Edit Details", "Delete"};
        } else {
            options = new String[]{"Mark as Paid", "Skip this Cycle", "Pause for 1 Month", "Edit Details", "Delete"};
        }

        new AlertDialog.Builder(this)
                .setTitle(sub.name)
                .setItems(options, (dialog, which) -> {
                    String choice = options[which];
                    if ("Mark as Paid".equals(choice)) {
                        confirmMarkAsPaid(sub);
                    } else if ("Skip this Cycle".equals(choice)) {
                        repository.skipCycle(sub.id, () -> runOnUiThread(() -> {
                            loadSubscriptions();
                            Toast.makeText(this, "Cycle skipped for " + sub.name, Toast.LENGTH_SHORT).show();
                        }));
                    } else if ("Pause for 1 Month".equals(choice)) {
                        String pauseDate = today.plusMonths(1).toString();
                        repository.pauseSubscription(sub.id, pauseDate, () -> runOnUiThread(() -> {
                            loadSubscriptions();
                            Toast.makeText(this, sub.name + " paused until " + pauseDate, Toast.LENGTH_SHORT).show();
                        }));
                    } else if ("Resume Subscription".equals(choice)) {
                        repository.resumeSubscription(sub.id, () -> runOnUiThread(() -> {
                            loadSubscriptions();
                            Toast.makeText(this, sub.name + " resumed", Toast.LENGTH_SHORT).show();
                        }));
                    } else if ("Edit Details".equals(choice)) {
                        showSubscriptionSheet(sub);
                    } else if ("Delete".equals(choice)) {
                        confirmDeleteSubscription(sub);
                    }
                })
                .show();
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
        AutoCompleteTextView autoCategory = view.findViewById(R.id.autoCompleteCategory);
        AutoCompleteTextView autoPaymentMethod = view.findViewById(R.id.autoCompletePaymentMethod);
        TextInputEditText editDueDate = view.findViewById(R.id.editSubDueDate);
        TextInputEditText editEndDate = view.findViewById(R.id.editSubEndDate);
        TextInputEditText editTrialDate = view.findViewById(R.id.editSubTrialDate);
        ChipGroup chipGroupCycle = view.findViewById(R.id.chipGroupCycle);
        ChipGroup chipGroupReminder = view.findViewById(R.id.chipGroupReminder);
        MaterialSwitch switchActive = view.findViewById(R.id.switchIsActive);
        MaterialSwitch switchAuto = view.findViewById(R.id.switchAutoAdd);
        View btnSave = view.findViewById(R.id.btnSaveSubscription);

        // Set up Category autocomplete
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        if (autoCategory != null) {
            autoCategory.setAdapter(catAdapter);
            autoCategory.setText(CATEGORIES[0], false);
        }

        // Set up Payment Method autocomplete
        ArrayAdapter<String> payAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, PAYMENT_METHODS);
        if (autoPaymentMethod != null) {
            autoPaymentMethod.setAdapter(payAdapter);
            autoPaymentMethod.setText(PAYMENT_METHODS[0], false);
        }

        if (isEditing) {
            if (textTitle != null) textTitle.setText("Edit Subscription");
            if (btnSave instanceof TextView) ((TextView) btnSave).setText("Save Changes");
            if (editName != null) editName.setText(existingSub.name);
            if (editAmount != null) editAmount.setText(String.format(Locale.US, "%.2f", existingSub.amount));
            if (editDueDate != null) editDueDate.setText(existingSub.nextDueDate != null ? existingSub.nextDueDate : "");
            if (editEndDate != null) editEndDate.setText(existingSub.endDate != null ? existingSub.endDate : "");
            if (editTrialDate != null) editTrialDate.setText(existingSub.trialEndsOn != null ? existingSub.trialEndsOn : "");

            if (autoCategory != null && existingSub.category != null) {
                autoCategory.setText(existingSub.category, false);
            }
            if (autoPaymentMethod != null && existingSub.paymentMethod != null) {
                autoPaymentMethod.setText(existingSub.paymentMethod, false);
            }

            if (chipGroupCycle != null) {
                if ("Quarterly".equalsIgnoreCase(existingSub.billingCycle)) {
                    chipGroupCycle.check(R.id.chipQuarterly);
                } else if ("Yearly".equalsIgnoreCase(existingSub.billingCycle)) {
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
            LocalDate defaultDue = LocalDate.now().plusDays(7);
            if (editDueDate != null) {
                editDueDate.setText(defaultDue.toString());
            }
            if (chipGroupCycle != null) chipGroupCycle.check(R.id.chipMonthly);
            if (chipGroupReminder != null) chipGroupReminder.check(R.id.chipRemind2Days);
            if (switchActive != null) switchActive.setChecked(true);
            if (switchAuto != null) switchAuto.setChecked(false);
        }

        // Date Pickers
        setupDatePicker(editDueDate);
        setupDatePicker(editEndDate);
        setupDatePicker(editTrialDate);

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String name = editName != null && editName.getText() != null ? editName.getText().toString().trim() : "";
                String amountStr = editAmount != null && editAmount.getText() != null ? editAmount.getText().toString().trim() : "";
                String dueDate = editDueDate != null && editDueDate.getText() != null ? editDueDate.getText().toString().trim() : "";
                String endDate = editEndDate != null && editEndDate.getText() != null ? editEndDate.getText().toString().trim() : "";
                String trialDate = editTrialDate != null && editTrialDate.getText() != null ? editTrialDate.getText().toString().trim() : "";
                String category = autoCategory != null ? autoCategory.getText().toString().trim() : "Subscription";
                String paymentMethod = autoPaymentMethod != null ? autoPaymentMethod.getText().toString().trim() : "UPI";

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
                    if (selectedChipId == R.id.chipQuarterly) {
                        cycle = "Quarterly";
                    } else if (selectedChipId == R.id.chipYearly) {
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
                    double oldAmount = existingSub.amount;
                    existingSub.name = name;
                    existingSub.amount = amount;
                    existingSub.amountMinor = Math.round(amount * 100.0);
                    existingSub.billingCycle = cycle;
                    existingSub.nextDueDate = dueDate;
                    existingSub.endDate = endDate.isEmpty() ? null : endDate;
                    existingSub.trialEndsOn = trialDate.isEmpty() ? null : trialDate;
                    existingSub.category = category.isEmpty() ? "Subscription" : category;
                    existingSub.paymentMethod = paymentMethod.isEmpty() ? "UPI" : paymentMethod;
                    existingSub.reminderDaysBefore = reminderDays;
                    existingSub.isActive = isActive;
                    existingSub.autoAddTransaction = autoAdd;

                    if (Math.abs(oldAmount - amount) > 0.001) {
                        repository.updatePrice(existingSub.id, amount, LocalDate.now().toString(), null);
                    }

                    repository.update(existingSub, () -> {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            loadSubscriptions();
                            Toast.makeText(this, "Subscription updated!", Toast.LENGTH_SHORT).show();
                            checkAndRequestNotificationPermission();
                        });
                    });
                } else {
                    SubscriptionEntity newSub = new SubscriptionEntity(name, amount, cycle, dueDate, category);
                    newSub.amountMinor = Math.round(amount * 100.0);
                    newSub.paymentMethod = paymentMethod;
                    newSub.endDate = endDate.isEmpty() ? null : endDate;
                    newSub.trialEndsOn = trialDate.isEmpty() ? null : trialDate;
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

    private void setupDatePicker(TextInputEditText editText) {
        if (editText == null) return;
        editText.setOnClickListener(v -> {
            LocalDate initial = LocalDate.now();
            try {
                String cur = editText.getText() != null ? editText.getText().toString().trim() : "";
                if (!cur.isEmpty()) {
                    initial = LocalDate.parse(cur);
                }
            } catch (Exception ignored) {}

            new DatePickerDialog(
                    this,
                    (dp, year, month, dayOfMonth) -> {
                        LocalDate picked = LocalDate.of(year, month + 1, dayOfMonth);
                        editText.setText(picked.toString());
                    },
                    initial.getYear(),
                    initial.getMonthValue() - 1,
                    initial.getDayOfMonth()
            ).show();
        });
    }

    public static String formatRelativeDueDate(String dateStr) {
        return formatRelativeDueDate(dateStr, LocalDate.now());
    }

    public static String formatRelativeDueDate(String dateStr, LocalDate today) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return "Due soon";
        }
        try {
            LocalDate dueDate = LocalDate.parse(dateStr.trim());
            long diffDays = RecurrenceCalculator.daysUntil(dueDate, today);

            if (diffDays == 0) {
                return "Due today";
            } else if (diffDays == 1) {
                return "Due tomorrow";
            } else if (diffDays > 1 && diffDays <= 7) {
                return "Due in " + diffDays + " days";
            } else if (diffDays < 0) {
                return "Overdue by " + Math.abs(diffDays) + "d";
            } else {
                return "Due " + dueDate.format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()));
            }
        } catch (Exception e) {
            return "Due " + dateStr;
        }
    }
}
