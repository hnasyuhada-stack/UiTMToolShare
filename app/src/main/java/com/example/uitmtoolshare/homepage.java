package com.example.uitmtoolshare;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

// Ensure you import your app's R, not android.R!
import com.example.uitmtoolshare.R;

public class homepage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_homepage);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(homepage.this, ProfileActivity.class));
                    return true;
                } else if (itemId == R.id.nav_add) {
                    startActivity(new Intent(homepage.this, AddItemActivity.class));
                    return true;
                } else if (itemId == R.id.nav_home) {
                    startActivity(new Intent(homepage.this, ItemListActivity.class));
                    return true;
                } else if (itemId == R.id.nav_qr) {
                    startActivity(new Intent(homepage.this, QRScannerActivity.class));
                    return true;
                }
                return false;
            }
        });

        // Optional: Set the default selected tab
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
}
