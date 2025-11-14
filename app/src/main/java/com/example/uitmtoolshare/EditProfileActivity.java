package com.example.uitmtoolshare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class EditProfileActivity extends AppCompatActivity {

    EditText emailField, usernameField, phoneField, passwordField;
    FirebaseAuth mAuth;
    FirebaseUser user;
    DatabaseReference dbRef;

    String oldEmail, oldUsername, oldPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        emailField = findViewById(R.id.editEmail);
        usernameField = findViewById(R.id.editUsername);
        phoneField = findViewById(R.id.editPhone);
        passwordField = findViewById(R.id.editPassword);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        dbRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());

        // Pre-fill fields
        emailField.setText(user.getEmail());
        oldEmail = user.getEmail();

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    oldUsername = snapshot.child("username").getValue(String.class);
                    oldPhone = snapshot.child("phone").getValue(String.class);

                    usernameField.setText(oldUsername);
                    phoneField.setText(oldPhone);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void updateProfile(View v) {
        String newEmail = emailField.getText().toString().trim();
        String newUsername = usernameField.getText().toString().trim();
        String newPhone = phoneField.getText().toString().trim();
        String newPassword = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(newEmail) || TextUtils.isEmpty(newUsername) || TextUtils.isEmpty(newPhone)) {
            Toast.makeText(this, "Username, Email and Phone cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if anything actually changed
        boolean emailChanged = !newEmail.equals(oldEmail);
        boolean usernameChanged = !newUsername.equals(oldUsername);
        boolean phoneChanged = !newPhone.equals(oldPhone);
        boolean passwordChanged = !TextUtils.isEmpty(newPassword);

        if (!emailChanged && !usernameChanged && !phoneChanged && !passwordChanged) {
            Toast.makeText(this, "No changes detected.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update email if changed
        if (emailChanged) {
            user.updateEmail(newEmail).addOnCompleteListener(emailTask -> {
                if (emailTask.isSuccessful()) {
                    dbRef.child("email").setValue(newEmail);
                    // Proceed to next update
                    updatePasswordUsernamePhone(newPassword, newUsername, newPhone);
                } else {
                    String errorMessage = emailTask.getException() != null ? emailTask.getException().getMessage() : "Email update failed";
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // If email not changed, continue with others
            updatePasswordUsernamePhone(newPassword, newUsername, newPhone);
        }
    }

    private void updatePasswordUsernamePhone(String newPassword, String newUsername, String newPhone) {
        // Password (if changed)
        if (!TextUtils.isEmpty(newPassword)) {
            user.updatePassword(newPassword).addOnCompleteListener(passTask -> {
                if (!passTask.isSuccessful()) {
                    String errorMessage = passTask.getException() != null ? passTask.getException().getMessage() : "Password update failed";
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    return;
                }
                // Password changed, now update others
                updateUsernamePhone(newUsername, newPhone);
            });
        } else {
            // No password change, just update others
            updateUsernamePhone(newUsername, newPhone);
        }
    }

    private void updateUsernamePhone(String username, String phone) {
        dbRef.child("username").setValue(username);
        dbRef.child("phone").setValue(phone);

        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(EditProfileActivity.this, ProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
