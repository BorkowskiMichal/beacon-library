package be.kunstmaan.beaconpositionningexample;

import be.kunstmaan.beaconpositionning.BeaconLocation;
import io.flutter.app.FlutterApplication;

public class MyApp extends FlutterApplication{

    BeaconLocation beaconLocation;


    @Override
    public void onCreate()

    {
        super.onCreate();
        beaconLocation = new BeaconLocation(this, 10, "users", "position");

    }
}
