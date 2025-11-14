package com.example.uitmtoolshare;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class EditItemActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int CAPTURE_IMAGE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private EditText nameField, categoryField, descriptionField, conditionField, locationField, priceField;
    private ImageView imagePreview;
    private Button updateButton, chooseImageButton, captureImageButton;
    private RadioGroup typeRadioGroup;
    private TextView labelPrice;

    private Item item;
    private Uri imageUri;
    private String newImagePath = null; // Local path for new image
    private String selectedType = "Share"; // Default

    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);

        // Initialize views
        nameField = findViewById(R.id.editItemName);
        categoryField = findViewById(R.id.editItemCategory);
        descriptionField = findViewById(R.id.editItemDescription);
        conditionField = findViewById(R.id.editItemCondition);
        locationField = findViewById(R.id.editItemLocation);
        priceField = findViewById(R.id.editItemPrice);
        labelPrice = findViewById(R.id.labelEditPrice);
        imagePreview = findViewById(R.id.editItemImage);
        updateButton = findViewById(R.id.updateItemButton);
        chooseImageButton = findViewById(R.id.buttonChooseImage);
        captureImageButton = findViewById(R.id.buttonCaptureImage);
        typeRadioGroup = findViewById(R.id.typeRadioGroup);

        // Firebase storage reference
        storageReference = FirebaseStorage.getInstance().getReference("item_images");

        item = (Item) getIntent().getSerializableExtra("item");

        // Fill existing data
        if (item != null) {
            nameField.setText(item.name);
            categoryField.setText(item.category);
            descriptionField.setText(item.description);
            conditionField.setText(item.condition);
            locationField.setText(item.location);

            // Set type radio button
            if (item.type != null) {
                if (item.type.equalsIgnoreCase("Share")) {
                    typeRadioGroup.check(R.id.typeShare);
                    selectedType = "Share";
                } else if (item.type.equalsIgnoreCase("Rent")) {
                    typeRadioGroup.check(R.id.typeRent);
                    selectedType = "Rent";
                } else if (item.type.equalsIgnoreCase("Sell") || item.type.equalsIgnoreCase("Sell/Swap")) {
                    typeRadioGroup.check(R.id.typeSell);
                    selectedType = "Sell";
                }
            }

            // Show price if available
            if (item.price != null && !item.type.equalsIgnoreCase("Share")) {
                priceField.setText(item.price.toString());
            }

            // Load image using Glide or Picasso (Firebase URL should be used)
            if (item.imagePath != null && !item.imagePath.isEmpty()) {
                File file = new File(getFilesDir(), item.imagePath);
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    imagePreview.setImageBitmap(bitmap);
                }
            }
        }

        // Show/hide price label and field based on initial type
        if (selectedType.equals("Share")) {
            labelPrice.setVisibility(View.GONE);
            priceField.setVisibility(View.GONE);
        } else {
            labelPrice.setVisibility(View.VISIBLE);
            priceField.setVisibility(View.VISIBLE);
        }

        // RadioGroup logic to show/hide price field and label
        typeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.typeShare) {
                selectedType = "Share";
                labelPrice.setVisibility(View.GONE);
                priceField.setVisibility(View.GONE);
            } else if (checkedId == R.id.typeRent) {
                selectedType = "Rent";
                labelPrice.setVisibility(View.VISIBLE);
                priceField.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.typeSell) {
                selectedType = "Sell";
                labelPrice.setVisibility(View.VISIBLE);
                priceField.setVisibility(View.VISIBLE);
            }
        });

        // Image pickers
        chooseImageButton.setOnClickListener(this::chooseImage);
        captureImageButton.setOnClickListener(this::captureImage);

        updateButton.setOnClickListener(v -> updateItem());
    }

    public void chooseImage(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    public void captureImage(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAPTURE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == PICK_IMAGE || requestCode == CAPTURE_IMAGE)
                && resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE) {
                imageUri = data.getData();
                imagePreview.setImageURI(imageUri);
                newImagePath = saveImageLocally(imageUri);
            } else if (requestCode == CAPTURE_IMAGE) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                imageUri = saveCapturedImage(photo);
                if (imageUri != null) {
                    imagePreview.setImageURI(imageUri);
                    newImagePath = imageUri.getLastPathSegment();
                }
            }
        }
    }

    private String saveImageLocally(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            File file = new File(getFilesDir(), System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);
            out.flush();
            out.close();
            return file.getName();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image locally", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private Uri saveCapturedImage(Bitmap bitmap) {
        try {
            File file = new File(getFilesDir(), System.currentTimeMillis() + "_captured.jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);
            out.flush();
            out.close();
            return Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save captured image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void updateItem() {
        String newName = nameField.getText().toString().trim();
        String newCategory = categoryField.getText().toString().trim();
        String newDescription = descriptionField.getText().toString().trim();
        String newCondition = conditionField.getText().toString().trim();
        String newLocation = locationField.getText().toString().trim();
        String newPrice = priceField.getText().toString().trim();

        if (!selectedType.equals("Share") && newPrice.isEmpty()) {
            Toast.makeText(this, "Please enter a price for Rent/Sell items", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newName.isEmpty()) {
            Toast.makeText(this, "Item name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Items");

        dbRef.orderByChild("imagePath").equalTo(item.imagePath)
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            DatabaseReference itemRef = snap.getRef();

                            itemRef.child("name").setValue(newName);
                            itemRef.child("category").setValue(newCategory);
                            itemRef.child("description").setValue(newDescription);
                            itemRef.child("condition").setValue(newCondition);
                            itemRef.child("location").setValue(newLocation);
                            itemRef.child("type").setValue(selectedType);

                            if (!selectedType.equals("Share")) {
                                itemRef.child("price").setValue(newPrice);
                            } else {
                                itemRef.child("price").setValue("");
                            }

                            // ✅ If new image is selected
                            if (imageUri != null) {
                                // 🔥 Delete old image first
                                FirebaseStorage.getInstance()
                                        .getReferenceFromUrl(item.imagePath)
                                        .delete()
                                        .addOnSuccessListener(unused -> {
                                            // After deletion, upload new image
                                            String filename = System.currentTimeMillis() + ".jpg";
                                            StorageReference newImageRef = storageReference.child(filename);

                                            newImageRef.putFile(imageUri)
                                                    .addOnSuccessListener(taskSnapshot ->
                                                            newImageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                                                itemRef.child("imagePath").setValue(downloadUri.toString());

                                                                Toast.makeText(this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                                                                Intent intent = new Intent(this, homepage.class);
                                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                                startActivity(intent);
                                                                finish();
                                                            }))
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(this, "New image upload failed", Toast.LENGTH_SHORT).show());
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Failed to delete old image", Toast.LENGTH_SHORT).show());
                            } else {
                                // No image change — just update fields
                                Toast.makeText(this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, homepage.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Failed to locate item in database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


}
