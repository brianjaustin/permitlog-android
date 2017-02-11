package team.tr.permitlog;

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
        return FirebaseDatabase.getInstance().getReference().child(userId).child("times");
    }

    public static void callWithTotal(final IntToVoid funcObj) {
        /* Eventually calls funcObj.func(totalTime) where totalTime is the number of hours logged. */
        //Get the data:
        DatabaseReference timesRef = getTimesRef();
        timesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Accumulate seconds for accuracy:
                int totalTimeInSec = 0;
                //Loop through the children:
                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    //Find the time elapsed during the drive and add it to totalTimeSec:
                    totalTimeInSec +=
                            ((long)(logSnapshot.child("end").getValue())-(long)(logSnapshot.child("start").getValue()))/1000;
                }
                //Convert totalTimeInSec to hours and call funcObj.func:
                funcObj.func(totalTimeInSec/3600);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "While trying to get logs: "+databaseError.getMessage());
            }
        });
    }

    public static void callWithDay(final IntToVoid funcObj) {
        /* Eventually calls funcObj.func(totalTime) where totalTime is the number of hours logged during the day. */
        //Get the data:
        DatabaseReference timesRef = getTimesRef();
        timesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Accumulate seconds for accuracy:
                int dayTimeInSec = 0;
                //Loop through the children:
                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    //If this was during the day:
                    if (!((boolean) logSnapshot.child("night").getValue())) {
                        //Find the time elapsed during the drive and add it to totalTimeSec:
                        dayTimeInSec +=
                                ((long) (logSnapshot.child("end").getValue()) - (long) (logSnapshot.child("start").getValue())) / 1000;
                    }
                }
                //Convert dayTimeInSec to hours and call funcObj.func:
                funcObj.func(dayTimeInSec/3600);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "While trying to get logs: "+databaseError.getMessage());
            }
        });
    }

    public static void callWithNight(final IntToVoid funcObj) {
        /* Eventually calls funcObj.func(totalTime) where totalTime is the number of hours logged during the night. */
        //Get the data:
        DatabaseReference timesRef = getTimesRef();
        timesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Accumulate seconds for accuracy:
                int nightTimeInSec = 0;
                //Loop through the children:
                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    //If this was during the night:
                    if ((boolean) logSnapshot.child("night").getValue()) {
                        //Find the time elapsed during the drive and add it to totalTimeSec:
                        nightTimeInSec +=
                                ((long) (logSnapshot.child("end").getValue()) - (long) (logSnapshot.child("start").getValue())) / 1000;
                    }
                }
                //Convert nightTimeInSec to hours and call funcObj.func:
                funcObj.func(nightTimeInSec/3600);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "While trying to get logs: "+databaseError.getMessage());
            }
        });
    }
}
