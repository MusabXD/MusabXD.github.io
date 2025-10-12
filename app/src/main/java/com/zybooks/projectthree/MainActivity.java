package com.zybooks.projectthree;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private EditText usernameField, passwordField;
    private Button loginButton, createAccountButton;
    private SignInButton googleSignInButton;

    private FirebaseRepository firebaseRepo;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if user is already logged in
        firebaseRepo = new FirebaseRepository();
        if (firebaseRepo.isUserLoggedIn()) {
            goToInventory();
            return;
        }

        initializeViews();
        setupGoogleSignIn();
        setupClickListeners();
    }

    private void initializeViews() {
        usernameField = findViewById(R.id.usernameField);
        passwordField = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);

        // Set the size of Google Sign-In button
        googleSignInButton.setSize(SignInButton.SIZE_WIDE);
    }

    private void setupGoogleSignIn() {
        // Configure Google Sign In with your Web Client ID
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        // Email login
        loginButton.setOnClickListener(v -> attemptEmailLogin());

        // Create account
        createAccountButton.setOnClickListener(v -> createEmailAccount());

        // Google Sign-In
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void attemptEmailLogin() {
        String email = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please enter email and password");
            return;
        }

        if (!isValidEmail(email)) {
            showToast("Please enter a valid email address");
            return;
        }

        showLoading(true);
        firebaseRepo.signInWithEmail(email, password, new FirebaseRepository.FirebaseAuthCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    showLoading(false);
                    showToast("Login successful");
                    goToInventory();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    String userFriendlyError = getAuthErrorMessage(error);
                    showToast(userFriendlyError);
                });
            }
        });
    }

    private void createEmailAccount() {
        String email = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please enter email and password");
            return;
        }

        if (!isValidEmail(email)) {
            showToast("Please enter a valid email address");
            return;
        }

        if (password.length() < 6) {
            showToast("Password must be at least 6 characters");
            return;
        }

        showLoading(true);
        firebaseRepo.createUserWithEmail(email, password, new FirebaseRepository.FirebaseAuthCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    showLoading(false);
                    showToast("Account created successfully");
                    goToInventory();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    String userFriendlyError = getAuthErrorMessage(error);
                    showToast(userFriendlyError);
                });
            }
        });
    }

    private void signInWithGoogle() {
        showLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Google Sign-In was successful, authenticate with Firebase
            if (account != null && account.getIdToken() != null) {
                String idToken = account.getIdToken();
                firebaseRepo.signInWithGoogle(idToken, new FirebaseRepository.FirebaseAuthCallback() {
                    @Override
                    public void onSuccess() {
                        showLoading(false);
                        showToast("Google sign-in successful");
                        goToInventory();
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        showToast("Google sign-in failed: " + error);
                    }
                });
            } else {
                showLoading(false);
                showToast("Google sign-in failed: No account information");
            }
        } catch (ApiException e) {
            showLoading(false);
            String errorMessage = getGoogleSignInErrorMessage(e.getStatusCode());
            showToast("Google sign-in failed: " + errorMessage);

            // Sign out from Google if there was an error
            googleSignInClient.signOut();
        }
    }

    private String getGoogleSignInErrorMessage(int statusCode) {
        switch (statusCode) {
            case com.google.android.gms.common.ConnectionResult.NETWORK_ERROR:
                return "Network error. Please check your internet connection";
            case com.google.android.gms.common.ConnectionResult.TIMEOUT:
                return "Connection timeout. Please try again";
            case com.google.android.gms.common.ConnectionResult.INTERNAL_ERROR:
                return "Internal error. Please try again";
            case com.google.android.gms.common.ConnectionResult.SIGN_IN_FAILED:
                return "Sign-in failed. Please try again";
            case com.google.android.gms.common.ConnectionResult.SERVICE_UPDATING:
                return "Service updating. Please try again later";
            case com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                return "Please update Google Play Services";
            case com.google.android.gms.common.ConnectionResult.SERVICE_DISABLED:
                return "Google Play Services disabled";
            case com.google.android.gms.common.ConnectionResult.SERVICE_INVALID:
                return "Invalid Google Play Services";
            case com.google.android.gms.common.ConnectionResult.API_UNAVAILABLE:
                return "Google Play Services unavailable";
            case com.google.android.gms.common.ConnectionResult.RESOLUTION_REQUIRED:
                return "Resolution required for Google Sign-In";
            case com.google.android.gms.common.ConnectionResult.RESTRICTED_PROFILE:
                return "Google Sign-In not available on restricted profiles";
            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                return "Google Sign-In cancelled";
            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                return "Google Sign-In failed";
            case GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS:
                return "Sign-in already in progress";
            default:
                return "Unknown error (Code: " + statusCode + "). Please try again";
        }
    }

    private String getAuthErrorMessage(String error) {
        if (error == null) return "Unknown error occurred";

        if (error.contains("network error") || error.contains("offline")) {
            return "Network error. Please check your internet connection";
        } else if (error.contains("invalid email") || error.contains("malformed")) {
            return "Invalid email address or password";
        } else if (error.contains("user not found")) {
            return "No account found with this email. Please create an account";
        } else if (error.contains("wrong password")) {
            return "Incorrect password. Please try again";
        } else if (error.contains("email already in use")) {
            return "An account with this email already exists. Please login instead";
        } else if (error.contains("weak password")) {
            return "Password is too weak. Please use a stronger password";
        } else if (error.contains("too many requests")) {
            return "Too many attempts. Please try again later";
        } else {
            return error; // Return original error if no match
        }
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void showLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        createAccountButton.setEnabled(!isLoading);
        googleSignInButton.setEnabled(!isLoading);

        if (isLoading) {
            loginButton.setText("Logging in...");
            createAccountButton.setText("Creating...");
        } else {
            loginButton.setText("Login with Email");
            createAccountButton.setText("Create Account");
        }
    }

    private void goToInventory() {
        Intent intent = new Intent(MainActivity.this, InventoryActivity.class);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly
        if (firebaseRepo.isUserLoggedIn()) {
            goToInventory();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
    }
}