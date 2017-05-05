package team.tr.permitlog;

import android.content.Context;
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
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import android.support.multidex.*;

import java.util.Arrays;
import java.util.LinkedList;

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
    public static final int HOME_MENU_INDEX = 0;
    public static final int SIGN_OUT_MENU_INDEX = 5;
    // Is the menu button disabled?
    private boolean menuDisabled = false;
    // Fragments, titles, and arguments for menu items
    private Class menuFragmentClasses[] = {HomeFragment.class, LogFragment.class, DriversFragment.class, SettingsFragment.class, AboutFragment.class};
    private String menuTitles[] = {"Permit Log", "Driving Log", "Drivers", "Goals", "About"};
    private Bundle menuArgs[] = {null, null, null, null, null};
    // Keeps track of previous menus:
    private LinkedList<Integer> fragmentsStack = new LinkedList<>();

    // Sign in request code
    private static final int RC_SIGN_IN = 123;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

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
        if (savedInstanceState == null) navigateBasedOnUser();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save menuArgs during an orientation change:
        outState.putParcelableArray("menuArgs", menuArgs);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Get menuArgs from when we previously saved it during an orientation change:
        menuArgs = (Bundle[])savedInstanceState.getParcelableArray("menuArgs");
        super.onRestoreInstanceState(savedInstanceState);
    }

    public void transitionFragment(int position, boolean pushFragmentOnStack) {
        /* This function switches the current fragment according to the menu position passed in. */
        // Assuming the user is signed in:
        if (currentUser != null) {
            // Sign out button clicked -> Sign out and return
            if (position == SIGN_OUT_MENU_INDEX) {
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
                return;
            }
            // We need to instantiate the fragment and get the title based off position:
            String title = menuTitles[position];
            // This error message will be shown if there is an error with fragment:
            Toast fragmentError = Toast.makeText(this, "There was an error when making the "+title+" part of the app. Sorry!", Toast.LENGTH_SHORT);
            Fragment fragment;
            try {
                fragment = (Fragment) menuFragmentClasses[position].newInstance();
            }
            // If there is an error, notify the user, log it, and return:
            catch (IllegalAccessException | InstantiationException e) {
                fragmentError.show();
                Log.e(TAG, "While making "+title+" fragment: "+e);
                return;
            }
            // Set fragment's arguments:
            fragment.setArguments(menuArgs[position]);
            // Transition fragments:
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
            // Change the title
            getSupportActionBar().setTitle(title);
            // Highlight the menu item:
            mDrawerList.setItemChecked(position, true);
            // Add this fragment to the stack if we should:
            if (pushFragmentOnStack) fragmentsStack.push(position);
        }
        //Otherwise, force the user to sign in:
        else showSignIn();
    }
    // By default, push fragments onto the stack:
    public void transitionFragment(int position) { transitionFragment(position, true); }

    public void saveArguments(int position, Bundle args) {
        /* Saves args for fragment where position represents the index of the fragment in the menu */
        menuArgs[position] = args;
    }

    private void navigateBasedOnUser() {
        // If no user is logged in, show the FirebaseUI login screen.
        if (currentUser == null) {
            showSignIn();
        } else {
            // Transition to the home fragment
            transitionFragment(HOME_MENU_INDEX);
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

    // When back button is pressed, go back to previous fragment if possible:
    @Override
    public void onBackPressed() {
        if (fragmentsStack.size() > 1) {
            fragmentsStack.pop();
            transitionFragment(fragmentsStack.peek(), false);
        }
    }

    // These two methods disable and enable the menu button:
    public void disableMenuButton() {
        menuDisabled = true;
    }

    public void enableMenuButton() {
        menuDisabled = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If the home button is clicked and it is enabled, open/close the menu
        if (item.getItemId() == android.R.id.home && !menuDisabled) {
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
                else if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Log.e(TAG, "Network connection error");
                    Toast.makeText(this, R.string.network_connection_error, Toast.LENGTH_SHORT).show();
                }
                else if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Log.e(TAG, "Unknown error");
                    Toast.makeText(this, R.string.unknown_auth_error, Toast.LENGTH_SHORT).show();
                    showSignIn();
                }
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
            // Transition based off of position:
            transitionFragment(position);
            // Close the drawer
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }
}
