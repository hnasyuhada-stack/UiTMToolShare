package com.example.uitmtoolshare;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    EditText e1, e2, usernameField, phoneField;
    TextView passwordStrengthText;
    FirebaseAuth mAuth;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // FIX: This now works, since your root ConstraintLayout has id="main"
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Fields
        e1 = findViewById(R.id.editText); // Email
        e2 = findViewById(R.id.editText2); // Password
        usernameField = findViewById(R.id.editTextUsername);
        phoneField = findViewById(R.id.editTextPhone);

        // Use the password strength TextView already in the layout!
        passwordStrengthText = findViewById(R.id.passwordStrengthText);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");

        setupLiveValidation();
    }

    private void setupLiveValidation() {
        // Email validation
        e1.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString();
                if (!email.contains("@") || !email.endsWith(".com")) {
                    e1.setError("Email must contain '@' and end with '.com'");
                } else {
                    e1.setError(null);
                }
            }
        });
        // Make the "Already have an account? Log in" clickable
        TextView loginLink = findViewById(R.id.loginLink);
        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });


        // Password strength + icon
        e2.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                String strength = getPasswordStrength(password);

                passwordStrengthText.setVisibility(View.VISIBLE);
                passwordStrengthText.setText("Password strength: " + strength);

                // Color code text
                switch (strength) {
                    case "Weak":
                        passwordStrengthText.setTextColor(ContextCompat.getColor(RegisterActivity.this, android.R.color.holo_red_dark));
                        break;
                    case "Medium":
                        passwordStrengthText.setTextColor(ContextCompat.getColor(RegisterActivity.this, android.R.color.holo_orange_dark));
                        break;
                    default:
                        passwordStrengthText.setTextColor(ContextCompat.getColor(RegisterActivity.this, android.R.color.holo_green_dark));
                        break;
                }

                boolean isValid = strength.equals("Strong") || strength.equals("Very Strong");
                setPasswordIcon(e2, isValid);
            }
        });
    }

    private String getPasswordStrength(String password) {
        if (password.length() < 6) {
            return "Weak";
        } else if (!password.matches(".*[A-Z].*") || !password.matches(".*[0-9].*")) {
            return "Medium";
        } else if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return "Strong";
        } else {
            return "Very Strong";
        }
    }

    private void setPasswordIcon(EditText editText, boolean isValid) {
        int iconId = isValid ? R.drawable.ic_check_green : R.drawable.ic_warning_red;
        Drawable icon = ContextCompat.getDrawable(this, iconId);
        if (icon != null) {
            icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            editText.setCompoundDrawables(null, null, icon, null);
            editText.setCompoundDrawablePadding(16);
        }
    }

    public void createUser(View v) {
        String email = e1.getText().toString().trim();
        String password = e2.getText().toString().trim();
        String username = usernameField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(username) || TextUtils.isEmpty(phone)) {
            Toast.makeText(getApplicationContext(), "All fields are required", Toast.LENGTH_SHORT).show();
        } else if (!email.contains("@") || !email.endsWith(".com")) {
            Toast.makeText(getApplicationContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
        } else if (!phone.matches("^6\\d{9,11}$")) {
            Toast.makeText(getApplicationContext(), "Phone must start with country code (e.g. 60123456789)", Toast.LENGTH_LONG).show();
        } else {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                String uid = mAuth.getCurrentUser().getUid();
                                User user = new User(email, username, phone);
                                dbRef.child(uid).setValue(user);

                                Toast.makeText(getApplicationContext(), "User created successfully. Please log in.", Toast.LENGTH_SHORT).show();
                                finish();
                                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                            } else {
                                Toast.makeText(getApplicationContext(), "Registration failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
}
