package team.tr.permitlog;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class DriversFragment extends Fragment {
    // For logging
    private static final String TAG = "DriversFragment";

    // Firebase uid
    private String userId;

    // Firebase database reference
    private DatabaseReference driversRef;

    // ArrayLists for storing drivers
    private ArrayList<String> driverNames;
    private ArrayList<String> driverIds;

    public DriversFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get the uid from the main activity
        userId = getArguments().getString("uid");

        // Get Firebase database reference
        driversRef = FirebaseDatabase.getInstance().getReference().child(userId).child("drivers");

        // Add data to the list from Firebase
        driversRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                // Add the new item to the ListView
                String name = dataSnapshot.child("name").child("first").toString() + dataSnapshot.child("name").child("last").toString();
                driverNames.add(name);
                driverIds.add(dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Fetching drivers failed
                Log.w(TAG, "Fetching drivers failed:", databaseError.toException());
            }
        });

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_drivers, container, false);
    }

}
