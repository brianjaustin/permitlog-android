package team.tr.permitlog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class DriverDialog extends AppCompatActivity {

    private boolean editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dialog);

        Intent intent = getIntent();
        if (!intent.getStringExtra("driverId").toString().equals("")) {
            // A driver is being edited
            editing = true;

            // Set title
            getSupportActionBar().setTitle(R.string.driver_edit_title);

            // TODO: Add code to retreive values
        }
        else {
            // A new driver is being added
            editing = false;

            // Set title
            getSupportActionBar().setTitle(R.string.driver_add_title);
        }
    }
}
