package com.example.uitmtoolshare;

public class Item implements java.io.Serializable {
    public String name;
    public String category;
    public String description;
    public String condition;
    public String location;
    public String imagePath;
    public String owner;
    public String status;
    public Double latitude;
    public Double longitude;
    public String borrowedBy;
    public String borrowedByEmail; // Who borrowed it
    public String purchasedByEmail; //purhcase item
    public String type; // Share, Rent, Sell/Swap
    public String price; // Keep as String for flexibility (e.g., "Free", "N/A", etc.)

    public Item() {
        // Required empty constructor for Firebase
    }

    public Item(String name, String category, String description, String condition,
                String location, String imagePath, String owner, String status,
                Double latitude, Double longitude, String borrowedBy, String borrowedByEmail,
                String purchasedByEmail, String type, String price) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.condition = condition;
        this.location = location;
        this.imagePath = imagePath;
        this.owner = owner;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.borrowedBy = borrowedBy;
        this.borrowedByEmail = borrowedByEmail;
        this.purchasedByEmail = purchasedByEmail;
        this.type = type;
        this.price = price;
    }
}
