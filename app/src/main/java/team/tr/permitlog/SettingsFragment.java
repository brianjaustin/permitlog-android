package team.tr.permitlog;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SettingsFragment extends Fragment {
    //For logging:
    public static String TAG = "SettingsFragment";
    //The root view for this fragment, used to find elements by id:
    private View rootView;
    //Firebase Reference:
    private DatabaseReference goalsRef;
    //JSON object holding information about all the states:
    private JSONObject states;
    //The old state the user had before coming to this page:
    private String oldStateName;
    //The current state name that the user has selected from the dropdown box:
    private String stateName;
    //Is this state just the first option that tells the user to select a state:
    private boolean isFillerState;
    //Does this state require a form?
    private boolean needsForm;
    //These are the goals of the users that they had before entering this page:
    private DrivingTimes oldGoals = new DrivingTimes();
    //"The TextEdits": ABC's hit new sitcom, Tuesdays at 8/9c
    private EditText totalEdit;
    private EditText dayEdit;
    private EditText nightEdit;
    private EditText weatherEdit;
    private EditText adverseEdit;
    //Spinner holding all the different states:
    private Spinner spinner;
    //Adapter holding data for spinner:
    private ArrayAdapter<String> adapter;

    private DrivingTimes getGoals() {
        /* Gets goals from TextEdits */

        // Get the values
        String totalGoal = totalEdit.getText().toString();
        String dayGoal = dayEdit.getText().toString();
        String nightGoal = nightEdit.getText().toString();
        String weatherGoal = weatherEdit.getText().toString();
        String adverseGoal = adverseEdit.getText().toString();

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
        if (weatherGoal.trim().equals("")) {
            weatherGoal = "0";
        }
        if (adverseGoal.trim().equals("")) {
            adverseGoal = "0";
        }

        DrivingTimes goalsObj = new DrivingTimes(); //This will store all of the goals
        //Parse all of the strings into a long:
        goalsObj.total = Long.parseLong(totalGoal);
        goalsObj.day = Long.parseLong(dayGoal);
        goalsObj.night = Long.parseLong(nightGoal);
        goalsObj.weather = Long.parseLong(weatherGoal);
        goalsObj.adverse = Long.parseLong(adverseGoal);
        //Finally, return the DrivingTimes object:
        return goalsObj;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Get current goals from text edits:
        DrivingTimes curGoals = getGoals();
        // Save current values for each goal type (persist for rotation)
        for (String goalType : DrivingTimes.TIME_TYPES) {
            outState.putLong(goalType, curGoals.getTime(goalType));
        }
        // Also, save oldStateName as whatever state the user is currently on:
        outState.putString("oldStateName", adapter.getItem(spinner.getSelectedItemPosition()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LayoutInflater lf = getActivity().getLayoutInflater();
        //pass the correct layout name for the fragment
        rootView =  lf.inflate(R.layout.fragment_settings, container, false);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        goalsRef = FirebaseDatabase.getInstance().getReference().child(userId).child("goals");

        try {
            //Open the data file from the Assets folder
            InputStream is = getContext().getAssets().open("states.json");
            int size = is.available(); //How long is it
            byte[] buffer = new byte[size]; //Create a buffer to put the bytes into
            is.read(buffer); //Read the file into the buffer
            is.close();
            //Create a UTF-8 String from the buffer, and parse the JSON inside
            states = new JSONObject(new String(buffer, "UTF-8"));
        } catch (IOException ex) { //If we can't access the file for some reason
            ex.printStackTrace(); //Crash
            throw new RuntimeException("Can't read states.json");
        } catch(JSONException ex){ //If the JSON is invalid
            ex.printStackTrace(); //Crash
            throw new RuntimeException("Can't parse states.json");
        } //TODO: Better way of handling these unlikely errors?

        //We get the state names in an iterator, but we need an array, so this converts it
        Iterator<String> statesIter = states.keys();
        List<String> statesList = new ArrayList<>();
        while(statesIter.hasNext())
            statesList.add(statesIter.next());

        spinner = (Spinner) rootView.findViewById(R.id.state_spinner);
        adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, statesList); //Pass the state names to an array adapter
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); //May want it to be a dialog box instead
        spinner.setAdapter(adapter); //Set the adapter
        //Call onStateSelect when the user picks a new state:
        spinner.setOnItemSelectedListener(onStateSelect);
        // Get the EditText views
        totalEdit = (EditText) rootView.findViewById(R.id.totalEdit);
        dayEdit = (EditText) rootView.findViewById(R.id.dayEdit);
        nightEdit = (EditText) rootView.findViewById(R.id.nightEdit);
        weatherEdit = (EditText) rootView.findViewById(R.id.weatherEdit);
        adverseEdit = (EditText) rootView.findViewById(R.id.adverseEdit);

        // Set the values
        if (savedInstanceState == null) {
            goalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //Record the user's goals prior to coming here:
                    for (String goalType : DrivingTimes.TIME_TYPES) {
                        if (dataSnapshot.hasChild(goalType)) {
                            oldGoals.setTime(goalType, (long)dataSnapshot.child(goalType).getValue());
                        }
                    }
                    //If the user has a state, set the spinner to the position where the state is:
                    if (dataSnapshot.hasChild("stateName")) {
                        oldStateName = dataSnapshot.child("stateName").getValue().toString();
                        spinner.setSelection(adapter.getPosition(oldStateName));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, databaseError.getMessage());
                }
            });
        } else {
            // Get previous (but unsaved) values
            for (String goalType : DrivingTimes.TIME_TYPES) {
                oldGoals.setTime(goalType, savedInstanceState.getLong(goalType));
            }
            oldStateName = savedInstanceState.getString("oldStateName");
        }

        // Save the goals when the button is clicked
        Button saveButton = (Button) rootView.findViewById(R.id.settings_save);
        saveButton.setOnClickListener(onSaveClick);

        return rootView;
    }

    private AdapterView.OnItemSelectedListener onStateSelect = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            /* This is called when the user picks a new state from the spinner */

            //Get the state the user selected:
            stateName = (String) parent.getItemAtPosition(pos);
            //If this is the default position, set isFillerState:
            isFillerState = (pos == 0);
            try {
                //Get the object containing info about this state:
                JSONObject state = states.getJSONObject(stateName);
                //Get the Boolean value of the "needsForm" key for this state:
                needsForm = state.getBoolean("needsForm");
                //For each type of goal ("total", "day", "night", "weather", "adverse")
                for (String curGoal : DrivingTimes.TIME_TYPES) {
                    //Get the required number of hours for this goal in this state
                    int curValue = state.getInt(curGoal);

                    //This stores whether we want the views to be invisible or visible
                    int visibility;
                    //If this is the default state
                    //or the goal for this state is 0 and it is not the Custom state,
                    //hide the views related to this goal
                    if(isFillerState || ((curValue == 0) && !stateName.equals("Custom")))
                        visibility = View.GONE;
                    //Otherwise, make the views for this goal visible
                    else visibility = View.VISIBLE;

                    Resources res = getResources();
                    //Hide or show the inputs and descriptions based on visibility:
                    int descViewId = res.getIdentifier(curGoal + "Desc", "id", getContext().getPackageName());
                    rootView.findViewById(descViewId).setVisibility(visibility);
                    int inputViewId = res.getIdentifier(curGoal + "Input", "id", getContext().getPackageName());
                    rootView.findViewById(inputViewId).setVisibility(visibility);
                    //Get the textbox for this goal:
                    int editViewId = res.getIdentifier(curGoal + "Edit", "id", getContext().getPackageName());
                    EditText editView = (EditText) rootView.findViewById(editViewId);
                    //If this is the custom state, make the textbox whatever the old goal was:
                    if(stateName.equals("Custom") || stateName.equals(oldStateName)) {
                        editView.setText(String.valueOf(oldGoals.getTime(curGoal)));
                    }
                    //Otherwise, make it equal to whatever the goal is for this state:
                    else editView.setText(String.valueOf(curValue));
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
                throw new RuntimeException("Inner JSON parsing error");
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    };

    //This weird one-liner is done so that onSaveClick looks like a regular function:
    private View.OnClickListener onSaveClick = new View.OnClickListener() { @Override public void onClick(View view) {
        // Check if the user is signed in:
        boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
        // Don't do anything if the user isn't signed in:
        if (!isSignedIn) return;
        // Give an error message if the user is literally on "--Select a State--":
        if (isFillerState) {
            Toast.makeText(getContext(), "Please select a state.", Toast.LENGTH_LONG).show();
            return;
        }

        //Get the goals from the text edit:
        DrivingTimes userGoals = getGoals();

        // Save the values
        goalsRef.child("stateName").setValue(stateName);
        goalsRef.child("needsForm").setValue(needsForm);
        //Loop through all goal types and save the data accordingly:
        for (String goalType : DrivingTimes.TIME_TYPES) {
            goalsRef.child(goalType).setValue(userGoals.getTime(goalType));
        }

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
