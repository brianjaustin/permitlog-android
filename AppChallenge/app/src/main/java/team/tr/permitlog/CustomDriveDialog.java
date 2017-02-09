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
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CustomDriveDialog extends AppCompatActivity {
    //TAG for logging:
    public static String TAG = "CustomDriveDialog";

    // Firebase listener
    private DatabaseReference timesRef;
    // Object that holds all data relevant to the driver spinner:
    private DriverAdapter spinnerData;

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

        // Get the spinner:
        Spinner driversSpinner = (Spinner)findViewById(R.id.drivers_spinner);
        // Add the items to the spinner:
        spinnerData = new DriverAdapter(this, userId, android.R.layout.simple_spinner_item);
        driversSpinner.setAdapter(spinnerData.driversAdapter);

        // Set the toolbar as the action bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar ab = getSupportActionBar(); //Create action bar object
        ab.setDisplayHomeAsUpEnabled(true); //Enable back button

        //Sets default date
        setDate(startingTime.get(Calendar.YEAR), startingTime.get(Calendar.MONTH), startingTime.get(Calendar.DAY_OF_MONTH));
        //Set start time to the current time
        setTime(startingTime.get(Calendar.HOUR_OF_DAY), startingTime.get(Calendar.MINUTE));
        //Make sure setTime() sets the ending time:
        isUserChoosingEndingTime = true;
        //Set the ending time to the current time:
        setTime(startingTime.get(Calendar.HOUR_OF_DAY), startingTime.get(Calendar.MINUTE));
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
        //Get the position of the spinner:
        Spinner driversSpinner = (Spinner)findViewById(R.id.drivers_spinner);
        int spinnerPosition = driversSpinner.getSelectedItemPosition();
        //If nothing is selected, then the spinner must be empty, so show an error to the user and do not proceed:
        if (spinnerPosition == Spinner.INVALID_POSITION) {
            Toast.makeText(applicationCon, getResources().getString(R.string.go_to_add_driver_menu), Toast.LENGTH_LONG).show();
            return;
        }
        //Otherwise, get the driver id from the selected position:
        String driverId = spinnerData.driverIds.get(spinnerPosition);
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

    @Override
    protected void onDestroy() {
        // Since this activity is being stopped, we don't need to listen to the drivers anymore:
        spinnerData.stopListening();
        super.onDestroy();
    }
}

