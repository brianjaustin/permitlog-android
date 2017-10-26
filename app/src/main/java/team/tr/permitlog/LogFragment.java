package team.tr.permitlog;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.support.v4.content.FileProvider;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.Alignment;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import static android.text.format.DateUtils.FORMAT_NUMERIC_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_YEAR;

public class LogFragment extends ListFragment {
    //For logging:
    public static String TAG = "LogFragment";

    // The root view for this fragment, used to find elements by id:
    private View rootView;
    // User ID from Firebase:
    private String userId;
    //Firebase reference:
    private DatabaseReference timesRef;
    private DatabaseReference goalsRef;
    //Does the user have day/night or weather or adverse goals?
    private boolean hasNightGoals, hasWeatherGoals, hasAdverseGoals;
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
    // Object that keeps track of total time and total time during night:
    private ElapsedTime totalUpdater;
    // Buttons for adding
    private FloatingActionButton maineBtn, northCarolinaBtn, manualBtn;
    //AsyncTask for generating PDF log in separate thread:
    private AsyncTask<Void, Void, Void> createPdfLogAsync;
    //Progress dialog for PDF log generation:
    private MaterialDialog proDialog;
    //Used to format numbers in logs:
    private DecimalFormat twoDecimalPlaces = new DecimalFormat("0.00");

    //Firebase listener:
    private ChildEventListener timesListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //If there are no snapshots, then we are currently showing "No logs", so get rid of that:
            if (logSnapshots.isEmpty()) logSummaries.clear();
            //Set the data, start listening to get data for this driver, and update the adapter:
            logSnapshots.add(dataSnapshot);
            logSummaries.add(genLogSummary(dataSnapshot));

            /*
            //Sort the logs by starting value
            Collections.sort(logSnapshots, new Comparator<DataSnapshot>() {
                @Override
                public int compare(DataSnapshot o1, DataSnapshot o2){
                    return Long.compare((long) o1.child("start").getValue(), (long) o2.child("start").getValue());
                }
            });
            */

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
            //Add "No logs" if there are no more logs:
            if (logSummaries.isEmpty()) logSummaries.add("No logs");
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

    private String genLogSummary(DataSnapshot dataSnapshot) {
        //Find the time elapsed during the drive:
        long driveTimeInSec =
                ((long)(dataSnapshot.child("end").getValue())-(long)(dataSnapshot.child("start").getValue()))/1000;

        //Format the time appropriately:
        String driveTimeString = ElapsedTime.formatSeconds(driveTimeInSec);
        //Format the date based off the starting time:
        String driveDate = DateUtils.formatDateTime(getContext(),
                (long)(dataSnapshot.child("start").getValue()), FORMAT_NUMERIC_DATE | FORMAT_SHOW_YEAR);

        //Finally return the time with the date:
        return (driveTimeString + " on " + driveDate);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get the correct view
        rootView = inflater.inflate(R.layout.fragment_log, container, false);

        //Get the uid
        FirebaseUser curUser = FirebaseAuth.getInstance().getCurrentUser();
        if (curUser != null) userId = curUser.getUid();
        //If the user is not signed in, then don't do anything:
        else return rootView;

        //Initialize timesRef and start listening:
        timesRef = FirebaseDatabase.getInstance().getReference().child(userId).child("times");
        goalsRef = FirebaseDatabase.getInstance().getReference().child(userId).child("goals");
        timesListener = FirebaseHelper.transformListener(timesListener, validLog, logIds);
        timesRef.addChildEventListener(timesListener);

        // Get the totals
        totalUpdater = new ElapsedTime(userId, null);

        //Initialize driversInfo to start listening to drivers:
        driversInfo = new DriverAdapter(getActivity(), userId, android.R.layout.simple_dropdown_item_1line);

        //Show "No logs" at the beginning:
        logSummaries.add("No logs");
        //Set the adapter:
        listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, logSummaries);
        setListAdapter(listAdapter);

        // Set add the drive button clicks for each state that needs a specific form:
        maineBtn = (FloatingActionButton) rootView.findViewById(R.id.export_maine);
        maineBtn.setOnClickListener(onMaineExport);
        northCarolinaBtn = (FloatingActionButton)rootView.findViewById(R.id.export_north_carolina);
        northCarolinaBtn.setOnClickListener(onNorthCarolinaExport);

        // Set add manual drive button click
        manualBtn = (FloatingActionButton) rootView.findViewById(R.id.export_manual);
        manualBtn.setOnClickListener(onManualExport);

        // Load PDF export resources
        PDFBoxResourceLoader.init(getContext());

        //Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        //This is in the onStart method and not the onCreateView method above
        //in order to avoid an IllegalStateException
        //that happens when trying to use getString() before the fragment is ready.

        //Update the maineBtn button based off the goals data:
        goalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Store if the user has day/night goals, weather goals or adverse goals:
                hasNightGoals = ((dataSnapshot.hasChild("day") && ((long)dataSnapshot.child("day").getValue() != 0)) ||
                        (dataSnapshot.hasChild("night") && ((long)dataSnapshot.child("night").getValue() != 0)));
                hasWeatherGoals = (dataSnapshot.hasChild("weather") && ((long)dataSnapshot.child("weather").getValue() != 0));
                hasAdverseGoals = (dataSnapshot.hasChild("adverse") && ((long)dataSnapshot.child("adverse").getValue() != 0));

                String stateName = ""; //Stores state of user and whether that state requires a form
                boolean needsForm = false;

                //If this is an old user, they might not have state data,
                //in which case, we tell them to set their state:
                if (!dataSnapshot.hasChild("stateName")) {
                    Toast.makeText(getContext(), getString(R.string.no_state_error), Toast.LENGTH_LONG).show();
                }
                //Otherwise, just set stateName and needsForm:
                else {
                    stateName = dataSnapshot.child("stateName").getValue().toString();
                    if (dataSnapshot.hasChild("needsForm")) needsForm = (boolean)dataSnapshot.child("needsForm").getValue();
                    //If the user has "stateName" but not "needsForm",
                    //then this is one of the few users who had a buggy version of the app:
                    else {
                        Log.d(TAG, "The weird buggy situation in LogFragment.");
                        //On this buggy, edge-case situation, needsForm is true iff the state is Maine:
                        if (stateName.equals("Maine")) needsForm = true;
                        else needsForm = false;
                    }
                }

                //This is the button for the user's state, or null if this state does not have a button:
                FloatingActionButton stateBtn = null;
                //If the user needs a form for their state or this is the custom state,
                //add the option to export to the state log:
                if(needsForm || stateName.equals("Custom")) {
                    //Set the correct button for each state:
                    if (stateName.equals("Maine")) stateBtn = maineBtn;
                    else if (stateName.equals("North Carolina")) stateBtn = northCarolinaBtn;
                    //By default, use Maine's button and call it "PDF Log Export":
                    else  {
                        stateBtn = maineBtn;
                        stateBtn.setLabelText("PDF Log Export");
                    }
                }

                //This is all of the state buttons:
                FloatingActionButton allStateBtns[] = {maineBtn, northCarolinaBtn};
                //Make all of the state buttons gone accept for the user's:
                for (FloatingActionButton button : allStateBtns) {
                    if (button != stateBtn) button.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
            }
        });
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        // Don't do anything if there are no logs:
        if (logIds.isEmpty()) return;
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //When the screen is rotated, cancel the progress bar and the AsyncTask, if they have been started,
        //because otherwise, the AsyncTask will crash when trying to use getContext() or getActivity():
        if (createPdfLogAsync != null) createPdfLogAsync.cancel(true);
        if (proDialog != null) proDialog.cancel();
    }

    private void startProgressDialog() {
        // Show the progress dialog
        proDialog = new MaterialDialog.Builder(getContext())
                .title("Generating PDF Log")
                .content("Please wait, do not rotate the screen or leave the app.")
                //The progress is incremented for every log plus 1 more for saving the PDF at the end:
                .progress(false, logSnapshots.size() + 1)
                .canceledOnTouchOutside(false)
                .show();
    }

    private PDAcroForm createNewPdf(String pdfName, List<PDDocument> pdfList) {
        //Since the following will use getActivity(),
        //check isCancelled() before continuing:
        if (createPdfLogAsync.isCancelled()) return null;

        // Create a new PDF:
        PDDocument pdfDocument;
        try {
            pdfDocument = PDDocument.load(getActivity().getAssets().open(pdfName));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        //Add this PDF to the list:
        pdfList.add(pdfDocument);

        // Get the form:
        PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm();
        // Check if the form was found
        if (acroForm == null) {
            Log.e(TAG, "FORM NOT FOUND");
            return null;
        }
        // Otherwise, return acroForm:
        return acroForm;
    }

    private void savePDFs(List<PDDocument> pdfList) {
        // Merge the files into one
        PDDocument totalDocument = new PDDocument();
        PDFMergerUtility mt = new PDFMergerUtility();
        for (PDDocument document : pdfList) {
            try {
                mt.appendDocument(totalDocument, document);
                //Close the old documents since they are merged in totalDocument
                document.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                return;
            }
        }

        //Since the following will use getContext(),
        //check isCancelled() before continuing:
        if (createPdfLogAsync.isCancelled()) return;

        // If we can write to external storage, save the PDF:
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File file = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "log.pdf");
            try {
                //Save and close the PDF file:
                totalDocument.save(file);
                totalDocument.close();
            } catch (IOException e) {
                // When there is an error, log it and notify the user:
                Log.e(TAG, e.getMessage());
                //Since the following is going to use getContext(),
                //check isCancelled() before continuing:
                if (createPdfLogAsync.isCancelled()) return;
                Toast.makeText(getContext(), R.string.save_pdf_error, Toast.LENGTH_SHORT).show();
            }
            //Increment the progress bar to 100%:
            proDialog.incrementProgress(1);

            //Since the following is going to use getActivity(),
            //check isCancelled() before continuing:
            if (createPdfLogAsync.isCancelled()) return;

            // Send the PDF file to the user
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            Uri fileUri = FileProvider.getUriForFile(getActivity(), getString(R.string.file_provider_authority), file);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);

                /*// This is for debugging the different kind of intents that can handle PDFs:
                Log.d(TAG, "Listing intents for PDF:");
                for(ResolveInfo info : getContext().getPackageManager().queryIntentActivities(intent,PackageManager.MATCH_ALL)){
                    Log.d(TAG, "Intent: " + info.toString());
                }*/
            //Get rid of the progress bar now that we're finished:
            proDialog.dismiss();

            //Since the following is going to use startActivity() or getContext(),
            //check isCancelled() before continuing:
            if (createPdfLogAsync.isCancelled()) return;
            try {
                //Show the user the dialog of apps that can handle PDFs:
                startActivity(Intent.createChooser(intent, "Send Driving Log"));
            } catch (android.content.ActivityNotFoundException exception) {
                // There is no app installed that can send this, so show an error:
                Toast.makeText(getContext(), R.string.export_pdf_error, Toast.LENGTH_LONG).show();
            }
        }
        // Otherwise, show the user an error message:
        else {
            Toast.makeText(getContext(), R.string.save_pdf_error, Toast.LENGTH_SHORT).show();
            try {
                //Don't forget to close the PDF file:
                totalDocument.close();
            } catch (IOException e) {}
        }
    }

    private String getElapsedTime(DataSnapshot logSnapshot) {
        long timeElapsed = (long) (logSnapshot.child("end").getValue()) - (long) (logSnapshot.child("start").getValue());
        double hoursElapsed = (double) timeElapsed / (1000.0 * 3600.0);
        String elapsedTimeStr = twoDecimalPlaces.format(hoursElapsed);
        return elapsedTimeStr;
    }

    private List<String> getDriverInfo(DataSnapshot logSnapshot) {
        // The following variables hold info about the drivers. These are their default values:
        String driverId = logSnapshot.child("driver_id").getValue().toString();
        int driverIndex = -1;
        DataSnapshot driverSnapshot = null;
        String driverName = "DELETED DRIVER";
        String driverAge = "";
        String driverLicense = "DELETED DRIVER";
        // If possible, get driver index and info:
        if (driversInfo.driverIds.contains(driverId)) {
            driverIndex = driversInfo.driverIds.indexOf(driverId);
            driverSnapshot = driversInfo.driverSnapshots.get(driverIndex);
            // Get the driver's name
            if (DriverAdapter.hasCompleteName.accept(driverSnapshot)) {
                driverName = driversInfo.driverNames.get(driverIndex);
            }
            // Get the driver's age
            if (driverSnapshot.hasChild("age"))
                driverAge += ", " + driverSnapshot.child("age").getValue().toString();
            // Get the driver's license number
            if (driverSnapshot.hasChild("license_number"))
                driverLicense = driverSnapshot.child("license_number").getValue().toString();
        }
        //Return the info:
        return Arrays.asList(driverName, driverAge, driverLicense);
    }

    private String getGoalTotal(String goalType) {
        //Convert goal to hours and format with two decimal places:
        return twoDecimalPlaces.format(totalUpdater.timeTracker.getTime(goalType) / (1000.0 * 3600.0));
    }

    private View.OnClickListener onMaineExport = new View.OnClickListener() { @Override public void onClick(View view) {
        // Close the floating menu
        FloatingActionMenu floatingMenu = (FloatingActionMenu) rootView.findViewById(R.id.export_menu);
        floatingMenu.close(false);

        // Don't do anything if the user isn't signed in
        boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity) getActivity());
        if (!isSignedIn) return;

        // Create the PDF log in an AsyncTask:
        // Note that this can not just be a constant instance member
        // because it needs to be created every time the method runs
        // since one AsyncTask can only be executed once.
        createPdfLogAsync = new AsyncTask<Void, Void, Void>() { @Override public Void doInBackground(Void... args) {
            //Stores all of the PDFs:
            ArrayList<PDDocument> logPages = new ArrayList<>();
            // Stores the current PDF form:
            PDAcroForm acroForm = null;

            //Loop through all the logs:
            for (int i = 0; i < logSnapshots.size(); i++) {
                if (i % 50 == 0) { // Begin a new page for every 50 logs:
                    acroForm = createNewPdf("maine_log.pdf", logPages);
                    //Abort if acroForm is null:
                    if (acroForm == null) return null;
                }

                // Get the log info:
                DataSnapshot logSnapshot = logSnapshots.get(i);
                long startMillis = (long) logSnapshot.child("start").getValue();
                //long endMillis = (long) logSnapshot.child("end").getValue();

                // Set the flags for formatDateTime():
                int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE |
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR;

                //Since we are about to use getContext(), make sure the AsyncTask is still running:
                if (isCancelled()) return null;
                // Format the Date/time field
                String dateTimeString = DateUtils.formatDateTime(getContext(), startMillis, flags);

                // Get the elapsed time
                String elapsedTimeString = getElapsedTime(logSnapshot);

                // Get info about the drivers:
                List<String> driverInfo = getDriverInfo(logSnapshot);
                // Extract the necessary info:
                String driverNameAndAge = driverInfo.get(0);
                String driverLicense = driverInfo.get(2);
                // Add the age if this is not a deleted driver:
                if (!driverNameAndAge.equals("DELETED DRIVER")) driverNameAndAge += ", "+driverInfo.get(1);

                try {
                    // Add the values to the PDF
                    int rowNum = (i % 50)+1;
                    PDTextField dateTimeField = (PDTextField)acroForm.getField("Date and TimeRow" + Integer.toString(rowNum));
                    PDTextField hoursField = (PDTextField)acroForm.getField("Number of Driving HoursRow" + Integer.toString(rowNum));
                    PDTextField nightField = (PDTextField)acroForm.getField("Number of After Dark Driving HoursRow" + Integer.toString(rowNum));
                    PDTextField driverField = (PDTextField)acroForm.getField("Supervising Drivers Name and AgeRow" + Integer.toString(rowNum));
                    PDTextField licenseField = (PDTextField)acroForm.getField("License Number of Supervising DriverRow" + Integer.toString(rowNum));
                    if (dateTimeField != null) dateTimeField.setValue(dateTimeString);
                    if (hoursField != null) hoursField.setValue(elapsedTimeString);
                    if (nightField != null && (boolean) logSnapshot.child("night").getValue()) {
                        nightField.setValue(elapsedTimeString);
                    } else if (nightField != null) {
                        nightField.setValue("0");
                    }
                    if (driverField != null) driverField.setValue(driverNameAndAge);
                    if (licenseField != null) licenseField.setValue(driverLicense);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
                //After each log is finished, increment the progress bar:
                proDialog.incrementProgress(1);
            }

            // Add the totals
            try {
                PDTextField totalHoursField = (PDTextField)acroForm.getField("TOTAL HOURS OF PRACTICE DRIVING");
                PDTextField totalNightField = (PDTextField)acroForm.getField("TOTAL HOURS OF NIGHT DRIVING");
                if (totalHoursField != null) totalHoursField.setValue(getGoalTotal("total"));
                if (totalNightField != null) totalNightField.setValue(getGoalTotal("night"));
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            //Finally, save the documents and present it to the user:
            savePDFs(logPages);

            return null;
        } };

        // Show the progress dialog
        startProgressDialog();
        // Create the PDF log asynchronously:
        createPdfLogAsync.execute();
    } };



    private View.OnClickListener onNorthCarolinaExport = new View.OnClickListener() { @Override public void onClick(View view) {
        // Close the floating menu
        FloatingActionMenu floatingMenu = (FloatingActionMenu) rootView.findViewById(R.id.export_menu);
        floatingMenu.close(false);

        // Don't do anything if the user isn't signed in
        boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity) getActivity());
        if (!isSignedIn) return;

        // Create the PDF log in an AsyncTask:
        createPdfLogAsync = new AsyncTask<Void, Void, Void>() { @Override public Void doInBackground(Void... args) {
            //Stores all of the PDFs:
            ArrayList<PDDocument> logPages = new ArrayList<>();
            // Stores the current PDF form:
            PDAcroForm acroForm = null;

            //Loop through all the logs:
            for (int i = 0; i < logSnapshots.size(); i++) {
                if (i % 20 == 0) { // Begin a new page for every 20 logs:
                    acroForm = createNewPdf("north_carolina_log.pdf", logPages);
                    //Abort if acroForm is null:
                    if (acroForm == null) return null;
                }

                // Get the log info:
                DataSnapshot logSnapshot = logSnapshots.get(i);
                long startMillis = (long) logSnapshot.child("start").getValue();
                //long endMillis = (long) logSnapshot.child("end").getValue();

                // Set the flags for formatDateTime():
                int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR;
                //Since we are about to use getContext(), make sure the AsyncTask is still running:
                if (isCancelled()) return null;
                // Format the Date field
                String dateString = DateUtils.formatDateTime(getContext(), startMillis, flags);

                // Get the elapsed time
                String elapsedTimeString = getElapsedTime(logSnapshot);

                // Get info about the drivers:
                List<String> driverInfo = getDriverInfo(logSnapshot);
                // Extract the necessary info:
                String driverName = driverInfo.get(0);
                String driverLicense = driverInfo.get(2);

                //Put all of this info into the form:
                try {
                    int curRow = (i % 20)+1;
                    PDTextField dateField = (PDTextField)acroForm.getField("DATERow"+curRow);
                    PDTextField timeOfDayField = (PDTextField)acroForm.getField("TIME OF DAYRow"+curRow);
                    PDTextField timeOfNightField = (PDTextField)acroForm.getField("TIME OF NIGHTRow"+curRow);
                    //Yes, this is quite an odd name for a field, but that's what the actual field name is:
                    PDTextField elapsedTimeField = (PDTextField)acroForm.getField("fill_"+(6*curRow+2));
                    PDTextField driverNameField = (PDTextField)acroForm.getField("SUPERVISING DRIVERS PRINTED NAMERow"+curRow);
                    PDTextField driverLicenseField = (PDTextField)acroForm.getField("SUPERVISING DRIVERS DL Number and StateRow"+curRow);
                    if (dateField != null) dateField.setValue(dateString);
                    //For time of day/time of night, mark the box depending on if this log was at day or night:
                    if ((boolean) logSnapshot.child("night").getValue()) {
                        if (timeOfNightField != null) timeOfNightField.setValue("X");
                    }
                    else {
                        if (timeOfDayField != null) timeOfDayField.setValue("X");
                    }
                    if (elapsedTimeField != null) elapsedTimeField.setValue(elapsedTimeString);
                    if (driverNameField != null) driverNameField.setValue(driverName);
                    if (driverLicenseField != null) driverLicenseField.setValue(driverLicense);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
                //After each log is finished, increment the progress bar:
                proDialog.incrementProgress(1);
            }

            // Add the totals
            try {
                PDTextField totalHoursField = (PDTextField)acroForm.getField("Gr a n d To t a l");
                PDTextField totalDayField = (PDTextField)acroForm.getField("To t a l Da y Ho u r s Driv en");
                PDTextField totalNightField = (PDTextField)acroForm.getField("To t a l Ni g h t Ho u r s Dr iv e n");
                if (totalHoursField != null) totalHoursField.setValue(getGoalTotal("total"));
                if (totalDayField != null) totalDayField.setValue(getGoalTotal("day"));
                if (totalNightField != null) totalNightField.setValue(getGoalTotal("night"));
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            //Save all the documents and present it to the user:
            savePDFs(logPages);
            
            return null;
        } };

        // Show the progress dialog
        startProgressDialog();
        // Create the PDF log asynchronously:
        createPdfLogAsync.execute();
    } };

    private View.OnClickListener onManualExport = new View.OnClickListener() { @Override public void onClick(View view) {
        // Close the floating menu
        FloatingActionMenu floatingMenu = (FloatingActionMenu) rootView.findViewById(R.id.export_menu);
        floatingMenu.close(false);

        // Don't do anything if the user isn't signed in
        boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
        if (!isSignedIn) return;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File sheetFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "log.xls");
            try {
                // This is needed to conserve memory:
                WorkbookSettings wbSettings = new WorkbookSettings();
                wbSettings.setUseTemporaryFileDuringWrite(true);
                // Create workbook and sheet in above file:
                WritableWorkbook wb = Workbook.createWorkbook(sheetFile, wbSettings);
                WritableSheet sheet = wb.createSheet("Sheet1", 0);

                // Create header format:
                WritableFont headerFont = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
                WritableCellFormat headerFormat = new WritableCellFormat(headerFont);
                headerFormat.setAlignment(Alignment.CENTRE);
                // Create header cells:
                String supervisorLit = "Supervisor";
                String licenseNumberLit = "License Number";
                ArrayList<String> headers = new ArrayList<>(
                        Arrays.asList("Month", "Date", "Year", "Duration", supervisorLit, "Age", licenseNumberLit)
                );

                //This represents the number of special goal types the user has:
                int numSpecialGoals = 0;
                //For each goal type, if the user has it, add it to the headers and then increment numSpecialGoals:
                String adverseLit = "Adverse Conditions";
                if (hasAdverseGoals) {
                    headers.add(4, adverseLit);
                    numSpecialGoals++;
                }
                String weatherLit = "Poor Weather";
                if (hasWeatherGoals) {
                    headers.add(4, weatherLit);
                    numSpecialGoals++;
                }
                String nightLit = "Day/Night";
                if (hasNightGoals) {
                    headers.add(4, nightLit);
                    numSpecialGoals++;
                }

                for (int i = 0; i < headers.size(); i++) {
                    // Put this cell in the top row and ith column:
                    Label headerCell = new Label(i, 0, headers.get(i));
                    headerCell.setCellFormat(headerFormat);
                    sheet.addCell(headerCell);
                    // For Supervisor and License Number, make the columns wide enough to fit "DELETED SUPERVSIOR"
                    if (headers.get(i).equals(supervisorLit) || headers.get(i).equals(licenseNumberLit)) {
                        sheet.setColumnView(i, 19);
                    }
                    // However, for most columns, set the column width according to the header:
                    else sheet.setColumnView(i, Math.max(10, headers.get(i).length()+3));
                }

                // Loop through the logs:
                for (int i = 0; i < logSnapshots.size(); i++) {
                    DataSnapshot logSnapshot = logSnapshots.get(i);
                    // Get the driver database key:
                    String driverId = logSnapshot.child("driver_id").getValue().toString();

                    // Get the calendar object:
                    Calendar startDate = Calendar.getInstance();
                    startDate.setTimeInMillis((long) logSnapshot.child("start").getValue());
                    // Add the month
                    String month = new DateFormatSymbols().getShortMonths()[startDate.get(Calendar.MONTH)];
                    Label monthCell = new Label(0, i+1, month);
                    sheet.addCell(monthCell);
                    // Add the day
                    Label dayCell = new Label(1, i+1, Integer.toString(startDate.get(Calendar.DAY_OF_MONTH)));
                    sheet.addCell(dayCell);
                    // Add the year
                    Label yearCell = new Label(2, i+1, Integer.toString(startDate.get(Calendar.YEAR)));
                    sheet.addCell(yearCell);
                    // Add the duration
                    long timeElapsed = (long)(logSnapshot.child("end").getValue())-(long)(logSnapshot.child("start").getValue());
                    Label durationCell = new Label(3, i+1, ElapsedTime.formatSeconds(timeElapsed/1000));
                    sheet.addCell(durationCell);

                    // Add the day/night if the user has day/night goals:
                    if (hasNightGoals) {
                        Label nightCell = new Label(headers.indexOf(nightLit), i + 1,
                                ((boolean) logSnapshot.child("night").getValue()) ? "Night" : "Day");
                        sheet.addCell(nightCell);
                    }

                    // Add the weather if the user has weather goals:
                    if (hasWeatherGoals) {
                        //Assume that the "weather" property is false if not present
                        //since older logs will not have "weather" or "adverse" property:
                        Label weatherCell = new Label(headers.indexOf(weatherLit), i + 1,
                                logSnapshot.hasChild("weather") ? logSnapshot.child("weather").getValue().toString() : "false");
                        sheet.addCell(weatherCell);
                    }

                    // Add the adverse if the user has adverse goals:
                    if (hasAdverseGoals) {
                        //Assume the "adverse" property is false if not present:
                        Label adverseCell = new Label(headers.indexOf(adverseLit), i + 1,
                                logSnapshot.hasChild("adverse") ? logSnapshot.child("adverse").getValue().toString() : "false");
                        sheet.addCell(adverseCell);
                    }

                    // Get the driver info if possible:
                    String driverName = "DELETED SUPERVISOR", driverAge = "DELETED", driverLicense = "DELETED SUPERVISOR";
                    if (driversInfo.driverIds.contains(driverId)) {
                        int driverIndex = driversInfo.driverIds.indexOf(driverId);
                        DataSnapshot driverSnapshot = driversInfo.driverSnapshots.get(driverIndex);
                        if (DriverAdapter.hasCompleteName.accept(driverSnapshot)) driverName = driversInfo.driverNames.get(driverIndex);
                        if (driverSnapshot.hasChild("age")) driverAge = driverSnapshot.child("age").getValue().toString();
                        if (driverSnapshot.hasChild("license_number")) driverLicense = driverSnapshot.child("license_number").getValue().toString();
                    }

                    // Add it to the sheet, using numSpecialGoals to make sure it comes after all of the goal types:
                    Label nameCell = new Label(4+numSpecialGoals, i+1, driverName);
                    sheet.addCell(nameCell);
                    Label ageCell = new Label(5+numSpecialGoals, i+1, driverAge);
                    sheet.addCell(ageCell);
                    Label licenseCell = new Label(6+numSpecialGoals, i+1, driverLicense);
                    sheet.addCell(licenseCell);
                }

                // Save and close the Workbook:
                wb.write();
                wb.close();
            } catch (IOException | WriteException e) {
                Log.e(TAG, "While trying to make spreadsheet: " + e);
                return;
            }

            // Create an intent in order to send the spreadsheet to another app:
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.ms-excel");
            Uri fileUri = FileProvider.getUriForFile(getActivity(), getString(R.string.file_provider_authority), sheetFile);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);

            // Log the different kind of intents that can handle Excel spreadsheets:
            Log.d(TAG, "Listing intents for spreadsheet:");
            for(ResolveInfo info : getContext().getPackageManager().queryIntentActivities(intent,PackageManager.MATCH_ALL)){
                Log.d(TAG, "Intent: " + info.toString());
            }

            try {
                startActivity(Intent.createChooser(intent, "Send Driving Log"));
            } catch (android.content.ActivityNotFoundException exception) {
                // There is no app installed that can send this, so show an error:
                Toast.makeText(getContext(), R.string.export_xls_error, Toast.LENGTH_LONG).show();
            }
        }
    } };

    @Override
    public void onDestroyView() {
        //When we are done here, stop listening:
        timesRef.removeEventListener(timesListener);
        driversInfo.stopListening();
        totalUpdater.stopListening();
        super.onDestroyView();
    }
}
