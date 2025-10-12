package com.zybooks.projectthree;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.EventListener;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FirebaseRepository {
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public FirebaseRepository() {
        try {
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();

            // Enable offline persistence
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            db.setFirestoreSettings(settings);
        } catch (Exception e) {
            Log.e("Firebase", "Error initializing Firebase", e);
        }
    }

    // ========== EMAIL/PASSWORD AUTH METHODS ==========
    public void createUserWithEmail(String email, String password, FirebaseAuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        Log.d("Firebase", "User created successfully: " + email);

                        if (user != null) {
                            user.sendEmailVerification().addOnCompleteListener(vTask -> {
                                if (vTask.isSuccessful()) {
                                    Log.d("Firebase", "Verification email sent to: " + email);
                                }
                            });

                            callback.onSuccess();

                            // Continue Firestore write in background
                            storeUserInFirestore(user.getUid(), email, new FirebaseAuthCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.d("Firebase", "User stored in Firestore in background");
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e("Firebase", "Firestore store failed in background: " + error);
                                }
                            });

                        } else {
                            callback.onSuccess();
                        }
                    } else {
                        Log.e("Firebase", "Error creating user: " + email, task.getException());
                        callback.onError(task.getException() != null ?
                                task.getException().getMessage() : "Unknown error");
                    }
                });
    }


    public void signInWithEmail(String email, String password, FirebaseAuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firebase", "User signed in successfully: " + email);
                        callback.onSuccess();
                    } else {
                        Log.e("Firebase", "Error signing in: " + email, task.getException());
                        callback.onError(task.getException() != null ?
                                task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    // ========== GOOGLE SIGN-IN METHODS ==========
    public void signInWithGoogle(String idToken, FirebaseAuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            Log.d("Firebase", "Google sign-in successful: " + user.getEmail());
                            // Store user info in Firestore
                            storeUserInFirestore(user.getUid(), user.getEmail(), callback);
                        } else {
                            callback.onSuccess();
                        }
                    } else {
                        Log.e("Firebase", "Google sign-in failed", task.getException());
                        callback.onError(task.getException() != null ?
                                task.getException().getMessage() : "Google sign-in failed");
                    }
                });
    }

    // ========== USER MANAGEMENT ==========
    private void storeUserInFirestore(String userId, String email, FirebaseAuthCallback callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "User stored in Firestore: " + email);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error storing user in Firestore: " + email, e);
                    // Still consider it success since auth worked
                    callback.onSuccess();
                });
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void signOut() {
        try {
            auth.signOut();
            Log.d("Firebase", "User signed out successfully");
        } catch (Exception e) {
            Log.e("Firebase", "Error signing out", e);
        }
    }


    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    // ========== INVENTORY METHODS (User-specific) ==========
    public void addItem(String name, int quantity, FirebaseCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not logged in");
            return;
        }

        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("quantity", quantity);
        item.put("category", "");
        item.put("userId", user.getUid()); // Link item to user
        item.put("createdAt", FieldValue.serverTimestamp());

        db.collection("inventory")
                .add(item)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firebase", "Item added successfully: " + name);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error adding item: " + name, e);
                    callback.onError(e.getMessage());
                });
    }

    public void updateItem(String itemId, String name, int quantity, FirebaseCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not logged in");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("quantity", quantity);

        db.collection("inventory")
                .document(itemId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "Item updated successfully: " + name);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error updating item: " + name, e);
                    callback.onError(e.getMessage());
                });
    }

    public void deleteItem(String itemId, FirebaseCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not logged in");
            return;
        }

        db.collection("inventory")
                .document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "Item deleted successfully: " + itemId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error deleting item: " + itemId, e);
                    callback.onError(e.getMessage());
                });
    }

    public void assignCategory(String itemId, String category, FirebaseCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not logged in");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("category", category);

        db.collection("inventory")
                .document(itemId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "Category assigned successfully: " + category + " to item " + itemId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error assigning category: " + category + " to item " + itemId, e);
                    callback.onError(e.getMessage());
                });
    }

    // Get items with real-time updates (user-specific)
    public ListenerRegistration getItemsLive(EventListener<QuerySnapshot> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return null;
        }

        return db.collection("inventory")
                .whereEqualTo("userId", user.getUid()) // Only get user's items
                .addSnapshotListener(listener);
    }

    // ========== CATEGORY METHODS ==========
    public void addCategory(String category, FirebaseCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not logged in");
            return;
        }

        Map<String, Object> cat = new HashMap<>();
        cat.put("name", category);
        cat.put("userId", user.getUid()); // Link category to user
        cat.put("createdAt", FieldValue.serverTimestamp());

        db.collection("categories")
                .document(category + "_" + user.getUid()) // Make category user-specific
                .set(cat)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "Category added successfully: " + category);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error adding category: " + category, e);
                    callback.onError(e.getMessage());
                });
    }

    public void getCategories(FirebaseCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not logged in");
            return;
        }

        db.collection("categories")
                .whereEqualTo("userId", user.getUid()) // Only get user's categories
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> categories = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String categoryName = doc.getString("name");
                        if (categoryName != null && !categoryName.isEmpty()) {
                            categories.add(categoryName);
                        }
                    }
                    Log.d("Firebase", "Categories loaded: " + categories.size());
                    callback.onCategoriesLoaded(categories);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error loading categories", e);
                    callback.onError(e.getMessage());
                });
    }

    // Get categories with real-time updates (user-specific)
    public ListenerRegistration getCategoriesLive(EventListener<QuerySnapshot> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return null;
        }

        return db.collection("categories")
                .whereEqualTo("userId", user.getUid()) // Only get user's categories
                .addSnapshotListener(listener);
    }

    // ========== BATCH OPERATIONS ==========
    public void migrateFromLocal(List<InventoryItem> localItems, FirebaseCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not logged in");
            return;
        }

        if (localItems == null || localItems.isEmpty()) {
            callback.onError("No items to migrate");
            return;
        }

        // Use batched write for atomic operation
        WriteBatch batch = db.batch();

        for (InventoryItem item : localItems) {
            Map<String, Object> firebaseItem = new HashMap<>();
            firebaseItem.put("name", item.getName());
            firebaseItem.put("quantity", item.getQuantity());
            firebaseItem.put("category", item.getCategory() != null ? item.getCategory() : "");
            firebaseItem.put("userId", user.getUid());
            firebaseItem.put("createdAt", FieldValue.serverTimestamp());

            DocumentReference docRef = db.collection("inventory").document(String.valueOf(item.getId()));
            batch.set(docRef, firebaseItem);
        }

        // Commit the batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Migration", "Migration completed successfully for " + localItems.size() + " items");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("Migration", "Migration failed", e);
                    callback.onError("Migration failed: " + e.getMessage());
                });
    }

    // ========== SEARCH METHODS ==========
    public void searchItemsByName(String query, FirebaseCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not logged in");
            return;
        }

        db.collection("inventory")
                .whereEqualTo("userId", user.getUid())
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<InventoryItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        InventoryItem item = InventoryItem.fromFirestore(doc.getId(), doc.getData());
                        items.add(item);
                    }
                    callback.onItemsLoaded(items);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error searching items: " + query, e);
                    callback.onError(e.getMessage());
                });
    }

    // ========== CALLBACK INTERFACES ==========
    public interface FirebaseAuthCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface FirebaseCallback {
        void onSuccess();
        void onError(String error);

        default void onCategoriesLoaded(List<String> categories) {}
        default void onItemsLoaded(List<InventoryItem> items) {}
    }
}