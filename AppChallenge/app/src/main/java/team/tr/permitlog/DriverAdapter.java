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
    public static String TAG = "DriverAdapter";
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
            try {
                //Figure out what the name is:
                String name = dataSnapshot.child("name").child("first").getValue().toString() + " "
                        + dataSnapshot.child("name").child("last").getValue().toString();
                //Add it to driverNames:
                driverNames.add(name);
                //Add the key of this snapshot to driverIds:
                driverIds.add(dataSnapshot.getKey());
                //Update adapter:
                driversAdapter.notifyDataSetChanged();
            }
            //Log NullPointerExceptions from invalid drivers:
            catch (NullPointerException e) {
                Log.e(TAG, "The following is not a valid driver: "+dataSnapshot.getKey());
            }
        }
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            try {
                // Get the index of the item changed
                int driverIndex = driverIds.indexOf(dataSnapshot.getKey());
                //Figure out what the name is:
                String name = dataSnapshot.child("name").child("first").getValue().toString() + " "
                        + dataSnapshot.child("name").child("last").getValue().toString();
                //Set it in driverNames:
                driverNames.set(driverIndex, name);
                //Update adapter:
                driversAdapter.notifyDataSetChanged();
            }
            //Log NullPointerExceptions from invalid drivers:
            catch (NullPointerException e) {
                Log.e(TAG, "The following is not a valid driver: "+dataSnapshot.getKey());
            }
        }
        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            // Get the index of the item removed
            int driverIndex = driverIds.indexOf(dataSnapshot.getKey());
            // Remove this value from _both_ ArrayLists
            driverIds.remove(driverIndex);
            driverNames.remove(driverIndex);
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
    //Keeps track of if the spinner is listening:
    private boolean isListening = false;

    public DriverAdapter(Context context, String userId, int layout) {
        //Initialize driversRef
        driversRef = FirebaseHelper.getDatabase().getReference().child(userId).child("drivers");
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
            driverIds.clear();
            driversAdapter.notifyDataSetChanged();
            driversRef.addChildEventListener(driversListener);
        }
        isListening = true;
    }
}
