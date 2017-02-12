package team.tr.permitlog;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
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

public class LogFragment extends ListFragment {
    //For logging:
    public static String TAG = "LogFragment";
    //Firebase reference:
    DatabaseReference timesRef;
    //This holds all of the keys of the logs in the database:
    private ArrayList<String> logIds = new ArrayList<>();
    //This holds all of the summaries of the logs that we will show in the ListView:
    private ArrayList<String> logSummaries = new ArrayList<>();
    //This is the ListView's adapter:
    private ArrayAdapter<String> listAdapter;
    //Firebase listener:
    private ChildEventListener timesListener = new ChildEventListener() {
        private String genLogSummary(DataSnapshot dataSnapshot) {
            //Find the time elapsed during the drive:
            long driveTimeInSec =
                    ((long)(dataSnapshot.child("end").getValue())-(long)(dataSnapshot.child("start").getValue()))/1000;
            //Format the time appropriately:
            String driveTimeString = ElapsedTime.formatSeconds(driveTimeInSec);
            //This is the summary of the log shown to the user:
            String logSummary = "Drove for "+driveTimeString;
            //Was the drive at night? Add "at night"/"during the day" appropriately.
            boolean isDriveAtNight = (boolean)(dataSnapshot.child("night").getValue());
            if (isDriveAtNight) logSummary += " at night";
            else logSummary += " during the day";
            //Finally return the summary:
            return logSummary;
        }
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //Set the data and update the adapter:
            logIds.add(dataSnapshot.getKey());
            logSummaries.add(genLogSummary(dataSnapshot));
            listAdapter.notifyDataSetChanged();
        }
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            //Find the location of the log:
            int logIndex = logIds.indexOf(dataSnapshot.getKey());
            //Update the data and adapter:
            logIds.set(logIndex, dataSnapshot.getKey());
            logSummaries.set(logIndex, genLogSummary(dataSnapshot));
            listAdapter.notifyDataSetChanged();
        }
        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            //Find the location of the log:
            int logIndex = logIds.indexOf(dataSnapshot.getKey());
            //Remove the data and update the adapter:
            logIds.remove(logIndex);
            logSummaries.remove(logIndex);
            listAdapter.notifyDataSetChanged();
        }
        // The following must be implemented in order to complete the abstract class:
        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // If there is an error, log it:
            Log.w(TAG, "Fetching driving logs failed:", databaseError.toException());
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Get the uid
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //Initialize timesRef and start listening:
        timesRef = FirebaseDatabase.getInstance().getReference().child(userId).child("times");
        timesRef.addChildEventListener(timesListener);
        //Set the adapter:
        listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, logSummaries);
        setListAdapter(listAdapter);
        //Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_log, container, false);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        // Check if the user is signed in:
        boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
        // Don't do anything if the user isn't signed in:
        if (!isSignedIn) return;
        // Get the ID of the log clicked
        String logId = logIds.get(position);
        // Open the dialog to edit
        Intent intent = new Intent(view.getContext(), CustomDriveDialog.class);
        intent.putExtra("logId", logId);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        //When we are done here, stop listening:
        timesRef.removeEventListener(timesListener);
        super.onDestroyView();
    }
}