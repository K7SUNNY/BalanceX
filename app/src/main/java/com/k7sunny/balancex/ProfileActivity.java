package com.k7sunny.balancex;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.k7sunny.balancex.data.entity.TransactionEntity;
import com.k7sunny.balancex.data.repository.TransactionRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 101;
    private static final String PREF_NAME = "UserProfile";

    private SharedPreferences sharedPreferences;

    // Main Screen Views
    private ImageView profileAvatarImage;
    private TextView textProfileFullName;
    private TextView textProfileBio;
    private TextView textProfileStatEntries;
    private TextView textProfileStatDays;
    private TextView textProfileStatCurrency;
    private TextView textProfileCompany;
    private TextView textProfileEmail;
    private TextView textProfilePhone;
    private TextView textProfileAddress;
    private SwitchMaterial switchIncludeInPdf;

    // Temporary storage for sheet avatar picker
    private Uri pendingImageUri;
    private ImageView sheetAvatarPreviewRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.applyTheme(this);
        setContentView(R.layout.activity_profile);

        // Apply Edge-to-Edge System Bar Insets
        View root = findViewById(R.id.profileRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(root);
        }

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        initViews();
        loadProfileData();
        loadLedgerStats();
    }

    private void initViews() {
        profileAvatarImage = findViewById(R.id.profileAvatarImage);
        textProfileFullName = findViewById(R.id.textProfileFullName);
        textProfileBio = findViewById(R.id.textProfileBio);
        textProfileStatEntries = findViewById(R.id.textProfileStatEntries);
        textProfileStatDays = findViewById(R.id.textProfileStatDays);
        textProfileStatCurrency = findViewById(R.id.textProfileStatCurrency);
        textProfileCompany = findViewById(R.id.textProfileCompany);
        textProfileEmail = findViewById(R.id.textProfileEmail);
        textProfilePhone = findViewById(R.id.textProfilePhone);
        textProfileAddress = findViewById(R.id.textProfileAddress);
        switchIncludeInPdf = findViewById(R.id.switchIncludeInPdf);

        // Back Button
        View btnBack = findViewById(R.id.btnProfileBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                SettingsManager.vibrate(this);
                finish();
            });
        }

        // Top "Edit" Button
        View btnTopEdit = findViewById(R.id.btnProfileTopEdit);
        if (btnTopEdit != null) {
            btnTopEdit.setOnClickListener(v -> {
                SettingsManager.vibrate(this);
                showEditProfileBottomSheet();
            });
        }

        // Camera Badge on Avatar
        View btnChangeAvatar = findViewById(R.id.btnChangeAvatarBadge);
        if (btnChangeAvatar != null) {
            btnChangeAvatar.setOnClickListener(v -> {
                SettingsManager.vibrate(this);
                showEditProfileBottomSheet();
            });
        }

        // Bottom Primary "Edit Profile Details" Button
        View btnOpenEdit = findViewById(R.id.btnOpenEditProfile);
        if (btnOpenEdit != null) {
            btnOpenEdit.setOnClickListener(v -> {
                SettingsManager.vibrate(this);
                showEditProfileBottomSheet();
            });
        }

        // PDF Statement Branding Switch
        if (switchIncludeInPdf != null) {
            boolean isChecked = sharedPreferences.getBoolean("includeInPdf", true);
            switchIncludeInPdf.setChecked(isChecked);
            switchIncludeInPdf.setOnCheckedChangeListener((buttonView, checked) -> {
                SettingsManager.vibrate(this);
                sharedPreferences.edit().putBoolean("includeInPdf", checked).apply();
            });
        }

        // Vault Backup Shortcut Button
        View btnBackup = findViewById(R.id.btnProfileVaultBackup);
        if (btnBackup != null) {
            btnBackup.setOnClickListener(v -> {
                SettingsManager.vibrate(this);
                backupLedgerData();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
        loadLedgerStats();
    }

    private void loadProfileData() {
        String name = sharedPreferences.getString("userName", "User Name");
        String bio = sharedPreferences.getString("bio", "Personal Ledger");
        String company = sharedPreferences.getString("companyName", "");
        String email = sharedPreferences.getString("email", "");
        String phone = sharedPreferences.getString("phone", "");
        String address = sharedPreferences.getString("address", "");
        String imageUriString = sharedPreferences.getString("profileImageUri", "");

        if (textProfileFullName != null) {
            textProfileFullName.setText(name.isEmpty() ? "User Name" : name);
        }
        if (textProfileBio != null) {
            textProfileBio.setText(bio.isEmpty() ? "Personal Ledger" : bio);
        }
        if (textProfileCompany != null) {
            textProfileCompany.setText(company.isEmpty() ? "Not configured" : company);
            textProfileCompany.setTextColor(ContextCompat.getColor(this, company.isEmpty() ? R.color.text_tertiary : R.color.text_primary));
        }
        if (textProfileEmail != null) {
            textProfileEmail.setText(email.isEmpty() ? "Not configured" : email);
            textProfileEmail.setTextColor(ContextCompat.getColor(this, email.isEmpty() ? R.color.text_tertiary : R.color.text_primary));
        }
        if (textProfilePhone != null) {
            textProfilePhone.setText(phone.isEmpty() ? "Not configured" : phone);
            textProfilePhone.setTextColor(ContextCompat.getColor(this, phone.isEmpty() ? R.color.text_tertiary : R.color.text_primary));
        }
        if (textProfileAddress != null) {
            textProfileAddress.setText(address.isEmpty() ? "Not configured" : address);
            textProfileAddress.setTextColor(ContextCompat.getColor(this, address.isEmpty() ? R.color.text_tertiary : R.color.text_primary));
        }

        if (profileAvatarImage != null) {
            ProfileHelper.loadAvatar(this, profileAvatarImage);
        }
    }

    private void loadLedgerStats() {
        TransactionRepository repo = new TransactionRepository(this);
        repo.getAllTransactions(transactions -> runOnUiThread(() -> {
            int count = (transactions != null) ? transactions.size() : 0;
            if (textProfileStatEntries != null) {
                textProfileStatEntries.setText(String.valueOf(count));
            }

            Set<String> uniqueDates = new HashSet<>();
            if (transactions != null) {
                for (TransactionEntity t : transactions) {
                    if (t.date != null && !t.date.trim().isEmpty() && !t.date.equalsIgnoreCase("N/A")) {
                        uniqueDates.add(t.date);
                    }
                }
            }
            if (textProfileStatDays != null) {
                int days = uniqueDates.size();
                textProfileStatDays.setText(days + (days == 1 ? " Day" : " Days"));
            }

            if (textProfileStatCurrency != null) {
                String full = SettingsManager.getCurrencyFull(this);
                int paren = full.indexOf('(');
                String shortCurr = (paren > 0) ? full.substring(0, paren).trim() : SettingsManager.getCurrencySymbol(this);
                textProfileStatCurrency.setText(shortCurr);
            }
        }));
    }

    private void showEditProfileBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_edit_profile, null);
        dialog.setContentView(view);

        View btnClose = view.findViewById(R.id.btnCloseEditProfile);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        sheetAvatarPreviewRef = view.findViewById(R.id.editSheetAvatarPreview);
        TextInputEditText editName = view.findViewById(R.id.editSheetFullName);
        TextInputEditText editBio = view.findViewById(R.id.editSheetBio);
        TextInputEditText editCompany = view.findViewById(R.id.editSheetCompany);
        TextInputEditText editEmail = view.findViewById(R.id.editSheetEmail);
        TextInputEditText editPhone = view.findViewById(R.id.editSheetPhone);
        TextInputEditText editAddress = view.findViewById(R.id.editSheetAddress);

        // Pre-fill existing data
        if (editName != null) editName.setText(sharedPreferences.getString("userName", ""));
        if (editBio != null) editBio.setText(sharedPreferences.getString("bio", ""));
        if (editCompany != null) editCompany.setText(sharedPreferences.getString("companyName", ""));
        if (editEmail != null) editEmail.setText(sharedPreferences.getString("email", ""));
        if (editPhone != null) editPhone.setText(sharedPreferences.getString("phone", ""));
        if (editAddress != null) editAddress.setText(sharedPreferences.getString("address", ""));

        String savedUriString = sharedPreferences.getString("profileImageUri", "");
        pendingImageUri = savedUriString.isEmpty() ? null : Uri.parse(savedUriString);

        if (sheetAvatarPreviewRef != null) {
            ProfileHelper.loadAvatar(this, sheetAvatarPreviewRef);
        }

        // Change Photo Button
        View btnChangePhoto = view.findViewById(R.id.btnSheetChangePhoto);
        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> {
                SettingsManager.vibrate(this);
                openImageGallery();
            });
        }

        // Remove Photo Button
        View btnRemovePhoto = view.findViewById(R.id.btnSheetRemovePhoto);
        if (btnRemovePhoto != null) {
            btnRemovePhoto.setOnClickListener(v -> {
                SettingsManager.vibrate(this);
                pendingImageUri = null;
                ProfileHelper.removeProfileImage(this);
                if (sheetAvatarPreviewRef != null) {
                    sheetAvatarPreviewRef.setImageResource(R.drawable.ic_account);
                }
                if (profileAvatarImage != null) {
                    profileAvatarImage.setImageResource(R.drawable.ic_account);
                }
                Toast.makeText(ProfileActivity.this, "Profile photo removed", Toast.LENGTH_SHORT).show();
            });
        }

        // Save Button
        MaterialButton btnSave = view.findViewById(R.id.btnSheetSaveProfile);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                SettingsManager.vibrate(this);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (editName != null) editor.putString(ProfileHelper.KEY_USER_NAME, editName.getText().toString().trim());
                if (editBio != null) editor.putString(ProfileHelper.KEY_BIO, editBio.getText().toString().trim());
                if (editCompany != null) editor.putString(ProfileHelper.KEY_COMPANY_NAME, editCompany.getText().toString().trim());
                if (editEmail != null) editor.putString(ProfileHelper.KEY_EMAIL, editEmail.getText().toString().trim());
                if (editPhone != null) editor.putString(ProfileHelper.KEY_PHONE, editPhone.getText().toString().trim());
                if (editAddress != null) editor.putString(ProfileHelper.KEY_ADDRESS, editAddress.getText().toString().trim());
                editor.commit(); // Synchronous commit for instant update

                loadProfileData();
                dialog.dismiss();
                Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();
    }

    private static final int PERMISSION_REQ_STORAGE = 102;

    private void openImageGallery() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQ_STORAGE
                );
                return;
            }
        }
        launchImagePickerIntent();
    }

    private void launchImagePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        try {
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open photo gallery", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchImagePickerIntent();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri pickedUri = data.getData();
            if (pickedUri != null) {
                Uri internalUri = ProfileHelper.saveProfileImage(this, pickedUri);
                pendingImageUri = internalUri;
                ProfileHelper.loadAvatar(this, sheetAvatarPreviewRef);
                ProfileHelper.loadAvatar(this, profileAvatarImage);
            }
        }
    }

    private void backupLedgerData() {
        TransactionRepository repo = new TransactionRepository(this);
        repo.getAllTransactions(transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "No transactions to backup.", Toast.LENGTH_SHORT).show());
                return;
            }
            try {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(transactions);
                File backupDir = new File(getCacheDir(), "backups");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
                File backupFile = new File(backupDir, "BalanceX_Backup_" + timeStamp + ".json");
                try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                    fos.write(json.getBytes(StandardCharsets.UTF_8));
                }

                Uri fileUri = FileProvider.getUriForFile(
                        ProfileActivity.this,
                        getPackageName() + ".provider",
                        backupFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/json");
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BalanceX Ledger Backup (" + timeStamp + ")");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is the offline JSON ledger backup from BalanceX (" + transactions.size() + " records).");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> startActivity(Intent.createChooser(shareIntent, "Share Ledger Backup")));
            } catch (Exception e) {
                Log.e("ProfileActivity", "Error sharing backup", e);
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Failed to create backup: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}
