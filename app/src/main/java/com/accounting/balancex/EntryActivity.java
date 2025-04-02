package com.accounting.balancex;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.widget.Toast;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;


public class EntryActivity extends AppCompatActivity {
    private static final int PICK_CONTACT_REQUEST = 1;
    private static final int CONTACT_PERMISSION_CODE = 101;
    private static final int STORAGE_PERMISSION_CODE = 102;
    private EditText amountInput, receiverName, description, utr, comments, categoryInput,transactionId;
    private CheckBox confirmCheckBox;
    private TextView dateTextView, clearAllButton; // Removed timeTextView
    private RadioGroup paymentMethodGroup, transactionTypeGroup;
    private Button saveButton, addFromContactButton;
    private Calendar calendar;
    private static final String FOLDER_NAME = "Accounting";
    private static final String FILE_NAME = "transactions.json";
    private LinearLayout navHome, navTransactions, navEntry;
    private static final int REQUEST_IMAGE_CAPTURE = 103;
    private static final int REQUEST_IMAGE_PICK = 104;
    private static final int CAMERA_PERMISSION_CODE = 105;
    private Button attachReceiptButton;
    private Uri imageUri;
    private static final String TESS_DATA_PATH = "/tessdata/";
    private static final String TESS_LANG = "eng+hin+osd";
    private TessBaseAPI tessBaseAPI;
    // Declare dialog variable at the top of EntryActivity
    private AlertDialog loadingDialog;
    private boolean isReceiptAttached = false;  // Default: No receipt attached
    private LinearLayout moreDetailsUPI, transactionDetail;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        initializeUI();
        requestStoragePermission();
        setNavigationListeners();
        setDateField(); // Setting up the date field
        copyTessDataFiles(); // Ensure trained data is available
        initTesseract(); // Now initialize Tesseracts

        attachReceiptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isReceiptAttached = true;  // ✅ Mark receipt as attached

                button_attach_receipt();  // ✅ Open camera/gallery for receipt selection

                selectPaymentMethod("UPI");  // ✅ Auto-select UPI
            }
        });

        clearAllButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear All Fields?")
                    .setMessage("Are you sure you want to clear all input fields?")
                    .setPositiveButton("Yes", (dialog, which) -> clearAllFields())
                    .setNegativeButton("No", null)
                    .show();
        });
        moreDetailsUPI.setVisibility(View.GONE);
        paymentMethodGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioButtonUPI) {
                    // Slide in moreDetailsUPI
                    moreDetailsUPI.setVisibility(View.VISIBLE);
                    moreDetailsUPI.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in));

                    // Move the transactionDetail layout with same timing
                    transactionDetail.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up));
                } else if (checkedId == R.id.radioButtonCash) {
                    // Slide out moreDetailsUPI
                    Animation slideOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out);
                    moreDetailsUPI.startAnimation(slideOut);
                    slideOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            moreDetailsUPI.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });

                    // Slide transactionDetail layout down to adjust
                    Animation slideDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);
                    transactionDetail.startAnimation(slideDown);
                }
            }
        });

    }
    private void initializeUI() {
        dateTextView = findViewById(R.id.dateTextView);
        amountInput = findViewById(R.id.amountInput);
        receiverName = findViewById(R.id.receiverName);
        description = findViewById(R.id.description);
        utr = findViewById(R.id.utr);
        transactionId = findViewById(R.id.transactionId);
        comments = findViewById(R.id.comments);
        categoryInput = findViewById(R.id.inputCategory);
        paymentMethodGroup = findViewById(R.id.paymentMethodGroup);
        transactionTypeGroup = findViewById(R.id.TransactiontypeGroup);
        saveButton = findViewById(R.id.saveButton);
        addFromContactButton = findViewById(R.id.addFromContactButton);
        confirmCheckBox = findViewById(R.id.confirmCheckBox);
        navHome = findViewById(R.id.navHome);
        navTransactions = findViewById(R.id.navTransactions);
        navEntry = findViewById(R.id.navEntry);
//        utr_layout = findViewById(R.id.utr_layout);
//        transactionId_layout = findViewById(R.id.transactionId_layout);
        moreDetailsUPI = findViewById(R.id.moreDetailsUPI);
        clearAllButton = findViewById(R.id.clearAllButton);
        transactionDetail = findViewById(R.id.transactionDetail);

        // Initially disable save button
        saveButton.setEnabled(false);
        saveButton.setBackgroundResource(R.drawable.rounded_corner_container_disabled);
        // Checkbox listener
        confirmCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                saveButton.setEnabled(true);
                saveButton.setBackgroundResource(R.drawable.rounded_corner_container_save);
            } else {
                saveButton.setEnabled(false);
                saveButton.setBackgroundResource(R.drawable.rounded_corner_container_disabled);
            }
        });
        saveButton.setOnClickListener(v -> {
            if (!confirmCheckBox.isChecked()) {
                Log.d("EntryActivity", "Checkbox not checked! Showing Toast...");
                runOnUiThread(() ->
                        Toast.makeText(getApplicationContext(), "Please check the checkbox", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            // Validate required fields before showing the dialog
            boolean isValid = true;
            if (amountInput.getText().toString().isEmpty()) {
                amountInput.setBackgroundResource(R.drawable.red_outline);
                isValid = false;
            } else {
                amountInput.setBackgroundResource(R.drawable.rounded_input_background);
            }

            if (receiverName.getText().toString().isEmpty()) {
                receiverName.setBackgroundResource(R.drawable.red_outline);
                isValid = false;
            } else {
                receiverName.setBackgroundResource(R.drawable.rounded_input_background);
            }

            int selectedTypeId = transactionTypeGroup.getCheckedRadioButtonId();
            if (selectedTypeId == -1) {
                transactionTypeGroup.setBackgroundResource(R.drawable.red_outline);
                isValid = false;
            } else {
                transactionTypeGroup.setBackgroundResource(0);
            }

            int selectedPaymentId = paymentMethodGroup.getCheckedRadioButtonId();
            if (selectedPaymentId == -1) {
                paymentMethodGroup.setBackgroundResource(R.drawable.red_outline);
                isValid = false;
            } else {
                paymentMethodGroup.setBackgroundResource(0);
            }

            if (!isValid) {
                Toast.makeText(this, "Please fill all required fields!", Toast.LENGTH_SHORT).show();
                return;  // Stop execution if fields are missing
            }

            // Now that everything is valid, show the saving dialog and proceed
            showSavingDialog();
            new Handler().postDelayed(this::saveTransaction, 1500);
        });

        //Add from contact button calling
        addFromContactButton.setOnClickListener(v -> requestContactPermission());
        //Attach receipt button calling
        attachReceiptButton = findViewById(R.id.button_attach_receipt);
        attachReceiptButton.setOnClickListener(v -> showImageSourceDialog());
    }
    private void setNavigationListeners() {
        hideNavText(); // Hide text initially

        // Apply animation to the correct tab
        applyNavAnimation(findViewById(R.id.navEntry)); // Change to R.id.navEntry in EntryActivity
        // Navigation Click Listeners
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            vibrateDevice();
            finish();
        });

        findViewById(R.id.navTransactions).setOnClickListener(v -> {
            startActivity(new Intent(this, TransactionActivity.class));
            vibrateDevice();
            finish();
        });

        findViewById(R.id.navEntry).setOnClickListener(v -> {
            Toast.makeText(this, "Already on Entry Page", Toast.LENGTH_SHORT).show();
        });
    }
    // Override onBackPressed to go back to MainActivity
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        vibrateDevice(); // Optional if you want feedback on back press
        finish();
    }
    private void hideNavText() {
        ((TextView) ((LinearLayout) findViewById(R.id.navHome)).getChildAt(1)).setVisibility(View.INVISIBLE);
        ((TextView) ((LinearLayout) findViewById(R.id.navTransactions)).getChildAt(1)).setVisibility(View.INVISIBLE);
        ((TextView) ((LinearLayout) findViewById(R.id.navEntry)).getChildAt(1)).setVisibility(View.INVISIBLE);
    }

    private void applyNavAnimation(LinearLayout selectedNavItem) {
        // Get the TextView inside the selected navigation item
        TextView textView = (TextView) ((LinearLayout) selectedNavItem).getChildAt(1);
        // Load the animation
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_nav);

        // Apply the animation and make it visible
        textView.setVisibility(View.VISIBLE);
        textView.startAnimation(slideUp);
    }
    private void setDateField() {
        calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateTextView.setText(sdf.format(calendar.getTime())); // Display the current date by default

        dateTextView.setOnClickListener(v -> {
            Log.d("EntryActivity", "Date TextView clicked");
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    EntryActivity.this, (view, year, month, dayOfMonth) -> {
                Log.d("EntryActivity", "Selected date: " + year + "-" + (month + 1) + "-" + dayOfMonth);
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                dateTextView.setText(sdf.format(calendar.getTime())); // Update TextView with selected date
            },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            // Set the maximum date to today's date to prevent future date selection
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            datePickerDialog.show();
        });
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                Uri contactUri = data.getData();
                String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
                try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        String contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        receiverName.setText(contactName);
                    }
                }
            }
        }
        //upi receipt scanner section
        if (requestCode == 104 && resultCode == RESULT_OK) {
            try {
                Uri imageUri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                if (bitmap != null) {
                    processImage(bitmap); // Process OCR
                } else {
                    Log.e("Tesseract", "Bitmap is null, cannot process image.");
                }
            } catch (Exception e) {
                Log.e("Tesseract", "Error processing image: " + e.getMessage());
            }
        }
    }
    private void showSavingDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait, transaction being saved...");
        progressDialog.setCancelable(false); // Prevent dismissal
        progressDialog.show();
    }
    // Helper method to return "N/A" if the input is empty
    private String getOrNA(EditText editText) {
        String text = editText.getText().toString().trim();
        return text.isEmpty() ? "NA" : text; // Use "NA" instead of "N/A"
    }
    private void saveTransaction() {
        String selectedDate = dateTextView.getText().toString();
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.getTime());

        RadioButton selectedTransactionType = findViewById(transactionTypeGroup.getCheckedRadioButtonId());
        String transactionType = selectedTransactionType != null ? selectedTransactionType.getText().toString() : "N/A";

        RadioButton selectedPaymentMethod = findViewById(paymentMethodGroup.getCheckedRadioButtonId());
        String paymentMethod = selectedPaymentMethod != null ? selectedPaymentMethod.getText().toString() : "N/A";

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
                transactionsArray = new JSONArray(content);

                for (int i = transactionsArray.length() - 1; i >= 0; i--) {
                    JSONObject obj = transactionsArray.getJSONObject(i);
                    if (obj.has("entryId") && obj.getString("entryId").startsWith(formattedDate)) {
                        entryCounter = Integer.parseInt(obj.getString("entryId").substring(8)) + 1;
                        break;
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        String entryId = formattedDate + String.format("%06d", entryCounter);
        JSONObject transaction = new JSONObject();

        try {
            transaction.put("entryId", entryId);
            transaction.put("date", selectedDate.isEmpty() ? "N/A" : selectedDate);
            transaction.put("amount", getOrNA(amountInput));
            transaction.put("receiver", getOrNA(receiverName));
            transaction.put("description", getOrNA(description));
            transaction.put("utr", getOrNA(utr));
            transaction.put("transactionId", getOrNA(transactionId));
            transaction.put("comments", getOrNA(comments));
            transaction.put("category", getOrNA(categoryInput));
            transaction.put("textType", transactionType);
            transaction.put("paymentMethod", paymentMethod);

            transactionsArray.put(transaction);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(transactionsArray.toString(4));
            }

            Toast.makeText(this, "Transaction saved successfully!", Toast.LENGTH_SHORT).show();

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    // UPI receipt scanner sections
    private void button_attach_receipt() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            Toast.makeText(this, "Camera permission requested!", Toast.LENGTH_SHORT).show();
        } else {
            showImageSourceDialog();
        }
    }
    //tesseract
    private void initTesseract() {
        tessBaseAPI = new TessBaseAPI(); // Initialize the object
        File tessDir = getFilesDir();
        String tessPath = tessDir.getAbsolutePath(); // Internal storage path

        // Copy tessdata from assets if it doesn't exist
        File trainedData = new File(tessPath + "/tessdata/eng.traineddata");
        if (!trainedData.exists()) {
            copyTessDataFromAssets();
        }

        if (tessBaseAPI.init(tessPath, "eng", TessBaseAPI.OEM_DEFAULT)) {
            Log.d("Tesseract", "Tesseract initialized successfully");
        } else {
            Log.e("Tesseract", "Tesseract initialization failed!");
            tessBaseAPI = null; // Set to null if initialization fails
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
            tessDir.mkdirs(); // Create tessdata folder if it doesn't exist
        }
        String[] languages = {"eng", "hin", "osd"}; // Required languages

        for (String lang : languages) {
            File trainedDataFile = new File(tessDir, lang + ".traineddata");

            if (!trainedDataFile.exists()) { // Copy only if missing
                try (InputStream in = getAssets().open("tessdata/" + lang + ".traineddata");
                     OutputStream out = new FileOutputStream(trainedDataFile)) {

                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    Log.d("Tesseract", lang + ".traineddata copied successfully!");

                } catch (IOException e) {
                    Log.e("Tesseract", "Failed to copy " + lang + ".traineddata: " + e.getMessage());
                }
            } else {
                Log.d("Tesseract", lang + ".traineddata already exists, skipping copy.");
            }
        }
    }
    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Attach Receipt")
                .setItems(new String[]{"Capture Photo", "Choose from Gallery"}, (dialog, which) -> {
                    // Show warning message before proceeding
                    showWarningDialog(which);
                })
                .show();
    }

    private void showWarningDialog(int selectedOption) {
        AlertDialog.Builder warningBuilder = new AlertDialog.Builder(this);
        warningBuilder.setTitle("Warning!")
                .setMessage("Scanning receipts may result in incorrect or unregistered inputs.\n\nDo you want to continue?")
                .setCancelable(false);

        // Create the dialog first
        AlertDialog warningDialog = warningBuilder.create();

        // Set buttons initially (without actions)
        warningBuilder.setNegativeButton("Go Back", (dialog, which) -> dialog.dismiss());
        warningBuilder.setPositiveButton("Go (5)", null); // Set initial text but no action yet

        warningDialog = warningBuilder.create();
        warningDialog.show();

        // Get the "Go" button and disable it initially
        Button goButton = warningDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        goButton.setEnabled(false);

        // Start a countdown timer for 5 seconds
        AlertDialog finalWarningDialog = warningDialog;
        new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {
                goButton.setText("Go (" + (millisUntilFinished / 1000) + ")");
            }

            public void onFinish() {
                goButton.setEnabled(true);
                goButton.setText("Go");

                // Set the final action when the button is clicked
                goButton.setOnClickListener(v -> {
                    if (selectedOption == 0) {
                        captureImageFromCamera();
                    } else {
                        pickImageFromGallery();
                    }
                    finalWarningDialog.dismiss();
                });
            }
        }.start();
    }
    private void captureImageFromCamera() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            Toast.makeText(this, "Camera opened!", Toast.LENGTH_SHORT).show();
        }
    }
    private void pickImageFromGallery() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickIntent, REQUEST_IMAGE_PICK);
        Toast.makeText(this, "Gallery opened!", Toast.LENGTH_SHORT).show();
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
        }
    }

    private void processImage(Bitmap bitmap) {
        showLoadingDialog(); // Show loading animation

        new Thread(() -> {
            if (tessBaseAPI == null) {
                Log.e("Tesseract", "TessBaseAPI is null! Re-initializing...");
                initTesseract();
            }

            if (tessBaseAPI != null) {
                tessBaseAPI.setImage(bitmap);
                String extractedText = tessBaseAPI.getUTF8Text();
                Log.d("Tesseract", "Extracted Text: " + extractedText);

                runOnUiThread(() -> {
                    dismissLoadingDialog(); // Hide loading animation
                    populateForm(extractedText); // Fill the form with extracted text
                });
            } else {
                Log.e("Tesseract", "Failed to initialize Tesseract API.");
                runOnUiThread(this::dismissLoadingDialog); // Hide loading animation if OCR fails
            }
        }).start();
    }
    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevent user from closing it manually
        loadingDialog = builder.create();
        loadingDialog.show();
    }
    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
    //fills form automatically from images
    private void populateForm(String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFC);

        Pattern amountPattern = Pattern.compile("(?:Rs|INR|Amount|€|₹|¥|§|\\$)\\s*[:₹]?[\\s]*([0-9]+(?:[.,][0-9]+)?)");
        Pattern utrPattern = Pattern.compile("(UTR: |UPI transaction ID: |UPI transaction ID|UPI Ref ID:|UPI Ref ID)[:\\s]*([A-Za-z0-9]+)");
        Pattern transactionIdPattern = Pattern.compile("Transaction ID\\s*([A-Za-z0-9]+)");
        Pattern datePattern = Pattern.compile("(\\d{2} [A-Za-z]{3,10} \\d{4})");
        Pattern receiverPattern = Pattern.compile("(to|To|Paid to|From|receiver|To:|Paid To:|Received from|From:|Received From:)[:\\s]*([A-Za-z ]+)");
        //Matching time
        Matcher amountMatcher = amountPattern.matcher(text);
        Matcher utrMatcher = utrPattern.matcher(text);
        Matcher transactionIdMatcher = transactionIdPattern.matcher(text);
        Matcher dateMatcher = datePattern.matcher(text);
        Matcher receiverMatcher = receiverPattern.matcher(text);
        if (amountMatcher.find()) {
            String amount = amountMatcher.group(1).replaceAll(",", ""); // Remove commas
            amountInput.setText(amount);
        }
        if (utrMatcher.find()) {
            utr.setText(utrMatcher.group(2));
        }
        if (dateMatcher.find()) {
            String extractedDate = dateMatcher.group(1);
            String formattedDate = formatDate(extractedDate);
            dateTextView.setText(formattedDate);

            // Update calendar instance
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                Date parsedDate = sdf.parse(formattedDate);
                if (parsedDate != null) {
                    calendar.setTime(parsedDate); // Ensure calendar is updated
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (receiverMatcher.find()) {
            receiverName.setText(receiverMatcher.group(2));
        }
        if (transactionIdMatcher.find()) {
            ((EditText) findViewById(R.id.transactionId)).setText(transactionIdMatcher.group(1));
        }
        // Auto-select Payment Method (If user selects "Attach Receipt", it's UPI)
        if (isReceiptAttached) {
            selectPaymentMethod("UPI");
        }
        // Auto-select Transaction Type
        if (text.contains("Paid to")) {
            selectTransactionType("Debit");
        } else if (text.contains("Received from") || text.contains("From")) {
            selectTransactionType("Credit");
        }

        Log.d("Tesseract", "Form updated with extracted data.");
    }
        // Function to auto-select a payment method in RadioGroup
    private void selectPaymentMethod(String method) {
        for (int i = 0; i < paymentMethodGroup.getChildCount(); i++) {
            View view = paymentMethodGroup.getChildAt(i);
                if (view instanceof RadioButton) {
                    RadioButton radioButton = (RadioButton) view;
                    if (radioButton.getText().toString().equalsIgnoreCase(method)) {
                        radioButton.setChecked(true);
                        break;
                    }
                }
            }
        }
    // Function to auto-select a transaction type in RadioGroup
    private void selectTransactionType(String type) {
        for (int i = 0; i < transactionTypeGroup.getChildCount(); i++) {
            View view = transactionTypeGroup.getChildAt(i);
            if (view instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) view;
                if (radioButton.getText().toString().equalsIgnoreCase(type)) {
                    radioButton.setChecked(true);
                    break;
                }
            }
        }
    }
    private String formatDate(String extractedDate) {
        SimpleDateFormat inputFormatShort = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH); // e.g., 02 Mar 2025
        SimpleDateFormat inputFormatFull = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH); // e.g., 02 March 2025
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH); // Desired format
        try {
            Date date;
            if (extractedDate.matches("\\d{2} [A-Za-z]{3} \\d{4}")) { // Short month format
                date = inputFormatShort.parse(extractedDate);
            } else if (extractedDate.matches("\\d{2} [A-Za-z]+ \\d{4}")) { // Full month format
                date = inputFormatFull.parse(extractedDate);
            } else {
                return extractedDate; // If format doesn't match, return original
            }
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return extractedDate; // Return original if parsing fails
        }
    }
    //clear all
    private void clearAllFields() {
        amountInput.setText("");
        receiverName.setText("");
        description.setText("");
        utr.setText("");
        transactionId.setText("");
        comments.setText("");
        categoryInput.setText("");

        // Reset the date field to today’s date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        calendar = Calendar.getInstance();
        dateTextView.setText(sdf.format(calendar.getTime()));

        // Uncheck selected radio buttons
        transactionTypeGroup.clearCheck();
        paymentMethodGroup.clearCheck();

        Toast.makeText(this, "All fields cleared!", Toast.LENGTH_SHORT).show();
    }
    public void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(30); // Deprecated in API 26+, but works for older versions
            }
        }
    }
}
