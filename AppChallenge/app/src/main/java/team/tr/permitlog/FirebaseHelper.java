package team.tr.permitlog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {
    //This stores the database:
    private static FirebaseDatabase mDatabase;

    public static FirebaseDatabase getDatabase() {
        /* This function returns the FirebaseDatabase for which the setPersistenceEnabled() has been set. */
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabase.setPersistenceEnabled(true);
        }
        return mDatabase;
    }

    public static boolean signInIfNeeded(MainActivity mainActivity) {
        /* This function shows the sign in screen if the user is not signed in
           and then returns if the user was signed in or not. */
        //Figure out if the user is signed in:
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean signedIn = (currentUser != null);
        //If the user is not signed in, then show the sign in screen:
        if (!signedIn) mainActivity.showSignIn();
        //Finally, return the boolean:
        return signedIn;
    }
}
