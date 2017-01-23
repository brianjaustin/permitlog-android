package team.tr.permitlog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    // For logging
    private static final String TAG = "MainActivity";

    // Firebase variables:
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Sign in request code
    private static final int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the current user from Firebase.
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        // Log whether currentUser is null or not:
        Log.d(TAG, "Is the user not signed in? "+Boolean.toString(currentUser == null));
        // If no user is logged in, show the FirebaseUI login screen.
        if (currentUser == null) {
            startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setProviders(Arrays.asList(
                                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
                        ))
                        .build(),
                    RC_SIGN_IN
            );
        }
    }

    //This is for when we start an activity and then want a result from it back:
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If this is for the sign in activity:
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == ResultCodes.OK) {
                Log.d(TAG, "Login was successful");
                // Now that the user is signed in, update currentUser:
                currentUser = mAuth.getCurrentUser();
            } else {
                // If there is not a success, try to figure out what went wrong:
                if (response == null) Log.e(TAG, "User pressed back button");
                else if (response.getErrorCode() == ErrorCodes.NO_NETWORK) Log.e(TAG, "Network connection error");
                else if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) Log.e(TAG, "Unknown error");
                else Log.e(TAG, "Unknown response");
            }
            // Debug currentUser again:
            Log.d(TAG, "Is the user not signed in? "+Boolean.toString(currentUser == null));
        }
    }
}
