package team.tr.permitlog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.firebase.database.DataSnapshot;
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract class representing an AsyncTask that generates log in background
 */
public abstract class GenericLogTask extends AsyncTask<Void,Integer,PDDocument> {
    private static final String TAG = "GenericLogTask";
    // Used to format numbers in logs:
    private static final DecimalFormat twoDecimalPlaces = new DecimalFormat("0.00");
    // Context used to create PDFs: (WeakReference helps prevent memory leaks)
    protected WeakReference<Activity> activityRef;
    // List of data snapshots from the log:
    protected ArrayList<DataSnapshot> logSnapshots;
    // Object used to get info about supervisors:
    protected DriverAdapter driversInfo;
    // Object that keeps track of total time achieved for each goals:
    protected ElapsedTime goalTracker;
    // Dialog that shows the task's progress:
    protected MaterialDialog proDialog;
    // Initialize the instance variables in constructor:
    GenericLogTask(Activity activity, ArrayList<DataSnapshot> snapshots, DriverAdapter adapter, ElapsedTime tracker) {
        activityRef = new WeakReference<>(activity);
        logSnapshots = snapshots;
        driversInfo = adapter;
        goalTracker = tracker;
    }

    @Override
    public void onPreExecute() {
        // Get the activity, but return if it is no longer valid:
        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Start the progress dialog and save it in proDialog:
        proDialog = new MaterialDialog.Builder(activity)
                .title("Generating PDF Log")
                .content("Please wait. A dialog asking you to send the driving log to another app will appear when the log is ready.")
                .progress(false, logSnapshots.size()+1)
                .canceledOnTouchOutside(false)
                .show();
    }

    @Override
    public void onProgressUpdate(Integer... progressParams) {
        proDialog.setProgress(progressParams[0]); //Update proDialog
    }

    // Helper method that gets the elapsed time in a drive:
    public String getElapsedTime(DataSnapshot logSnapshot) {
        long timeElapsed = (long) (logSnapshot.child("end").getValue()) - (long) (logSnapshot.child("start").getValue());
        double hoursElapsed = (double) timeElapsed / (1000.0 * 3600.0);
        return twoDecimalPlaces.format(hoursElapsed);
    }

    // Helper method that gets info about driver:
    public List<String> getDriverInfo(DataSnapshot logSnapshot) {
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
                driverAge += driverSnapshot.child("age").getValue().toString();
            // Get the driver's license number
            if (driverSnapshot.hasChild("license_number"))
                driverLicense = driverSnapshot.child("license_number").getValue().toString();
        }
        //Return the info:
        return Arrays.asList(driverName, driverAge, driverLicense);
    }

    // Helper method that adds a document to pdfList and returns a pdf form:
    public PDAcroForm createNewPdf(Context context, String pdfName, List<PDDocument> pdfList) {
        // Create a new PDF:
        PDDocument pdfDocument;
        try {
            pdfDocument = PDDocument.load(context.getAssets().open(pdfName));
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

    // Helper method that merges a bunch of PDFs into one:
    public PDDocument mergePDFs(List<PDDocument> pdfList) {
        PDDocument totalDocument = new PDDocument();
        PDFMergerUtility mt = new PDFMergerUtility();
        // Loop through the list and add all documents to totalDocument:
        for (PDDocument document : pdfList) {
            try {
                mt.appendDocument(totalDocument, document);
                //Close the old documents since they are merged in totalDocument
                document.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                return null;
            }
        }
        //Finally, return the merged PDF:
        return totalDocument;
    }

    public String getGoalTotal(String goalType) {
        //Convert goal to hours and format with two decimal places:
        return twoDecimalPlaces.format(goalTracker.timeTracker.getTime(goalType) / (1000.0 * 3600.0));
    }

    @Override
    public void onPostExecute(PDDocument document) {
        // Get the activity, but don't do anything if it's invalid
        Activity activity = activityRef.get();
        if ((activity == null) || activity.isFinishing()) {
            Log.d(TAG, "Could not find activity after generating log");
            return;
        }
        // Show error message if doInBackground() failed
        if (document == null) {
            Toast.makeText(activity, R.string.create_pdf_error, Toast.LENGTH_SHORT).show();
            return;
        }

        // If we can write to external storage, save the PDF:
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "log.pdf");
            try {
                //Save and close the PDF file:
                document.save(file);
                document.close();
            } catch (IOException e) {
                // When there is an error, log it and notify the user:
                Log.e(TAG, "Error while saving PDF: "+e.getMessage());
                Toast.makeText(activity, R.string.save_pdf_error, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                //Get rid of the progress bar now that we're finished:
                proDialog.dismiss();
            } catch (Exception e) {
                //This might throw some weird error, but it doesn't affect anything else, so it's OK:
                Log.d(TAG, "Error when closing progress dialog: "+e.getMessage());
            }

            // Send the PDF file to the user
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            Uri fileUri = FileProvider.getUriForFile(activity, activity.getString(R.string.file_provider_authority), file);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);

            /*// This is for debugging the different kind of intents that can handle PDFs:
            Log.d(TAG, "Listing intents for PDF:");
            for(ResolveInfo info : getContext().getPackageManager().queryIntentActivities(intent,PackageManager.MATCH_ALL)){
                Log.d(TAG, "Intent: " + info.toString());
            } // */

            try {
                //Show the user the dialog of apps that can handle PDFs:
                activity.startActivity(Intent.createChooser(intent, "Send Driving Log"));
            } catch (android.content.ActivityNotFoundException exception) {
                // There is no app installed that can send this, so show an error:
                Toast.makeText(activity, R.string.export_pdf_error, Toast.LENGTH_LONG).show();
            }
        }
        // Otherwise, show the user an error message:
        else {
            Toast.makeText(activity, R.string.save_pdf_error, Toast.LENGTH_SHORT).show();
            try {
                //Don't forget to close the PDF file:
                document.close();
            } catch (IOException e) {
                Log.e(TAG, "Error when closing document: "+e.getMessage());
            }
        }
    }

    /* private void printAllFields(PDAcroForm acroForm) {
        //Helper method that lists all of the field names in an acroForm:
        List<PDField> allFields = acroForm.getFields();
        for (PDField field : allFields) {
            Log.d(TAG, field.getFullyQualifiedName());
        }
    } // */
}
