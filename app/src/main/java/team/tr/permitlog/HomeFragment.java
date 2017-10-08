package team.tr.permitlog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;
import com.google.android.gms.ads.VideoOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment {
    //The root view for this fragment, used to find elements by id:
    private View rootView;

    private final String TAG = "HomeFragment";
    private String userId;
    // Have we shown the tutorial yet?
    private boolean shownTutorial = false;
    // Are we currently showing the tutorial?
    private boolean showingTutorial = false;

    // Store drive start/stop times
    private Calendar startingTime = Calendar.getInstance();
    private Calendar endingTime = Calendar.getInstance();

    // Start/stop buttons
    private Button startButton;
    private Button stopButton;

    // Variables to update the drive_time label
    private String formattedTime;
    private Timer timer;

    // The driver spinner:
    private Spinner driversSpinner;
    // Position of spinner as set in Bundle:
    private int spinnerPosition;
    // Object that holds all data relevant to the driver spinner:
    private DriverAdapter spinnerData;

    // This holds the user's goals:
    private long totalGoal, dayGoal, nightGoal, weatherGoal, adverseGoal;
    // TextViews:
    private TextView totalTimeView, dayTimeView, nightTimeView, weatherTimeView, adverseTimeView;
    // Object that updates the time totals:
    private ElapsedTime timeUpdater;
    // Firebase reference for goals and ongoing drive:
    private DatabaseReference goalsRef, ongoingRef;
    // Callback for timesListener:
    private DrivingTimesConsumer timeCallback = new DrivingTimesConsumer() {
        @Override public void accept(DrivingTimes timeObj) {
            // Update the "Time Completed" section:
            updateGoalTextViews();
        }
    };

    //Called when the user rotates the screen:
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save values so they can be restored after the screen finishes rotating:
        saveToBundle(outState);
    }

    @Override
    public void onPause() {
        //If we are not showing the tutorial and the stop button is enabled,
        //then there is an ongoing drive that is being paused:
        if (!showingTutorial && stopButton.isEnabled()) {
            //Stop the timer since we will restart it once the app resumes:
            timer.cancel();
            //Just set the starting time:
            ongoingRef.child("start").setValue(startingTime.getTimeInMillis());
        }
        super.onPause();
    }

    private void saveToBundle(Bundle outState) {
        /* Takes Bundle and sets info about autodrive in Bundle. */
        outState.putInt("spinnerPosition", driversSpinner.getSelectedItemPosition());
        outState.putBoolean("showingTutorial", showingTutorial);
    }

    private void loadFromBundle(Bundle savedInstanceState) {
        /* Takes Bundle and sets info about autodrive from Bundle. */
        showingTutorial = savedInstanceState.getBoolean("showingTutorial");
        spinnerPosition = savedInstanceState.getInt("spinnerPosition");
        // Set spinnerPosition if possible:
        if (spinnerData.driverIds.size() > spinnerPosition) driversSpinner.setSelection(spinnerPosition);
            // Otherwise, keep listening to the adapter and set it when possible:
        else spinnerData.driversAdapter.registerDataSetObserver(setSpinnerPosition);
    }

    @Override
    public void onResume() {
        super.onResume();
        ongoingRef.addListenerForSingleValueEvent(resumeIncompleteDrive);
    }

    private ValueEventListener resumeIncompleteDrive = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            //If there is an incomplete drive:
            if (dataSnapshot.exists()) {
                Log.d(TAG, "There's an incomplete drive in Firebase!");
                //Get the starting time from the incomplete value:
                startingTime.setTimeInMillis((long)dataSnapshot.child("start").getValue());
                //If the user has completed the ongoing drive and just needs to go to the dialog
                if (dataSnapshot.hasChild("end")) {
                    //Get the ending time:
                    endingTime.setTimeInMillis((long)dataSnapshot.child("end").getValue());
                    //Show the dialog using the driver ID:
                    showDialog(dataSnapshot.child("driver_id").getValue().toString());
                } else { //Otherwise, if the drive is still ongoing:
                    //Enable the start button and update the timer label:
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    timerUpdateLabel();
                }
            }
        }

        //Empty method needed to complete abstract class
        @Override
        public void onCancelled(DatabaseError databaseError) {}
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LayoutInflater lf = getActivity().getLayoutInflater();
        //pass the correct layout name for the fragment
        rootView = lf.inflate(R.layout.fragment_home, container, false);

        // Set start drive button click
        startButton = (Button) rootView.findViewById(R.id.start_drive);
        startButton.setOnClickListener(onStartDrive);

        // Set stop drive button click
        stopButton = (Button) rootView.findViewById(R.id.stop_drive);
        stopButton.setOnClickListener(onStopDrive);

        // Set add drive button click
        FloatingActionButton addDrive = (FloatingActionButton) rootView.findViewById(R.id.add_drive);
        addDrive.setOnClickListener(onAddDrive);

        // Set add driver button click
        FloatingActionButton addDriver = (FloatingActionButton) rootView.findViewById(R.id.add_driver);
        addDriver.setOnClickListener(onAddDriver);

        // Get the drivers spinner:
        driversSpinner = (Spinner) rootView.findViewById(R.id.drivers_spinner);
        // Get the UID:
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Add the items to the spinner:
        spinnerData = new DriverAdapter(getActivity(), userId, android.R.layout.simple_spinner_item);
        driversSpinner.setAdapter(spinnerData.driversAdapter);

        // Initialize the TextViews:
        totalTimeView=(TextView)rootView.findViewById(R.id.time_elapsed);
        dayTimeView=(TextView)rootView.findViewById(R.id.day_elapsed);
        nightTimeView=(TextView)rootView.findViewById(R.id.night_elapsed);
        weatherTimeView=(TextView)rootView.findViewById(R.id.weather_elapsed);
        adverseTimeView=(TextView)rootView.findViewById(R.id.adverse_elapsed);

        // Start listening to logs:
        timeUpdater = new ElapsedTime(userId, timeCallback);
        // Get data about the goals, if available:
        goalsRef = FirebaseDatabase.getInstance().getReference().child(userId).child("goals");
        goalsRef.addValueEventListener(goalsListener);
        // Get reference to ongoing drive:
        ongoingRef = FirebaseDatabase.getInstance().getReference().child(userId).child("ongoing");

        //Initialize the ad:
        final NativeExpressAdView adView = new NativeExpressAdView(getContext());
        adView.setAdUnitId(getString(R.string.ad_unit_id));
        adView.setVideoOptions(new VideoOptions.Builder()
                .setStartMuted(true)
                .build());
        //Center the ad horizontally:
        LinearLayout.LayoutParams adParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        adParams.gravity = Gravity.CENTER_HORIZONTAL;
        adView.setLayoutParams(adParams);
        //Initialize the ad request:
        final AdRequest request = new AdRequest.Builder()
                .addTestDevice("588673E3A47A5C68AD8CD4FE6FA5A4ED")
                .build();

        //Get the LinearLayout container
        final LinearLayout adContainer = (LinearLayout)rootView.findViewById(R.id.home_container);
        //Get displayMetrics in order to convert pixels to dp:
        final DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        //Wait for the LinearLayout to load in order to get its width:
        adContainer.post(new Runnable() {
            @Override
            public void run() {
                //Get the width of the LinearLayout in dp:
                int adContainerWidthDp = (int)(adContainer.getWidth()/displayMetrics.density);
                //Set the ad size to take up the whole LinearLayout, but also be within 280 and 1200 in order to meet the small template:
                adView.setAdSize(new AdSize(Math.min(1200, Math.max(280, adContainerWidthDp)), 100));
                //Add the ad to the screen and load the request:
                adContainer.addView(adView);
                adView.loadAd(request);
            }
        });

        // Get the values from rotate, if possible:
        if (savedInstanceState != null) loadFromBundle(savedInstanceState);
        // Otherwise, get values from last onDestroyView(), if possible:
        else {
            Bundle args = getArguments();
            if (args != null) loadFromBundle(args);
        }
        return rootView;
    }

    private DataSetObserver setSpinnerPosition = new DataSetObserver() { @Override public void onChanged() {
        // Set spinnerPosition if possible:
        if (spinnerData.driverIds.size() > spinnerPosition) {
            driversSpinner.setSelection(spinnerPosition);
            // Stop listening to data changes once we've done this:
            spinnerData.driversAdapter.unregisterDataSetObserver(setSpinnerPosition);
        }
    } };

    private void timerUpdateLabel() {
        // Get the time label:
        final TextView driveTime = (TextView) rootView.findViewById(R.id.drive_time);

        timer = new Timer();
        final String timerStr = timer.toString();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Get the difference between the start and current time in seconds
                long timeDiff = ((new Date()).getTime()-startingTime.getTimeInMillis())/1000;
                // Format this time appropriately:
                formattedTime = DateUtils.formatElapsedTime(timeDiff);
                // Remember to add hours:
                if (timeDiff < 3600) formattedTime = "0:"+formattedTime;
                // Update the label in the UI thread using post():
                driveTime.post(new Runnable() {
                    @Override
                    public void run() {
                        driveTime.setText(formattedTime);
                    }
                });
            }
        }, 0, 1000);
    }

    private ValueEventListener goalsListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // If they have not seen the tutorial, assume they are a new user, so show the tutorial:
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            if (!shownTutorial && prefs.getBoolean("tutorial", true)) showTutorial();

            // Set totalGoal, or set it to 0 if not present:
            if (dataSnapshot.hasChild("total")) totalGoal = (long)dataSnapshot.child("total").getValue();
            else totalGoal = 0;
            // Do the same for day and night:
            if (dataSnapshot.hasChild("day")) dayGoal = (long)dataSnapshot.child("day").getValue();
            else dayGoal = 0;
            if (dataSnapshot.hasChild("night")) nightGoal = (long)dataSnapshot.child("night").getValue();
            else nightGoal = 0;
            if (dataSnapshot.hasChild("weather")) weatherGoal = (long)dataSnapshot.child("weather").getValue();
            else weatherGoal = 0;
            if (dataSnapshot.hasChild("adverse")) adverseGoal = (long)dataSnapshot.child("adverse").getValue();
            else adverseGoal = 0;
            // Update the "Time Completed" section
            updateGoalTextViews();

            //If the user does not have a state yet but they have some goal:
            if (!dataSnapshot.hasChild("stateName") &&
                ((totalGoal != 0) || (dayGoal != 0) || (nightGoal != 0) || (weatherGoal != 0) || (adverseGoal != 0))) {
                Log.d(TAG, "Adding stateName for this user");
                //If the user's goals match the state of Maine:
                if ((totalGoal == 70) && (dayGoal == 0) && (nightGoal == 10) && (weatherGoal == 0) && (adverseGoal == 0)) {
                    //Make the state Maine and set the needsForm flag:
                    goalsRef.child("stateName").setValue("Maine");
                    goalsRef.child("needsForm").setValue(true);
                }
                //Otherwise, make the state Custom and unset the needsForm flag:
                else {
                    goalsRef.child("stateName").setValue("Custom");
                    goalsRef.child("needsForm").setValue(false);
                }
            }
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.e(TAG, "While trying to start settings: "+databaseError.getMessage());
        }
    };

    private void showTutorial() {
        // Set showing and shownTutorial:
        showingTutorial = shownTutorial = true;

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500); // half second between each showcase view
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity());
        sequence.setConfig(config);

        //Adds each screen to the sequence, then runs it
        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                    .setDismissText("OK")
                    .setContentText(R.string.tutorial_text_intro)
                    .setTarget(rootView.findViewById(R.id.FAB_image_view))
                    .withoutShape()
                    .setDismissOnTouch(true)
                    .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(rootView.findViewById(R.id.FAB_image_view))
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_text_fam)
                        .setDismissOnTouch(true)
                        .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(rootView.findViewById(R.id.drivers_spinner))
                        .withRectangleShape()
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_text_spinner)
                        .setDismissOnTouch(true)
                        .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(rootView.findViewById(R.id.start_drive))
                        .withRectangleShape()
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_text_start)
                        .setDismissOnTouch(true)
                        .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(rootView.findViewById(R.id.stop_drive))
                        .withRectangleShape()
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_text_stop)
                        .setDismissOnTouch(true)
                        .setListener(new IShowcaseListener() {
                            //Enable the stop button while showcasing it:
                            @Override
                            public void onShowcaseDisplayed(MaterialShowcaseView materialShowcaseView) {
                                stopButton.setEnabled(true);
                                startButton.setEnabled(false);
                            }
                            //Disable it when finished:
                            @Override
                            public void onShowcaseDismissed(MaterialShowcaseView materialShowcaseView) {
                                stopButton.setEnabled(false);
                                startButton.setEnabled(true);
                            }
                        })
                        .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_text_menu)
                        .setTarget(rootView.findViewById(R.id.FAB_image_view))
                        .withoutShape()
                        .setDismissOnTouch(true)
                        .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_redirect)
                        .setTarget(rootView.findViewById(R.id.FAB_image_view))
                        .withoutShape()
                        .setDismissOnTouch(true)
                        .setListener(new IShowcaseListener() {
                            //Necessary to complete abstract class:
                            @Override
                            public void onShowcaseDisplayed(MaterialShowcaseView materialShowcaseView) {}
                            //Once the tutorial is over:
                            @Override
                            public void onShowcaseDismissed(MaterialShowcaseView materialShowcaseView) {
                                // The tutorial is over, so unset showingTutorial:
                                showingTutorial = false;
                                // Make sure this isn't shown again on this device (in case the user does not set goals)
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("tutorial", false);
                                editor.commit();
                                // Transition to the goals:
                                MainActivity curActivity = (MainActivity)getActivity();
                                curActivity.transitionFragment(curActivity.GOALS_MENU_INDEX);
                            }
                        })
                        .build());
        sequence.start();
    }


    public void updateGoalTextViews() {
        // Pads numbers with "0" if they are only one digit:
        DecimalFormat padder = new DecimalFormat("00");
        // Convert the time to seconds and format appropriately:
        String totalTimeStr = ElapsedTime.formatSeconds(timeUpdater.timeTracker.total/1000);
        // Show the goal if it is nonzero:
        Log.d(TAG, "updateGoal totalGoal: " + String.valueOf(totalGoal));
        if (totalGoal == 0) totalTimeView.setVisibility(View.GONE);
        else {
            totalTimeView.setVisibility(View.VISIBLE);
            totalTimeView.setText("Total: " + totalTimeStr + "/" + padder.format(totalGoal) + ":00");
        }
        // Do the same for day and night:
        String dayTimeStr = ElapsedTime.formatSeconds(timeUpdater.timeTracker.day/1000);
        if (dayGoal == 0) dayTimeView.setVisibility(View.GONE);
        else{
            dayTimeView.setVisibility(View.VISIBLE);
            dayTimeView.setText("Day: "+dayTimeStr+"/"+padder.format(dayGoal)+":00");
        }
        String nightTimeStr = ElapsedTime.formatSeconds(timeUpdater.timeTracker.night/1000);
        if (nightGoal == 0) nightTimeView.setVisibility(View.GONE);
        else{
            nightTimeView.setVisibility(View.VISIBLE);
            nightTimeView.setText("Night: "+nightTimeStr+"/"+padder.format(nightGoal)+":00");
        }
        String weatherTimeStr = ElapsedTime.formatSeconds(timeUpdater.timeTracker.weather/1000);
        if (weatherGoal == 0) weatherTimeView.setVisibility(View.GONE);
        else{
            weatherTimeView.setVisibility(View.VISIBLE);
            weatherTimeView.setText("Poor Weather: "+weatherTimeStr+"/"+padder.format(weatherGoal)+":00");
        }
        String adverseTimeStr = ElapsedTime.formatSeconds(timeUpdater.timeTracker.adverse/1000);
        if (adverseGoal == 0) adverseTimeView.setVisibility(View.GONE);
        else{
            adverseTimeView.setVisibility(View.VISIBLE);
            adverseTimeView.setText("Adverse: "+adverseTimeStr+"/"+padder.format(adverseGoal)+":00");
        }
    }

    //This is the listener for the "Start Drive" button.
    //The weird indentation is done like this in order to make the indentation like a regular function.
    private View.OnClickListener onStartDrive = new View.OnClickListener() { @Override public void onClick(View view) {
        // Check if the driver field is empty
        Context myContext = rootView.getContext();
        if (spinnerData.driverIds.isEmpty()) {
            Toast.makeText(myContext, getResources().getString(R.string.go_to_add_driver_menu), Toast.LENGTH_LONG).show();
            return;
        }

        // Disable this button
        Button startButton = (Button) view;
        startButton.setEnabled(false);

        // Enable the "Stop Drive" button
        Button stopButton = (Button) rootView.findViewById(R.id.stop_drive);
        stopButton.setEnabled(true);

        // Grab the start time
        startingTime.setTime(new Date());
        Log.d(TAG, "startingTime: " + startingTime);

        // Start updating the label
        timerUpdateLabel();
    } };

    //This is the listener for the "Stop Drive" button.
    private View.OnClickListener onStopDrive = new View.OnClickListener() { @Override public void onClick(View view) {
        // Stop the timer
        timer.cancel();

        // Disable this button
        Button stopButton = (Button) view;
        stopButton.setEnabled(false);

        // Enable the "Start Drive" button
        Button startButton = (Button) rootView.findViewById(R.id.start_drive);
        startButton.setEnabled(true);

        // Check if the driver field is empty
        int spinnerPosition = driversSpinner.getSelectedItemPosition();
        if (spinnerData.driverIds.isEmpty()) {
            Toast.makeText(getContext(), getResources().getString(R.string.go_to_add_driver_menu), Toast.LENGTH_LONG).show();
            return;
        }
        // If it is not empty, get the driver
        final String driverId = spinnerData.driverIds.get(spinnerPosition);
        //Save driver ID in Firebase:
        ongoingRef.child("driver_id").setValue(driverId);

        // Grab the stop time
        endingTime.setTime(new Date());
        // Save the ending time in Firebase:
        ongoingRef.child("end").setValue(endingTime.getTimeInMillis());
        Log.d(TAG, "endTime: " + endingTime);

        showDialog(driverId);
    } };

    private void showDialog(final String driverId) {
        /* Shows dialog to user asking them to select if the drive was at night, in poor weather, or in adverse conditions
           and saves drive with startingTime, endingTime, and driverId */

        //Figure out which checkboxes we want to show the user
        final ArrayList<String> dialogOptions = new ArrayList<String>();
        if(nightGoal != 0) dialogOptions.add("Night");
        if(weatherGoal != 0) dialogOptions.add("Poor Weather");
        if(adverseGoal != 0) dialogOptions.add("Adverse Conditions");
        //This is a list of the options the user selected:
        final List<CharSequence> selections = new ArrayList<>();

        if(dialogOptions.size() > 0) {
            // Ask about the drive
            new MaterialDialog.Builder(getContext())
                    .content(R.string.save_dialog_content)
                    .positiveText(R.string.save)
                    .items(dialogOptions.toArray(new String[dialogOptions.size()]))
                    .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, Integer[] indexes, CharSequence[] texts) {
                            selections.clear();
                            selections.addAll(Arrays.asList(texts));
                            return true;
                        }
                    })
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            // Save the drive (at night) and says success
                            boolean duringNight = false, duringWeather = false, duringAdverse = false;
                            for (CharSequence selection : selections) {
                                if (selection.toString().equals("Night")) duringNight = true;
                                if (selection.toString().equals("Poor Weather")) duringWeather = true;
                                if (selection.toString().equals("Adverse Conditions"))duringAdverse = true;
                            }
                            saveDrive(duringNight, duringWeather, duringAdverse, driverId);
                            resetLabel();
                            Toast.makeText(getContext(), R.string.drive_saved, Toast.LENGTH_SHORT).show();
                        }
                    })
                    //Do not cancel the dialog if the user accidentally touches on the outside:
                    .canceledOnTouchOutside(false)
                    .alwaysCallMultiChoiceCallback()
                    .show();
        } else { //They're not tracking anything other then total time
            saveDrive(false, false, false, driverId);
            resetLabel();
            Toast.makeText(getContext(), R.string.drive_saved, Toast.LENGTH_SHORT).show();
        }
    }

    //This is the listener for the "Custom Drive" button.
    private View.OnClickListener onAddDrive = new View.OnClickListener() { @Override public void onClick(View view) {
        // Check if the user is signed in:
        boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
        // Don't do anything if the user isn't signed in:
        if (!isSignedIn) return;

        // Close the plus button menu
        FloatingActionMenu floatingMenu = (FloatingActionMenu) rootView.findViewById(R.id.menu);
        floatingMenu.close(false);

        // Open the activity (which masquerades as a dialog)
        Intent intent = new Intent(view.getContext(), CustomDriveDialog.class);
        startActivity(intent);
    } };

    //This is the listener for the "Add Driver" button.
    private View.OnClickListener onAddDriver = new View.OnClickListener() { @Override public void onClick(View view) {
        // Check if the user is signed in:
        boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
        // Don't do anything if the user isn't signed in:
        if (!isSignedIn) return;

        // Close the plus button menu
        FloatingActionMenu floatingMenu = (FloatingActionMenu) rootView.findViewById(R.id.menu);
        floatingMenu.close(false);

        // Open the activity (which masquerades as a dialog)
        Intent intent = new Intent(view.getContext(), DriverDialog.class);
        intent.putExtra("driverId", "");
        startActivity(intent);
    } };

    public void saveDrive(boolean night, boolean weather, boolean adverse, String driverId) {
        // Check if the user is signed in:
        boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
        // Don't do anything if the user isn't signed in:
        if (!isSignedIn) return;
        // Delete the ongoing drive now that it's over:
        ongoingRef.removeValue();

        // Set the ending time so that startingTime-endingTime is exact in minutes:
        endingTime.set(Calendar.SECOND, startingTime.get(Calendar.SECOND));
        endingTime.set(Calendar.MILLISECOND, startingTime.get(Calendar.MILLISECOND));

        // Connect to the database
        DatabaseReference driveRef = FirebaseDatabase.getInstance().getReference().child(userId).child("times").push();
        driveRef.child("start").setValue(startingTime.getTimeInMillis());
        driveRef.child("end").setValue(endingTime.getTimeInMillis());
        driveRef.child("night").setValue(night);
        driveRef.child("weather").setValue(weather);
        driveRef.child("adverse").setValue(adverse);
        driveRef.child("driver_id").setValue(driverId);
    }

    private void resetLabel() {
        TextView driveTime = (TextView) rootView.findViewById(R.id.drive_time);
        driveTime.setText("0:00:00");
    }

    @Override
    public void onDestroyView() {
        // Since this activity is being stopped, we don't need to listen to the drivers or logs anymore:
        spinnerData.stopListening();
        timeUpdater.stopListening();
        goalsRef.removeEventListener(goalsListener);

        // Save the state in the activity so it will be passed into the arguments
        Bundle state = new Bundle();
        saveToBundle(state);
        MainActivity mainActivity = (MainActivity)getActivity();
        mainActivity.saveArguments(MainActivity.HOME_MENU_INDEX, state);

        super.onDestroyView();
    }
}