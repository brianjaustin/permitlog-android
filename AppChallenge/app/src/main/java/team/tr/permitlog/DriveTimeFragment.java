package team.tr.permitlog;

import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

public class DriveTimeFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    //This creates a new dialog so that the user can pick the date:
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //Get the default time from the arguments:
        int hour = getArguments().getInt("hour");
        int minute = getArguments().getInt("minute");
        //Return a TimePickerDialog which has a default time of what was set in the constructor:
        return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hour, int minute) {
        //Call the setDate method on the activity so they can deal with the time:
        ((CustomDriveDialog)getActivity()).setTime(hour, minute);
    }
}
