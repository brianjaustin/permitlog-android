package team.tr.permitlog;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class DriversFragment extends ListFragment {
    // For logging
    private static final String TAG = "DriversFragment";

    // Firebase uid
    private String userId;

    // Firebase database reference
    private DatabaseReference driversRef;

    // ArrayLists for storing drivers
    private ArrayList<String> driverNames;
    private ArrayList<String> driverIds;
    // Adapter for ListView
    private ArrayAdapter adapter;

    // Firebase listener
    private ChildEventListener driversListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            // Add the new item to the ListView
            String name = dataSnapshot.child("name").child("first").getValue().toString() + " "
                    + dataSnapshot.child("name").child("last").getValue().toString();
            driverNames.add(name);
            driverIds.add(dataSnapshot.getKey());
            adapter.notifyDataSetChanged();
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
    };

    public DriversFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LayoutInflater lf = getActivity().getLayoutInflater();

        // Get the uid from the main activity
        userId = getArguments().getString("uid");

        // Get Firebase database reference
        driversRef = FirebaseDatabase.getInstance().getReference().child(userId).child("drivers");

        // Add ArrayAdapter to the ListView
        driverNames = new ArrayList<>();
        driverIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(lf.getContext(), android.R.layout.simple_list_item_1, driverNames);
        setListAdapter(adapter);

        // Add data to the list from Firebase
        driversRef.addChildEventListener(driversListener);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_drivers, container, false);
    }
}
