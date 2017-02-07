package team.tr.permitlog;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverDialog extends AppCompatActivity {

    // Whether or not this dialog is being used to edit an existing driver
    private boolean editing;

    // Firebase reference
    private DatabaseReference driverRef;

    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dialog);
        Intent intent = getIntent();

        // Setup the firebase reference
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        driverRef = FirebaseDatabase.getInstance().getReference().child(userId).child("drivers");

        // Set the toolbar as the action bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar ab = getSupportActionBar(); //Create action bar object
        ab.setDisplayHomeAsUpEnabled(true); //Enable back button

        if (!intent.getStringExtra("driverId").equals("")) {
            // A driver is being edited
            editing = true;
            driverId = intent.getStringExtra("driverId");

            // Set title
            ab.setTitle(R.string.driver_edit_title);

            // TODO: Add code to retrieve values
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
        //Code to handle X button in top right
        if (item.getItemId() == android.R.id.home ) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onCancel(View view){
        finish();
    }

    public void onSaveClick(View view) {
        // Get values from the text fields
        EditText firstNameField = (EditText)findViewById(R.id.driver_firstname);
        EditText lastNameField = (EditText)findViewById(R.id.driver_lastname);
        EditText licenseField = (EditText)findViewById(R.id.driver_license);

        String firstName = firstNameField.getText().toString();
        String lastName = lastNameField.getText().toString();
        String licenseNumber = licenseField.getText().toString();

        // Check if any value is empty
        if (firstName.matches("") || lastName.matches("") || licenseNumber.matches("")) {
            Toast.makeText(this, R.string.driver_dialog_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (editing) {
            // Update existing values
            DatabaseReference editRef = driverRef.child(driverId);
            editRef.child("name").child("first").setValue(firstName);
            editRef.child("name").child("last").setValue(lastName);
            editRef.child("license_number").setValue(licenseNumber);
        } else {
            // Push a new driver
            DatabaseReference newRef = driverRef.push();
            newRef.child("name").child("first").setValue(firstName);
            newRef.child("name").child("last").setValue(lastName);
            newRef.child("license_number").setValue(licenseNumber);
        }

        // Close the dialog
        Toast.makeText(this, R.string.driver_dialog_success, Toast.LENGTH_SHORT).show();
        finish();
    }
}

