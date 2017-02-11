package team.tr.permitlog;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class PermitLog extends Application {

    private String TAG = "PermitLog";

    @Override
    public void onCreate() { // This method is only called once per app run
        super.onCreate();

        // Enable persistence
        FirebaseApp.initializeApp(getApplicationContext());
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        Log.d(TAG, "onCreate fired; offline database enabled");
    }
}
