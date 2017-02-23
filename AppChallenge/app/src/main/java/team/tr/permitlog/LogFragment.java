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

    // Object that keeps track of total time and total time during night:
    private ElapsedTime totalUpdater;

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
        totalUpdater = new ElapsedTime(userId, null);

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
            if (totalHoursField != null) totalHoursField.setValue(df.format(totalUpdater.totalTime / (1000.0 * 3600.0)));
            if (totalNightField != null) totalNightField.setValue(df.format(totalUpdater.nightTime / (1000.0 * 3600.0)));
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
                            "Month", "Date", "Year", "Duration", "Day/Night",
                            "Supervising Driver", "Age", "License Number"
                    };
                    for (int i = 0; i < headers.length; i++) {
                        // Put this cell in the top row and ith column:
                        Label headerCell = new Label(i, 0, headers[i]);
                        headerCell.setCellFormat(headerFormat);
                        sheet.addCell(headerCell);
                        // Set the column width according to the header:
                        sheet.setColumnView(i, Math.max(10, headers[i].length()+3));
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

                        // Get the driver info if possible:
                        String driverName = "DELETED DRIVER", driverAge = "DELETED", driverLicense = "DELETED DRIVER";
                        if (driversInfo.driverIds.contains(driverId)) {
                            int driverIndex = driversInfo.driverIds.indexOf(driverId);
                            DataSnapshot driverSnapshot = driversInfo.driverSnapshots.get(driverIndex);
                            if (DriverAdapter.hasCompleteName.accept(driverSnapshot)) driverName = driversInfo.driverNames.get(driverIndex);
                            if (driverSnapshot.hasChild("age")) driverAge = driverSnapshot.child("age").getValue().toString();
                            if (driverSnapshot.hasChild("license_number")) driverLicense = driverSnapshot.child("license_number").getValue().toString();
                        }

                        // Add it to the sheet:
                        Label nameCell = new Label(5, i+1, driverName);
                        sheet.addCell(nameCell);
                        Label ageCell = new Label(6, i+1, driverAge);
                        sheet.addCell(ageCell);
                        Label licenseCell = new Label(7, i+1, driverLicense);
                        sheet.addCell(licenseCell);
                    }

                    // Save and close the Workbook:
                    wb.write();
                    wb.close();
                } catch (IOException | WriteException e) {
                    Log.e(TAG, "While trying to make spreadsheet: " + e);
                    return;
                }

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/vnd.ms-excel");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(sheetFile));
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