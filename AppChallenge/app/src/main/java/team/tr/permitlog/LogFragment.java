package team.tr.permitlog;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

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
    //Firebase listener:
    ChildEventListener timesListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //Find the time elapsed during the drive:
            long driveTimeInSec =
                    ((long)(dataSnapshot.child("end").getValue())-(long)(dataSnapshot.child("start").getValue()))/1000;
            //Format the time appropriately:
            String driveTimeString = DateUtils.formatElapsedTime(driveTimeInSec);
            //If the time is less than an hour, then add "0:" to the beginning:
            if (driveTimeInSec < 3600) driveTimeString = "0:"+driveTimeString;
            //This is the summary of the log shown to the user:
            String logSummary = "Drove for "+driveTimeString;
            //Was the drive at night? Add "at night"/"during the day" appropriately.
            boolean isDriveAtNight = (boolean)(dataSnapshot.child("night").getValue());
            if (isDriveAtNight) logSummary += " at night";
            else logSummary += " during the day";
            //Update the data and adapter:
            logSummaries.add(logSummary);
            listAdapter.notifyDataSetChanged();
        }
        // The following two methods will be implemented later:
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {}
        // The following must be implemented in order to complete the abstract class:
        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
        @Override
        public void onCancelled(DatabaseError databaseError) {
            // If there is an error, log it:
            Log.w(TAG, "Fetching driving logs failed:", databaseError.toException());
        }
    };
    //This holds all of the summaries of the logs that we will show in the ListView:
    private ArrayList<String> logSummaries = new ArrayList<>();
    //This is the ListView's adapter:
    private ArrayAdapter<String> listAdapter;

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
    public void onDestroyView() {
        //When we are done here, stop listening:
        timesRef.removeEventListener(timesListener);
        super.onDestroyView();
    }
}
