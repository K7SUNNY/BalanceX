package com.accounting.balancex.ui.goals;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.accounting.balancex.R;
import com.accounting.balancex.SettingsManager;
import com.accounting.balancex.data.entity.BudgetGoalEntity;
import com.accounting.balancex.data.entity.TransactionEntity;
import com.accounting.balancex.data.repository.BudgetGoalRepository;
import com.accounting.balancex.data.repository.TransactionRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.HorizontalScrollView;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GoalsBudgetsActivity extends AppCompatActivity {

    private BudgetGoalRepository repository;
    private TransactionRepository transactionRepository;
    private LinearLayout containerItems;
    private TextView tabBudgets, tabGoals;
    private boolean isBudgetsTab = true;
    private Map<String, Double> categoryExpenseMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.applyTheme(this);
        setContentView(R.layout.activity_goals_budgets);

        setupWindowInsets();

        repository = new BudgetGoalRepository(this);
        transactionRepository = new TransactionRepository(this);

        containerItems = findViewById(R.id.containerBudgetsGoalsItems);
        tabBudgets = findViewById(R.id.tabBudgets);
        tabGoals = findViewById(R.id.tabGoals);

        findViewById(R.id.btnBackGoals).setOnClickListener(v -> finish());

        tabBudgets.setOnClickListener(v -> {
            if (!isBudgetsTab) {
                isBudgetsTab = true;
                updateTabState();
                loadItems();
            }
        });

        tabGoals.setOnClickListener(v -> {
            if (isBudgetsTab) {
                isBudgetsTab = false;
                updateTabState();
                loadItems();
            }
        });

        View.OnClickListener addListener = v -> showAddBudgetGoalSheet();
        findViewById(R.id.btnAddGoalTop).setOnClickListener(addListener);
        findViewById(R.id.btnAddNewBudgetGoal).setOnClickListener(addListener);

        calculateCategorySpends(this::loadItems);
    }

    private void setupWindowInsets() {
        View root = findViewById(R.id.goalsBudgetsRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(root);
        }
    }

    private void updateTabState() {
        if (isBudgetsTab) {
            tabBudgets.setBackgroundResource(R.drawable.selected_title);
            tabBudgets.setTextColor(ContextCompat.getColor(this, R.color.color_primary));
            tabGoals.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tabGoals.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            tabGoals.setBackgroundResource(R.drawable.selected_title);
            tabGoals.setTextColor(ContextCompat.getColor(this, R.color.color_primary));
            tabBudgets.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tabBudgets.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void calculateCategorySpends(Runnable onComplete) {
        transactionRepository.getAllTransactions(transactions -> {
            categoryExpenseMap.clear();
            Map<String, Double> monthly =
                    com.accounting.balancex.finance.FinancialHealthEngine.calculateMonthlyCategoryExpenses(transactions);
            categoryExpenseMap.putAll(monthly);
            repository.syncBudgetSpends(categoryExpenseMap, () -> {
                if (onComplete != null) onComplete.run();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        calculateCategorySpends(this::loadItems);
    }

    private void loadItems() {
        if (isBudgetsTab) {
            repository.getBudgets(items -> runOnUiThread(() -> renderBudgets(items)));
        } else {
            repository.getGoals(items -> runOnUiThread(() -> renderGoals(items)));
        }
    }

    private void renderBudgets(List<BudgetGoalEntity> budgets) {
        if (containerItems == null) return;
        containerItems.removeAllViews();

        if (budgets == null || budgets.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No category budgets configured yet.\nTap below to set a monthly spending limit.");
            emptyText.setGravity(android.view.Gravity.CENTER);
            emptyText.setPadding(24, 48, 24, 48);
            emptyText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            containerItems.addView(emptyText);
            return;
        }

        String symbol = SettingsManager.getCurrencySymbol(this);

        for (int i = 0; i < budgets.size(); i++) {
            BudgetGoalEntity b = budgets.get(i);

            // Compute dynamic spent from transactions if matching category exists
            String catKey = (b.category != null ? b.category : b.title).trim().toLowerCase();
            double spent = b.currentAmount;
            if (categoryExpenseMap.containsKey(catKey)) {
                spent = categoryExpenseMap.get(catKey);
            }

            View itemView = getLayoutInflater().inflate(R.layout.item_budget_manage, containerItems, false);
            TextView title = itemView.findViewById(R.id.textBudgetManageTitle);
            TextView pctText = itemView.findViewById(R.id.textBudgetManagePct);
            TextView spentText = itemView.findViewById(R.id.textBudgetManageSpent);
            TextView remText = itemView.findViewById(R.id.textBudgetManageRemaining);
            LinearProgressIndicator progress = itemView.findViewById(R.id.progressBudgetManage);
            ImageView btnDelete = itemView.findViewById(R.id.btnDeleteBudget);
            View divider = itemView.findViewById(R.id.dividerBudgetManage);

            int pct = b.targetAmount > 0 ? (int) Math.round((spent / b.targetAmount) * 100) : 0;
            double rem = Math.max(0, b.targetAmount - spent);

            if (title != null) title.setText(b.title);
            if (pctText != null) {
                pctText.setText(pct + "% used");
                if (pct >= 100) {
                    pctText.setTextColor(ContextCompat.getColor(this, R.color.finance_expense));
                }
            }
            if (spentText != null) {
                spentText.setText("Spent: " + symbol + String.format(Locale.getDefault(), "%,.2f", spent));
            }
            if (remText != null) {
                remText.setText("Remaining: " + symbol + String.format(Locale.getDefault(), "%,.2f", rem));
            }
            if (progress != null) {
                progress.setProgress(Math.min(100, pct));
                if (pct >= 100) {
                    progress.setIndicatorColor(ContextCompat.getColor(this, R.color.finance_expense));
                } else if (pct >= 80) {
                    progress.setIndicatorColor(ContextCompat.getColor(this, R.color.color_tertiary));
                } else {
                    progress.setIndicatorColor(ContextCompat.getColor(this, R.color.finance_income));
                }
            }

            if (divider != null && i == budgets.size() - 1) {
                divider.setVisibility(View.GONE);
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Delete Budget")
                            .setMessage("Remove " + b.title + " budget?")
                            .setPositiveButton("Delete", (d, w) -> {
                                repository.delete(b.id, this::loadItems);
                                Toast.makeText(this, "Budget removed", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }

            containerItems.addView(itemView);
        }
    }

    private void renderGoals(List<BudgetGoalEntity> goals) {
        if (containerItems == null) return;
        containerItems.removeAllViews();

        if (goals == null || goals.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No savings targets yet.\nTap below to set a savings goal!");
            emptyText.setGravity(android.view.Gravity.CENTER);
            emptyText.setPadding(24, 48, 24, 48);
            emptyText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            containerItems.addView(emptyText);
            return;
        }

        String symbol = SettingsManager.getCurrencySymbol(this);

        for (int i = 0; i < goals.size(); i++) {
            BudgetGoalEntity g = goals.get(i);

            View itemView = getLayoutInflater().inflate(R.layout.item_goal_manage, containerItems, false);
            TextView title = itemView.findViewById(R.id.textGoalManageTitle);
            TextView pctText = itemView.findViewById(R.id.textGoalManagePct);
            TextView savedText = itemView.findViewById(R.id.textGoalManageSaved);
            TextView btnDeposit = itemView.findViewById(R.id.btnAddGoalDeposit);
            LinearProgressIndicator progress = itemView.findViewById(R.id.progressGoalManage);
            ImageView btnDelete = itemView.findViewById(R.id.btnDeleteGoal);
            View divider = itemView.findViewById(R.id.dividerGoalManage);

            int pct = g.targetAmount > 0 ? (int) Math.round((g.currentAmount / g.targetAmount) * 100) : 0;

            if (title != null) title.setText(g.title);
            if (pctText != null) pctText.setText(pct + "% Achieved");
            if (savedText != null) {
                savedText.setText("Saved: " + symbol + String.format(Locale.getDefault(), "%,.2f", g.currentAmount)
                        + " / " + symbol + String.format(Locale.getDefault(), "%,.2f", g.targetAmount));
            }
            if (progress != null) progress.setProgress(Math.min(100, pct));

            if (divider != null && i == goals.size() - 1) {
                divider.setVisibility(View.GONE);
            }

            if (btnDeposit != null) {
                btnDeposit.setOnClickListener(v -> showAddDepositDialog(g));
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Delete Goal")
                            .setMessage("Remove goal " + g.title + "?")
                            .setPositiveButton("Delete", (d, w) -> {
                                repository.delete(g.id, this::loadItems);
                                Toast.makeText(this, "Goal removed", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }

            containerItems.addView(itemView);
        }
    }

    private void showAddDepositDialog(BudgetGoalEntity goal) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter amount saved (e.g. 5000)");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Savings Deposit")
                .setMessage("Add to " + goal.title + ":")
                .setView(input)
                .setPositiveButton("Add", (d, w) -> {
                    String str = input.getText().toString().trim();
                    try {
                        double addAmt = Double.parseDouble(str);
                        if (addAmt <= 0) {
                            Toast.makeText(this, "Deposit amount must be greater than zero", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        goal.currentAmount += addAmt;
                        repository.update(goal, this::loadItems);
                        Toast.makeText(this, "Savings updated!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddBudgetGoalSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_add_budget_goal, null);
        dialog.setContentView(view);

        ChipGroup chipGroup = view.findViewById(R.id.chipGroupBudgetType);
        AutoCompleteTextView editTitle = view.findViewById(R.id.editBudgetGoalTitle);
        TextInputEditText editTarget = view.findViewById(R.id.editBudgetTargetAmount);
        TextInputEditText editInitial = view.findViewById(R.id.editBudgetInitialAmount);
        TextInputLayout layoutTitle = view.findViewById(R.id.inputLayoutTitle);
        TextInputLayout layoutTarget = view.findViewById(R.id.inputLayoutTarget);
        TextInputLayout layoutInitial = view.findViewById(R.id.inputLayoutInitialAmount);
        HorizontalScrollView scrollCategories = view.findViewById(R.id.scrollCategoryChips);
        ChipGroup chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        View btnSave = view.findViewById(R.id.btnSaveBudgetGoal);

        // Prepopulate available categories
        List<String> defaultCategories = Arrays.asList(
                "Food & Dining", "Shopping", "Fuel", "Groceries",
                "Bills", "Entertainment", "Health", "General"
        );
        Set<String> categorySet = new LinkedHashSet<>(defaultCategories);

        transactionRepository.getAllTransactions(transactions -> {
            if (transactions != null) {
                for (TransactionEntity t : transactions) {
                    if (t.category != null && !t.category.trim().isEmpty() && !t.category.equalsIgnoreCase("NA")) {
                        categorySet.add(t.category.trim());
                    }
                }
            }
            runOnUiThread(() -> {
                List<String> categoryList = new ArrayList<>(categorySet);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, categoryList);
                if (editTitle != null) {
                    editTitle.setAdapter(adapter);
                }

                if (chipGroupCategories != null) {
                    chipGroupCategories.removeAllViews();
                    for (String cat : categoryList) {
                        Chip chip = new Chip(this);
                        chip.setText(cat);
                        chip.setCheckable(true);
                        chip.setClickable(true);
                        chip.setOnClickListener(c -> {
                            if (editTitle != null) {
                                editTitle.setText(cat);
                                editTitle.dismissDropDown();
                            }
                        });
                        chipGroupCategories.addView(chip);
                    }
                }
            });
        });

        if (editTitle != null) {
            editTitle.setOnClickListener(v -> {
                boolean isBudget = chipGroup == null || chipGroup.getCheckedChipId() == R.id.chipTypeBudget;
                if (isBudget) {
                    editTitle.showDropDown();
                }
            });
        }

        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                boolean isGoal = checkedIds.contains(R.id.chipTypeGoal);
                if (layoutInitial != null) {
                    layoutInitial.setVisibility(isGoal ? View.VISIBLE : View.GONE);
                }
                if (scrollCategories != null) {
                    scrollCategories.setVisibility(isGoal ? View.GONE : View.VISIBLE);
                }
                if (layoutTitle != null) {
                    layoutTitle.setHint(isGoal ? "Goal Title (e.g. Vacation, New Laptop)" : "Select or Type Category");
                }
                if (layoutTarget != null) {
                    layoutTarget.setHint(isGoal ? "Target Savings Amount" : "Monthly Spending Limit");
                }
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String title = editTitle != null && editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
                String targetStr = editTarget != null && editTarget.getText() != null ? editTarget.getText().toString().trim() : "";
                String initStr = editInitial != null && editInitial.getText() != null ? editInitial.getText().toString().trim() : "0";

                boolean isGoal = chipGroup != null && chipGroup.getCheckedChipId() == R.id.chipTypeGoal;

                if (title.isEmpty()) {
                    Toast.makeText(this, isGoal ? "Please enter a goal title" : "Please select or enter a category", Toast.LENGTH_SHORT).show();
                    return;
                }

                double target;
                try {
                    target = Double.parseDouble(targetStr);
                } catch (Exception e) {
                    Toast.makeText(this, "Please enter a valid target amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (target <= 0) {
                    Toast.makeText(this, isGoal ? "Target amount must be greater than zero" : "Budget limit must be greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }

                double initial = 0;
                if (isGoal) {
                    try {
                        initial = Double.parseDouble(initStr);
                    } catch (Exception e) {
                        Toast.makeText(this, "Please enter a valid initial amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (initial < 0) {
                        Toast.makeText(this, "Initial amount cannot be negative", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (initial > target) {
                        Toast.makeText(this, "Initial amount cannot exceed target amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                String type = isGoal ? BudgetGoalEntity.TYPE_GOAL : BudgetGoalEntity.TYPE_BUDGET;

                // Canonicalize category against existing categories if matched case-insensitively
                String canonicalCategory = title;
                if (!isGoal) {
                    for (String cat : categorySet) {
                        if (cat.equalsIgnoreCase(title)) {
                            canonicalCategory = cat;
                            break;
                        }
                    }
                }

                BudgetGoalEntity entity = new BudgetGoalEntity(
                        canonicalCategory, target, initial, canonicalCategory, type, isGoal ? "#2563EB" : "#D97706"
                );
                repository.insert(entity, () -> {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        calculateCategorySpends(this::loadItems);
                        Toast.makeText(this, (isGoal ? "Goal" : "Budget") + " created!", Toast.LENGTH_SHORT).show();
                    });
                });
            });
        }

        dialog.show();
    }
}
