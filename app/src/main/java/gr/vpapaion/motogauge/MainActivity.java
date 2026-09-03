package gr.vpapaion.motogauge;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

public class MainActivity extends Activity implements SensorEventListener, LocationListener {
    private static final int LOCATION_REQUEST = 40;
    private GaugeView gauge;
    private SensorManager sensors;
    private LocationManager locations;
    private Sensor rotationSensor;
    private float zeroRoll;
    private boolean calibrated;
    private float filteredLean;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();
        gauge = new GaugeView(this);
        gauge.setOnCalibrateListener(() -> { calibrated = false; filteredLean = 0f; gauge.resetLean(); });
        setContentView(gauge);
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locations = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if (rotationSensor == null) rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        gauge.setSensorAvailable(rotationSensor != null);
        requestGps();
    }

    private void hideSystemUi() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) { c.hide(WindowInsets.Type.systemBars()); c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE); }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(5894 | 4096);
        }
    }

    @Override protected void onResume() {
        super.onResume(); hideSystemUi();
        if (rotationSensor != null) sensors.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        startGps();
    }

    @Override protected void onPause() {
        sensors.unregisterListener(this);
        // Opening the runtime permission dialog pauses the Activity before the
        // location permission has been granted. removeUpdates() is permission
        // protected too, so calling it unconditionally crashes first launch.
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try { locations.removeUpdates(this); }
            catch (SecurityException ignored) { }
        }
        super.onPause();
    }

    private void requestGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        else startGps();
    }

    private void startGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try { locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 250L, 0f, this); gauge.setGpsStatus("GPS SEARCHING"); }
            catch (Exception e) { gauge.setGpsStatus("ENABLE GPS"); }
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == LOCATION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startGps();
        else gauge.setGpsStatus("GPS PERMISSION NEEDED");
    }

    @Override public void onLocationChanged(Location location) {
        float speed = location.hasSpeed() ? Math.max(0f, location.getSpeed() * 3.6f) : 0f;
        gauge.setSpeed(speed, location.hasAccuracy() ? location.getAccuracy() : -1f);
    }

    @Override public void onProviderDisabled(String provider) { gauge.setGpsStatus("ENABLE GPS"); }
    @Override public void onProviderEnabled(String provider) { gauge.setGpsStatus("GPS SEARCHING"); }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override public void onSensorChanged(SensorEvent event) {
        float[] rotation = new float[9];
        float[] screen = new float[9];
        float[] orientation = new float[3];
        SensorManager.getRotationMatrixFromVector(rotation, event.values);
        // Convert the phone's natural portrait coordinates to the forced landscape display.
        SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, screen);
        SensorManager.getOrientation(screen, orientation);
        float roll = (float) Math.toDegrees(orientation[2]);
        if (!calibrated) { zeroRoll = roll; calibrated = true; }
        float lean = wrapDegrees(roll - zeroRoll);
        if (Math.abs(lean) < 0.5f) lean = 0f;
        filteredLean += 0.16f * (lean - filteredLean);
        gauge.setLean(filteredLean);
    }

    private static float wrapDegrees(float angle) {
        while (angle > 180f) angle -= 360f;
        while (angle < -180f) angle += 360f;
        return angle;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
