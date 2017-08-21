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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save current values (persist for rotation)
        outState.putString("total", totalEdit.getText().toString());
        outState.putString("day", dayEdit.getText().toString());
        outState.putString("night", nightEdit.getText().toString());
        outState.putString("weather", weatherEdit.getText().toString());
        outState.putString("adverse", adverseEdit.getText().toString());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LayoutInflater lf = getActivity().getLayoutInflater();
        //pass the correct layout name for the fragment
        rootView =  lf.inflate(R.layout.fragment_settings, container, false);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        goalsRef = FirebaseDatabase.getInstance().getReference().child(userId).child("goals");

        final JSONObject states; //Final so we can access it inside the try loop
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

        final Spinner spinner = (Spinner) rootView.findViewById(R.id.state_spinner);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, statesList); //Pass the state names to an array adapter
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); //May want it to be a dialog box instead
        spinner.setAdapter(adapter); //Set the adapter
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                stateName = (String) parent.getItemAtPosition(pos); //Get Selection
                isFillerState = (pos == 0); //If this is the default position, set isFillerState
                try {
                    JSONObject state = states.getJSONObject(stateName); //Get State Object
                    Iterator<String> stateIter = state.keys(); //So we can iterate through the keys
                    //Get the Boolean value of the "needsForm" key for this state:
                    needsForm = state.getBoolean(stateIter.next());
                    while (stateIter.hasNext()) { //For each key ("total", "day", "night", "weather", "adverse")
                        String curGoal = stateIter.next(); //Get the current goal
                        int curValue = state.getInt(curGoal); //And the value for that goal

                        int visibility; //This stores whether we want the views to be invisible or visible
                        //If this is the "--Select a State--" option
                        //or the goal for this state is 0 and it is not the Custom state,
                        //hide the views related to this goal
                        if((pos == 0) || ((curValue == 0) && !stateName.equals("Custom")))
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
                        if(stateName.equals("Custom")) {
                            editView.setText(Long.toString(oldGoals.getTime(curGoal)));
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
        });
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
                    if (dataSnapshot.hasChild("total")) {
                        oldGoals.total = (long)dataSnapshot.child("total").getValue();
                    }
                    if (dataSnapshot.hasChild("day")) {
                        oldGoals.day = (long)dataSnapshot.child("day").getValue();
                    }
                    if (dataSnapshot.hasChild("night")) {
                        oldGoals.night = (long)dataSnapshot.child("night").getValue();
                    }
                    if (dataSnapshot.hasChild("weather")) {
                        oldGoals.weather = (long)dataSnapshot.child("weather").getValue();
                    }
                    if (dataSnapshot.hasChild("adverse")) {
                        oldGoals.adverse = (long)dataSnapshot.child("weather").getValue();
                    }
                    //If the user has a state, set the spinner to the position where the state is:
                    if (dataSnapshot.hasChild("stateName")) {
                        spinner.setSelection(adapter.getPosition(dataSnapshot.child("stateName").getValue().toString()));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, databaseError.getMessage());
                }
            });
        } else {
            // Get previous (but unsaved) values
            oldGoals.total = Long.parseLong(savedInstanceState.getString("total"));
            oldGoals.day = Long.parseLong(savedInstanceState.getString("day"));
            oldGoals.night = Long.parseLong(savedInstanceState.getString("night"));
            oldGoals.weather = Long.parseLong(savedInstanceState.getString("weather"));
            oldGoals.adverse = Long.parseLong(savedInstanceState.getString("adverse"));
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
        // Give an error message if the user is literally on "--Select a State--":
        if (isFillerState) {
            Toast.makeText(getContext(), "Please select a state.", Toast.LENGTH_LONG).show();
            return;
        }

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

        // Save the values
        goalsRef.child("stateName").setValue(stateName);
        goalsRef.child("needsForm").setValue(needsForm);
        goalsRef.child("total").setValue(Integer.parseInt(totalGoal));
        goalsRef.child("day").setValue(Integer.parseInt(dayGoal));
        goalsRef.child("night").setValue(Integer.parseInt(nightGoal));
        goalsRef.child("weather").setValue(Integer.parseInt(weatherGoal));
        goalsRef.child("adverse").setValue(Integer.parseInt(adverseGoal));

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
