package team.tr.permitlog;

import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.DatePicker;

public class DriveDateFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    //This creates a new dialog so that the user can pick the date:
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //Get the default date from the arguments:
        int year = getArguments().getInt("year");
        int month = getArguments().getInt("month");
        int day = getArguments().getInt("day");
        //Return a DatePickerDialog which has a default date of today:
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        //Call the setDate method on the activity so they can deal with the date:
        ((CustomDriveDialog)getActivity()).setDate(year, month, day);
    }
}