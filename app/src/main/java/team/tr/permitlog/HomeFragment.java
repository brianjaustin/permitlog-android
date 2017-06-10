package team.tr.permitlog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment {
    //The root view for this fragment, used to find elements by id:
    private View rootView;

    private final String TAG = "HomeFragment";
    private String userId;
    // Are we showing the tutorial right now?
    private boolean showingTutorial = false;
    // Have we shown the tutorial yet?
    private boolean shownTutorial = false;

    // Store drive start/stop times
    private Date startingTime = new Date();
    private Date endingTime = new Date();

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
    private long totalGoal, dayGoal, nightGoal;
    // TextViews:
    private TextView totalTimeView, dayTimeView, nightTimeView;
    // Object that updates totalTime, dayTime, and nightTime:
    private ElapsedTime timeUpdater;
    // Firebase reference for goals:
    private DatabaseReference goalsRef;
    // Callback for timesListener:
    private TriLongConsumer timeCallback = new TriLongConsumer() {
        @Override public void accept(long totalTimeP, long dayTimeP, long nightTimeP) {
            // Update the "Time Completed" section:
            updateGoalTextViews();
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save values for rotate
        saveToBundle(outState);
    }

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
        // Start listening to logs:
        timeUpdater = new ElapsedTime(userId, timeCallback);
        // Get data about the goals, if available:
        goalsRef = FirebaseDatabase.getInstance().getReference().child(userId).child("goals");
        goalsRef.addValueEventListener(goalsListener);

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

    private void saveToBundle(Bundle outState) {
        /* Takes Bundle and sets info about autodrive in Bundle. */
        outState.putBoolean("startEnabled", startButton.isEnabled());
        outState.putBoolean("stopEnabled", stopButton.isEnabled());
        outState.putLong("startTime", startingTime.getTime());
        outState.putInt("spinnerPosition", driversSpinner.getSelectedItemPosition());
    }

    private void loadFromBundle(Bundle savedInstanceState) {
        /* Takes Bundle and sets info about autodrive from Bundle. */
        startButton.setEnabled(savedInstanceState.getBoolean("startEnabled"));
        stopButton.setEnabled(savedInstanceState.getBoolean("stopEnabled"));
        startingTime.setTime(savedInstanceState.getLong("startTime"));
        spinnerPosition = savedInstanceState.getInt("spinnerPosition");
        // Set spinnerPosition if possible:
        if (spinnerData.driverIds.size() > spinnerPosition) driversSpinner.setSelection(spinnerPosition);
        // Otherwise, keep listening to the adapter and set it when possible:
        else spinnerData.driversAdapter.registerDataSetObserver(setSpinnerPosition);

        // Start updating the label
        if (savedInstanceState.getBoolean("stopEnabled")) timerUpdateLabel();
    };

    private DataSetObserver setSpinnerPosition = new DataSetObserver() { @Override public void onChanged() {
        // Set spinnerPosition if possible:
        if (spinnerData.driverIds.size() > spinnerPosition) {
            driversSpinner.setSelection(spinnerPosition);
            // Stop listening to data changes once we've done this:
            spinnerData.driversAdapter.unregisterDataSetObserver(setSpinnerPosition);
        }
    } };

    private void timerUpdateLabel() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Get the difference between the start and current time in seconds
                long timeDiff = ((new Date()).getTime()-startingTime.getTime())/1000;
                // Format this time appropriately:
                formattedTime = DateUtils.formatElapsedTime(timeDiff);
                // Remember to add hours:
                if (timeDiff < 3600) formattedTime = "0:"+formattedTime;
                mHandler.obtainMessage(1).sendToTarget();
            }
        }, 0, 1000);
    }

    private ValueEventListener goalsListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Set totalGoal, or set it to 0 if not present:
            if (dataSnapshot.hasChild("total")) {
                totalGoal = (long)dataSnapshot.child("total").getValue();
            }
            else {
                totalGoal = 0;
                // If they don't have goals and they have not seen the tutorial, assume they are a new user, so show the tutorial:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                if (!shownTutorial && prefs.getBoolean("tutorial", true)) showTutorial();
            }
            // Do the same for day and night:
            if (dataSnapshot.hasChild("day")) dayGoal = (long)dataSnapshot.child("day").getValue();
            else dayGoal = 0;
            if (dataSnapshot.hasChild("night")) nightGoal = (long)dataSnapshot.child("night").getValue();
            else nightGoal = 0;
            // Update the "Time Completed" section
            updateGoalTextViews();
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
                    .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(rootView.findViewById(R.id.FAB_image_view))
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_text_fam)
                        .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(rootView.findViewById(R.id.drivers_spinner))
                        .withRectangleShape()
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_text_spinner)
                        .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(rootView.findViewById(R.id.start_drive))
                        .withRectangleShape()
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_text_start)
                        .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setTarget(rootView.findViewById(R.id.stop_drive))
                        .withRectangleShape()
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_text_stop)
                        .setListener(new IShowcaseListener() {
                            //Enable the stop button while showcasing it:
                            @Override
                            public void onShowcaseDisplayed(MaterialShowcaseView materialShowcaseView) {
                                stopButton.setEnabled(true);
                            }
                            //Disable it when finished:
                            @Override
                            public void onShowcaseDismissed(MaterialShowcaseView materialShowcaseView) {
                                stopButton.setEnabled(false);
                            }
                        })
                        .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_text_menu)
                        .setTarget(rootView.findViewById(R.id.FAB_image_view))
                        .withoutShape()
                        .build());

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(getActivity())
                        .setDismissText("OK")
                        .setContentText(R.string.tutorial_redirect)
                        .setTarget(rootView.findViewById(R.id.FAB_image_view))
                        .withoutShape()
                        .setListener(new IShowcaseListener() {
                            //Necessary to complete abstract class:
                            @Override
                            public void onShowcaseDisplayed(MaterialShowcaseView materialShowcaseView) {}
                            //Once the tutorial is over:
                            @Override
                            public void onShowcaseDismissed(MaterialShowcaseView materialShowcaseView) {
                                // Make sure this isn't shown again on this device (in case the user does not set goals)
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("tutorial", false);
                                editor.commit();
                                // The tutorial has ended:
                                showingTutorial = false;
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
        String totalTimeStr = ElapsedTime.formatSeconds(timeUpdater.totalTime/1000);
        // Show the goal if it is nonzero:
        if (totalGoal == 0) totalTimeView.setText("Total: "+totalTimeStr);
        else totalTimeView.setText("Total: "+totalTimeStr+"/"+padder.format(totalGoal)+":00");
        // Do the same for day and night:
        String dayTimeStr = ElapsedTime.formatSeconds(timeUpdater.dayTime/1000);
        if (dayGoal == 0) dayTimeView.setText("Day: "+dayTimeStr);
        else dayTimeView.setText("Day: "+dayTimeStr+"/"+padder.format(dayGoal)+":00");
        String nightTimeStr = ElapsedTime.formatSeconds(timeUpdater.nightTime/1000);
        if (nightGoal == 0) nightTimeView.setText("Night: "+nightTimeStr);
        else nightTimeView.setText("Night: "+nightTimeStr+"/"+padder.format(nightGoal)+":00");
    }

    //This is the listener for the "Start Drive" button.
    //The weird indentation is done like this in order to make the indentation like a regular function.
    private View.OnClickListener onStartDrive = new View.OnClickListener() { @Override public void onClick(View view) {
        // Don't do anything if we're showing the tutorial:
        if (showingTutorial) return;
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
        startingTime = new Date();
        Log.d(TAG, "startingTime: " + startingTime);

        // Start updating the label
        timerUpdateLabel();
    } };

    public Handler mHandler = new Handler() {
        // Set the time
        public void handleMessage(Message msg) {
            TextView driveTime = (TextView) rootView.findViewById(R.id.drive_time);
            driveTime.setText(formattedTime);
        }
    };

    //This is the listener for the "Stop Drive" button.
    private View.OnClickListener onStopDrive = new View.OnClickListener() { @Override public void onClick(View view) {
        // Don't do anything if we're showing the tutorial:
        if (showingTutorial) return;
        // Stop the timer
        timer.cancel();

        // Disable this button
        Button stopButton = (Button) view;
        stopButton.setEnabled(false);

        // Enable the "Start Drive" button
        Button startButton = (Button) rootView.findViewById(R.id.start_drive);
        startButton.setEnabled(true);

        final Context myContext = rootView.getContext();

        // Check if the driver field is empty
        int spinnerPosition = driversSpinner.getSelectedItemPosition();
        if (spinnerData.driverIds.isEmpty()) {
            Toast.makeText(myContext, getResources().getString(R.string.go_to_add_driver_menu), Toast.LENGTH_LONG).show();
            return;
        }
        // If it is not empty, get the driver
        final String driverId = spinnerData.driverIds.get(spinnerPosition);

        // Grab the stop time
        endingTime = new Date();
        Log.d(TAG, "endTime: " + endingTime);

        // Ask if the driving took place at night
        new MaterialDialog.Builder(myContext)
                .content(R.string.at_night_dialog_content)
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .neutralText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        // Save the drive (at night) and says success
                        saveDrive(true, driverId);
                        Toast.makeText(myContext, R.string.drive_saved, Toast.LENGTH_SHORT).show();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        // Save the daytime drive and say success
                        saveDrive(false, driverId);
                        Toast.makeText(myContext, R.string.drive_saved, Toast.LENGTH_SHORT).show();
                    }
                })
                .onAny(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        // Set the timer label to zero
                        resetLabel();
                    }
                })
                .show();
    } };

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

    public void saveDrive(boolean night, String driverId) {
        // Check if the user is signed in:
        boolean isSignedIn = FirebaseHelper.signInIfNeeded((MainActivity)getActivity());
        // Don't do anything if the user isn't signed in:
        if (!isSignedIn) return;
        // Connect to the database
        DatabaseReference driveRef = FirebaseDatabase.getInstance().getReference().child(userId).child("times").push();
        driveRef.child("start").setValue(startingTime.getTime());
        driveRef.child("end").setValue(endingTime.getTime());
        driveRef.child("night").setValue(night);
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
        // Save the state:
        Bundle state = new Bundle();
        saveToBundle(state);
        MainActivity mainActivity = (MainActivity)getActivity();
        mainActivity.saveArguments(MainActivity.HOME_MENU_INDEX, state);
        super.onDestroyView();
    }
}