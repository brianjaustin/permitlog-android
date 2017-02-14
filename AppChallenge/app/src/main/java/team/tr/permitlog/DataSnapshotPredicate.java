package team.tr.permitlog;

import com.google.firebase.database.DataSnapshot;

public interface DataSnapshotPredicate {
    //Takes in a DataSnapshot and returns a boolean:
    boolean accept(DataSnapshot dataSnapshot);
}
