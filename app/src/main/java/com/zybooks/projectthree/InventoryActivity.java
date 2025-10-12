package com.zybooks.projectthree;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    EditText itemNameInput, itemQtyInput;
    Button addItemButton, addCategoryButton, addItemToCategoryButton;
    RecyclerView recyclerView;
    Spinner categorySpinner, sortSpinner;

    DatabaseHelper db;
    InventoryAdapter adapter;
    ArrayList<InventoryItem> itemList;

    ArrayAdapter<String> categoryAdapter;
    List<String> categoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_grid);

        // --- Bind UI ---
        itemNameInput = findViewById(R.id.itemNameInput);
        itemQtyInput = findViewById(R.id.itemQtyInput);
        addItemButton = findViewById(R.id.addItemButton);
        addCategoryButton = findViewById(R.id.addCategoryButton);
        addItemToCategoryButton = findViewById(R.id.addItemToCategoryButton);
        categorySpinner = findViewById(R.id.categorySpinner);
        sortSpinner = findViewById(R.id.sortSpinner);
        recyclerView = findViewById(R.id.recyclerView);

        db = new DatabaseHelper(this);

        // --- Setup category spinner ---
        categoryList = new ArrayList<>();
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        loadCategories(); // initial load

        // --- Setup sort spinner ---
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Sort by Name (A-Z)", "Sort by Name (Z-A)", "Sort by Qty (Low→High)", "Sort by Qty (High→Low)"}
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        // --- Setup RecyclerView ---
        itemList = db.getItemsByCategory("All"); // no need for dbHelperToList()
        adapter = new InventoryAdapter(itemList, db, this::refreshAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // --- Spinner Listeners ---
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                filterAndSort();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                filterAndSort();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- Add item (no category) ---
        addItemButton.setOnClickListener(v -> {
            String name = itemNameInput.getText().toString().trim();
            String qtyStr = itemQtyInput.getText().toString().trim();

            if (name.isEmpty() || qtyStr.isEmpty()) {
                showToast("Please fill out both fields");
            } else {
                try {
                    int quantity = Integer.parseInt(qtyStr);
                    db.addInventoryItem(name, quantity);
                    itemNameInput.setText("");
                    itemQtyInput.setText("");
                    refreshAdapter();
                    showToast("Item added");
                } catch (NumberFormatException e) {
                    showToast("Quantity must be a number");
                }
            }
        });

        // --- Add category ---
        addCategoryButton.setOnClickListener(v -> {
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

                        boolean success = db.addCategory(categoryName);
                        if (success) {
                            loadCategories(); // refresh spinner
                            showToast("Category added");
                        } else {
                            showToast("Category already exists");
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // --- Add item to selected category ---
        addItemToCategoryButton.setOnClickListener(v -> {
            List<InventoryItem> selected = adapter.getSelectedItems();
            if (selected.isEmpty()) {
                showToast("No items selected");
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
                        if (category.equals("All")) {
                            showToast("Please select a valid category");
                            return;
                        }

                        for (InventoryItem item : selected) {
                            db.assignItemToCategory(item.getId(), category);
                        }
                        adapter.clearSelection();
                        refreshAdapter();
                        showToast("Items added to " + category);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

    }

    // --- Refresh RecyclerView ---
    private void refreshAdapter() {
        filterAndSort();
    }

    // --- Load categories into spinner ---
    private void loadCategories() {
        categoryList.clear();
        categoryList.add("All"); // default option
        categoryList.addAll(db.getAllCategories());
        categoryAdapter.notifyDataSetChanged();
    }

    // --- Filter + Sort combined ---
    private void filterAndSort() {
        String selectedCategory = (String) categorySpinner.getSelectedItem();
        ArrayList<InventoryItem> filteredList = db.getItemsByCategory(
                (selectedCategory == null) ? "All" : selectedCategory
        );

        int sortOption = sortSpinner.getSelectedItemPosition();
        switch (sortOption) {
            case 0: // Name A-Z
                Collections.sort(filteredList, Comparator.comparing(InventoryItem::getName, String.CASE_INSENSITIVE_ORDER));
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

        itemList.clear();
        itemList.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }

    // --- Toast helper ---
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
