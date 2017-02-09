package team.tr.permitlog;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;


public class DriversFragment extends ListFragment {
    // For logging
    private static final String TAG = "DriversFragment";

    // Firebase uid
    private String userId;
    // Store data for the list:
    private DriverAdapter listData;

    public DriversFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LayoutInflater lf = getActivity().getLayoutInflater();

        // Get the uid from the main activity
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Create adapter and add it to the ListView
        listData = new DriverAdapter(getActivity(), userId, android.R.layout.simple_list_item_1);
        setListAdapter(listData.driversAdapter);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_drivers, container, false);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        // Get the ID of the driver clicked
        String driverId = listData.driverIds.get(position);
        // Open the dialog to edit
        Intent intent = new Intent(view.getContext(), DriverDialog.class);
        intent.putExtra("driverId", driverId);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        // Detach the data listener
        listData.stopListening();
        super.onDestroyView();
    }
}
