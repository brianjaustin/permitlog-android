package team.tr.permitlog;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class DriverSpinner {
    //TAG for logging
    public static String TAG = "DriverSpinner";
    //Firebase reference
    public DatabaseReference driversRef;
    //Store the drivers and their IDs:
    public ArrayList<String> driverNames = new ArrayList<String>();
    public ArrayList<String> driverIds = new ArrayList<String>();
    //Array adapter for holding items in spinner
    public ArrayAdapter<String> driversAdapter;

    //Firebase listener
    public ChildEventListener driversListener = new ChildEventListener() {
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

    public DriverSpinner(Context context, String userId, Spinner driversSpinner) {
        //Initialize driversRef
        driversRef = FirebaseDatabase.getInstance().getReference().child(userId).child("drivers");
        // Create the adapter for driverNames that will be used for the spinner:
        driversAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, driverNames);
        // Add the data from driversRef to driverNames and driverIds:
        driversRef.addChildEventListener(driversListener);
        // This sets the layout that will be used when all of the choices are shown:
        driversAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Use driversAdapter for the spinner:
        driversSpinner.setAdapter(driversAdapter);
    }

    public void stopListening() {
        /* This function should be called on the onDestroy() of an activity and onDestroyView() of a fragment
           to avoid the listener from picking up on incomplete changes to the database. */
        driversRef.removeEventListener(driversListener);
    }
}
