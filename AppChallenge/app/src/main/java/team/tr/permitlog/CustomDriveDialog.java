package team.tr.permitlog;

import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CustomDriveDialog extends AppCompatActivity {
    //TAG for logging:
    public static String TAG = "CustomDriveDialog";

    // Firebase reference, array adapter for holding items in spinner, and Firebase listener
    private DatabaseReference timesRef, driversRef;
    private ArrayAdapter<String> driversAdapter;
    private ChildEventListener driversListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //Figure out what the name is:
            String name = dataSnapshot.child("name").child("first").getValue().toString() + " "
                    + dataSnapshot.child("name").child("last").getValue().toString();
            //Add it to driverNames:
            driverNames.add(name);
            //Add the key of this snapshot to driverIds:
            driverIds.add(dataSnapshot.getKey());
            driversAdapter.notifyDataSetChanged();
        }

        // The following must be implemented in order to complete the abstract class:
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {}
        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Fetching drivers failed
            Log.w(TAG, "Fetching drivers failed:", databaseError.toException());
        }
    };
    //Store the drivers and their IDs:
    private ArrayList<String> driverNames;
    private ArrayList<String> driverIds;

    //This is true if and only if the user has chosen a date:
    private boolean hasUserChosenDate = false;
    //This is true if and only if the user has chosen a starting time:
    private boolean hasUserChosenStartingTime = false;
    //This is true if and only if the user has chosen a ending time:
    private boolean hasUserChosenEndingTime = false;
    //This is true if and only if the user is currently choosing the ending time:
    private boolean isUserChoosingEndingTime = false;
    //This stores the time that the user said they started driving at:
    private Calendar startingTime = Calendar.getInstance();
    //This stores the time that the user said they stopped driving at:
    private Calendar endingTime = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_dialog);

        // Setup the firebase reference for times and drivers:
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        timesRef = FirebaseDatabase.getInstance().getReference().child(userId).child("times");
        driversRef = FirebaseDatabase.getInstance().getReference().child(userId).child("drivers");
        // Initialize driverNames and driverIds:
        driverNames = new ArrayList<String>();
        driverIds = new ArrayList<String>();
        // Create the adapter for driverNames that will be used for the spinner:
        driversAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, driverNames);

        // Add the data from driversRef to driverNames and driverIds:
        driversRef.addChildEventListener(driversListener);

        // Get the spinner of drivers:
        Spinner driversSpinner = (Spinner)findViewById(R.id.drivers_spinner);
        // This sets the layout that will be used when all of the choices are shown:
        driversAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Use driversAdapter for the spinner:
        driversSpinner.setAdapter(driversAdapter);

        // Set the toolbar as the action bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar ab = getSupportActionBar(); //Create action bar object
        ab.setDisplayHomeAsUpEnabled(true); //Enable back button
    }

    public void pickDate(View view) {
        /* This function is called when the user presses the "pick the date" button. */
        //This fragment will allow the user to pick the date:
        DialogFragment dateFragment = new DriveDateFragment();
        //Start the date fragment:
        dateFragment.show(getSupportFragmentManager(), "DriveDateFragment");
    }

    public void setDate(int year, int month, int day) {
        /* This function is called when the user actually picks a date using the dialog setup in the pickDate method. */
        //Set startingTime and endingTime:
        startingTime.set(year, month, day);
        endingTime.set(year, month, day);
        //Now that we have set the date, set hasUserChosenDate:
        hasUserChosenDate = true;
        //Get the text of the month:
        String monthText = new DateFormatSymbols().getMonths()[month];
        //Display the date to the user:
        TextView driveDateView = (TextView)findViewById(R.id.drive_date);
        driveDateView.setText(monthText+" "+day+", "+year);
        //Remember to log startingTime and endingTime for debugging:
        Log.d(TAG, "Started driving on: "+startingTime.getTime().toString());
        Log.d(TAG, "Stopped driving on: "+endingTime.getTime().toString());
    }

    public void pickTime(Calendar driveTime) {
        //This fragment will allow the user to pick the date:
        DialogFragment timeFragment = new DriveTimeFragment(driveTime.get(Calendar.HOUR_OF_DAY), driveTime.get(Calendar.MINUTE));
        //Start the date fragment:
        timeFragment.show(getSupportFragmentManager(), "DriveTimeFragment");
    }

    public void pickStartTime(View view) {
        /* This function is called when the user presses the "pick the start time" button. */
        pickTime(startingTime);
    }

    public void pickEndTime(View view) {
        /* This function is called when the user presses the "pick the end time" button. */
        //Set isUserChoosingEndTime so we know the user is setting the ending time later on:
        isUserChoosingEndingTime = true;
        pickTime(endingTime);
    }

    public void setTime(int hour, int minute) {
        /* This function is called when the user actually picks a time using the dialog setup in the pickTime method. */
        //This is the time we will set:
        Calendar driveTime;
        //This is the TextView we will use to show the user the time they chose:
        TextView driveTimeView;
        //If the user is choosing the end time:
        if (isUserChoosingEndingTime) {
            //We will set the ending time:
            driveTime = endingTime;
            //Get the view for the ending time:
            driveTimeView = (TextView)findViewById(R.id.drive_end_time);
            //They are no longer choosing the ending time, so unset the boolean:
            isUserChoosingEndingTime = false;
            //They have chosen the ending time:
            hasUserChosenEndingTime = true;
        }
        //Otherwise, if they are choosing the start time:
        else {
            //We will set the starting time:
            driveTime = startingTime;
            //Get the view for the starting time:
            driveTimeView = (TextView)findViewById(R.id.drive_start_time);
            //They have chosen the starting time:
            hasUserChosenStartingTime = true;
        }
        //Set the drive time:
        driveTime.set(Calendar.HOUR_OF_DAY, hour);
        driveTime.set(Calendar.MINUTE, minute);
        //Display the time to the user:
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
        timeFormat.setTimeZone(driveTime.getTimeZone());
        driveTimeView.setText(timeFormat.format(driveTime.getTime()));
        //Remember to log startingTime and endingTime for debugging:
        Log.d(TAG, "Started driving on: "+startingTime.getTime().toString());
        Log.d(TAG, "Stopped driving on: "+endingTime.getTime().toString());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Code to handle X button in top right
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onCancel(View view) { finish(); }

    public void onSaveClick(View view) {
        //For Toast:
        Context applicationCon = getApplicationContext();
        //Show errors if the user has not filled in all fields:
        if (!hasUserChosenDate) Toast.makeText(applicationCon, "Please fill in the date.", Toast.LENGTH_SHORT).show();
        else if (!hasUserChosenStartingTime) Toast.makeText(applicationCon, "Please fill when you started driving.", Toast.LENGTH_SHORT).show();
        else if (!hasUserChosenEndingTime) Toast.makeText(applicationCon, "Please fill when you stopped driving.", Toast.LENGTH_SHORT).show();
        else {
            //Get the position of the spinner:
            Spinner driversSpinner = (Spinner)findViewById(R.id.drivers_spinner);
            int spinnerPosition = driversSpinner.getSelectedItemPosition();
            //If nothing is selected, then the spinner must be empty, so show an error to the user and do not proceed:
            if (spinnerPosition == Spinner.INVALID_POSITION) {
                Toast.makeText(applicationCon, "Please add the driver that accompanied you by going to the \"Add Driver\" menu.", Toast.LENGTH_SHORT).show();
                return;
            }

            //Otherwise, get the driver id from the selected position:
            String driverId = driverIds.get(spinnerPosition);
            //Get if this is at night or not:
            boolean isDriveAtNight = ((CheckBox)findViewById(R.id.drive_at_night_checkbox)).isChecked();
            //If the ending time is before the start time, add the ending time by one day:
            if (endingTime.before(startingTime)) endingTime.add(Calendar.DATE, 1);

            //Finally, push a new log:
            DatabaseReference newLogRef = timesRef.push();
            newLogRef.child("start").setValue(startingTime.getTimeInMillis());
            newLogRef.child("end").setValue(endingTime.getTimeInMillis());
            newLogRef.child("night").setValue(isDriveAtNight);
            newLogRef.child("driver_id").setValue(driverId);

            //Notify user and close the dialog
            Toast.makeText(applicationCon, "Custom drive saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        // Since this activity is being stopped, we don't need to listen to the drivers anymore:
        driversRef.removeEventListener(driversListener);
        super.onDestroy();
    }
}

