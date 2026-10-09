package com.k7sunny.balancex;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntryActivity extends AppCompatActivity {

    private static final int PICK_CONTACT_REQUEST = 1;
    private static final int CONTACT_PERMISSION_CODE = 101;
    private static final int STORAGE_PERMISSION_CODE = 102;
    private static final int REQUEST_IMAGE_CAPTURE = 103;
    private static final int REQUEST_IMAGE_PICK = 104;
    private static final int CAMERA_PERMISSION_CODE = 105;

    private static final String FOLDER_NAME = "Accounting";
    private static final String FILE_NAME = "transactions.json";

    // UI Elements
    private TextView textCurrencySymbol;
    private EditText amountInput;
    private EditText receiverName;
    private TextView dateTextView;
    private TextView clearAllButton;
    private TextView tabExpense, tabIncome;
    private RecyclerView recyclerViewCategories;
    private TextView textSelectedAccount;
    private View rowDate, rowPaymentAccount, rowAddDetails;
    private TextView badgeDetailsStatus;
    private LinearLayout layoutPayeeSuggestions;
    private View btnQuickScanReceipt, btnQuickRecentPayee;
    private TextView textQuickRecentPayee;
    private Button saveButton;
    private ImageView addFromContactButton;

    // Additional Details (Bottom Sheet values)
    private String utrVal = "";
    private String transactionIdVal = "";
    private String descriptionVal = "";
    private String commentsVal = "";

    // State
    private boolean isExpense = true;
    private String selectedCategory = "Food & Dining";
    private String selectedPaymentMethod = "UPI";
    private Calendar calendar;
    private boolean isReceiptAttached = false;

    // OCR / Tesseract
    private TessBaseAPI tessBaseAPI;
    private AlertDialog loadingDialog;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.applyTheme(this);
        setContentView(R.layout.activity_entry);

        View entryRoot = findViewById(R.id.entry_root);
        if (entryRoot != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(entryRoot, (v, windowInsets) -> {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, 0);
                View navbar = findViewById(R.id.navbar);
                if (navbar != null) {
                    int baseBottomPadding = (int) (6 * getResources().getDisplayMetrics().density);
                    navbar.setPadding(
                            navbar.getPaddingLeft(),
                            navbar.getPaddingTop(),
                            navbar.getPaddingRight(),
                            baseBottomPadding + insets.bottom);
                }
                return windowInsets;
            });
        }

        initializeUI();
        setupSegmentedToggle();
        setupCategories();
        setDateField();
        loadRecentPayees();
        requestStoragePermission();
        setNavigationListeners();
        copyTessDataFiles();
        initTesseract();

        applySettingsDefaults();
    }

    private void initializeUI() {
        textCurrencySymbol = findViewById(R.id.textCurrencySymbol);
        amountInput = findViewById(R.id.amountInput);
        receiverName = findViewById(R.id.receiverName);
        dateTextView = findViewById(R.id.dateTextView);
        clearAllButton = findViewById(R.id.clearAllButton);
        tabExpense = findViewById(R.id.tabExpense);
        tabIncome = findViewById(R.id.tabIncome);
        recyclerViewCategories = findViewById(R.id.recyclerViewCategories);
        textSelectedAccount = findViewById(R.id.textSelectedAccount);
        rowDate = findViewById(R.id.rowDate);
        rowPaymentAccount = findViewById(R.id.rowPaymentAccount);
        rowAddDetails = findViewById(R.id.rowAddDetails);
        badgeDetailsStatus = findViewById(R.id.badgeDetailsStatus);
        layoutPayeeSuggestions = findViewById(R.id.layoutPayeeSuggestions);
        btnQuickScanReceipt = findViewById(R.id.button_attach_receipt);
        btnQuickRecentPayee = findViewById(R.id.btnQuickRecentPayee);
        textQuickRecentPayee = findViewById(R.id.textQuickRecentPayee);
        saveButton = findViewById(R.id.saveButton);
        addFromContactButton = findViewById(R.id.addFromContactButton);

        // Header Actions (back button removed as Entry is a primary tab in BottomNav)
        if (clearAllButton != null) {
            clearAllButton.setOnClickListener(v -> {
                vibrateDevice();
                new AlertDialog.Builder(this)
                        .setTitle("Clear All Fields?")
                        .setMessage("Are you sure you want to clear all input fields?")
                        .setPositiveButton("Yes", (dialog, which) -> clearAllFields())
                        .setNegativeButton("No", null)
                        .show();
            });
        }

        // Row Interactions
        if (rowPaymentAccount != null) {
            rowPaymentAccount.setOnClickListener(v -> showPaymentAccountChooser());
        }

        if (rowAddDetails != null) {
            rowAddDetails.setOnClickListener(v -> showAdditionalDetailsBottomSheet());
        }

        if (addFromContactButton != null) {
            addFromContactButton.setOnClickListener(v -> requestContactPermission());
        }

        // Quick Actions
        if (btnQuickScanReceipt != null) {
            btnQuickScanReceipt.setOnClickListener(v -> {
                vibrateDevice();
                button_attach_receipt();
            });
        }

        if (btnQuickRecentPayee != null) {
            btnQuickRecentPayee.setOnClickListener(v -> showRecentPayeesChooser());
        }

        // Save Action
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> validateAndSaveTransaction());
        }
    }

    private void setupSegmentedToggle() {
        if (tabExpense != null) {
            tabExpense.setOnClickListener(v -> {
                if (!isExpense) {
                    vibrateDevice();
                    isExpense = true;
                    updateSegmentedUI();
                    setupCategories();
                }
            });
        }

        if (tabIncome != null) {
            tabIncome.setOnClickListener(v -> {
                if (isExpense) {
                    vibrateDevice();
                    isExpense = false;
                    updateSegmentedUI();
                    setupCategories();
                }
            });
        }
    }

    private void updateSegmentedUI() {
        if (isExpense) {
            tabExpense.setBackgroundResource(R.drawable.bg_entry_expense_active);
            tabExpense.setTextColor(Color.WHITE);
            tabIncome.setBackgroundResource(android.R.color.transparent);
            tabIncome.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            saveButton.setText(R.string.save_transaction);
        } else {
            tabIncome.setBackgroundResource(R.drawable.bg_entry_income_active);
            tabIncome.setTextColor(Color.WHITE);
            tabExpense.setBackgroundResource(android.R.color.transparent);
            tabExpense.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            saveButton.setText("Record Income");
        }
    }

    // ==================== CATEGORIES ====================

    private final List<String> customExpenseCategories = new ArrayList<>();
    private final List<String> customIncomeCategories = new ArrayList<>();

    public static class CategoryItem {
        public String name;
        public int iconRes;

        public CategoryItem(String name, int iconRes) {
            this.name = name;
            this.iconRes = iconRes;
        }
    }

    private void setupCategories() {
        List<CategoryItem> categoryList = new ArrayList<>();
        if (isExpense) {
            categoryList.add(new CategoryItem("Food & Dining", R.drawable.ic_cat_food));
            categoryList.add(new CategoryItem("Shopping", R.drawable.ic_cat_shopping));
            categoryList.add(new CategoryItem("Fuel", R.drawable.ic_cat_fuel));
            categoryList.add(new CategoryItem("Groceries", R.drawable.ic_cat_groceries));
            categoryList.add(new CategoryItem("Bills", R.drawable.ic_cat_bills));
            categoryList.add(new CategoryItem("Entertainment", R.drawable.ic_cat_entertainment));
            categoryList.add(new CategoryItem("Health", R.drawable.ic_cat_health));
            categoryList.add(new CategoryItem("General", R.drawable.ic_cat_general));
            for (String custom : customExpenseCategories) {
                categoryList.add(new CategoryItem(custom, R.drawable.ic_cat_general));
            }
            categoryList.add(new CategoryItem("+ Custom", R.drawable.ic_add));
        } else {
            categoryList.add(new CategoryItem("Salary", R.drawable.ic_cat_salary));
            categoryList.add(new CategoryItem("Business", R.drawable.ic_cat_shopping));
            categoryList.add(new CategoryItem("Investments", R.drawable.ic_investment));
            categoryList.add(new CategoryItem("Gifts", R.drawable.ic_gift));
            categoryList.add(new CategoryItem("Refund", R.drawable.ic_cat_bills));
            categoryList.add(new CategoryItem("General", R.drawable.ic_cat_general));
            for (String custom : customIncomeCategories) {
                categoryList.add(new CategoryItem(custom, R.drawable.ic_cat_general));
            }
            categoryList.add(new CategoryItem("+ Custom", R.drawable.ic_add));
        }

        boolean exists = false;
        for (CategoryItem item : categoryList) {
            if (item.name.equalsIgnoreCase(selectedCategory)) {
                exists = true;
                break;
            }
        }
        if (!exists && !categoryList.isEmpty()) {
            selectedCategory = categoryList.get(0).name;
        }

        if (recyclerViewCategories != null) {
            recyclerViewCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerViewCategories.setAdapter(new CategoryAdapter(categoryList));
        }
    }

    private void showCustomCategoryInputDialog() {
        vibrateDevice();
        final EditText input = new EditText(this);
        input.setHint("e.g. Subscriptions, Travel");
        input.setSingleLine(true);
        input.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        input.setHintTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        input.setBackgroundResource(R.drawable.rounded_input_background);
        int pad = (int) (14 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin / 2, margin, margin / 2);
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Custom Category")
                .setMessage("Enter category name to log this transaction:")
                .setView(container)
                .setPositiveButton("Add & Select", (dialog, which) -> {
                    String customName = input.getText().toString().trim();
                    if (!customName.isEmpty()) {
                        vibrateDevice();
                        if (isExpense) {
                            if (!customExpenseCategories.contains(customName)) {
                                customExpenseCategories.add(customName);
                            }
                        } else {
                            if (!customIncomeCategories.contains(customName)) {
                                customIncomeCategories.add(customName);
                            }
                        }
                        selectedCategory = customName;
                        setupCategories();
                        if (recyclerViewCategories != null && recyclerViewCategories.getAdapter() != null) {
                            int count = recyclerViewCategories.getAdapter().getItemCount();
                            if (count > 1) {
                                recyclerViewCategories.smoothScrollToPosition(count - 2);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<CategoryItem> items;

        public CategoryAdapter(List<CategoryItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_badge, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryItem item = items.get(position);
            holder.textCategoryName.setText(item.name);
            holder.iconCategory.setImageResource(item.iconRes);

            boolean isAddAction = (item.iconRes == R.drawable.ic_add);
            boolean isSelected = item.name.equalsIgnoreCase(selectedCategory);

            if (isAddAction) {
                holder.circleContainer.setBackgroundResource(R.drawable.bg_category_circle_inactive);
                holder.iconCategory.setColorFilter(ContextCompat.getColor(EntryActivity.this, R.color.color_accent));
                holder.textCategoryName.setTextColor(ContextCompat.getColor(EntryActivity.this, R.color.color_accent));
                holder.textCategoryName.setTypeface(null, Typeface.BOLD);
            } else if (isSelected) {
                holder.circleContainer.setBackgroundResource(R.drawable.bg_category_circle_selected);
                holder.iconCategory.setColorFilter(ContextCompat.getColor(EntryActivity.this, R.color.color_primary));
                holder.textCategoryName.setTextColor(ContextCompat.getColor(EntryActivity.this, R.color.color_primary));
                holder.textCategoryName.setTypeface(null, Typeface.BOLD);
            } else {
                holder.circleContainer.setBackgroundResource(R.drawable.bg_category_circle_inactive);
                holder.iconCategory.setColorFilter(ContextCompat.getColor(EntryActivity.this, R.color.text_secondary));
                holder.textCategoryName.setTextColor(ContextCompat.getColor(EntryActivity.this, R.color.text_secondary));
                holder.textCategoryName.setTypeface(null, Typeface.NORMAL);
            }

            holder.itemView.setOnClickListener(v -> {
                vibrateDevice();
                if (isAddAction) {
                    showCustomCategoryInputDialog();
                } else {
                    selectedCategory = item.name;
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View circleContainer;
            ImageView iconCategory;
            TextView textCategoryName;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                circleContainer = itemView.findViewById(R.id.circleCategoryContainer);
                iconCategory = itemView.findViewById(R.id.iconCategory);
                textCategoryName = itemView.findViewById(R.id.textCategoryName);
            }
        }
    }

    // ==================== BOTTOM SHEET (ADDITIONAL DETAILS) ====================

    private void showAdditionalDetailsBottomSheet() {
        vibrateDevice();
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_entry_details, null);
        dialog.setContentView(sheetView);

        EditText sheetUtr = sheetView.findViewById(R.id.sheetUtr);
        EditText sheetTransactionId = sheetView.findViewById(R.id.sheetTransactionId);
        EditText sheetDescription = sheetView.findViewById(R.id.sheetDescription);
        EditText sheetComments = sheetView.findViewById(R.id.sheetComments);
        View sheetAttachReceipt = sheetView.findViewById(R.id.sheetAttachReceipt);
        TextView sheetReceiptText = sheetView.findViewById(R.id.sheetReceiptText);
        View btnSaveSheetDetails = sheetView.findViewById(R.id.btnSaveSheetDetails);
        View btnDoneSheet = sheetView.findViewById(R.id.btnDoneSheet);
        View btnCancelSheet = sheetView.findViewById(R.id.btnCancelSheet);

        // Pre-fill existing values
        if (sheetUtr != null && !utrVal.isEmpty()) sheetUtr.setText(utrVal);
        if (sheetTransactionId != null && !transactionIdVal.isEmpty()) sheetTransactionId.setText(transactionIdVal);
        if (sheetDescription != null && !descriptionVal.isEmpty()) sheetDescription.setText(descriptionVal);
        if (sheetComments != null && !commentsVal.isEmpty()) sheetComments.setText(commentsVal);

        if (isReceiptAttached && sheetReceiptText != null) {
            sheetReceiptText.setText("Receipt Attached ✓");
        }

        if (sheetAttachReceipt != null) {
            sheetAttachReceipt.setOnClickListener(v -> {
                dialog.dismiss();
                showImageSourceDialog();
            });
        }

        Runnable saveSheet = () -> {
            vibrateDevice();
            if (sheetUtr != null) utrVal = sheetUtr.getText().toString().trim();
            if (sheetTransactionId != null) transactionIdVal = sheetTransactionId.getText().toString().trim();
            if (sheetDescription != null) descriptionVal = sheetDescription.getText().toString().trim();
            if (sheetComments != null) commentsVal = sheetComments.getText().toString().trim();

            updateDetailsBadge();
            dialog.dismiss();
        };

        if (btnSaveSheetDetails != null) btnSaveSheetDetails.setOnClickListener(v -> saveSheet.run());
        if (btnDoneSheet != null) btnDoneSheet.setOnClickListener(v -> saveSheet.run());
        if (btnCancelSheet != null) btnCancelSheet.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateDetailsBadge() {
        boolean hasDetails = !utrVal.isEmpty() || !transactionIdVal.isEmpty() ||
                !descriptionVal.isEmpty() || !commentsVal.isEmpty() || isReceiptAttached;
        if (badgeDetailsStatus != null) {
            if (hasDetails) {
                badgeDetailsStatus.setText("Details Added ✓");
                badgeDetailsStatus.setTextColor(ContextCompat.getColor(this, R.color.color_accent));
            } else {
                badgeDetailsStatus.setText("Optional");
                badgeDetailsStatus.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            }
        }
    }

    // ==================== PAYMENT ACCOUNT CHOOSER ====================

    private void showPaymentAccountChooser() {
        vibrateDevice();
        String[] methods = new String[]{"UPI", "Cash", "Card", "Net Banking", "Cheque"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Payment Method / Account")
                .setItems(methods, (dialog, which) -> {
                    vibrateDevice();
                    selectedPaymentMethod = methods[which];
                    if (textSelectedAccount != null) {
                        textSelectedAccount.setText(selectedPaymentMethod);
                    }
                })
                .show();
    }

    // ==================== DATE FIELD ====================

    private void setDateField() {
        calendar = Calendar.getInstance();
        updateDateDisplay();

        View.OnClickListener listener = v -> {
            vibrateDevice();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    EntryActivity.this, (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateDisplay();
            },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        };

        if (rowDate != null) rowDate.setOnClickListener(listener);
        if (dateTextView != null) dateTextView.setOnClickListener(listener);
    }

    private void updateDateDisplay() {
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        if (dateTextView != null) {
            dateTextView.setText(displayFormat.format(calendar.getTime()));
        }
    }

    // ==================== RECENT PAYEES SUGGESTIONS & CHOOSER ====================

    private final List<String> allRecentPayees = new ArrayList<>();

    private void showRecentPayeesChooser() {
        vibrateDevice();
        if (allRecentPayees.isEmpty()) {
            Toast.makeText(this, "No previous payees found in history", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> options = new ArrayList<>(allRecentPayees);
        options.add("👤 Pick from Contacts");

        CharSequence[] items = options.toArray(new CharSequence[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Recent Payees & Contacts")
                .setItems(items, (dialog, which) -> {
                    vibrateDevice();
                    if (which == items.length - 1) {
                        requestContactPermission();
                    } else {
                        String selected = options.get(which);
                        receiverName.setText(selected);
                        receiverName.setSelection(selected.length());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadRecentPayees() {
        new Thread(() -> {
            Set<String> uniquePayees = new LinkedHashSet<>();
            try {
                File file = com.k7sunny.balancex.data.db.DatabaseMigrator.findJsonFile(this);
                if (file == null) {
                    file = new File("/storage/emulated/0/Documents/Accounting/transactions.json");
                }
                if (file.exists() && file.length() > 0) {
                    FileInputStream fis = new FileInputStream(file);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    fis.close();

                    JSONArray array = new JSONArray(sb.toString());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String receiver = obj.optString("receiver", "").trim();
                        if (!receiver.isEmpty() && !receiver.equalsIgnoreCase("Unknown") &&
                                !receiver.equalsIgnoreCase("N/A") && !receiver.equalsIgnoreCase("NA")) {
                            uniquePayees.add(receiver);
                            if (uniquePayees.size() >= 25) break;
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (uniquePayees.isEmpty()) {
                uniquePayees.add("DMart");
                uniquePayees.add("Amazon");
                uniquePayees.add("Fuel");
            }

            allRecentPayees.clear();
            allRecentPayees.addAll(uniquePayees);
            List<String> payeeList = new ArrayList<>(uniquePayees);
            int chipLimit = Math.min(payeeList.size(), 5);
            List<String> chipList = payeeList.subList(0, chipLimit);

            runOnUiThread(() -> {
                if (layoutPayeeSuggestions != null) {
                    layoutPayeeSuggestions.removeAllViews();
                    for (String payee : chipList) {
                        TextView chip = new TextView(this);
                        chip.setText(payee);
                        chip.setTextSize(12);
                        chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                        chip.setBackgroundResource(R.drawable.bg_chip_suggest);
                        chip.setPadding(
                                (int) (12 * getResources().getDisplayMetrics().density),
                                (int) (5 * getResources().getDisplayMetrics().density),
                                (int) (12 * getResources().getDisplayMetrics().density),
                                (int) (5 * getResources().getDisplayMetrics().density)
                        );
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMarginEnd((int) (6 * getResources().getDisplayMetrics().density));
                        chip.setLayoutParams(params);
                        chip.setClickable(true);
                        chip.setFocusable(true);
                        chip.setOnClickListener(v -> {
                            vibrateDevice();
                            receiverName.setText(payee);
                            receiverName.setSelection(payee.length());
                        });
                        layoutPayeeSuggestions.addView(chip);
                    }
                }
                if (textQuickRecentPayee != null && !payeeList.isEmpty()) {
                    textQuickRecentPayee.setText(payeeList.get(0));
                }
            });
        }).start();
    }

    // ==================== TRANSACTION SAVING ====================

    private void validateAndSaveTransaction() {
        vibrateDevice();

        String rawAmount = amountInput.getText().toString().trim();
        if (rawAmount.isEmpty()) {
            amountInput.requestFocus();
            Toast.makeText(this, "Please enter an amount!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amountVal;
        try {
            amountVal = Double.parseDouble(rawAmount.replaceAll("[^0-9.]", ""));
            if (amountVal <= 0) {
                amountInput.requestFocus();
                Toast.makeText(this, "Amount must be greater than zero!", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            amountInput.requestFocus();
            Toast.makeText(this, "Please enter a valid amount!", Toast.LENGTH_SHORT).show();
            return;
        }

        String receiver = receiverName.getText().toString().trim();
        if (receiver.isEmpty()) {
            receiver = isExpense ? "General Expense" : "Income";
        }

        showSavingDialog();
        final String finalReceiver = receiver;
        new Handler().postDelayed(() -> saveTransaction(finalReceiver), 1000);
    }

    private void saveTransaction(String receiver) {
        SimpleDateFormat storageDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDate = storageDateFormat.format(calendar.getTime());
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.getTime());

        String transactionType = isExpense ? "Debit" : "Credit";
        String paymentMethod = selectedPaymentMethod;
        String amount = amountInput.getText().toString().trim();

        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), FOLDER_NAME);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, FILE_NAME);
        JSONArray transactionsArray = new JSONArray();
        int entryCounter = 1;

        try {
            if (file.exists()) {
                String content = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                }
                if (content != null && !content.trim().isEmpty()) {
                    transactionsArray = new JSONArray(content);

                    for (int i = transactionsArray.length() - 1; i >= 0; i--) {
                        JSONObject obj = transactionsArray.getJSONObject(i);
                        if (obj.has("entryId") && obj.getString("entryId").startsWith(formattedDate)) {
                            entryCounter = Integer.parseInt(obj.getString("entryId").substring(8)) + 1;
                            break;
                        }
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        String entryId = formattedDate + String.format(Locale.getDefault(), "%06d", entryCounter);
        JSONObject transaction = new JSONObject();

        try {
            transaction.put("entryId", entryId);
            transaction.put("date", selectedDate.isEmpty() ? "N/A" : selectedDate);
            transaction.put("amount", amount);
            transaction.put("receiver", receiver);
            transaction.put("description", descriptionVal.isEmpty() ? "NA" : descriptionVal);
            transaction.put("utr", utrVal.isEmpty() ? "NA" : utrVal);
            transaction.put("transactionId", transactionIdVal.isEmpty() ? "NA" : transactionIdVal);
            transaction.put("comments", commentsVal.isEmpty() ? "NA" : commentsVal);
            transaction.put("category", selectedCategory);
            transaction.put("textType", transactionType);
            transaction.put("paymentMethod", paymentMethod);

            transactionsArray.put(transaction);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(transactionsArray.toString(4));
            }

            // Sync with Room
            try {
                com.k7sunny.balancex.data.repository.TransactionRepository repo =
                        new com.k7sunny.balancex.data.repository.TransactionRepository(this);
                com.k7sunny.balancex.data.entity.TransactionEntity entity =
                        new com.k7sunny.balancex.data.entity.TransactionEntity();
                entity.entryId = Long.parseLong(entryId);
                entity.date = selectedDate.isEmpty() ? "N/A" : selectedDate;
                entity.amount = amount;
                entity.receiver = receiver;
                entity.description = descriptionVal.isEmpty() ? "NA" : descriptionVal;
                entity.utr = utrVal.isEmpty() ? "NA" : utrVal;
                entity.transactionId = transactionIdVal.isEmpty() ? "NA" : transactionIdVal;
                entity.comments = commentsVal.isEmpty() ? "NA" : commentsVal;
                entity.category = selectedCategory;
                entity.textType = transactionType;
                entity.paymentMethod = paymentMethod;
                repo.insert(entity, null);
                com.k7sunny.balancex.notifications.AppNotificationManager.postTransactionNotification(this, entity);
            } catch (Exception ignored) {}

            Toast.makeText(this, "Transaction saved successfully!", Toast.LENGTH_SHORT).show();

            // Finish and return to MainActivity
            onBackPressed();

        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving transaction!", Toast.LENGTH_SHORT).show();
        }

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showSavingDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving transaction...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    // ==================== CLEAR FIELDS ====================

    private void clearAllFields() {
        amountInput.setText("");
        receiverName.setText("");
        utrVal = "";
        transactionIdVal = "";
        descriptionVal = "";
        commentsVal = "";
        isReceiptAttached = false;
        updateDetailsBadge();

        calendar = Calendar.getInstance();
        updateDateDisplay();
        applySettingsDefaults();

        Toast.makeText(this, "All fields cleared!", Toast.LENGTH_SHORT).show();
    }

    // ==================== SETTINGS DEFAULTS & NAVIGATION ====================

    private void applySettingsDefaults() {
        String symbol = SettingsManager.getCurrencySymbol(this);
        if (textCurrencySymbol != null) {
            textCurrencySymbol.setText(symbol);
        }

        String defaultType = SettingsManager.getDefaultTransactionType(this);
        if (defaultType != null && defaultType.toLowerCase().contains("credit")) {
            isExpense = false;
        } else {
            isExpense = true;
        }
        updateSegmentedUI();
        setupCategories();

        selectedPaymentMethod = SettingsManager.getDefaultPaymentMethod(this);
        if (textSelectedAccount != null) {
            textSelectedAccount.setText(selectedPaymentMethod);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingsManager.applyTheme(this);
        String symbol = SettingsManager.getCurrencySymbol(this);
        if (textCurrencySymbol != null) {
            textCurrencySymbol.setText(symbol);
        }
    }

    private void setNavigationListeners() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                vibrateDevice();
                finish();
            });
        }

        View navTransactions = findViewById(R.id.navTransactions);
        if (navTransactions != null) {
            navTransactions.setOnClickListener(v -> {
                startActivity(new Intent(this, HistoryActivity.class));
                vibrateDevice();
                finish();
            });
        }

        View navAdd = findViewById(R.id.navAdd);
        if (navAdd != null) {
            navAdd.setOnClickListener(v -> {
                Toast.makeText(this, "Already on Entry Page", Toast.LENGTH_SHORT).show();
            });
        }

        View navExport = findViewById(R.id.navExport);
        if (navExport != null) {
            navExport.setOnClickListener(v -> {
                startActivity(new Intent(this, ReportsActivity.class));
                vibrateDevice();
                finish();
            });
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                vibrateDevice();
                finish();
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        vibrateDevice();
        finish();
    }

    // ==================== CONTACT PICKER ====================

    private void requestContactPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACT_PERMISSION_CODE);
        } else {
            openContactPicker();
        }
    }

    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT_REQUEST);
    }

    // ==================== RECEIPT SCANNER & TESSERACT OCR ====================

    private void button_attach_receipt() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            Toast.makeText(this, "Camera permission requested!", Toast.LENGTH_SHORT).show();
        } else {
            showImageSourceDialog();
        }
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Attach Receipt")
                .setItems(new String[]{"Capture Photo", "Choose from Gallery"}, (dialog, which) -> {
                    showWarningDialog(which);
                })
                .show();
    }

    private void showWarningDialog(int selectedOption) {
        AlertDialog.Builder warningBuilder = new AlertDialog.Builder(this);
        warningBuilder.setTitle("Receipt Scanner")
                .setMessage("Scanning receipts will parse transaction details automatically using OCR.\n\nDo you want to continue?")
                .setCancelable(false);

        warningBuilder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        warningBuilder.setPositiveButton("Proceed (3)", null);

        AlertDialog warningDialog = warningBuilder.create();
        warningDialog.show();

        Button goButton = warningDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        goButton.setEnabled(false);

        new CountDownTimer(3000, 1000) {
            public void onTick(long millisUntilFinished) {
                goButton.setText("Proceed (" + (millisUntilFinished / 1000) + ")");
            }

            public void onFinish() {
                goButton.setEnabled(true);
                goButton.setText("Proceed");

                goButton.setOnClickListener(v -> {
                    if (selectedOption == 0) {
                        captureImageFromCamera();
                    } else {
                        pickImageFromGallery();
                    }
                    warningDialog.dismiss();
                });
            }
        }.start();
    }

    private void captureImageFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void pickImageFromGallery() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickIntent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImageSourceDialog();
            } else {
                Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CONTACT_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openContactPicker();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
            try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    receiverName.setText(contactName);
                    receiverName.setSelection(contactName.length());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if ((requestCode == REQUEST_IMAGE_PICK || requestCode == REQUEST_IMAGE_CAPTURE) && resultCode == RESULT_OK) {
            isReceiptAttached = true;
            updateDetailsBadge();

            Bitmap bitmap = null;
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                try {
                    Uri imageUri = data.getData();
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null && data.getExtras() != null) {
                bitmap = (Bitmap) data.getExtras().get("data");
            }

            if (bitmap != null) {
                processImage(bitmap);
            }
        }
    }

    private void initTesseract() {
        tessBaseAPI = new TessBaseAPI();
        File tessDir = getFilesDir();
        String tessPath = tessDir.getAbsolutePath();

        File trainedData = new File(tessPath + "/tessdata/eng.traineddata");
        if (!trainedData.exists()) {
            copyTessDataFromAssets();
        }

        if (tessBaseAPI.init(tessPath, "eng", TessBaseAPI.OEM_DEFAULT)) {
            Log.d("Tesseract", "Tesseract initialized successfully");
        } else {
            Log.e("Tesseract", "Tesseract initialization failed!");
            tessBaseAPI = null;
        }
    }

    private void copyTessDataFromAssets() {
        try {
            AssetManager assetManager = getAssets();
            InputStream in = assetManager.open("tessdata/eng.traineddata");
            File tessDir = new File(getFilesDir(), "tessdata");
            if (!tessDir.exists()) {
                tessDir.mkdirs();
            }
            File outFile = new File(tessDir, "eng.traineddata");
            OutputStream out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e("Tesseract", "Error copying tessdata: " + e.getMessage());
        }
    }

    private void copyTessDataFiles() {
        File tessDir = new File(getFilesDir(), "tessdata");
        if (!tessDir.exists()) {
            tessDir.mkdirs();
        }
        String[] languages = {"eng", "hin", "osd"};

        for (String lang : languages) {
            File trainedDataFile = new File(tessDir, lang + ".traineddata");
            if (!trainedDataFile.exists()) {
                try (InputStream in = getAssets().open("tessdata/" + lang + ".traineddata");
                     OutputStream out = new FileOutputStream(trainedDataFile)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                } catch (IOException e) {
                    Log.e("Tesseract", "Failed to copy " + lang + ".traineddata: " + e.getMessage());
                }
            }
        }
    }

    private void processImage(Bitmap bitmap) {
        showLoadingDialog();
        new Thread(() -> {
            if (tessBaseAPI == null) {
                initTesseract();
            }

            if (tessBaseAPI != null) {
                tessBaseAPI.setImage(bitmap);
                String extractedText = tessBaseAPI.getUTF8Text();
                Log.d("Tesseract", "Extracted Text: " + extractedText);

                runOnUiThread(() -> {
                    dismissLoadingDialog();
                    populateForm(extractedText);
                });
            } else {
                runOnUiThread(this::dismissLoadingDialog);
            }
        }).start();
    }

    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void populateForm(String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFC);

        Pattern amountPattern = Pattern.compile("(?:Rs|INR|Amount|€|₹|¥|§|\\$)\\s*[:₹]?[\\s]*([0-9]+(?:[.,][0-9]+)?)");
        Pattern utrPattern = Pattern.compile("(UTR: |UPI transaction ID: |UPI transaction ID|UPI Ref ID:|UPI Ref ID)[:\\s]*([A-Za-z0-9]+)");
        Pattern transactionIdPattern = Pattern.compile("Transaction ID\\s*([A-Za-z0-9]+)");
        Pattern datePattern = Pattern.compile("(\\d{2} [A-Za-z]{3,10} \\d{4})");
        Pattern receiverPattern = Pattern.compile("(to|To|Paid to|From|receiver|To:|Paid To:|Received from|From:|Received From:)[:\\s]*([A-Za-z ]+)");

        Matcher amountMatcher = amountPattern.matcher(text);
        Matcher utrMatcher = utrPattern.matcher(text);
        Matcher transactionIdMatcher = transactionIdPattern.matcher(text);
        Matcher dateMatcher = datePattern.matcher(text);
        Matcher receiverMatcher = receiverPattern.matcher(text);

        if (amountMatcher.find()) {
            String amount = amountMatcher.group(1).replaceAll(",", "");
            amountInput.setText(amount);
        }
        if (utrMatcher.find()) {
            utrVal = utrMatcher.group(2);
        }
        if (transactionIdMatcher.find()) {
            transactionIdVal = transactionIdMatcher.group(1);
        }
        if (dateMatcher.find()) {
            String extractedDate = dateMatcher.group(1);
            String formattedDate = formatDate(extractedDate);
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                Date parsedDate = sdf.parse(formattedDate);
                if (parsedDate != null) {
                    calendar.setTime(parsedDate);
                    updateDateDisplay();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (receiverMatcher.find()) {
            receiverName.setText(receiverMatcher.group(2).trim());
        }

        selectedPaymentMethod = "UPI";
        if (textSelectedAccount != null) {
            textSelectedAccount.setText("UPI");
        }

        if (text.contains("Paid to")) {
            isExpense = true;
        } else if (text.contains("Received from") || text.contains("From")) {
            isExpense = false;
        }
        updateSegmentedUI();
        setupCategories();
        updateDetailsBadge();

        Toast.makeText(this, "Receipt details parsed!", Toast.LENGTH_SHORT).show();
    }

    private String formatDate(String extractedDate) {
        SimpleDateFormat inputFormatShort = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        SimpleDateFormat inputFormatFull = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        try {
            Date date;
            if (extractedDate.matches("\\d{2} [A-Za-z]{3} \\d{4}")) {
                date = inputFormatShort.parse(extractedDate);
            } else if (extractedDate.matches("\\d{2} [A-Za-z]+ \\d{4}")) {
                date = inputFormatFull.parse(extractedDate);
            } else {
                return extractedDate;
            }
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return extractedDate;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                        .setTitle("Storage Permission Required")
                        .setMessage("This app requires access to manage all files on your device. Please grant permission to continue.")
                        .setPositiveButton("Allow", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setTitle("Storage Permission Required")
                        .setMessage("This app needs storage access to save your transactions. Please grant permission.")
                        .setPositiveButton("Allow", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }
    }

    public void vibrateDevice() {
        SettingsManager.vibrate(this);
    }
}
