package team.tr.permitlog;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

public class DriverDialog extends AppCompatActivity {

    private boolean editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dialog);
        Intent intent = getIntent();
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar ab = getSupportActionBar(); //Create action bar object
        ab.setDisplayHomeAsUpEnabled(true); //Enable back button

        if (!intent.getStringExtra("driverId").toString().equals("")) {
            // A driver is being edited
            editing = true;

            // Set title
            ab.setTitle(R.string.driver_edit_title);

            // TODO: Add code to retreive values
        }
        else {
            // A new driver is being added
            editing = false;

            // Set title
            ab.setTitle(R.string.driver_add_title);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home ) {
            Toast.makeText(getApplicationContext(), "Overroad", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        }
        // other menu select events may be present here

        return super.onOptionsItemSelected(item);
    }
}

