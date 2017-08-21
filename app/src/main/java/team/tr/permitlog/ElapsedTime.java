package team.tr.permitlog;

import android.text.format.DateUtils;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ElapsedTime {
    //For logging:
    public static String TAG = "ElapsedTime";

    public static String formatSeconds(long seconds) {
        /* Takes number of seconds and returns a String with format HH:MM. */
        String timeString = DateUtils.formatElapsedTime(seconds);
        //If the time is less than an hour, then add "00:" to the beginning:
        if (seconds < 3600) timeString = "00:" + timeString;
        //Otherwise, if the time is less than 10 hours, then add "0" to the beginning:
        else if (seconds < 36000) timeString = "0" + timeString;
        //Removes seconds from time, because it's unneeded information
        return timeString.substring(0, timeString.length()-3);
    }

    //Firebase reference:
    private DatabaseReference timesRef;
    //Store the logs' IDs, duration, and whether they were during the day or night:
    private ArrayList<String> logIds = new ArrayList<>();
    private ArrayList<Long> logDurations = new ArrayList<>();
    private ArrayList<Boolean> logsAtNight = new ArrayList<>();
    private ArrayList<Boolean> logsBadWeather = new ArrayList<>();
    private ArrayList<Boolean> logsAdverse = new ArrayList<>();
    //Store the total times for overall and different categories (i.e. day, night,
    public DrivingTimes timeTracker = new DrivingTimes();
    //This is the callback called whenever there is a change to the above variables:
    private DrivingTimesConsumer callback;

    //Firebase listener:
    private ChildEventListener timesListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //Add the data to logDurations and logsAtNight:
            long duration = (long)dataSnapshot.child("end").getValue() - (long)dataSnapshot.child("start").getValue();
            logDurations.add(duration);
            //Add whether or not it is night/weather/adverse to the appropriate conditions:
            boolean night = (boolean)dataSnapshot.child("night").getValue();
            logsAtNight.add(night);
            //For weather and adverse, outdated users may not have this property, so make sure to use .hasChild()
            boolean weather = dataSnapshot.hasChild("weather") && (boolean)dataSnapshot.child("weather").getValue();
            logsBadWeather.add(weather);
            boolean adverse = dataSnapshot.hasChild("adverse") && (boolean)dataSnapshot.child("adverse").getValue();
            logsAdverse.add(adverse);
            //Update the time totals:
            timeTracker.total += duration;
            if (night) timeTracker.night += duration;
            else timeTracker.day += duration;
            if (weather) timeTracker.weather += duration;
            if (adverse) timeTracker.adverse += duration;
            //Call the callback:
            if (callback != null) callback.accept(timeTracker);
        }
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            //Find the snapshot in logIds:
            int logIndex = logIds.indexOf(dataSnapshot.getKey());
            //Subtract totalTime and dayTime/nightTime by the time specified in logDurations:
            timeTracker.total -= logDurations.get(logIndex);
            if (logsAtNight.get(logIndex)) timeTracker.night -= logDurations.get(logIndex);
            else timeTracker.day -= logDurations.get(logIndex);
            if (logsBadWeather.get(logIndex)) timeTracker.weather -= logDurations.get(logIndex);
            if (logsAdverse.get(logIndex)) timeTracker.adverse -= logDurations.get(logIndex);
            //Update the data in logDurations and logsAtNight:
            long duration = (long)dataSnapshot.child("end").getValue() - (long)dataSnapshot.child("start").getValue();
            logDurations.set(logIndex, duration);
            boolean atNight = (boolean)dataSnapshot.child("night").getValue();
            //For weather and adverse, outdated users may not have this property, so make sure to use .hasChild()
            boolean weather = dataSnapshot.hasChild("weather") && (boolean)dataSnapshot.child("weather").getValue();
            boolean adverse = dataSnapshot.hasChild("adverse") && (boolean)dataSnapshot.child("adverse").getValue();
            logsAtNight.set(logIndex, atNight);
            logsBadWeather.set(logIndex, weather);
            logsAdverse.set(logIndex, adverse);
            //Update the time totals:
            timeTracker.total += duration;
            if (atNight) timeTracker.night += duration;
            else timeTracker.day += duration;
            if (weather) timeTracker.weather += duration;
            if (adverse) timeTracker.adverse += duration;
            //Call the callback:
            if (callback != null) callback.accept(timeTracker);
        }
        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            //Find the snapshot in logIds:
            int logIndex = logIds.indexOf(dataSnapshot.getKey());
            //Subtract the time totals by the time specified in logDurations:
            timeTracker.total -= logDurations.get(logIndex);
            if (logsAtNight.get(logIndex)) timeTracker.night -= logDurations.get(logIndex);
            else timeTracker.day -= logDurations.get(logIndex);
            if (logsBadWeather.get(logIndex)) timeTracker.weather -= logDurations.get(logIndex);
            if (logsAdverse.get(logIndex)) timeTracker.adverse -= logDurations.get(logIndex);
            //Remove the data from logDurations and logsAtNight:
            logDurations.remove(logIndex);
            logsAtNight.remove(logIndex);
            logsBadWeather.remove(logIndex);
            logsAdverse.remove(logIndex);
            //Call the callback:
            if (callback != null) callback.accept(timeTracker);
        }
        // The following must be implemented in order to complete the abstract class:
        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.e(TAG, "While trying to get logs: "+databaseError.getMessage());
        }
    };

    public ElapsedTime(String userId, DrivingTimesConsumer callback) {
        //Get the DatabaseReference:
        timesRef = FirebaseDatabase.getInstance().getReference().child(userId).child("times");
        //Set the callback:
        this.callback = callback;
        //Transform the listener so that it only listens for complete logs:
        timesListener = FirebaseHelper.transformListener(timesListener, LogFragment.validLog, logIds);
        //Start listening:
        timesRef.addChildEventListener(timesListener);
    }

    public void stopListening() {
        //Stop listening:
        timesRef.removeEventListener(timesListener);
    }
}
