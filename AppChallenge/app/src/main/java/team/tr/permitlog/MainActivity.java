package team.tr.permitlog;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    // For logging
    private static final String TAG = "MainActivity";

    // Firebase variables:
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private static boolean isPersistenceEnabled = false;

    // For menu
    private String[] menuItems;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private static final int HOME_MENU_INDEX = 0;
    private static final int LOG_MENU_INDEX = 1;
    private static final int DRIVERS_MENU_INDEX = 2;
    private static final int SETTINGS_MENU_INDEX = 3;
    private static final int ABOUT_MENU_INDEX = 4;
    private static final int SIGN_OUT_MENU_INDEX = 5;

    // Sign in request code
    private static final int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup menu
        menuItems = getResources().getStringArray(R.array.menu_items);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, menuItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // Show the menu button in the title bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24px);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Set persistence if not set:
        // isPersistenceEnabled stays after Instant Run, so Instant Run does not call setPersistenceEnabled() again
        // as if it did, the app would crash.
        if (!isPersistenceEnabled) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            isPersistenceEnabled = true;
        }

        // Highlight the home menu item by default
        mDrawerList.setItemChecked(HOME_MENU_INDEX, true);

        // Get the current user from Firebase.
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        // Log whether currentUser is null or not:
        Log.d(TAG, "Is the user not signed in? "+Boolean.toString(currentUser == null));
        navigateBasedOnUser();
    }


    private void transitionFragment(Fragment fragment, int position, String title) {
        /* This function switches the current fragment to the object passed in and sets the title. */
        // Assuming the user is signed in:
        if (currentUser != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
            // Change the title
            getSupportActionBar().setTitle(title);
            mDrawerList.setItemChecked(position, true);
        }
        //Otherwise, force the user to sign in:
        else showSignIn();
    }

    private void navigateBasedOnUser() {
        // If no user is logged in, show the FirebaseUI login screen.
        if (currentUser == null) {
            showSignIn();
        } else {
            // Transition to the home fragment
            transitionFragment(new HomeFragment(), HOME_MENU_INDEX, "Permit Log");
            // Get the goal values; if none exist, show the settings page to set them 
            DatabaseReference goalsRef = FirebaseDatabase.getInstance().getReference().child(currentUser.getUid()).child("goals");
            goalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // If there is no total goal set, switch to the settings:
                    if (!dataSnapshot.hasChild("total")) {
                        transitionFragment(new SettingsFragment(), SETTINGS_MENU_INDEX, "Settings");
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "While trying to start settings: "+databaseError.getMessage());
                }
            });
        }
    }

    // Show the sign in screen using Firebase UI
    public void showSignIn() {
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If the home button is clicked, open/close the menu
        if (item.getItemId() == android.R.id.home) {
            if(mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                mDrawerLayout.closeDrawer(Gravity.LEFT);
            } else {
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }

        }
        return super.onOptionsItemSelected(item);
    }

    // This is for when we start an activity and then want a result from it back:
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If this is for the sign in activity:
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == ResultCodes.OK) {
                Log.d(TAG, "Login was successful");
                // Transition to the home/settings fragment based on what the user needs to do from here:
                navigateBasedOnUser();
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

    // Handle menu item clicks
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case HOME_MENU_INDEX: // Home button clicked
                    // Transition to the home fragment
                    transitionFragment(new HomeFragment(), position, "Permit Log");
                    break;

                case LOG_MENU_INDEX: // Log button clicked
                    // Transition to the log fragment
                    transitionFragment(new LogFragment(), position, "Driving Log");
                    break;

                case DRIVERS_MENU_INDEX: // Drivers button clicked
                    // Transition to the drivers fragment
                    transitionFragment(new DriversFragment(), position, "Drivers");
                    break;

                case SETTINGS_MENU_INDEX: // Settings button clicked
                    // Transition to the settings fragment
                    transitionFragment(new SettingsFragment(), position, "Settings");
                    break;

                case SIGN_OUT_MENU_INDEX: // Sign out button clicked
                    AuthUI.getInstance()
                            .signOut(MainActivity.this)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    // Set currentUser:
                                    currentUser = null;
                                    // Show the sign in now that they are signed out:
                                    showSignIn();
                                }
                            });
                    break;

                default:
                    mDrawerList.setItemChecked(position, true); // Highlight the clicked item
            }

            // Close the drawer
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }
}
