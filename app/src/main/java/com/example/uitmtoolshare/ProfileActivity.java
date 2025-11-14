package com.example.uitmtoolshare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private TextView emailText, usernameText, phoneText;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        emailText = findViewById(R.id.textViewEmail);
        usernameText = findViewById(R.id.textViewUsername);
        phoneText = findViewById(R.id.textViewPhone);

        // If not authenticated, kick to login
        if (user == null) {
            finish();
            startActivity(new Intent(this, MainActivity.class));
            return;
        }

        loadProfileData();
    }

    public void signout(View v){
        auth.signOut();
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        // Clear the back stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void editProfile(View v){
        Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void loadProfileData() {
        if (user == null) return;

        emailText.setText("Email: " + user.getEmail());

        dbRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);

                    usernameText.setText("Username: " + (username != null ? username : "-"));
                    phoneText.setText("Phone: " + (phone != null ? phone : "-"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Optionally handle DB errors here
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always refresh profile when coming back
        user = auth.getCurrentUser();
        loadProfileData();
    }
}
