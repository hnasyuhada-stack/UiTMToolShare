package com.example.uitmtoolshare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.RGBLuminanceSource;

public class QRScannerActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 200;
    private static final String CHANNEL_ID = "item_borrow_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);


        Button scanButton = findViewById(R.id.buttonScanCamera);
        Button selectImageButton = findViewById(R.id.buttonSelectImage);

        scanButton.setOnClickListener(v -> startCameraScan());
        selectImageButton.setOnClickListener(v -> openGallery());
    }

    private void startCameraScan() {
        new IntentIntegrator(this)
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                .setPrompt("Scan Item QR Code")
                .setCameraId(0)
                .setBeepEnabled(true)
                .setOrientationLocked(true)
                .setCaptureActivity(CaptureActivityPortrait.class)
                .initiateScan();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_SHORT).show();
                updateItemStatus(result.getContents());
            } else {
                Toast.makeText(this, "Camera scan returned null", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                scanQRCodeFromImage(imageUri);
            }
        } else {
            Toast.makeText(this, "Unrecognized result", Toast.LENGTH_SHORT).show();
        }
    }

    private void scanQRCodeFromImage(Uri uri) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(binaryBitmap);

            if (result != null) {
                updateItemStatus(result.getText());
            } else {
                Toast.makeText(this, "No QR code found in image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to read QR code from image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateItemStatus(String fileName) {
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        String currentEmail = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail();

        FirebaseDatabase.getInstance().getReference("Items")
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean found = false;
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        String imagePath = snap.child("imagePath").getValue(String.class);
                        if (imagePath != null && imagePath.contains(fileName)) {
                            found = true;

                            String type = snap.child("type").getValue(String.class);
                            String currentStatus = snap.child("status").getValue(String.class);
                            String borrowedByEmail = snap.child("borrowedByEmail").getValue(String.class);
                            String owner = snap.child("owner").getValue(String.class);
                            String itemName = snap.child("name").getValue(String.class);

                            //  Sell items
                            if (type != null && type.equalsIgnoreCase("Sell")) {
                                if ("available".equalsIgnoreCase(currentStatus)) {
                                    snap.getRef().child("status").setValue("unavailable");
                                    snap.getRef().child("purchasedByEmail").setValue(currentEmail); // Track purchaser
                                    Toast.makeText(this, "Item marked as purchased.", Toast.LENGTH_SHORT).show();
                                    NotificationHelper.showPurchaseNotification(QRScannerActivity.this, itemName);

                                } else if ("unavailable".equalsIgnoreCase(currentStatus)) {
                                    String purchasedEmail = snap.child("purchasedByEmail").getValue(String.class);
                                    if (purchasedEmail != null && purchasedEmail.equals(currentEmail)) {
                                        Toast.makeText(this, "You have already marked this item as purchased.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, "This sell item is already marked as unavailable.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(this, "Sell item has unknown status: " + currentStatus, Toast.LENGTH_SHORT).show();
                                }

                                return; // Skip borrowing logic for sell items
                            }

                            if ("borrowed".equals(currentStatus)) {
                                if (borrowedByEmail == null || !borrowedByEmail.equals(currentEmail)) {
                                    Toast.makeText(this, "Item is already borrowed by someone else", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                snap.getRef().child("status").setValue("available");
                                snap.getRef().child("borrowedBy").setValue(null);
                                snap.getRef().child("borrowedByEmail").setValue(null);
                                Toast.makeText(this, "Item returned successfully", Toast.LENGTH_SHORT).show();
                                NotificationHelper.showReturnNotification(QRScannerActivity.this, itemName);

                            } else if ("available".equals(currentStatus)) {
                                if (currentUid.equals(owner)) {
                                    Toast.makeText(this, "You cannot borrow your own item", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                snap.getRef().child("status").setValue("borrowed");
                                snap.getRef().child("borrowedBy").setValue(currentUid);
                                snap.getRef().child("borrowedByEmail").setValue(currentEmail);
                                Toast.makeText(this, "Item borrowed successfully", Toast.LENGTH_SHORT).show();
                                NotificationHelper.showBorrowNotification(QRScannerActivity.this, itemName);

                            } else {
                                Toast.makeText(this, "Invalid status", Toast.LENGTH_SHORT).show();
                            }

                            break;
                        }
                    }

                    if (!found) {
                        Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show());
    }

}

