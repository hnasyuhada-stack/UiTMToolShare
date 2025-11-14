package com.example.uitmtoolshare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;

public class ItemDetailActivity extends AppCompatActivity {

    ImageView itemImage, whatsappIcon;
    TextView itemName, itemCategory, itemDescription, itemCondition, itemLocation, itemStatus, phoneText;
    TextView itemType, itemPrice; // NEW
    Button editButton, deleteButton, qrButton, viewMapButton;
    Item item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        itemImage = findViewById(R.id.detailImage);
        itemName = findViewById(R.id.detailName);
        itemCategory = findViewById(R.id.detailCategory);
        itemDescription = findViewById(R.id.detailDescription);
        itemCondition = findViewById(R.id.detailCondition);
        itemLocation = findViewById(R.id.detailLocation);
        itemType = findViewById(R.id.detailType);   // NEW
        itemPrice = findViewById(R.id.detailPrice); // NEW
        editButton = findViewById(R.id.editItemButton);
        deleteButton = findViewById(R.id.deleteItemButton);
        qrButton = findViewById(R.id.qrItemButton);
        itemStatus = findViewById(R.id.detailStatus);
        viewMapButton = findViewById(R.id.viewMapButton);
        phoneText = findViewById(R.id.phoneText);
        whatsappIcon = findViewById(R.id.whatsappIcon);

        item = (Item) getIntent().getSerializableExtra("item");

        if (item != null) {
            itemName.setText(item.name);
            itemCategory.setText("Category: " + item.category);
            itemDescription.setText("Description: " + item.description);
            itemCondition.setText("Condition: " + item.condition);
            itemLocation.setText("Location: " + item.location);

            // Show type (Share, Rent, Sell/Swap)
            if (item.type != null && !item.type.isEmpty()) {
                itemType.setText("Type: " + item.type);

                // Set chip background
                switch (item.type.toLowerCase()) {
                    case "rent":
                        itemType.setBackgroundResource(R.drawable.chip_type);
                        itemType.setTextColor(getResources().getColor(android.R.color.white));
                        break;
                    case "sell":
                        itemType.setBackgroundResource(R.drawable.chip_status_reserved);
                        itemType.setTextColor(getResources().getColor(android.R.color.white));
                        break;
                    case "share":
                        itemType.setBackgroundResource(R.drawable.chip_type);
                        itemType.setTextColor(getResources().getColor(android.R.color.white));
                        break;
                    default:
                        itemType.setBackgroundResource(R.drawable.chip_type);
                        itemType.setTextColor(getResources().getColor(android.R.color.white));
                        break;
                }
                itemType.setVisibility(View.VISIBLE);
            } else {
                itemType.setVisibility(View.GONE);
            }

            // Show price if type is Rent or Sell (and price not null/empty)
            if ((item.type != null && (item.type.equalsIgnoreCase("Rent") || item.type.equalsIgnoreCase("Sell")))
                    && item.price != null && !item.price.toString().isEmpty()) {
                itemPrice.setText("Price: RM" + item.price.toString());
                itemPrice.setBackgroundResource(R.drawable.chip_price);
                itemPrice.setTextColor(getResources().getColor(android.R.color.white));
                itemPrice.setVisibility(View.VISIBLE);
            } else {
                itemPrice.setVisibility(View.GONE);
            }

            // Load image from Firebase Storage using Glide
            if (item.imagePath != null && !item.imagePath.isEmpty()) {
                // Use Glide to load the image from the Firebase Storage URL
                Glide.with(this)
                        .load(item.imagePath) // Firebase Storage URL
                        .placeholder(R.drawable.placeholder) // Placeholder image
                        .into(itemImage);
            } else {
                itemImage.setImageResource(R.drawable.placeholder);
            }

            itemImage.setOnClickListener(v -> {
                if (item.imagePath != null && !item.imagePath.isEmpty()) {
                    Glide.with(this)
                            .load(item.imagePath) // Firebase Storage URL
                            .into(new ImageView(this)); // Fullscreen or dialog image view
                }
            });

            String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (item.owner != null && item.owner.equals(currentUid)) {
                editButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);
                qrButton.setVisibility(View.VISIBLE);

                editButton.setOnClickListener(v -> {
                    Intent editIntent = new Intent(ItemDetailActivity.this, EditItemActivity.class);
                    editIntent.putExtra("item", item);
                    startActivity(editIntent);
                });

                deleteButton.setOnClickListener(v -> confirmDelete());

                qrButton.setOnClickListener(v -> showQRCode(item.imagePath));
            } else {
                editButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
                qrButton.setVisibility(View.GONE);
            }

            // Status chip style (available = green, reserved = yellow, else red)
            if (item.status != null && !item.status.isEmpty()) {
                itemStatus.setText("Status: " + item.status);
                switch (item.status.toLowerCase()) {
                    case "available":
                        itemStatus.setBackgroundResource(R.drawable.chip_status_available);
                        itemStatus.setTextColor(getResources().getColor(android.R.color.white));
                        break;
                    case "reserved":
                        itemStatus.setBackgroundResource(R.drawable.chip_status_reserved);
                        itemStatus.setTextColor(getResources().getColor(android.R.color.black));
                        break;
                    default:
                        itemStatus.setBackgroundResource(R.drawable.chip_status_unavailable);
                        itemStatus.setTextColor(getResources().getColor(android.R.color.white));
                        break;
                }
            } else {
                itemStatus.setText("Status: Unknown");
                itemStatus.setBackgroundResource(R.drawable.chip_status_unavailable);
                itemStatus.setTextColor(getResources().getColor(android.R.color.white));
            }

            viewMapButton.setVisibility(View.VISIBLE);
            viewMapButton.setOnClickListener(v -> {
                Intent mapIntent = new Intent(ItemDetailActivity.this, ItemMapActivity.class);
                mapIntent.putExtra("location", item.location); // Use address string
                mapIntent.putExtra("name", item.name);
                startActivity(mapIntent);
            });

            // Fetch phone number from Firebase Users node
            FirebaseDatabase.getInstance().getReference("Users")
                    .child(item.owner)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            String phoneNumber = snapshot.child("phone").getValue(String.class);
                            phoneText.setText(phoneNumber != null ? phoneNumber : "No phone number");

                            if (phoneNumber != null) {
                                whatsappIcon.setVisibility(View.VISIBLE);
                                whatsappIcon.setOnClickListener(v -> {
                                    String formattedNumber = phoneNumber.replace("+", "").replace(" ", "");
                                    String url = "https://wa.me/" + formattedNumber;
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(url));
                                    try {
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                whatsappIcon.setVisibility(View.GONE);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load owner contact", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Delete the image from Firebase Storage first
                    if (item.imagePath != null && !item.imagePath.isEmpty()) {
                        com.google.firebase.storage.FirebaseStorage.getInstance()
                                .getReferenceFromUrl(item.imagePath)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    // Now delete the item data from Firebase Realtime Database
                                    FirebaseDatabase.getInstance().getReference("Items")
                                            .orderByChild("imagePath").equalTo(item.imagePath)
                                            .get().addOnSuccessListener(snapshot -> {
                                                for (DataSnapshot snap : snapshot.getChildren()) {
                                                    snap.getRef().removeValue();
                                                }
                                                Intent intent = new Intent(this, homepage.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                                finish();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to delete image from storage", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // If no image path, just delete the item data
                        FirebaseDatabase.getInstance().getReference("item_images")
                                .orderByChild("imagePath").equalTo(item.imagePath)
                                .get().addOnSuccessListener(snapshot -> {
                                    for (DataSnapshot snap : snapshot.getChildren()) {
                                        snap.getRef().removeValue();
                                    }
                                    Intent intent = new Intent(this, homepage.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showQRCode(String imagePath) {
        try {
            String fileName = new File(Uri.parse(item.imagePath).getPath()).getName();
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(fileName, BarcodeFormat.QR_CODE, 600, 600);

            ImageView qrImage = new ImageView(this);
            qrImage.setImageBitmap(bitmap);

            new AlertDialog.Builder(this)
                    .setTitle("Item QR Code")
                    .setView(qrImage)
                    .setPositiveButton("Close", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }
}
