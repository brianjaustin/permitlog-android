package team.tr.permitlog;

import android.app.Activity;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for creating Maine log in the background
 */
public class MaineLogTask extends GenericLogTask {
    private static final String TAG = "MaineLogTask";
    // Call the constructor from GenericLogTask
    MaineLogTask(Activity activity, ArrayList<DataSnapshot> snapshots, DriverAdapter adapter, ElapsedTime tracker) {
        super(activity, snapshots, adapter, tracker);
    }

    @Override
    public PDDocument doInBackground(Void... args) {
        // Stores all of the PDFs:
        ArrayList<PDDocument> logPages = new ArrayList<>();
        // Stores the current PDF form:
        PDAcroForm acroForm = null;
        // Keeps track of progress:
        int progress = 0;

        //Loop through all the logs:
        for (int i = 0; i < logSnapshots.size(); i++) {
            if (i % 50 == 0) { // Begin a new page for every 50 logs:
                //Get the activity, but exit if it is no longer valid:
                Activity activity = activityRef.get();
                if ((activity == null) || activity.isFinishing()) return null;

                // Use the utility method to create a new PDF and add it to logPages:
                acroForm = createNewPdf(activity, "maine_log.pdf", logPages);
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

            //Get the activity, but exit if it is no longer valid:
            Activity activity = activityRef.get();
            if ((activity == null) || activity.isFinishing()) return null;
            // Format the Date/time field
            String dateTimeString = DateUtils.formatDateTime(activity, startMillis, flags);

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
            progress += 1;
            publishProgress(progress);
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

        // Finally, merge all the documents together:
        PDDocument finalDocument = mergePDFs(logPages);
        // If merge fails, exit the method:
        if (finalDocument == null) return null;
        // Otherwise, increment the progress and return finalDocument:
        progress += 1;
        publishProgress(progress);
        return finalDocument;
    }
}
