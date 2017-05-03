package team.tr.permitlog;

import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;

public class FirebaseHelper {
    //For logging:
    private static String TAG = "FirebaseHelper";

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

    public static ChildEventListener transformListener(final ChildEventListener normal, final DataSnapshotPredicate isCompleteChild, final ArrayList<String> ids) {
        /* Returns ChildEventListener that can deal with incomplete children using ids,
               which should be a clear ArrayList at first.
           Checks if child is complete with isCompleteChild. */
        return new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot childSnapshot, String s) {
                //If this is a complete child, add it to ids and call onChildAdded():
                if (isCompleteChild.accept(childSnapshot)) {
                    ids.add(childSnapshot.getKey());
                    normal.onChildAdded(childSnapshot, s);
                }
                //Otherwise, log them for being incomplete:
                else {
                    Log.d(TAG, "The following is not a valid child: "+childSnapshot.getKey());
                }
            }
            @Override
            public void onChildChanged(DataSnapshot childSnapshot, String s) {
                //If this is a complete child:
                if (isCompleteChild.accept(childSnapshot)) {
                    //If this child is in ids, simple call onChildChanged():
                    if (ids.contains(childSnapshot.getKey())) normal.onChildChanged(childSnapshot, s);
                    //Otherwise, log them for not being in ids and add them:
                    else {
                        Log.d(TAG, "The following is being added to ids from onChildChanged(): "+childSnapshot.getKey());
                        onChildAdded(childSnapshot, s);
                    }
                }
                //Otherwise, log them for being incomplete:
                else {
                    Log.d(TAG, "The following is not a valid child: "+childSnapshot.getKey());
                }
            }
            @Override
            public void onChildRemoved(DataSnapshot childSnapshot) {
                //If this is in ids, remove them from ids and call onChildRemoved():
                if (ids.contains(childSnapshot.getKey())) {
                    normal.onChildRemoved(childSnapshot);
                    ids.remove(childSnapshot.getKey());
                }
                //Otherwise, log them for being incomplete:
                else {
                    Log.d(TAG, "The following was not added to ids for some reason: "+childSnapshot.getKey());
                }
            }
            //Simply copy normal on onChildMoved() and onCancelled():
            @Override
            public void onChildMoved(DataSnapshot d, String s) { normal.onChildMoved(d, s); }
            @Override
            public void onCancelled(DatabaseError e) { normal.onCancelled(e); }
        };
    }
}
