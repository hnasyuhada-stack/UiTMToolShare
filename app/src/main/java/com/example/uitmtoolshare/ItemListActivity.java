package com.example.uitmtoolshare;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class ItemListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    Spinner filterSpinner;
    EditText editSearch;
    ImageButton buttonOpenFilter;

    int filterMaxPrice = Integer.MAX_VALUE;
    String filterLocation = "";
    String filterType = ""; // "Share", "Rent", "Sell" or ""
    int spinnerFilter = 0;
    String searchKeyword = "";

    ArrayList<Item> allItems = new ArrayList<>();
    ArrayList<Item> displayedItems = new ArrayList<>();
    ItemAdapter adapter;
    DatabaseReference dbRef;

    String currentUserId;
    String currentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        currentEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        recyclerView = findViewById(R.id.recyclerViewItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter(this, displayedItems);
        recyclerView.setAdapter(adapter);

        filterSpinner = findViewById(R.id.filterSpinner);
        editSearch = findViewById(R.id.editSearch);
        buttonOpenFilter = findViewById(R.id.buttonOpenFilter);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                this,
                R.layout.spinner_selected_item,
                R.id.textMain,
                getResources().getStringArray(R.array.item_filters)
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return super.getView(position, convertView, parent);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = getLayoutInflater().inflate(R.layout.spinner_dropdown_item, parent, false);
                TextView text = view.findViewById(R.id.textDropdown);
                text.setText(getItem(position));
                return view;
            }
        };
        filterSpinner.setAdapter(spinnerAdapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinnerFilter = position;
                applyFilter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchKeyword = s.toString().trim();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        buttonOpenFilter.setOnClickListener(v -> showFilterSheet());

        dbRef = FirebaseDatabase.getInstance().getReference("Items");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allItems.clear();
                for (DataSnapshot itemSnap : snapshot.getChildren()) {
                    Item item = itemSnap.getValue(Item.class);
                    if (item != null) {
                        allItems.add(item);
                    }
                }
                applyFilter();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // ✅ Re-added bottom navigation bar handling
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_profile) {
                startActivity(new android.content.Intent(ItemListActivity.this, ProfileActivity.class));
                return true;
            } else if (itemId == R.id.nav_add) {
                startActivity(new android.content.Intent(ItemListActivity.this, AddItemActivity.class));
                return true;
            } else if (itemId == R.id.nav_home) {
                // Already here
                return true;
            } else if (itemId == R.id.nav_qr) {
                startActivity(new android.content.Intent(ItemListActivity.this, QRScannerActivity.class));
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_home); // highlight Home
    }

    private void showFilterSheet() {
        BottomSheetDialog sheetDialog = new BottomSheetDialog(ItemListActivity.this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null);
        sheetDialog.setContentView(sheetView);

        SeekBar seekBarPrice = sheetView.findViewById(R.id.seekBarPrice);
        TextView textPriceValue = sheetView.findViewById(R.id.textPriceValue);
        EditText editLocation = sheetView.findViewById(R.id.editLocation);
        RadioGroup radioType = sheetView.findViewById(R.id.radioType);
        Button buttonApplyFilter = sheetView.findViewById(R.id.buttonApplyFilter);
        Button buttonResetFilter = sheetView.findViewById(R.id.buttonResetFilter);

        seekBarPrice.setMax(500);
        seekBarPrice.setProgress(filterMaxPrice == Integer.MAX_VALUE ? seekBarPrice.getMax() : filterMaxPrice);
        textPriceValue.setText(filterMaxPrice == Integer.MAX_VALUE ? "Up to RM" + seekBarPrice.getMax() : "Up to RM" + filterMaxPrice);
        editLocation.setText(filterLocation);

        if (filterType.equals("Share")) radioType.check(R.id.typeShare);
        else if (filterType.equals("Rent")) radioType.check(R.id.typeRent);
        else if (filterType.equals("Sell")) radioType.check(R.id.typeSell);
        else radioType.clearCheck();

        seekBarPrice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textPriceValue.setText("Up to RM" + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        buttonApplyFilter.setOnClickListener(v -> {
            filterMaxPrice = seekBarPrice.getProgress();
            filterLocation = editLocation.getText().toString().trim();

            int checkedId = radioType.getCheckedRadioButtonId();
            if (checkedId == R.id.typeShare) filterType = "Share";
            else if (checkedId == R.id.typeRent) filterType = "Rent";
            else if (checkedId == R.id.typeSell) filterType = "Sell";
            else filterType = "";

            applyFilter();
            sheetDialog.dismiss();
        });

        buttonResetFilter.setOnClickListener(v -> {
            seekBarPrice.setProgress(seekBarPrice.getMax());
            textPriceValue.setText("Up to RM" + seekBarPrice.getMax());
            editLocation.setText("");
            radioType.clearCheck();

            filterMaxPrice = Integer.MAX_VALUE;
            filterLocation = "";
            filterType = "";

            applyFilter();
            sheetDialog.dismiss();
        });

        sheetDialog.show();
    }

    private void applyFilter() {
        displayedItems.clear();

        for (Item item : allItems) {
            boolean matches = true;

            if (!searchKeyword.isEmpty() && item.name != null &&
                    !item.name.toLowerCase().contains(searchKeyword.toLowerCase())) {
                matches = false;
            }

            switch (spinnerFilter) {
                case 0: // My Items
                    if (item.owner == null || !item.owner.equals(currentUserId)) {
                        matches = false;
                    }
                    break;
                case 1: // Borrowed by Me
                    if (item.status == null || !"borrowed".equals(item.status)
                            || currentEmail == null || !currentEmail.equals(item.borrowedByEmail))
                        matches = false;
                    break;
                case 2: // Lent Out by Me
                    if (item.status == null || !"borrowed".equals(item.status)
                            || item.owner == null || !item.owner.equals(currentUserId))
                        matches = false;
                    break;
                case 3: // Purchased by Me (NEW)
                    if (item.status == null || !"unavailable".equals(item.status)
                            || item.type == null || !item.type.equalsIgnoreCase("Sell")
                            || item.purchasedByEmail == null || !item.purchasedByEmail.equals(currentEmail))
                        matches = false;
                    break;
                case 4: // All Items
                default:
                    break;
            }
                if (item.price != null) {
                    try {
                        double price = Double.parseDouble(item.price.toString());
                        if (price > filterMaxPrice) matches = false;
                    } catch (Exception ignored) {
                    }
                }

                if (!filterLocation.isEmpty() && item.location != null &&
                        !item.location.toLowerCase().contains(filterLocation.toLowerCase())) {
                    matches = false;
                }

                if (!filterType.isEmpty() && item.type != null &&
                        !item.type.equalsIgnoreCase(filterType)) {
                    matches = false;
                }

                if (matches) {
                    displayedItems.add(item);
                }
            }
            adapter.notifyDataSetChanged();
        }
    }

