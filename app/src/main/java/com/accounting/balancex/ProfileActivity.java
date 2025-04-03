package com.accounting.balancex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {
    private TextView userName, bio, companyName, email, phone, address,editProfileButton;
    private ImageView backImage,profileImage;
    private SharedPreferences sharedPreferences;
    private static final int EDIT_PROFILE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Find Views
        userName = findViewById(R.id.userNameView);
        bio = findViewById(R.id.bioView);
        companyName = findViewById(R.id.companyNameView);
        email = findViewById(R.id.emailView);
        phone = findViewById(R.id.phoneView);
        address = findViewById(R.id.addressView);
        ImageView editProfileBtn = findViewById(R.id.editProfileBtn);
        editProfileButton = findViewById(R.id.editProfileButton);
        backImage = findViewById(R.id.backImage);
        profileImage = findViewById(R.id.profileImage); // Profile ImageView

        backImage.setOnClickListener(v -> {finish();});

        editProfileButton.setOnClickListener( v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        // Load user data
        loadProfileData();

        // Edit profile button
        editProfileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivityForResult(intent, EDIT_PROFILE_REQUEST);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();  // Check for updates when activity is resumed
    }

    private void loadProfileData() {
        userName.setText(sharedPreferences.getString("userName", "Your Name"));
        bio.setText(sharedPreferences.getString("bio", "Your Bio"));
        companyName.setText(sharedPreferences.getString("companyName", "Company Name"));
        email.setText(sharedPreferences.getString("email", "example@gmail.com"));
        phone.setText(sharedPreferences.getString("phone", "1234567890"));
        address.setText(sharedPreferences.getString("address", "Your Address"));
        // Load profile image URI
        String imageUriString = sharedPreferences.getString("profileImageUri", "");
        if (!imageUriString.isEmpty()) {
            Uri imageUri = Uri.parse(imageUriString);
            profileImage.setImageURI(imageUri);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
            loadProfileData(); // Reload updated data
        }
    }
}
