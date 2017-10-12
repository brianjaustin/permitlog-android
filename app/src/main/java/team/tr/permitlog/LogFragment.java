package team.tr.permitlog;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v4.content.FileProvider;
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
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;

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
    private FloatingActionButton maineBtn, manualBtn;
    //Progress dialog for log generation
    private MaterialDialog proDialog;

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
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

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

        // Set add Maine drive button click
        maineBtn = (FloatingActionButton) rootView.findViewById(R.id.export_maine);
        maineBtn.setOnClickListener(onMaineExport);

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

                //If the user needs a form, add the option to export to the state log:
                if(needsForm || stateName.equals("Custom")) {
                    if (stateName.equals("Custom")) {
                        maineBtn.setLabelText("PDF Log Export");
                    } else {
                        maineBtn.setLabelText(stateName + " Log Export");
                    }
                }
                //Otherwise, just hide the maineBtn button:
                else {
                    maineBtn.setVisibility(View.GONE);
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

    private View.OnClickListener onMaineExport = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Close the floating menu
            FloatingActionMenu floatingMenu = (FloatingActionMenu) rootView.findViewById(R.id.export_menu);
            floatingMenu.close(false);

            // Don't do anything if the user isn't signed in
            boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
            if (!isSignedIn) return;

            // Turn createPdfLog into an AsyncTask:
            // Note that this can not just be an instance member because it needs to be created every time the method runs
            // since one AsyncTask can only be executed once.
            final AsyncTask<Boolean, Void, Void> createPdfLogAsync = new AsyncTask<Boolean, Void, Void>() { @Override public Void doInBackground(Boolean... bools) {
                createPdfLog(bools[0]);
                return null;
            } };
            // Create the PDF log asynchronously while showing the year:
            createPdfLogAsync.execute(true);
            // Show the progress dialog
            proDialog = new MaterialDialog.Builder(getContext())
                    .title("Generating PDF Log")
                    .content("Please wait")
                    .progress(false, logSnapshots.size())
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
        // For formatting doubles:
        DecimalFormat df = new DecimalFormat("0.00");
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
            if (showYear) flags |= FORMAT_SHOW_YEAR;
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
                PDFieldTreeNode driverField = acroForm.getField("Supervisor Name and AgeRow" + Integer.toString(rowNum));
                PDFieldTreeNode licenseField = acroForm.getField("License Number of SupervisorRow" + Integer.toString(rowNum));
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
            proDialog.incrementProgress(1);
        }

        // Add the totals
        try {
            PDFieldTreeNode totalHoursField = acroForm.getField("TOTAL HOURS OF PRACTICE DRIVING");
            PDFieldTreeNode totalNightField = acroForm.getField("TOTAL HOURS OF NIGHT DRIVING");
            if (totalHoursField != null) totalHoursField.setValue(df.format(totalUpdater.timeTracker.total / (1000.0 * 3600.0)));
            if (totalNightField != null) totalNightField.setValue(df.format(totalUpdater.timeTracker.night / (1000.0 * 3600.0)));
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
                Uri fileUri = FileProvider.getUriForFile(getActivity(), getString(R.string.file_provider_authority), file);
                intent.putExtra(Intent.EXTRA_STREAM, fileUri);

                // Log the different kind of intents that can handle PDFs:
                Log.d(TAG, "Listing intents for PDF:");
                for(ResolveInfo info : getContext().getPackageManager().queryIntentActivities(intent,PackageManager.MATCH_ALL)){
                    Log.d(TAG, "Intent: " + info.toString());
                }
                proDialog.dismiss();
                try {
                    //Show the user the dialog of apps that can handle PDFs:
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
                    String headers[] = {
                            "Month", "Date", "Year", "Duration", "Day/Night","Poor Weather",
                            "Adverse Conditions", "Supervisor", "Age", "License Number"
                    };
                    for (int i = 0; i < headers.length; i++) {
                        // Put this cell in the top row and ith column:
                        Label headerCell = new Label(i, 0, headers[i]);
                        headerCell.setCellFormat(headerFormat);
                        sheet.addCell(headerCell);
                        // For Supervisor and License Number, make the columns wide enough to fit "DELETED SUPERVSIOR"
                        if ((i == 7) || (i == 9)) sheet.setColumnView(i, 19);
                        // However, for most columns, set the column width according to the header:
                        else sheet.setColumnView(i, Math.max(10, headers[i].length()+3));
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
                        // Add the day/night
                        Label nightCell = new Label(4, i+1, ((boolean)logSnapshot.child("night").getValue()) ? "Night" : "Day");
                        sheet.addCell(nightCell);

                        //Assume that the "weather" property is false if not present
                        //since older logs will not have "weather" or "adverse" property:
                        Label weatherCell = new Label(5, i+1, logSnapshot.hasChild("weather") ? logSnapshot.child("weather").getValue().toString() : "false");
                        sheet.addCell(weatherCell);

                        //Assume the "adverse" property is false if not present:
                        Label adverseCell = new Label(6, i+1, logSnapshot.hasChild("adverse") ? logSnapshot.child("adverse").getValue().toString() : "false");
                        sheet.addCell(adverseCell);

                        // Get the driver info if possible:
                        String driverName = "DELETED SUPERVISOR", driverAge = "DELETED", driverLicense = "DELETED SUPERVISOR";
                        if (driversInfo.driverIds.contains(driverId)) {
                            int driverIndex = driversInfo.driverIds.indexOf(driverId);
                            DataSnapshot driverSnapshot = driversInfo.driverSnapshots.get(driverIndex);
                            if (DriverAdapter.hasCompleteName.accept(driverSnapshot)) driverName = driversInfo.driverNames.get(driverIndex);
                            if (driverSnapshot.hasChild("age")) driverAge = driverSnapshot.child("age").getValue().toString();
                            if (driverSnapshot.hasChild("license_number")) driverLicense = driverSnapshot.child("license_number").getValue().toString();
                        }

                        // Add it to the sheet:
                        Label nameCell = new Label(7, i+1, driverName);
                        sheet.addCell(nameCell);
                        Label ageCell = new Label(8, i+1, driverAge);
                        sheet.addCell(ageCell);
                        Label licenseCell = new Label(9, i+1, driverLicense);
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
        }
    };

    @Override
    public void onDestroyView() {
        //When we are done here, stop listening:
        timesRef.removeEventListener(timesListener);
        driversInfo.stopListening();
        totalUpdater.stopListening();
        super.onDestroyView();
    }
}
