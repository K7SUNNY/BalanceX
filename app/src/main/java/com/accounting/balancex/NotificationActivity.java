package com.accounting.balancex;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class NotificationActivity extends AppCompatActivity {
    private Button buttonRead;
    private Button buttonUnread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        buttonRead = findViewById(R.id.buttonRead);
        buttonUnread = findViewById(R.id.buttonUnread);

        // Set default selection to Unread
        buttonUnread.setBackgroundResource(R.drawable.selected_title);
        buttonUnread.setBackgroundTintList(null); // This fixes the initial purple tint
        buttonRead.setBackgroundResource(0);
        buttonRead.setBackgroundTintList(null);


        buttonUnread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonUnread.setBackgroundResource(R.drawable.selected_title);
                buttonRead.setBackgroundResource(0);
                buttonRead.setBackgroundTintList(null);
                buttonUnread.setBackgroundTintList(null);
            }
        });

        buttonRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonRead.setBackgroundResource(R.drawable.selected_title);
                buttonUnread.setBackgroundResource(0);
                buttonRead.setBackgroundTintList(null);
                buttonUnread.setBackgroundTintList(null);
            }
        });

        // Back button functionality
        ImageView backButton = findViewById(R.id.imageViewbackbutton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Closes the notification activity and returns to the previous screen
            }
        });
    }
}
