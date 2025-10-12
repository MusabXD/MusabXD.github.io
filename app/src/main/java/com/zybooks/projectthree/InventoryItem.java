package com.zybooks.projectthree;

import java.util.Map;

public class InventoryItem {
    private String id;
    private String name;
    private int quantity;
    private String category;

    // Required empty constructor for Firebase
    public InventoryItem() {}

    // Your existing constructor (for compatibility)
    public InventoryItem(int id, String name, int quantity) {
        this.id = String.valueOf(id);
        this.name = name;
        this.quantity = quantity;
        this.category = "";
    }

    // New constructor for Firebase
    public InventoryItem(String id, String name, int quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.category = "";
    }

    // Firebase document to object conversion
    public static InventoryItem fromFirestore(String documentId, Map<String, Object> data) {
        InventoryItem item = new InventoryItem();
        item.setId(documentId);
        item.setName((String) data.get("name"));

        // Handle quantity (Firestore uses Long for numbers)
        Object quantityObj = data.get("quantity");
        if (quantityObj instanceof Long) {
            item.setQuantity(((Long) quantityObj).intValue());
        } else if (quantityObj instanceof Integer) {
            item.setQuantity((Integer) quantityObj);
        } else {
            item.setQuantity(0); // Default if null
        }

        // Handle category - ensure it's never null
        String category = (String) data.get("category");
        item.setCategory(category); // This will use the setter that handles null

        return item;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getCategory() {
        return category != null ? category : "";
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setCategory(String category) {
        this.category = category != null ? category : "";
    }

}