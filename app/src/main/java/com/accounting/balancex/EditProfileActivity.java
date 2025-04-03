package com.accounting.balancex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class EditProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText userNameEdit, bioEdit, companyNameEdit, emailEdit, phoneEdit, addressEdit;
    private SharedPreferences sharedPreferences;
    private ImageView profileImage;
    private Uri imageUri; // To store selected image URI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Find Views
        userNameEdit = findViewById(R.id.userNameEdit);
        bioEdit = findViewById(R.id.bioEdit);
        companyNameEdit = findViewById(R.id.companyNameEdit);
        emailEdit = findViewById(R.id.emailEdit);
        phoneEdit = findViewById(R.id.phoneEdit);
        addressEdit = findViewById(R.id.addressEdit);
        TextView saveProfile = findViewById(R.id.saveProfile);
        ImageView backImage = findViewById(R.id.backImage);
        profileImage = findViewById(R.id.profileImage);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        // Load saved data
        loadProfileData();

        // Save button click
        saveProfile.setOnClickListener(v -> saveProfileData());

        // Back button click
        backImage.setOnClickListener(v -> finish()); // Just close the activity

        // Profile image click - open gallery
        profileImage.setOnClickListener(v -> openGallery());
    }

    private void loadProfileData() {
        userNameEdit.setText(sharedPreferences.getString("userName", ""));
        bioEdit.setText(sharedPreferences.getString("bio", ""));
        companyNameEdit.setText(sharedPreferences.getString("companyName", ""));
        emailEdit.setText(sharedPreferences.getString("email", ""));
        phoneEdit.setText(sharedPreferences.getString("phone", ""));
        addressEdit.setText(sharedPreferences.getString("address", ""));

        // Load profile image URI
        String imageUriString = sharedPreferences.getString("profileImageUri", "");
        if (!imageUriString.isEmpty()) {
            imageUri = Uri.parse(imageUriString);
            profileImage.setImageURI(imageUri);
        }
    }

    private void saveProfileData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userName", userNameEdit.getText().toString());
        editor.putString("bio", bioEdit.getText().toString());
        editor.putString("companyName", companyNameEdit.getText().toString());
        editor.putString("email", emailEdit.getText().toString());
        editor.putString("phone", phoneEdit.getText().toString());
        editor.putString("address", addressEdit.getText().toString());

        // Save profile image URI
        if (imageUri != null) {
            editor.putString("profileImageUri", imageUri.toString());
        }

        editor.apply();

        // Return result to ProfileActivity
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);
        finish(); // Close EditProfileActivity

    }
    // Open gallery to pick an image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
    }
}
