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
    //The TextEdits:
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
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, statesList); //Pass the state names to an array adapter
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); //May want it to be a dialog box instead
        spinner.setAdapter(adapter); //Set the adapter
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String stateName = (String) parent.getItemAtPosition(pos); //Get Selection
                try {
                    JSONObject state = states.getJSONObject(stateName); //Get State Object
                    Iterator<String> stateIter = state.keys(); //So we can iterate through the keys
                    goalsRef.child("stateName").setValue(stateName); //Record the stateName
                    goalsRef.child("needsForm").setValue(state.getBoolean(stateIter.next()));
                    while (stateIter.hasNext()) { //For each key
                        String curForm = stateIter.next(); //Get the current key
                        int curValue = state.getInt(curForm); //And the value for that key
                        goalsRef.child(curForm).setValue(curValue); //Save the current
                        int viewType;
                        if((curValue == 0 && !stateName.equals("Custom")))
                            viewType = View.GONE;
                        else
                            viewType = View.VISIBLE;
                        Resources res = getResources();
                        if(!stateName.equals(spinner.getItemAtPosition(0))){
                            int curView = res.getIdentifier(curForm + "Desc", "id", getContext().getPackageName());
                            rootView.findViewById(curView).setVisibility(viewType);
                            curView = res.getIdentifier(curForm + "Input", "id", getContext().getPackageName());
                            rootView.findViewById(curView).setVisibility(viewType);
                            curView = res.getIdentifier(curForm + "Edit", "id", getContext().getPackageName());
                            EditText curEdit = (EditText) rootView.findViewById(curView);
                            if(stateName.equals("Custom"))
                                curEdit.setText("");
                            else
                                curEdit.setText(String.valueOf(curValue));
                        }
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
                    if (dataSnapshot.hasChild("total")) {
                        totalEdit.setText(dataSnapshot.child("total").getValue().toString());
                    }
                    if (dataSnapshot.hasChild("day")) {
                        dayEdit.setText(dataSnapshot.child("day").getValue().toString());
                    }
                    if (dataSnapshot.hasChild("night")) {
                        nightEdit.setText(dataSnapshot.child("night").getValue().toString());
                    }
                    if (dataSnapshot.hasChild("weather")) {
                        weatherEdit.setText(dataSnapshot.child("weather").getValue().toString());
                    }
                    if (dataSnapshot.hasChild("adverse")) {
                        adverseEdit.setText(dataSnapshot.child("adverse").getValue().toString());
                    }
                    if (dataSnapshot.hasChild("stateName")) {
                        for(int i = 0; i<spinner.getCount();i++){
                            if(dataSnapshot.child("stateName").getValue().equals(spinner.getItemAtPosition(i))){
                                spinner.setSelection(i);
                                break;
                            }
                        }
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
            weatherEdit.setText(savedInstanceState.getString("weather"));
            adverseEdit.setText(savedInstanceState.getString("adverse"));
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
