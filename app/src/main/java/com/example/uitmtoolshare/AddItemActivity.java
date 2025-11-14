package com.example.uitmtoolshare;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AddItemActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int CAPTURE_IMAGE = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 102;

    private Uri imageUri;
    private String currentLocation = "";
    private LatLng lastLatLng = null;

    private ImageView imagePreview;
    private EditText nameField, categoryField, descriptionField, conditionField, locationField, priceField;
    private RadioGroup typeRadioGroup;
    private RadioButton radioShare, radioRent, radioSell;
    private Button chooseImageButton, captureImageButton, uploadButton;
    private TextView labelPrice;

    private FusedLocationProviderClient fusedLocationClient;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_item);

        imagePreview = findViewById(R.id.imagePreview);
        nameField = findViewById(R.id.itemName);
        categoryField = findViewById(R.id.itemCategory);
        descriptionField = findViewById(R.id.itemDescription);
        conditionField = findViewById(R.id.itemCondition);
        locationField = findViewById(R.id.itemLocation);
        priceField = findViewById(R.id.itemPrice);
        labelPrice = findViewById(R.id.labelItemPrice);
        typeRadioGroup = findViewById(R.id.typeRadioGroup);
        radioShare = findViewById(R.id.typeShare);
        radioRent = findViewById(R.id.typeRent);
        radioSell = findViewById(R.id.typeSell);

        chooseImageButton = findViewById(R.id.buttonChooseImage);
        captureImageButton = findViewById(R.id.buttonCaptureImage);
        uploadButton = findViewById(R.id.buttonUploadItem);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        storageReference = FirebaseStorage.getInstance().getReference("item_images");

        chooseImageButton.setOnClickListener(this::chooseImage);
        captureImageButton.setOnClickListener(this::captureImage);
        uploadButton.setOnClickListener(this::uploadItem);

        requestLocation();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }

        labelPrice.setVisibility(View.GONE);
        priceField.setVisibility(View.GONE);

        typeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.typeRent || checkedId == R.id.typeSell) {
                labelPrice.setVisibility(View.VISIBLE);
                priceField.setVisibility(View.VISIBLE);
            } else {
                labelPrice.setVisibility(View.GONE);
                priceField.setVisibility(View.GONE);
                priceField.setText("");
            }
        });

        locationField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                currentLocation = s.toString();
            }
        });
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkLocationSettingsAndFetch();
        }
    }

    private void checkLocationSettingsAndFetch() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(response -> getLastKnownLocation())
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e).startResolutionForResult(this, LOCATION_SETTINGS_REQUEST_CODE);
                        } catch (Exception ex) {
                            Toast.makeText(this, "Failed to resolve location settings", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Location services unavailable", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void getLastKnownLocation() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(500)
                .setNumUpdates(1);

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult != null ? locationResult.getLastLocation() : null;
                if (location != null) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    lastLatLng = new LatLng(lat, lng);

                    try {
                        Geocoder geocoder = new Geocoder(AddItemActivity.this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            currentLocation = addresses.get(0).getAddressLine(0);
                        } else {
                            currentLocation = lat + ", " + lng;
                        }
                        locationField.setText(currentLocation);
                    } catch (IOException e) {
                        e.printStackTrace();
                        currentLocation = lat + ", " + lng;
                        locationField.setText(currentLocation);
                    }
                } else {
                    locationField.setText("Location not available");
                }
            }
        }, getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettingsAndFetch();
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            getLastKnownLocation();
        }

        if ((requestCode == PICK_IMAGE || requestCode == CAPTURE_IMAGE) && resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE) {
                imageUri = data.getData();
                imagePreview.setImageURI(imageUri);
            } else if (requestCode == CAPTURE_IMAGE) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                Uri tempUri = MediaStore.Images.Media.insertImage(getContentResolver(), photo, "Captured", null) != null
                        ? Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), photo, "Captured", null))
                        : null;
                imageUri = tempUri;
                imagePreview.setImageURI(imageUri);
            }
        }
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

    public void uploadItem(View v) {
        if (imageUri == null || nameField.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Image and Name are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = "";
        int checkedTypeId = typeRadioGroup.getCheckedRadioButtonId();
        if (checkedTypeId == R.id.typeShare) type = "Share";
        else if (checkedTypeId == R.id.typeRent) type = "Rent";
        else if (checkedTypeId == R.id.typeSell) type = "Sell";

        if (type.isEmpty()) {
            Toast.makeText(this, "Please select a type (Share/Rent/Sell)", Toast.LENGTH_SHORT).show();
            return;
        }

        String price = priceField.getText().toString().trim();
        if ((type.equals("Rent") || type.equals("Sell")) && price.isEmpty()) {
            Toast.makeText(this, "Please enter a price.", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference imageRef = storageReference.child(System.currentTimeMillis() + ".jpg");
        String finalType = type;
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap<String, Object> item = new HashMap<>();
                    item.put("name", nameField.getText().toString().trim());
                    item.put("category", categoryField.getText().toString().trim());
                    item.put("description", descriptionField.getText().toString().trim());
                    item.put("condition", conditionField.getText().toString().trim());
                    item.put("imagePath", uri.toString());
                    item.put("owner", uid);
                    item.put("status", "available");
                    item.put("type", finalType);

                    if (!price.isEmpty()) item.put("price", price);
                    if (!currentLocation.isEmpty()) item.put("location", currentLocation);
                    if (lastLatLng != null) {
                        item.put("latitude", lastLatLng.latitude);
                        item.put("longitude", lastLatLng.longitude);
                    }

                    FirebaseDatabase.getInstance().getReference("Items")
                            .push().setValue(item)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Item uploaded successfully!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, homepage.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                                    finish();
                                } else {
                                    Toast.makeText(this, "Upload failed!", Toast.LENGTH_SHORT).show();
                                }
                            });
                }))
                .addOnFailureListener(e -> Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show());
    }
}
