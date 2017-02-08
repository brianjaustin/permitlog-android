package team.tr.permitlog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.auth.FirebaseAuth;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class HomeFragment extends Fragment {
    // Object that holds all data relevant to the driver spinner:
    private DriverSpinner spinnerData;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LayoutInflater lf = getActivity().getLayoutInflater();
        //pass the correct layout name for the fragment
        final View rootView =  lf.inflate(R.layout.fragment_home, container, false);

        // Set start drive button click
        Button startDrive = (Button) rootView.findViewById(R.id.start_drive);
        startDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: add code to log the start time of driving, maybe in a separate method
            }
        });


        // Set stop drive button click
        Button stopDrive = (Button) rootView.findViewById(R.id.stop_drive);
        stopDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: add code to log the stop time of driving, maybe in a separate method
            }
        });

        // Set add drive button click
        FloatingActionButton addDrive = (FloatingActionButton) rootView.findViewById(R.id.add_drive);
        addDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Close the plus button menu
                FloatingActionMenu floatingMenu = (FloatingActionMenu) rootView.findViewById(R.id.menu);
                floatingMenu.close(false);

                // Open the activity (which masquerades as a dialog)
                Intent intent = new Intent(view.getContext(), CustomDriveDialog.class);
                startActivity(intent);
            }
        });

        // Set add driver button click
        FloatingActionButton addDriver = (FloatingActionButton) rootView.findViewById(R.id.add_driver);
        addDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Close the plus button menu
                FloatingActionMenu floatingMenu = (FloatingActionMenu) rootView.findViewById(R.id.menu);
                floatingMenu.close(false);

                // Open the activity (which masquerades as a dialog)
                Intent intent = new Intent(view.getContext(), DriverDialog.class);
                intent.putExtra("driverId", "");
                startActivity(intent);
            }
        });

        // Get the drivers spinner:
        Spinner driversSpinner = (Spinner)rootView.findViewById(R.id.drivers_spinner);
        // Get the UID:
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Add the items to the spinner:
        spinnerData = new DriverSpinner(getActivity(), userId, driversSpinner);

        TextView text = (TextView) rootView.findViewById(R.id.time_elapsed);
        text.setText("test");
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        // Since this activity is being stopped, we don't need to listen to the drivers anymore:
        spinnerData.stopListening();
        super.onDestroy();
    }
}
