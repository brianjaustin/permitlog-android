package team.tr.permitlog;

import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDFieldTreeNode;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;

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

    //This holds the log information:
    private ArrayList<DataSnapshot> logSnapshots = new ArrayList<>();

    //This holds the driver information:
    private DriverAdapter driversInfo;

    // Holds total time drove overall and during night:
    private long totalTime;
    private long totalNight;
    // Firebase listener that updates totalTime and totalNight:
    private ValueEventListener totalListener;

    //Firebase listener:
    private ChildEventListener timesListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //Set the data, start listening to get data for this driver, and update the adapter:
            logSnapshots.add(dataSnapshot);
            logSummaries.add(genLogSummary(dataSnapshot));
            listAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            //Find the location of the log:
            int logIndex = logIds.indexOf(dataSnapshot.getKey());

            //Update the data and adapter:
            logSnapshots.set(logIndex, dataSnapshot);
            logSummaries.set(logIndex, genLogSummary(dataSnapshot));
            listAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            //Find the location of the log:
            int logIndex = logIds.indexOf(dataSnapshot.getKey());

            //Remove the data, stop listening to the driver, and update the adapter:
            logSnapshots.remove(logIndex);
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

    public static DataSnapshotPredicate validLog = new DataSnapshotPredicate() { @Override public boolean accept(DataSnapshot dataSnapshot) {
        /* Returns true iff there is the start, end, night, and driver_id children. */
        return dataSnapshot.hasChild("start") && dataSnapshot.hasChild("end")
                && dataSnapshot.hasChild("night") && dataSnapshot.hasChild("driver_id");
    } };

    private TriLongConsumer totalCallback = new TriLongConsumer() {
        @Override
        public void accept(long totalTimeP, long dayTimeP, long nightTimeP) {
            //Set the instance properties:
            totalTime = totalTimeP;
            totalNight = nightTimeP;
        }
    };

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get the correct view
        rootView = inflater.inflate(R.layout.fragment_log, container, false);

        //Get the uid
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //Initialize timesRef and start listening:
        timesRef = FirebaseDatabase.getInstance().getReference().child(userId).child("times");
        timesListener = FirebaseHelper.transformListener(timesListener, validLog, logIds);
        timesRef.addChildEventListener(timesListener);

        // Get the totals
        totalListener = ElapsedTime.startListening(userId, totalCallback);

        //Initialize driversInfo to start listening to drivers:
        driversInfo = new DriverAdapter(getActivity(), userId, android.R.layout.simple_dropdown_item_1line);

        //Set the adapter:
        listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, logSummaries);
        setListAdapter(listAdapter);

        // Set add drive button click
        FloatingActionButton addDrive = (FloatingActionButton) rootView.findViewById(R.id.export_maine);
        addDrive.setOnClickListener(onMaineExport);

        // Set add driver button click
        FloatingActionButton addDriver = (FloatingActionButton) rootView.findViewById(R.id.export_manual);
        addDriver.setOnClickListener(onManualExport);

        // Load PDF export resources
        PDFBoxResourceLoader.init(getContext());

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

            // This Toast, when shown, will tell the user creating the PDF could take a while:
            final Toast notice = Toast.makeText(getContext(), "Creating the PDF for the Maine log may take a few minutes. Please wait. You will be asked to send the driving log to another app when it's finished.", Toast.LENGTH_LONG);
            // Turn createPdfLog into an AsyncTask:
            // Note that this can not just be an instance member because it needs to be created every time the method runs
            // since one AsyncTask can only be executed once.
            final AsyncTask<Boolean, Void, Void> createPdfLogAsync = new AsyncTask<Boolean, Void, Void>() { @Override public Void doInBackground(Boolean... bools) {
                createPdfLog(bools[0]);
                return null;
            } };

            new MaterialDialog.Builder(getContext())
                    .content("Do you want the log to include the year in its dates?")
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .neutralText(R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            // Create the PDF log asynchronously while showing the year:
                            createPdfLogAsync.execute(true);
                            // Tell the user it might be a while:
                            notice.show();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            // Create the PDF log asynchronously while not showing the year:
                            createPdfLogAsync.execute(false);
                            // Tell the user it might be a while:
                            notice.show();
                        }
                    })
                    .show();
        }
    };

    private void createPdfLog(boolean showYear) {
        ArrayList<PDDocument> logPages = new ArrayList<>();

        // Get the PDF template
        AssetManager assetManager = getActivity().getAssets();
        PDDocument pdfDocument;
        try {
            pdfDocument = PDDocument.load(assetManager.open("maine_log.pdf"));
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            return;
        }

        // Get the PDF form
        PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm();

        // Check if the form was found
        if (acroForm == null) {
            Log.e(TAG, "FORM NOT FOUND");
            return;
        }

        // Set up a Formatter for formatDateRange()
        StringBuilder noAccumulate = new StringBuilder();
        Formatter fdrFormatter = new Formatter(noAccumulate, Locale.US);
        // Fill the fields
        int subtraction = 0;
        for (int i=0; i < logSnapshots.size(); i++) {
            if (i % 50 == 0 && i != 0 && i != logSnapshots.size()) { // Begin a new page
                // Save this section
                logPages.add(pdfDocument);

                // Start a new section
                try {
                    pdfDocument = PDDocument.load(assetManager.open("maine_log.pdf"));
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    return;
                }
                acroForm = pdfDocument.getDocumentCatalog().getAcroForm();

                subtraction += 50;
            }

            // Get the log info:
            DataSnapshot logSnapshot = logSnapshots.get(i);
            long startMillis = (long) logSnapshot.child("start").getValue();
            long endMillis = (long) logSnapshot.child("end").getValue();

            // Set the flags for formatDateRange():
            int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME;
            // Show the year if showYear is set:
            if (showYear) flags |= DateUtils.FORMAT_SHOW_YEAR;
            // Clear fdrFormatter so the result is not appended on to previous results of formatDateRange():
            noAccumulate.setLength(0);
            // Format the Date/time field
            String dateTimeString = DateUtils.formatDateRange(getContext(), fdrFormatter, startMillis, endMillis, flags).toString();
            // Split string into end and beginning:
            String dateTimeStringParts[] = dateTimeString.split("\u2013");
            // If there is two parts (i.e., the drive is more than a minute long):
            if (dateTimeStringParts.length > 1) {
                // Split the second part by commas:
                String endTimeStringParts[] = dateTimeStringParts[1].split(",");
                // If there is more than one part, that means endMillis is from a different day than startMillis.
                // However, we only want to show the first day, so get rid of the second one:
                if (endTimeStringParts.length > 1) dateTimeStringParts[1] = endTimeStringParts[1];

                // Finally, join the two parts together again with a regular hyphen:
                dateTimeString = TextUtils.join("-", dateTimeStringParts);
            }

            // Get the elapsed time
            long timeElapsed = (long)(logSnapshot.child("end").getValue())-(long)(logSnapshot.child("start").getValue());
            double hoursElapsed = (double)timeElapsed / (1000.0 * 3600.0);
            DecimalFormat df = new DecimalFormat("0.00");
            String stringElapsed = df.format(hoursElapsed);

            // The following variables hold info about the drivers. These are their default values:
            String driverId = logSnapshot.child("driver_id").getValue().toString();
            int driverIndex = -1;
            DataSnapshot driverSnapshot = null;
            String driverNameAndAge = "DELETED DRIVER";
            String driverLicense = "DELETED DRIVER";
            // If possible, get driver index and info:
            if (driversInfo.driverIds.contains(driverId)) {
                driverIndex = driversInfo.driverIds.indexOf(driverId);
                driverSnapshot = driversInfo.driverSnapshots.get(driverIndex);
                // Get the driver's name
                if (DriverAdapter.hasCompleteName.accept(driverSnapshot)) {
                    driverNameAndAge = driversInfo.driverNames.get(driverIndex);
                }
                // Get the driver's age
                if (driverSnapshot.hasChild("age")) driverNameAndAge += ", " + driverSnapshot.child("age").getValue().toString();
                // Get the driver's license number
                if (driverSnapshot.hasChild("license_number")) driverLicense = driverSnapshot.child("license_number").getValue().toString();
            }

            try {
                // Add the values to the PDF
                int rowNum = (i + 1) - subtraction;
                PDFieldTreeNode dateTimeField = acroForm.getField("Date and TimeRow" + Integer.toString(rowNum));
                PDFieldTreeNode hoursField = acroForm.getField("Number of Driving HoursRow" + Integer.toString(rowNum));
                PDFieldTreeNode nightField = acroForm.getField("Number of After Dark Driving HoursRow" + Integer.toString(rowNum));
                PDFieldTreeNode driverField = acroForm.getField("Supervising Drivers Name and AgeRow" + Integer.toString(rowNum));
                PDFieldTreeNode licenseField = acroForm.getField("License Number of Supervising DriverRow" + Integer.toString(rowNum));
                if (dateTimeField != null) dateTimeField.setValue(dateTimeString);
                if (hoursField != null) hoursField.setValue(stringElapsed);
                if (nightField != null && (boolean) logSnapshot.child("night").getValue()) {
                    nightField.setValue(stringElapsed);
                } else if (nightField != null) {
                    nightField.setValue("0");
                }
                if (driverField != null) driverField.setValue(driverNameAndAge);
                if (licenseField != null) licenseField.setValue(driverLicense);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        // Add the totals
        try {
            PDFieldTreeNode totalHoursField = acroForm.getField("TOTAL HOURS OF PRACTICE DRIVING");
            PDFieldTreeNode totalNightField = acroForm.getField("TOTAL HOURS OF NIGHT DRIVING");
            if (totalHoursField != null) totalHoursField.setValue(ElapsedTime.formatSeconds(totalTime / 1000));
            if (totalNightField != null) totalNightField.setValue(ElapsedTime.formatSeconds(totalNight / 1000));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        // Save the last page
        logPages.add(pdfDocument);

        // Merge the files into one
        PDDocument totalDocument = new PDDocument();
        PDFMergerUtility mt = new PDFMergerUtility();
        for (PDDocument document : logPages) {
            try {
                mt.appendDocument(totalDocument, document);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                return;
            }
        }

        // If we can write to external storage, save the PDF:
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File file = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "log.pdf");
            try {
                totalDocument.save(file);
                totalDocument.close();

                // Send the PDF file to the user
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/pdf");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                try {
                    startActivity(Intent.createChooser(intent, "Send Driving Log"));
                } catch (android.content.ActivityNotFoundException exception) {
                    // There is no app installed that can send this, so show an error:
                    Toast.makeText(getContext(), R.string.export_pdf_error, Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                // When there is an error, log it and notify the user:
                Log.e(TAG, e.getMessage());
                Toast.makeText(getContext(), R.string.save_pdf_error, Toast.LENGTH_SHORT).show();
            }
        }
        // Otherwise, show the user an error message:
        else Toast.makeText(getContext(), R.string.save_pdf_error, Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener onManualExport = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Close the floating menu
            FloatingActionMenu floatingMenu = (FloatingActionMenu) rootView.findViewById(R.id.export_menu);
            floatingMenu.close(false);

            // Don't do anything if the user isn't signed in
            boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
            if (!isSignedIn) return;

            // Setup the variable to hold the CSV file
            String logAsCsv = "month, day, year, duration, day/night, driver, license_number\n";
            // Loop through the logs:
            for (DataSnapshot logSnapshot : logSnapshots) {
                // Get the driver database key:
                String driverId = logSnapshot.child("driver_id").getValue().toString();
                // If possible, get driver index and info:
                int driverIndex = -1;
                DataSnapshot driverSnapshot = null;
                if (driversInfo.driverIds.contains(driverId)) {
                    driverIndex = driversInfo.driverIds.indexOf(driverId);
                    driverSnapshot = driversInfo.driverSnapshots.get(driverIndex);
                }
                // Get the calendar object:
                Calendar startDate = Calendar.getInstance();
                startDate.setTimeInMillis((long) logSnapshot.child("start").getValue());
                // Add the month
                String months[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                logAsCsv += months[startDate.get(Calendar.MONTH)] + ", ";

                // Add the day
                logAsCsv += startDate.get(Calendar.DAY_OF_MONTH) + ", ";

                // Add the year
                logAsCsv += startDate.get(Calendar.YEAR) + ", ";

                // Add the duration
                long timeElapsed = (long)(logSnapshot.child("end").getValue())-(long)(logSnapshot.child("start").getValue());
                logAsCsv += ElapsedTime.formatSeconds(timeElapsed/1000) + ", ";

                // Get night flag
                if ((boolean) logSnapshot.child("night").getValue()) {
                    // During the night
                    logAsCsv += "night, ";
                } else {
                    // During the day
                    logAsCsv += "day, ";
                }

                // Get the license name if available
                String driverName;
                if (driverSnapshot != null && DriverAdapter.hasCompleteName.accept(driverSnapshot)) {
                    driverName = driversInfo.driverNames.get(driverIndex);
                }
                else driverName = "DELETED DRIVER";
                logAsCsv += driverName + ", ";

                // Get the license number if available.
                String licenseId;
                if (driverSnapshot != null && driverSnapshot.hasChild("license_number")) {
                    licenseId = driverSnapshot.child("license_number").getValue().toString();
                }
                // If the license number is not available, just say the driver is unknown:
                else licenseId = "DELETED DRIVER";
                // Add the license number and a newline
                logAsCsv += licenseId+"\n";
            }

            // Send the CSV file to the user
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, logAsCsv);
            try {
                startActivity(Intent.createChooser(intent, "Send Driving Log"));
            } catch (android.content.ActivityNotFoundException exception) {
                // There is no app installed that can send this, so show an error:
                Toast.makeText(getContext(), R.string.export_email_error, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onDestroyView() {
        //When we are done here, stop listening:
        timesRef.removeEventListener(timesListener);
        driversInfo.stopListening();
        ElapsedTime.stopListening(totalListener);
        super.onDestroyView();
    }
}