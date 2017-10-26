package team.tr.permitlog;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DriversFragment extends ListFragment {
    // For logging
    private static final String TAG = "DriversFragment";

    // Firebase uid
    private String userId;
    // Store data for the list:
    private DriverAdapter listData;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_drivers, container, false);

        // Get the uid
        FirebaseUser curUser = FirebaseAuth.getInstance().getCurrentUser();
        if (curUser != null) userId = curUser.getUid();
            //If the user is not signed in, then don't do anything:
        else return rootView;

        // Create adapter and add it to the ListView
        listData = new DriverAdapter(getActivity(), userId, android.R.layout.simple_list_item_1);
        setListAdapter(listData.driversAdapter);

        return rootView;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        // Don't do anything if there are no drivers and the user clicks on "No drivers":
        if (listData.driverIds.isEmpty()) return;
        // Check if the user is signed in:
        boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
        // Don't do anything if the user isn't signed in:
        if (!isSignedIn) return;
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
