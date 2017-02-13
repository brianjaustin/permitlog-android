package team.tr.permitlog;

import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseHelper {
    public static boolean signInIfNeeded(MainActivity mainActivity) {
        /* This function shows the sign in screen if the user is not signed in
           and then returns if the user was signed in or not. */
        //Figure out if the user is signed in:
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean signedIn = (currentUser != null);
        //If the user is not signed in, then show the sign in screen:
        if (!signedIn) {
            Toast.makeText(mainActivity, R.string.unknown_auth_error, Toast.LENGTH_SHORT).show();
            mainActivity.showSignIn();
        }
        //Finally, return the boolean:
        return signedIn;
    }
}
