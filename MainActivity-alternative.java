package team.tr.permitlog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;

public class MainActivity2 extends AppCompatActivity {
    //For logging
    private static final String TAG = "MainActivity";

    // Firebase variables
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Sign in request code
    private static final int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the current user from Firebase. If no user is logged in,
        // show the FirebaseUI login screen.
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

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

    //This method is called after an activity started from here is finished and we want the result:
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If this is the signing in activity:
        if (requestCode == RC_SIGN_IN) {
            //Get the GoogleSignInResult:
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            //If it was successful, authenticate with Firebase:
            if (result != null && result.isSuccess()) {
                Log.d(TAG, "Google Success");
                GoogleSignInAccount account = result.getSignInAccount();
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Firebase success");
                                    //What should we do after authentication? Should we even do anything?
                                } else {
                                    //TODO: Output error to user
                                    Log.d(TAG, "Firebase failure");
                                }
                            }
                        });
            } else {
                //TODO: Output error to user
                Log.d(TAG, "Google Failure");
            }
        }
    }
}
