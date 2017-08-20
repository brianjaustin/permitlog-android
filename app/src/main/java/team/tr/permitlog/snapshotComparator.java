package team.tr.permitlog;

import com.google.firebase.database.DataSnapshot;

import java.util.Comparator;

public class snapshotComparator implements Comparator<DataSnapshot> {
    public int compare(DataSnapshot o1, DataSnapshot o2){
        return Long.compare((long) o1.child("start").getValue(), (long) o2.child("start").getValue());
    }
}
