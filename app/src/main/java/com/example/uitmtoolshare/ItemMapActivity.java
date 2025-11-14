package com.example.uitmtoolshare;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ItemMapActivity extends AppCompatActivity {

    MapView mapView;
    ImageView navLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load OSM config for tile downloading
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osm_prefs", MODE_PRIVATE));
        setContentView(R.layout.activity_item_map);

        mapView = findViewById(R.id.itemMapView);
        navLogo = findViewById(R.id.navLogo);

        // Show user's current location (blue dot)
        MyLocationNewOverlay myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        String addressString = getIntent().getStringExtra("location");
        String name = getIntent().getStringExtra("name");

        if (addressString == null || addressString.isEmpty()) {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressString, 1);
            if (addresses == null || addresses.isEmpty()) {
                Toast.makeText(this, "Failed to find location", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Address address = addresses.get(0);
            double lat = address.getLatitude();
            double lon = address.getLongitude();

            GeoPoint point = new GeoPoint(lat, lon);
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.getController().setZoom(17.0);
            mapView.getController().setCenter(point);

            // Place marker for the tool
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setTitle(name != null ? name : "Item Name");
            marker.setSubDescription(addressString); // This appears in info window if needed
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);

            // Show item name & location in a dialog when marker is tapped
            marker.setOnMarkerClickListener((m, map) -> {
                new android.app.AlertDialog.Builder(this)
                        .setTitle(marker.getTitle())
                        .setMessage(addressString)
                        .setPositiveButton("Navigate", (dialog, which) -> openNavigation(lat, lon))
                        .setNegativeButton("Close", null)
                        .show();
                return true;
            });

            // Clicking the navigation icon also opens navigation
            navLogo.setOnClickListener(v -> openNavigation(lat, lon));

            mapView.invalidate();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error finding location", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // Helper method: open Google Maps navigation or browser as fallback
    private void openNavigation(double lat, double lon) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lon);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback: open in browser
            String url = "https://maps.google.com/?q=" + lat + "," + lon;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }
}
