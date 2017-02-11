package team.tr.permitlog;

import android.text.format.DateUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ElapsedTime {
    //For logging:
    public static String TAG = "ElapsedTime";

    //This function gets the /times reference from the user's Firebase data:
    private static DatabaseReference getTimesRef() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return FirebaseHelper.getDatabase().getReference().child(userId).child("times");
    }

    public static String formatSeconds(long seconds) {
        /* Adds "0:" to DateUtils.formatElapsedTime() if seconds < 3600. */
        String secondsString = DateUtils.formatElapsedTime(seconds);
        //If the time is less than an hour, then add "0:" to the beginning:
        if (seconds < 3600) secondsString = "0:"+secondsString;
        return secondsString;
    }

    public static void callWithTotal(final LongConsumer funcObj) {
        /* Eventually calls funcObj.accept(totalTime) where totalTime is the number of milliseconds logged. */
        //Get the data:
        DatabaseReference timesRef = getTimesRef();
        timesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //This is the total:
                long totalTime = 0;
                //Loop through the children:
                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    //Find the time elapsed during the drive and add it to the total:
                    totalTime += (long)(logSnapshot.child("end").getValue())-(long)(logSnapshot.child("start").getValue());
                }
                //Call the callback:
                funcObj.accept(totalTime);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "While trying to get logs: "+databaseError.getMessage());
            }
        });
    }

    public static void callWithDay(final LongConsumer funcObj) {
        /* Eventually calls funcObj.accept(dayTime) where dayTime is the number of milliseconds logged during the day. */
        //Get the data:
        DatabaseReference timesRef = getTimesRef();
        timesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //This is the total:
                long dayTime = 0;
                //Loop through the children:
                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    //If this was during the day:
                    if (!((boolean) logSnapshot.child("night").getValue())) {
                        //Find the time elapsed during the drive and add it to the total:
                        dayTime += (long)(logSnapshot.child("end").getValue())-(long) (logSnapshot.child("start").getValue());
                    }
                }
                //Call the callback:
                funcObj.accept(dayTime);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "While trying to get logs: "+databaseError.getMessage());
            }
        });
    }

    public static void callWithNight(final LongConsumer funcObj) {
        /* Eventually calls funcObj.accept(nightTime) where nightTime is the number of milliseconds logged during the night. */
        //Get the data:
        DatabaseReference timesRef = getTimesRef();
        timesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //This is the total:
                long nightTime = 0;
                //Loop through the children:
                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    //If this was during the night:
                    if ((boolean) logSnapshot.child("night").getValue()) {
                        //Find the time elapsed during the drive and add it to the total:
                        nightTime += (long)(logSnapshot.child("end").getValue())-(long)(logSnapshot.child("start").getValue());
                    }
                }
                //Call the callback:
                funcObj.accept(nightTime);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "While trying to get logs: "+databaseError.getMessage());
            }
        });
    }
}
