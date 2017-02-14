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
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;

public class LogFragment extends ListFragment {
    //For logging:
    public static String TAG = "LogFragment";

    // The root view for this fragment, used to find elements by id:
    private View rootView;

    // User ID from Firebase:
    private String userId;

    //Firebase reference:
    private DatabaseReference timesRef;

    //This holds all of the keys of the logs in the database:
    private ArrayList<String> logIds = new ArrayList<>();

    //This holds all of the summaries of the logs that we will show in the ListView:
    private ArrayList<String> logSummaries = new ArrayList<>();

    //This is the ListView's adapter:
    private ArrayAdapter<String> listAdapter;

    // This holds the CSV data for export
    private String logAsCsv;

    private void saveToCsv(final DataSnapshot itemSnapshot) {
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child(userId)
                .child("drivers").child(itemSnapshot.child("driver_id").getValue().toString());

        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Add the start time
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis((long) itemSnapshot.child("start").getValue());
                logAsCsv += calendar.getTime().toString() + ", ";

                // Add the end time
                calendar.setTimeInMillis((long) itemSnapshot.child("end").getValue());
                logAsCsv += calendar.getTime().toString() + ", ";

                // Get night flag
                logAsCsv += itemSnapshot.child("night").getValue().toString() + ", ";

                // Get the license number if available.
                String licenseId;
                if (dataSnapshot.hasChild("license_number")) licenseId = dataSnapshot.child("license_number").getValue().toString();
                // If the license number is not available, just say the driver is unknown:
                else licenseId = "UNKNOWN DRIVER";
                // Add the license number and a newline
                logAsCsv += licenseId+"\n";
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

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

            // Prepare for manual export
            saveToCsv(dataSnapshot);
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
        // Get the correct view
        rootView = inflater.inflate(R.layout.fragment_log, container, false);

        //Get the uid
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Setup the variable to hold the CSV file
        logAsCsv = "start, stop, night, driver\n";

        //Initialize timesRef and start listening:
        timesRef = FirebaseDatabase.getInstance().getReference().child(userId).child("times");
        timesRef.addChildEventListener(timesListener);

        //Set the adapter:
        listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, logSummaries);
        setListAdapter(listAdapter);

        // Set add drive button click
        FloatingActionButton addDrive = (FloatingActionButton) rootView.findViewById(R.id.export_maine);
        addDrive.setOnClickListener(onMaineExport);

        // Set add driver button click
        FloatingActionButton addDriver = (FloatingActionButton) rootView.findViewById(R.id.export_manual);
        addDriver.setOnClickListener(onManualExport);

        //Inflate the layout for this fragment
        return rootView;
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

    private View.OnClickListener onMaineExport = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Close the floating menu
            FloatingActionMenu floatingMenu = (FloatingActionMenu) rootView.findViewById(R.id.export_menu);
            floatingMenu.close(false);

            // Don't do anything if the user isn't signed in
            boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
            if (!isSignedIn) return;

            // TODO: export to Maine Permitee Log PDF
        }
    };

    private View.OnClickListener onManualExport = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Close the floating menu
            FloatingActionMenu floatingMenu = (FloatingActionMenu) rootView.findViewById(R.id.export_menu);
            floatingMenu.close(false);

            // Don't do anything if the user isn't signed in
            boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
            if (!isSignedIn) return;

            // Send the CSV file to the user
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, logAsCsv);
            try {
                startActivity(Intent.createChooser(intent, "Send Driving Log"));
            } catch (android.content.ActivityNotFoundException exception) {
                // There is no email client installed
                Toast.makeText(rootView.getContext(), R.string.export_email_error, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onDestroyView() {
        //When we are done here, stop listening:
        timesRef.removeEventListener(timesListener);
        super.onDestroyView();
    }
}