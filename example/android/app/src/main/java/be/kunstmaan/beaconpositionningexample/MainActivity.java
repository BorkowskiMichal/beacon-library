package be.kunstmaan.beaconpositionningexample;

import android.Manifest;
import android.os.Bundle;

import io.flutter.app.FlutterActivity;
import io.flutter.plugins.GeneratedPluginRegistrant;


public class MainActivity extends FlutterActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    GeneratedPluginRegistrant.registerWith(this);

      this.requestPermissions(
            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
            1);
  }
}
