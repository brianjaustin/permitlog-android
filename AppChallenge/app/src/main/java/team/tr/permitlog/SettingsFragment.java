package team.tr.permitlog;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class SettingsFragment extends Fragment {
    //For logging:
    public static String TAG = "SettingsFragment";
    //The root view for this fragment, used to find elements by id:
    private View rootView;
    //Firebase Reference:
    private DatabaseReference goalsRef;
    //The TextEdits:
    private EditText totalEdit;
    private EditText dayEdit;
    private EditText nightEdit;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save current values (persist for rotation)
        outState.putString("total", totalEdit.getText().toString());
        outState.putString("day", dayEdit.getText().toString());
        outState.putString("night", nightEdit.getText().toString());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LayoutInflater lf = getActivity().getLayoutInflater();
        //pass the correct layout name for the fragment
        rootView =  lf.inflate(R.layout.fragment_settings, container, false);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        goalsRef = FirebaseDatabase.getInstance().getReference().child(userId).child("goals");

        // Get the EditText views
        totalEdit = (EditText) rootView.findViewById(R.id.goal_total);
        dayEdit = (EditText) rootView.findViewById(R.id.goal_day);
        nightEdit = (EditText) rootView.findViewById(R.id.goal_night);

        // Set the values
        if (savedInstanceState == null) {
            goalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild("total")) {
                        totalEdit.setText(dataSnapshot.child("total").getValue().toString());
                    }
                    if (dataSnapshot.hasChild("day")) {
                        dayEdit.setText(dataSnapshot.child("day").getValue().toString());
                    }
                    if (dataSnapshot.hasChild("night")) {
                        nightEdit.setText(dataSnapshot.child("night").getValue().toString());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, databaseError.getMessage());
                }
            });
        } else {
            // Get previous (but unsaved) values
            totalEdit.setText(savedInstanceState.getString("total"));
            dayEdit.setText(savedInstanceState.getString("day"));
            nightEdit.setText(savedInstanceState.getString("night"));
        }

        // Save the goals when the button is clicked
        Button saveButton = (Button) rootView.findViewById(R.id.settings_save);
        saveButton.setOnClickListener(onSaveClick);

        return rootView;
    }

    //This weird one-liner is done so that onSaveClick looks like a regular function:
    private View.OnClickListener onSaveClick = new View.OnClickListener() { @Override public void onClick(View view) {
        // Check if the user is signed in:
        boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
        // Don't do anything if the user isn't signed in:
        if (!isSignedIn) return;
        // Get the values
        String totalGoal = totalEdit.getText().toString();
        String dayGoal = dayEdit.getText().toString();
        String nightGoal = nightEdit.getText().toString();

        // Check to see if any is empty; if so, set it to zero
        if (totalGoal.trim().equals("")) {
            totalGoal = "0";
        }
        if (dayGoal.trim().equals("")) {
            dayGoal = "0";
        }
        if (nightGoal.trim().equals("")) {
            nightGoal = "0";
        }

        // Save the values
        goalsRef.child("total").setValue(Integer.parseInt(totalGoal));
        goalsRef.child("day").setValue(Integer.parseInt(dayGoal));
        goalsRef.child("night").setValue(Integer.parseInt(nightGoal));

        // Hide the keyboard
        hideKeyboard(getContext(), rootView);

        // Notify the user
        Toast.makeText(getContext(), R.string.settings_success, Toast.LENGTH_SHORT).show();
        // Go to HomeFragment:
        MainActivity mainActivity = (MainActivity)getActivity();
        mainActivity.transitionFragment(mainActivity.HOME_MENU_INDEX);
    } };

    private static void hideKeyboard(Context context, View view) {
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (view == null) return;

        // Hide the keyboard
        inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onPause() {
        // Hide the keyboard when the fragment is paused:
        hideKeyboard(getContext(), rootView);
        super.onPause();
    }
}
