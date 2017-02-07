package team.tr.permitlog;

import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormatSymbols;
import java.util.Calendar;

public class CustomDriveDialog extends AppCompatActivity {
    // Firebase reference
    private DatabaseReference timesRef;

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

        // Setup the firebase reference
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        timesRef = FirebaseDatabase.getInstance().getReference().child(userId).child("times");

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
        Log.d("CustomDriveDialog", "Started driving on: "+startingTime.getTime().toString());
        Log.d("CustomDriveDialog", "Stopped driving on: "+endingTime.getTime().toString());
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
        //Get the hour from driveTime:
        int hourDisplay = driveTime.get(Calendar.HOUR);
        //Set it to 12 if it's 0:
        if (hourDisplay == 0) hourDisplay = 12;
        //This string is AM if hour < 12 and PM otherwise:
        String AMorPM = hour < 12 ? "AM" : "PM";
        //Display the time to the user:
        driveTimeView.setText(hourDisplay+":"+driveTime.get(Calendar.MINUTE)+" "+AMorPM);
        //Remember to log startingTime and endingTime for debugging:
        Log.d("CustomDriveDialog", "Started driving on: "+startingTime.getTime().toString());
        Log.d("CustomDriveDialog", "Stopped driving on: "+endingTime.getTime().toString());
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

    public void onCancel(View view){
        finish();
    }

    public void onSaveClick(View view) {
        // TODO: actually call this function

        // Close the dialog
        finish();
    }
}

