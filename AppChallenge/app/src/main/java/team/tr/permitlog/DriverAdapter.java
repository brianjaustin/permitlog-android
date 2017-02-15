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

public class DriverAdapter {
    //TAG for logging
    private static String TAG = "DriverAdapter";
    //Firebase reference
    public DatabaseReference driversRef;
    //Store the drivers and their IDs:
    public ArrayList<String> driverNames = new ArrayList<>();
    public ArrayList<DataSnapshot> driverSnapshots = new ArrayList<>();
    public ArrayList<String> driverIds = new ArrayList<>();
    //Array adapter for holding items in spinner
    public ArrayAdapter<String> driversAdapter;

    //Firebase listener
    public ChildEventListener driversListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //Figure out what the name is:
            String name = createDriverName(dataSnapshot);
            //If driverSnapshots is empty, then there were no drivers previously.
            //Thus clear the "No drivers" from driverNames:
            if (driverSnapshots.isEmpty()) driverNames.clear();
            //Add it to driverNames and driverSnapshots:
            driverNames.add(name);
            driverSnapshots.add(dataSnapshot);
            //Update adapter:
            driversAdapter.notifyDataSetChanged();
        }
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            //Get the index of the item changed
            int driverIndex = driverIds.indexOf(dataSnapshot.getKey());
            //Figure out what the name is:
            String name = createDriverName(dataSnapshot);
            //Set it in driverNames and driverSnapshots:
            driverNames.set(driverIndex, name);
            driverSnapshots.set(driverIndex, dataSnapshot);
            //Update adapter:
            driversAdapter.notifyDataSetChanged();
        }
        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            // Get the index of the item removed
            int driverIndex = driverIds.indexOf(dataSnapshot.getKey());
            // Remove the item:
            driverNames.remove(driverIndex);
            driverSnapshots.remove(driverIndex);
            // Add "No drivers" if driverNames is empty:
            if (driverNames.isEmpty()) driverNames.add("No drivers");
            // Update adapter:
            driversAdapter.notifyDataSetChanged();
        }
        // The following must be implemented in order to complete the abstract class:
        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Fetching drivers failed
            Log.w(TAG, "Fetching drivers failed:", databaseError.toException());
        }
    };
    //Generates name for driver:
    public static String createDriverName(DataSnapshot dataSnapshot) {
        return dataSnapshot.child("name").child("first").getValue().toString() + " "
                + dataSnapshot.child("name").child("last").getValue().toString();
    }
    //This tells us if a driver has a complete name:
    public static DataSnapshotPredicate hasCompleteName = new DataSnapshotPredicate() { @Override public boolean accept(DataSnapshot dataSnapshot) {
        return dataSnapshot.child("name").hasChild("first") && dataSnapshot.child("name").hasChild("last");
    } };
    //Keeps track of if the spinner is listening:
    private boolean isListening = false;

    public DriverAdapter(Context context, String userId, int layout) {
        // Initialize driversRef
        driversRef = FirebaseDatabase.getInstance().getReference().child(userId).child("drivers");
        // Modify driversListener
        driversListener = FirebaseHelper.transformListener(driversListener, hasCompleteName, driverIds);
        // Create the adapter for driverNames that will be used for the spinner:
        driversAdapter = new ArrayAdapter<String>(context, layout, driverNames);
        // Add the data from driversRef to driverNames and driverIds:
        startListening();
        // This sets the layout that will be used when all of the choices are shown:
        driversAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    public void stopListening() {
        /* This function should be called on the onDestroy() of an activity and onDestroyView() of a fragment
           to avoid the listener from picking up on incomplete changes to the database. */
        if (isListening) driversRef.removeEventListener(driversListener);
        isListening = false;
    }

    public void startListening() {
        /* If stopListening() is put in the onPause() method, this should be put in the onResume() method
           so that the listener can start up again when the activity/fragment is started up again. */
        if (!isListening) {
            //Remember to reset the adapter so that things aren't added twice:
            driverNames.clear();
            driverNames.add("No drivers");
            driverIds.clear();
            driversAdapter.notifyDataSetChanged();
            driversRef.addChildEventListener(driversListener);
        }
        isListening = true;
    }
}
