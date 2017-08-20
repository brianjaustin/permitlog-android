package team.tr.permitlog;

import android.database.DataSetObserver;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CustomDriveDialog extends AppCompatActivity {
    //TAG for logging:
    public static String TAG = "CustomDriveDialog";

    // Firebase listener
    private DatabaseReference timesRef;
    // The spinner for selecting drivers:
    private Spinner driversSpinner;
    // Object that holds all data relevant to the driver spinner:
    private DriverAdapter spinnerData;
    // If we are editing a log, the database key of the log; otherwise, null
    private String logId;
    // The database key of the driver in the log from either getIntent() or getArguments():
    private String driverId;

    //This is true if and only if the user is currently choosing the ending time:
    private boolean isUserChoosingEndingTime = false;
    //This stores the time that the user said they started driving at:
    private Calendar startingTime = Calendar.getInstance();
    //This stores the time that the user said they stopped driving at:
    private Calendar endingTime = Calendar.getInstance();

    //The LinearLayout containers of the notices:
    private LinearLayout timeNoticeContainer, ddNoticeContainer;
    //The TextViews that are the notices:
    private TextView timeNotice, ddNotice;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current data
        outState.putLong("startTime", startingTime.getTimeInMillis());
        outState.putLong("endTime", endingTime.getTimeInMillis());
        outState.putString("driverId", spinnerData.driverIds.get(driversSpinner.getSelectedItemPosition()));
        outState.putBoolean("night", ((CheckBox)findViewById(R.id.night_checkbox)).isChecked());
        outState.putBoolean("weather", ((CheckBox) findViewById(R.id.weather_checkbox)).isChecked());
        outState.putBoolean("adverse", ((CheckBox) findViewById(R.id.adverse_checkbox)).isChecked());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_dialog);

        // Setup the Firebase reference
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        timesRef = FirebaseDatabase.getInstance().getReference().child(userId).child("times");

        // Get the spinner:
        driversSpinner = (Spinner)findViewById(R.id.drivers_spinner);
        // Add the items to the spinner:
        spinnerData = new DriverAdapter(this, userId, android.R.layout.simple_spinner_item);
        driversSpinner.setAdapter(spinnerData.driversAdapter);

        // Set the toolbar as the action bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar ab = getSupportActionBar(); //Create action bar object
        ab.setDisplayHomeAsUpEnabled(true); //Enable back button

        //Set the containers and notices:
        timeNoticeContainer = (LinearLayout)findViewById(R.id.time_notice_container);
        timeNotice = (TextView)findViewById(R.id.time_notice);
        ddNoticeContainer = (LinearLayout)findViewById(R.id.dd_notice_container);
        ddNotice = (TextView)findViewById(R.id.dd_notice);

        // Load the current values if possible:
        if (savedInstanceState != null) {
            // Set date and times:
            startingTime.setTimeInMillis(savedInstanceState.getLong("startTime"));
            endingTime.setTimeInMillis(savedInstanceState.getLong("endTime"));
            updateDateAndTime();
            // Select the driver in the spinner:
            selectDriver(savedInstanceState.getString("driverId"));
            // Update the checkBoxes using .post() so it is done on the UI thread:
            final boolean nightChecked = savedInstanceState.getBoolean("night");
            final boolean weatherChecked = savedInstanceState.getBoolean("weather");
            final boolean adverseChecked = savedInstanceState.getBoolean("adverse");
            final CheckBox nightBox = (CheckBox)findViewById(R.id.night_checkbox);
            final CheckBox weatherBox = (CheckBox)findViewById(R.id.weather_checkbox);
            final CheckBox adverseBox = (CheckBox)findViewById(R.id.adverse_checkbox);
            nightBox.post(new Runnable() {
                @Override
                public void run() {
                    nightBox.setChecked(nightChecked);
                    weatherBox.setChecked(weatherChecked);
                    adverseBox.setChecked(adverseChecked);
                }
            });
            // Set the title and show the delete button if we are editing a log:
            logId = getIntent().getStringExtra("logId");
            if (logId != null) {
                findViewById(R.id.custom_drive_delete).setVisibility(View.VISIBLE);
                ab.setTitle("Edit Drive Log");
            }
        }
        //Otherwise, look at the intent to see what we should set:
        else {
            //Get logId from the intent:
            logId = getIntent().getStringExtra("logId");
            //If logId is null, then we will create a new log, so set the date and times to the current time:
            if (logId == null) {
                updateDateAndTime();
                //Since this is a new log, there is no possibility of a deleted driver, so remove the notice:
                ddNoticeContainer.removeView(ddNotice);
            }
            //Otherwise, we are editing an old log:
            else {
                //Set the times according to the log:
                timesRef.child(logId).addListenerForSingleValueEvent(setLogData);
                //Show the Delete button:
                findViewById(R.id.custom_drive_delete).setVisibility(View.VISIBLE);
                //Change the title at the top:
                ab.setTitle("Edit Drive Log");
            }
        }
    }

    private ValueEventListener setLogData = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            //Set startingTime and endingTime using the data from the database:
            startingTime.setTimeInMillis((long)dataSnapshot.child("start").getValue());
            endingTime.setTimeInMillis((long)dataSnapshot.child("end").getValue());
            //Set the data and times according to the above:
            updateDateAndTime();
            //Set the night checkbox to whatever the log says:
            ((CheckBox)findViewById(R.id.night_checkbox)).setChecked(
                    (boolean)dataSnapshot.child("night").getValue()
            );
            ((CheckBox)findViewById(R.id.weather_checkbox)).setChecked(
                    (boolean)dataSnapshot.child("weather").getValue()
            );
            ((CheckBox)findViewById(R.id.adverse_checkbox)).setChecked(
                    (boolean)dataSnapshot.child("adverse").getValue()
            );
            //Adjust the spinner according to the driver:
            selectDriver(dataSnapshot.child("driver_id").getValue().toString());
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.e(TAG, "While trying to get data from /times/"+logId+"/: "+databaseError.getMessage());
        }
    };

    private void selectDriver(String driverId) {
        /* Selects the driver in driversSpinner corresponding to driverId. */
        //Set the instance property:
        this.driverId = driverId;
        //If the driver from the log has been found, then select it and remove the deleted driver notice:
        if (spinnerData.driverIds.contains(driverId)) {
            driversSpinner.setSelection(spinnerData.driverIds.indexOf(driverId));
            ddNoticeContainer.removeView(ddNotice);
        }
        //Otherwise, listen to data changes and select the driver from the log when it comes up:
        else spinnerData.driversAdapter.registerDataSetObserver(observeDrivers);
    }

    private DataSetObserver observeDrivers = new DataSetObserver() { @Override public void onChanged() {
        Log.d(TAG, "Have we found "+driverId+" yet? "+spinnerData.driverIds.contains(driverId));
        //If the driver from the log has been found, select it, remove the deleted driver notice, and stop listening to data changes:
        if (spinnerData.driverIds.contains(driverId)) {
            driversSpinner.setSelection(spinnerData.driverIds.indexOf(driverId));
            ddNoticeContainer.removeView(ddNotice);
            spinnerData.driversAdapter.unregisterDataSetObserver(observeDrivers);
        }
    } };

    public void updateDateAndTime() {
        //Set the date from startingTime:
        setDate(startingTime.get(Calendar.YEAR), startingTime.get(Calendar.MONTH), startingTime.get(Calendar.DAY_OF_MONTH));
        //Set the starting time:
        setTime(startingTime.get(Calendar.HOUR_OF_DAY), startingTime.get(Calendar.MINUTE));
        //Make sure setTime() sets the ending time:
        isUserChoosingEndingTime = true;
        //Set the ending time:
        setTime(endingTime.get(Calendar.HOUR_OF_DAY), endingTime.get(Calendar.MINUTE));
        //Set the Boolean back to the default value:
        isUserChoosingEndingTime = false;
    }

    public void pickDate(View view) {
        /* This function is called when the user presses the "pick the date" button. */
        //Create the arguments for the fragment:
        Bundle defaultDate = new Bundle();
        defaultDate.putInt("year", startingTime.get(Calendar.YEAR));
        defaultDate.putInt("month", startingTime.get(Calendar.MONTH));
        defaultDate.putInt("day", startingTime.get(Calendar.DAY_OF_MONTH));
        //This fragment will allow the user to pick the date, with the default being the date that was picked before:
        DialogFragment dateFragment = new DriveDateFragment();
        dateFragment.setArguments(defaultDate);
        //Start the date fragment:
        dateFragment.show(getSupportFragmentManager(), "DriveDateFragment");
    }

    public void setDate(int year, int month, int day) {
        /* This function is called when the user actually picks a date using the dialog setup in the pickDate method. */
        //Set startingTime and endingTime:
        startingTime.set(year, month, day);
        endingTime.set(year, month, day);
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
        //Create the arguments for the fragment:
        Bundle defaultTime = new Bundle();
        defaultTime.putInt("hour", driveTime.get(Calendar.HOUR_OF_DAY));
        defaultTime.putInt("minute", driveTime.get(Calendar.MINUTE));
        //This fragment will allow the user to pick the date:
        DialogFragment timeFragment = new DriveTimeFragment();
        timeFragment.setArguments(defaultTime);
        //Start the date fragment:
        timeFragment.show(getSupportFragmentManager(), "DriveTimeFragment");
    }

    public void pickStartTime(View view) {
        /* This function is called when the user presses the "pick the start time" button. */
        //Set isUserChoosingEndTime so we know the user is setting the starting time later on:
        isUserChoosingEndingTime = false;
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
        }
        //Otherwise, if they are choosing the start time:
        else {
            //We will set the starting time:
            driveTime = startingTime;
            //Get the view for the starting time:
            driveTimeView = (TextView)findViewById(R.id.drive_start_time);
        }
        //Set the drive time:
        driveTime.set(Calendar.HOUR_OF_DAY, hour);
        driveTime.set(Calendar.MINUTE, minute);
        //Display the time to the user:
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
        timeFormat.setTimeZone(driveTime.getTimeZone());
        driveTimeView.setText(timeFormat.format(driveTime.getTime()));
        //If endingTime is before startingTime, add the time notice if needed.
        if (endingTime.before(startingTime)) {
            if (timeNotice.getParent() == null) timeNoticeContainer.addView(timeNotice);
        }
        //Otherwise, remove the time notice if needed.
        else if (timeNotice.getParent() == timeNoticeContainer) timeNoticeContainer.removeView(timeNotice);
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

    public void onSaveClick(View view) {
        //Get the position of the spinner:
        Spinner driversSpinner = (Spinner)findViewById(R.id.drivers_spinner);
        int spinnerPosition = driversSpinner.getSelectedItemPosition();
        //If there are no driver ids, then show an error to the user and do not proceed:
        if (spinnerData.driverIds.isEmpty()) {
            Toast.makeText(this, getResources().getString(R.string.go_to_add_driver_menu), Toast.LENGTH_LONG).show();
            return;
        }
        //Otherwise, get the driver id from the selected position:
        String driverId = spinnerData.driverIds.get(spinnerPosition);
        //Get drive info:
        boolean isDriveAtNight = ((CheckBox)findViewById(R.id.night_checkbox)).isChecked();
        boolean isDriveBadWeather = ((CheckBox)findViewById(R.id.weather_checkbox)).isChecked();
        boolean isDriveAdverse = ((CheckBox)findViewById(R.id.adverse_checkbox)).isChecked();
        //If the ending time is before the start time, add the ending time by one day:
        if (endingTime.before(startingTime)) endingTime.add(Calendar.DATE, 1);
        //Get the logRef from logId, or from push() if logId is null:
        DatabaseReference logRef;
        if (logId == null) logRef = timesRef.push();
        else logRef = timesRef.child(logId);
        //Finally, set the log:
        logRef.child("start").setValue(startingTime.getTimeInMillis());
        logRef.child("end").setValue(endingTime.getTimeInMillis());
        logRef.child("night").setValue(isDriveAtNight);
        logRef.child("weather").setValue(isDriveBadWeather);
        logRef.child("adverse").setValue(isDriveAdverse);
        logRef.child("driver_id").setValue(driverId);
        //Notify user of new log/edits and close the dialog
        Toast.makeText(this, (logId == null) ? "Custom drive saved successfully." : "Edits to drive log saved successfully.", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onDeleteClick(View view) {
        //Delete the log:
        timesRef.child(logId).removeValue();
        //Notify user and close the dialog
        Toast.makeText(getApplicationContext(), "Drive log deleted successfully.", Toast.LENGTH_SHORT).show();
        finish();
    }

    //Close the dialog when someone presses the X in the top-left corner:
    public void onCancel(View view) { finish(); }

    @Override
    protected void onDestroy() {
        // Since this activity is being stopped, we don't need to listen to the drivers anymore:
        spinnerData.stopListening();
        super.onDestroy();
    }
}