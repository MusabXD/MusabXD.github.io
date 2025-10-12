package com.zybooks.projectthree;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private ArrayList<InventoryItem> items;
    private final DeleteItemListener deleteItemListener;
    private final UpdateItemListener updateItemListener;

    // Track selected rows (multi-select support)
    private final Set<Integer> selectedPositions = new HashSet<>();

    // Interfaces for callbacks
    public interface DeleteItemListener {
        void onDeleteItem(String itemId);
    }

    public interface UpdateItemListener {
        void onUpdateItem(String itemId, String name, int quantity);
    }

    public InventoryAdapter(ArrayList<InventoryItem> items,
                            DeleteItemListener deleteListener,
                            UpdateItemListener updateListener) {
        this.items = items;
        this.deleteItemListener = deleteListener;
        this.updateItemListener = updateListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryItem item = items.get(position);
        holder.name.setText(item.getName());
        holder.qty.setText("Qty: " + item.getQuantity());

        // Sync checkbox state with selection
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedPositions.contains(position));

        // Handle checkbox clicks
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPositions.add(holder.getAdapterPosition());
            } else {
                selectedPositions.remove(holder.getAdapterPosition());
            }
        });

        // Delete button
        holder.deleteBtn.setOnClickListener(v -> {
            if (deleteItemListener != null) {
                deleteItemListener.onDeleteItem(item.getId());
            }
        });

        // Normal click → update dialog (if not in multi-select mode)
        holder.itemView.setOnClickListener(v -> {
            if (selectedPositions.isEmpty()) {
                showUpdateDialog(v.getContext(), item);
            } else {
                // toggle checkbox when in selection mode
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Get selected items
    public List<InventoryItem> getSelectedItems() {
        List<InventoryItem> selected = new ArrayList<>();
        for (int pos : selectedPositions) {
            if (pos >= 0 && pos < items.size()) {
                selected.add(items.get(pos));
            }
        }
        return selected;
    }

    // Clear all selections
    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    // Update the entire list
    public void updateList(ArrayList<InventoryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    // Update popup
    private void showUpdateDialog(Context context, InventoryItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Item");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        EditText nameInput = new EditText(context);
        nameInput.setHint("Item Name");
        nameInput.setText(item.getName());
        layout.addView(nameInput);

        EditText qtyInput = new EditText(context);
        qtyInput.setHint("Quantity");
        qtyInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        qtyInput.setText(String.valueOf(item.getQuantity()));
        layout.addView(qtyInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String qtyText = qtyInput.getText().toString().trim();

            if (!name.isEmpty() && !qtyText.isEmpty()) {
                int qty = Integer.parseInt(qtyText);
                if (updateItemListener != null) {
                    updateItemListener.onUpdateItem(item.getId(), name, qty);
                }
            } else {
                Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ViewHolder
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, qty;
        final Button deleteBtn;
        final CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.itemNameText);
            qty = itemView.findViewById(R.id.itemQtyText);
            deleteBtn = itemView.findViewById(R.id.deleteButton);
            checkBox = itemView.findViewById(R.id.itemCheckBox);
        }
    }
}