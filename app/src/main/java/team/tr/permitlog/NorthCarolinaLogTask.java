package team.tr.permitlog;

import android.app.Activity;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class that helps create North Carolina log in the background.
 */
public class NorthCarolinaLogTask extends GenericLogTask {
    private static final String TAG = "MaineLogTask";
    // Call the constructor from GenericLogTask
    NorthCarolinaLogTask(Activity activity, ArrayList<DataSnapshot> snapshots, DriverAdapter adapter, ElapsedTime tracker) {
        super(activity, snapshots, adapter, tracker);
    }

    //These are the field indexes of the back page of the North Carolina form:
    private int ncBackPage[] = {
            27, 28, 29, 30, 31, 32,
            33, 34, 35, 36, 48, 49,
            44, 47, 50, 51, 52, 53,
            45, 54, 55, 56, 57, 58,
            46, 59, 60, 61, 62, 63,
            64, 65, 66, 67, 68, 69,
            70, 71, 74, 75, 76, 77,
            72, 73, 78, 79, 80, 81,
            82, 83, 84, 85, 86, 87,
            88, 89, 90, 91, 92, 93,
            94, 95, 96, 97, 98, 99,
            100, 101, 102, 103, 104, 105,
            106, 107, 108, 109, 110, 111,
            112, 113, 114, 115, 116, 117,
            118, 119, 120, 121, 122, 123,
            124, 125, 126, 127, 128, 129,
            130, 131, 132, 133, 134, 135,
            136, 137, 138, 139, 140, 141,
            142, 143, 144, 145, 146, 147,
            148, 149, 150, 151, 152, 153,
            154, 155, 156, 157, 158, 159,
            160, 161, 162, 163, 164, 165,
            166, 167, 168, 169, 170, 171,
            172, 173, 174, 175, 176, 177,
            178, 179, 180, 181, 182, 183,
            184, 185, 186, 187, 188, 189,
            190, 191, 192, 193, 194, 195,
            196, 197, 198, 199, 200, 201,
            202, 203, 204, 205, 206, 207,
            208, 209, 210, 211, 212, 213,
            214, 215, 216, 217, 218, 219,
            220, 221, 222, 223, 224, 225,
            226, 227, 228, 229, 230, 231,
            232, 233, 234, 235, 236, 237,
            238, 239, 240, 241, 242, 243,
            244, 245, 246, 247, 248, 249,
            250, 251, 252, 253, 254, 255,
            256, 257, 258, 259, 260, 261,
            262, 263, 264, 265, 266, 267,
            38, 39, 40, 41, 42, 43
    };

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
            if (i % 60 == 0) { // Begin a new page for every 60 logs:
                //Get the activity, but exit if it is no longer valid:
                Activity activity = activityRef.get();
                if ((activity == null) || activity.isFinishing()) return null;
                // Use the utility method to create new PDF and add it to logPages:
                acroForm = createNewPdf(activity, "north_carolina_log.pdf", logPages);
                //Abort if acroForm is null:
                if (acroForm == null) return null;
            }

            // Get the log info:
            DataSnapshot logSnapshot = logSnapshots.get(i);
            long startMillis = (long) logSnapshot.child("start").getValue();
            //long endMillis = (long) logSnapshot.child("end").getValue();

            //Get the activity, but exit if it is no longer valid:
            Activity activity = activityRef.get();
            if ((activity == null) || activity.isFinishing()) return null;
            DateFormat shortDate = android.text.format.DateFormat.getDateFormat(activity);
            // Format the Date field
            String dateString = shortDate.format(new Date(startMillis));
            // Format the time field
            String timeString = DateUtils.formatDateTime(activity, startMillis, DateUtils.FORMAT_SHOW_TIME);

            // Get the elapsed time
            String elapsedTimeString = getElapsedTime(logSnapshot);

            // Get info about the drivers:
            List<String> driverInfo = getDriverInfo(logSnapshot);
            // Extract the necessary info:
            String driverName = driverInfo.get(0);
            String driverLicense = driverInfo.get(2);

            //Put all of this info into the form:
            try {
                int curRow = (i % 60)+1;
                PDTextField dateField = null, timeOfDayField = null,
                        timeOfNightField = null, elapsedTimeField = null,
                        driverNameField = null, driverLicenseField = null;
                //For the front page, use the Row suffix to get the fields:
                if (curRow <= 20) {
                    dateField = (PDTextField) acroForm.getField("DATERow" + curRow);
                    timeOfDayField = (PDTextField) acroForm.getField("TIME OF DAYRow" + curRow);
                    timeOfNightField = (PDTextField) acroForm.getField("TIME OF NIGHTRow" + curRow);
                    //Yes, this is quite an odd name for a field, but that's what the actual field name is:
                    elapsedTimeField = (PDTextField) acroForm.getField("fill_" + (6 * curRow + 2));
                    driverNameField = (PDTextField) acroForm.getField("SUPERVISING DRIVERS PRINTED NAMERow" + curRow);
                    driverLicenseField = (PDTextField) acroForm.getField("SUPERVISING DRIVERS DL Number and StateRow" + curRow);
                }
                //For the back page, use the Text suffix to get the fields:
                else {
                    dateField = (PDTextField) acroForm.getField("Text"+ncBackPage[(curRow-21)*6]);
                    timeOfDayField = (PDTextField) acroForm.getField("Text"+ncBackPage[(curRow-21)*6+1]);
                    timeOfNightField = (PDTextField) acroForm.getField("Text"+ncBackPage[(curRow-21)*6+2]);
                    elapsedTimeField = (PDTextField) acroForm.getField("Text"+ncBackPage[(curRow-21)*6+3]);
                    driverNameField = (PDTextField) acroForm.getField("Text"+ncBackPage[(curRow-21)*6+4]);
                    driverLicenseField = (PDTextField) acroForm.getField("Text"+ncBackPage[(curRow-21)*6+5]);
                }

                //For the time, put the time in the day/night box depending on if the log was at day/night
                if ((boolean) logSnapshot.child("night").getValue()) {
                    if (timeOfNightField != null) timeOfNightField.setValue(timeString);
                } else {
                    if (timeOfDayField != null) timeOfDayField.setValue(timeString);
                }
                //Finally fill the rest of the fields in:
                if (dateField != null) dateField.setValue(dateString);
                if (elapsedTimeField != null) elapsedTimeField.setValue(elapsedTimeString);
                if (driverNameField != null) driverNameField.setValue(driverName);
                if (driverLicenseField != null) driverLicenseField.setValue(driverLicense);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            //After each log is finished, increment the progress bar:
            progress += 1;
            publishProgress(progress);
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

        // Finally, merge all the documents together:
        PDDocument finalDocument = mergePDFs(logPages);
        // If merge fails, exit the method:
        if (finalDocument == null) return null;
        // Otherwise, increment the progress and return finalDocument:
        // Otherwise, increment the progress and return finalDocument:
        progress += 1;
        publishProgress(progress);
        return finalDocument;
    }
}