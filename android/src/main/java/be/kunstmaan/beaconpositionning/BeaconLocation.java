package be.kunstmaan.beaconpositionning;

import android.content.Context;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BeaconLocation implements BootstrapNotifier,RangeNotifier {
    private static final String TAG = "plugin.BeaconLocation";
    private RegionBootstrap regionBootstrap;
    private Region allbeaconsregions;
    private BackgroundPowerSaver bgSaver;
    private BeaconManager beaconManager;

    private Context appContext;
    private long oldTimeStamp = 0;
    private long minIntervalUpdate;
    private String RegionName = "test.my.region";
    private String firestoreCollectionName;
    private String positionFieldName;


    public BeaconLocation(Context appContext, long minIntervalUpdate, String firestoreCollectionName, String positionFieldName ) {
        this.appContext = appContext;
        this.minIntervalUpdate = minIntervalUpdate;
        this.firestoreCollectionName = firestoreCollectionName;
        this.positionFieldName = positionFieldName;

        // To detect proprietary beacons, you must add a line likebelowcorresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        // beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        // wake up the app when any beacon is seen (you can specify specific id filers in the parameters below)
        beaconManager = BeaconManager.getInstanceForApplication(appContext);
        //beaconManager.setForegroundBetweenScanPeriod(5000);
        Beacon.setHardwareEqualityEnforced(true);
        bgSaver = new BackgroundPowerSaver(appContext);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));


        allbeaconsregions = new Region(RegionName, null, null, null);
        regionBootstrap = new RegionBootstrap(this, allbeaconsregions);
        //beaconManager.bind(this);
    }

    public void onBeaconServiceConnect() {
        try {
            beaconManager.startRangingBeaconsInRegion(new Region(RegionName, null, null, null));
            beaconManager.addRangeNotifier(this);
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }
    @Override
    public void didDetermineStateForRegion(int arg0, Region arg1) {
        Log.d(TAG, "Enter in  didDetermineStateForRegion call");
        onBeaconServiceConnect();
    }

    @Override
    public void didEnterRegion(Region arg0) {
        Log.d(TAG, "Got a didEnterRegion call");
        // This call to disable will make it so the activity below only gets launched the first time a beacon is seen (until the next time the app is launched)
        // if you want the Activity to launch every single time beacons come into view, remove this call.
        /*
        regionBootstrap.disable();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Important:  make sure to add android:launchMode="singleInstance" in the manifest
        // to keep multiple copies of this activity from getting created if the user has
        // already manually launched the app.
        this.startActivity(intent);
        */
    }

    @Override
    public void didExitRegion(Region arg0) {
        Log.d(TAG, "Enter in  didExitRegion call");
    }


    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if(beacons.size() > 0) {
            for (Beacon beacon : beacons) {
                if (beacon.getDistance() < 1.0) {
                    Long tsLong = System.currentTimeMillis()/1000;
                    if(tsLong - oldTimeStamp > minIntervalUpdate){
                        oldTimeStamp = tsLong;
                        String ts = tsLong.toString();
                        int beaconMajor = beacon.getId2().toInt();
                        int beaconMinor = beacon.getId3().toInt();
                        double distance = beacon.getDistance();
                        Log.d(TAG, "didRangeBeaconsInRegion: " + beaconMajor + " " + beaconMinor + " " + distance + " " + ts);
                        updateToFirestore(ts, String.valueOf(beaconMajor), String.valueOf(beaconMinor), String.valueOf(distance));
                    }

                }
            }
        }
    }

    private void updateToFirestore(final String ts, final String s, final String s1, final String distance) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currUser = FirebaseAuth.getInstance().getCurrentUser();

        if(currUser != null){
            final DocumentReference userDocRef = db.collection(firestoreCollectionName).document(currUser.getUid());

            userDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    Map<String, Object> update = new HashMap<>();
                    update.put(positionFieldName, new Position(ts, s, s1, distance).toMap());
                    userDocRef.set(update, SetOptions.merge()).addOnSuccessListener( new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Document has been saved");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Document could not be saved" +e.toString());
                        }
                    });
                    Log.d(TAG, "DocumentSnapshot successfully retrieved! ");
                }
            });
        }


    }


    @Override
    public Context getApplicationContext() {
        return this.appContext;
    }
}

class Position{
    private String time;
    private String major;
    private String minor;
    private String distance;

    Position(String time, String major, String minor, String distance) {
        this.time = time;
        this.major = major;
        this.minor = minor;
        this.distance = distance;
    }

    Map<String, String> toMap(){
        Map<String, String> map = new HashMap<>();
        map.put("time_stamp", time);
        map.put("major", major);
        map.put("minor", minor);
        map.put("distance", distance);
        return map;
    }
}
