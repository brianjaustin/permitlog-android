package team.tr.permitlog;

import android.text.format.DateUtils;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ElapsedTime {
    //For logging:
    public static String TAG = "ElapsedTime";

    public static String formatSeconds(long seconds) {
        /* Adds "0:" to DateUtils.formatElapsedTime() if seconds < 3600. */
        String secondsString = DateUtils.formatElapsedTime(seconds);
        //If the time is less than an hour, then add "0:" to the beginning:
        if (seconds < 3600) secondsString = "0:"+secondsString;
        return secondsString;
    }

    public static boolean validLog(DataSnapshot dataSnapshot) {
        /* Returns if dataSnapshot represents a valid log. */
        return dataSnapshot.hasChild("start") && dataSnapshot.hasChild("end")
                && dataSnapshot.hasChild("night") && dataSnapshot.hasChild("driver_id");
    }

    //Firebase reference:
    private static DatabaseReference timesRef;

    public static ValueEventListener startListening(String userId, final TriLongConsumer callback) {
        //Get the DatabaseReference if it hasn't been initialized yet:
        if (timesRef == null) timesRef = FirebaseDatabase.getInstance().getReference().child(userId).child("times");
        //Create the listener:
        ValueEventListener timesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //This are the totals:
                long totalTime = 0, dayTime = 0, nightTime = 0;
                //Loop through the children that are valid logs:
                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) if (validLog(logSnapshot)) {
                    //Find the time elapsed during the drive and add it to the total:
                    long timeElapsed = (long)(logSnapshot.child("end").getValue())-(long)(logSnapshot.child("start").getValue());
                    totalTime += timeElapsed;
                    //If this was during the night, add it to the night total:
                    if ((boolean) logSnapshot.child("night").getValue()) nightTime += timeElapsed;
                        //Otherwise, add it to the day total:
                    else dayTime += timeElapsed;
                }
                //Call the callbacks:
                callback.accept(totalTime, dayTime, nightTime);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "While trying to get logs: "+databaseError.getMessage());
            }
        };
        //Start listening:
        timesRef.addValueEventListener(timesListener);
        //Finally, return the timesListener:
        return timesListener;
    }

    public static void stopListening(ValueEventListener timesListener) {
        //Stop listening if possible:
        if (timesRef != null) timesRef.removeEventListener(timesListener);
    }
}
