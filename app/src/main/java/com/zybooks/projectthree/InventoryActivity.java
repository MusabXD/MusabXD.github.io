package com.zybooks.projectthree;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.*;

public class InventoryActivity extends AppCompatActivity {

    private EditText itemNameInput, itemQtyInput;
    private Button addItemButton, addCategoryButton, addItemToCategoryButton, logoutButton;
    private RecyclerView recyclerView;
    private Spinner categorySpinner, sortSpinner;

    private FirebaseRepository firebaseRepo;
    private InventoryAdapter adapter;
    private ArrayList<InventoryItem> itemList;

    private ArrayAdapter<String> categoryAdapter;
    private List<String> categoryList;
    private ListenerRegistration categoriesListener;
    private ListenerRegistration itemsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_grid);

        // Check if user is logged in
        firebaseRepo = new FirebaseRepository();
        if (!firebaseRepo.isUserLoggedIn()) {
            showToast("Please login first");
            finish();
            return;
        }

        setupViews();
        setupRecyclerView();
        setupCategorySpinner();
        setupSortSpinner();
        setupRealTimeListeners();
        setupButtonListeners();
    }

    private void setupViews() {
        itemNameInput = findViewById(R.id.itemNameInput);
        itemQtyInput = findViewById(R.id.itemQtyInput);
        addItemButton = findViewById(R.id.addItemButton);
        addCategoryButton = findViewById(R.id.addCategoryButton);
        addItemToCategoryButton = findViewById(R.id.addItemToCategoryButton);
        categorySpinner = findViewById(R.id.categorySpinner);
        sortSpinner = findViewById(R.id.sortSpinner);
        recyclerView = findViewById(R.id.recyclerView);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void setupRecyclerView() {
        itemList = new ArrayList<>();
        adapter = new InventoryAdapter(itemList, this::deleteItem, this::updateItem);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupCategorySpinner() {
        categoryList = new ArrayList<>();
        categoryList.add("All");
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterAndSort();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSortSpinner() {
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Sort by Name (A-Z)", "Sort by Name (Z-A)", "Sort by Qty (Low→High)", "Sort by Qty (High→Low)"}
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterAndSort();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRealTimeListeners() {
        setupItemsRealTimeListener();
        setupCategoriesRealTimeListener();
    }

    private void setupItemsRealTimeListener() {
        itemsListener = firebaseRepo.getItemsLive((querySnapshot, error) -> {
            if (error != null) {
                showToast("Error loading items: " + error.getMessage());
                return;
            }

            itemList.clear();
            if (querySnapshot != null) {
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    InventoryItem item = InventoryItem.fromFirestore(doc.getId(), doc.getData());
                    itemList.add(item);
                }
            }
            filterAndSort();
        });
    }

    private void setupCategoriesRealTimeListener() {
        categoriesListener = firebaseRepo.getCategoriesLive((querySnapshot, error) -> {
            if (error != null) {
                showToast("Error loading categories: " + error.getMessage());
                return;
            }

            List<String> categories = new ArrayList<>();
            if (querySnapshot != null) {
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String categoryName = doc.getString("name");
                    if (categoryName != null && !categoryName.isEmpty()) {
                        categories.add(categoryName);
                    }
                }
            }

            updateCategoriesSpinner(categories);
        });
    }

    private void updateCategoriesSpinner(List<String> categories) {
        List<String> updatedList = new ArrayList<>();
        updatedList.add("All"); // Always include "All" option

        // Add all categories and sort them
        updatedList.addAll(categories);
        if (updatedList.size() > 1) {
            Collections.sort(updatedList.subList(1, updatedList.size())); // Sort excluding "All"
        }

        // Preserve current selection
        String currentSelection = (String) categorySpinner.getSelectedItem();

        // Update the list
        categoryList.clear();
        categoryList.addAll(updatedList);

        // Notify the adapter
        categoryAdapter.notifyDataSetChanged();

        // Restore selection if it still exists
        if (currentSelection != null && categoryList.contains(currentSelection)) {
            int position = categoryList.indexOf(currentSelection);
            if (position >= 0) {
                categorySpinner.setSelection(position);
            }
        }
    }

    private void setupButtonListeners() {
        addItemButton.setOnClickListener(v -> addItem());
        addCategoryButton.setOnClickListener(v -> addCategory());
        addItemToCategoryButton.setOnClickListener(v -> assignCategoryToSelected());
        logoutButton.setOnClickListener(v -> logoutUser());
    }

    private void addItem() {
        String name = itemNameInput.getText().toString().trim();
        String qtyStr = itemQtyInput.getText().toString().trim();

        if (name.isEmpty() || qtyStr.isEmpty()) {
            showToast("Please fill out both fields");
            return;
        }

        try {
            int quantity = Integer.parseInt(qtyStr);
            if (quantity < 0) {
                showToast("Quantity cannot be negative");
                return;
            }

            firebaseRepo.addItem(name, quantity, new FirebaseRepository.FirebaseCallback() {
                @Override
                public void onSuccess() {
                    itemNameInput.setText("");
                    itemQtyInput.setText("");
                    showToast("Item added successfully");
                }

                @Override
                public void onError(String error) {
                    showToast("Error adding item: " + error);
                }
            });
        } catch (NumberFormatException e) {
            showToast("Quantity must be a valid number");
        }
    }

    private void deleteItem(String itemId) {
        firebaseRepo.deleteItem(itemId, new FirebaseRepository.FirebaseCallback() {
            @Override
            public void onSuccess() {
                showToast("Item deleted successfully");
            }
            @Override
            public void onError(String error) {
                showToast("Error deleting item: " + error);
            }
        });
    }

    private void updateItem(String itemId, String name, int quantity) {
        if (name.isEmpty()) {
            showToast("Item name cannot be empty");
            return;
        }

        if (quantity < 0) {
            showToast("Quantity cannot be negative");
            return;
        }

        firebaseRepo.updateItem(itemId, name, quantity, new FirebaseRepository.FirebaseCallback() {
            @Override
            public void onSuccess() {
                showToast("Item updated successfully");
            }
            @Override
            public void onError(String error) {
                showToast("Error updating item: " + error);
            }
        });
    }

    private void addCategory() {
        EditText input = new EditText(this);
        input.setHint("Category name");

        new android.app.AlertDialog.Builder(this)
                .setTitle("Add New Category")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String categoryName = input.getText().toString().trim();
                    if (categoryName.isEmpty()) {
                        showToast("Category name cannot be empty");
                        return;
                    }

                    // Check if category already exists locally (case-insensitive)
                    for (String existingCategory : categoryList) {
                        if (existingCategory.equalsIgnoreCase(categoryName)) {
                            showToast("Category already exists");
                            return;
                        }
                    }

                    firebaseRepo.addCategory(categoryName, new FirebaseRepository.FirebaseCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Category added: " + categoryName);
                            // The real-time listener will automatically update the spinner
                        }

                        @Override
                        public void onError(String error) {
                            showToast("Error: " + error);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void assignCategoryToSelected() {
        List<InventoryItem> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) {
            showToast("No items selected");
            return;
        }

        if (categoryList.size() <= 1) { // Only "All" exists
            showToast("Please create a category first");
            return;
        }

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryList);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(catAdapter);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Category")
                .setView(spinner)
                .setPositiveButton("Assign", (dialog, which) -> {
                    String category = (String) spinner.getSelectedItem();
                    if ("All".equals(category)) {
                        showToast("Please select a valid category");
                        return;
                    }

                    final int[] successCount = {0};
                    final int[] errorCount = {0};

                    for (InventoryItem item : selected) {
                        firebaseRepo.assignCategory(item.getId(), category, new FirebaseRepository.FirebaseCallback() {
                            @Override
                            public void onSuccess() {
                                successCount[0]++;
                            }
                            @Override
                            public void onError(String error) {
                                errorCount[0]++;
                            }
                        });
                    }

                    adapter.clearSelection();

                    if (errorCount[0] == 0) {
                        showToast("Items successfully added to " + category);
                    } else {
                        showToast(successCount[0] + " items assigned, " + errorCount[0] + " failed");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterAndSort() {
        String selectedCategory = (String) categorySpinner.getSelectedItem();
        ArrayList<InventoryItem> filteredList = new ArrayList<>();

        // Handle null selected category
        if (selectedCategory == null) {
            selectedCategory = "All";
        }

        // Filter by category - handle null categories safely
        for (InventoryItem item : itemList) {
            String itemCategory = item.getCategory();

            // Handle null or empty categories
            if (itemCategory == null) {
                itemCategory = "";
            }

            // Safe comparison - always put the literal first
            if ("All".equals(selectedCategory) || selectedCategory.equals(itemCategory)) {
                filteredList.add(item);
            }
        }

        // Sort
        int sortOption = sortSpinner.getSelectedItemPosition();
        switch (sortOption) {
            case 0: // Name A-Z
                Collections.sort(filteredList, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                break;
            case 1: // Name Z-A
                Collections.sort(filteredList, (a, b) -> b.getName().compareToIgnoreCase(a.getName()));
                break;
            case 2: // Qty low→high
                Collections.sort(filteredList, Comparator.comparingInt(InventoryItem::getQuantity));
                break;
            case 3: // Qty high→low
                Collections.sort(filteredList, (a, b) -> b.getQuantity() - a.getQuantity());
                break;
        }

        adapter.updateList(filteredList);
    }

    private void logoutUser() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    firebaseRepo.signOut();
                    showToast("Logged out successfully");
                    goToLogin();
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Add this helper method
    private void goToLogin() {
        Intent intent = new Intent(InventoryActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners
        if (categoriesListener != null) {
            categoriesListener.remove();
        }
        if (itemsListener != null) {
            itemsListener.remove();
        }
    }
}