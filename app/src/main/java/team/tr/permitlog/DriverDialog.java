package team.tr.permitlog;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DriverDialog extends AppCompatActivity {
    // For logging:
    private static String TAG = "DriverDialog";
    // Whether or not this dialog is being used to edit an existing driver
    private boolean editing;
    // If editing, holds the database key of the driver we are editing
    private String driverId;
    // Firebase reference
    private DatabaseReference driverRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dialog);
        Intent intent = getIntent();

        // Setup the Firebase reference
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

            // Show the delete button
            Button deleteButton = (Button) findViewById(R.id.driver_delete);
            deleteButton.setVisibility(View.VISIBLE);

            // Set the values
            final EditText editFirst = (EditText) findViewById(R.id.driver_firstname);
            final EditText editLast = (EditText) findViewById(R.id.driver_lastname);
            final EditText editLicense = (EditText) findViewById(R.id.driver_license);
            final EditText editAge = (EditText) findViewById(R.id.driver_age);
            DatabaseReference editRef = driverRef.child(driverId);
            editRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild("name")) {
                        editFirst.setText(dataSnapshot.child("name").child("first").getValue().toString());
                        editLast.setText(dataSnapshot.child("name").child("last").getValue().toString());
                    }
                    if (dataSnapshot.hasChild("license_number")) editLicense.setText(dataSnapshot.child("license_number").getValue().toString());
                    if (dataSnapshot.hasChild("age")) editAge.setText(dataSnapshot.child("age").getValue().toString());
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "While trying to get /drivers/"+driverId+"/: "+databaseError.getMessage());
                }
            });
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
        if (item.getItemId() == android.R.id.home) {
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
        EditText ageField = (EditText)findViewById(R.id.driver_age);

        String firstName = firstNameField.getText().toString();
        String lastName = lastNameField.getText().toString();
        String licenseNumber = licenseField.getText().toString();
        String driverAge = ageField.getText().toString();

        // Check if any value is empty
        if (firstName.trim().isEmpty() || lastName.trim().isEmpty() || licenseNumber.trim().isEmpty() || driverAge.trim().isEmpty()) {
            Toast.makeText(this, R.string.driver_dialog_error, Toast.LENGTH_SHORT).show();
            return;
        }

        //Check to make sure the driver has a valid age
        if (Integer.parseInt(driverAge) < 21 || Integer.parseInt(driverAge) > 117) {
            Toast.makeText(this, R.string.driver_age_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (editing) {
            // Update existing values
            DatabaseReference editRef = driverRef.child(driverId);
            editRef.child("name").child("first").setValue(firstName);
            editRef.child("name").child("last").setValue(lastName);
            editRef.child("license_number").setValue(licenseNumber);
            editRef.child("age").setValue(driverAge);
        } else {
            // Push a new driver
            DatabaseReference newRef = driverRef.push();
            newRef.child("name").child("first").setValue(firstName);
            newRef.child("name").child("last").setValue(lastName);
            newRef.child("license_number").setValue(licenseNumber);
            newRef.child("age").setValue(driverAge);
        }

        // Close the dialog
        Toast.makeText(this, R.string.driver_dialog_success, Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onDeleteClick(View view) {
        // Delete the driver
        DatabaseReference editRef = driverRef.child(driverId);
        editRef.removeValue();

        // Close the dialog
        Toast.makeText(this, R.string.driver_dialog_deleted, Toast.LENGTH_SHORT).show();
        finish();
    }
}

