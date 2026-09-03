package gr.vpapaion.motogauge;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.TextView;
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
    private final float[] baseQuaternion = new float[4];
    private boolean initialized;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
            gauge = new GaugeView(this);
            gauge.setOnCalibrateListener(() -> { calibrated = false; filteredLean = 0f; gauge.resetLean(); });
            setContentView(gauge);
            sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            locations = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (sensors != null) {
                rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
                if (rotationSensor == null) rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            }
            gauge.setSensorAvailable(rotationSensor != null);
            initialized = true;
            // Let the first frame render before opening Android's permission UI.
            gauge.postDelayed(this::requestGps, 700L);
        } catch (Throwable error) {
            showFatalError("STARTUP", error);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (!initialized) return;
        try {
            if (rotationSensor != null && sensors != null) sensors.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
            startGps();
        } catch (Throwable error) { showFatalError("RESUME", error); }
    }

    @Override protected void onPause() {
        if (sensors != null) sensors.unregisterListener(this);
        // Opening the runtime permission dialog pauses the Activity before the
        // location permission has been granted. removeUpdates() is permission
        // protected too, so calling it unconditionally crashes first launch.
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try { if (locations != null) locations.removeUpdates(this); }
            catch (SecurityException ignored) { }
        }
        super.onPause();
    }

    private void requestGps() {
        if (isFinishing() || gauge == null) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        else startGps();
    }

    private void startGps() {
        if (locations == null || gauge == null) return;
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
        try {
            float speed = location.hasSpeed() ? Math.max(0f, location.getSpeed() * 3.6f) : 0f;
            // GPS commonly reports 0.5–2 km/h while stationary. MotoGauge is a
            // motorcycle display, so treat values below 3 km/h as GPS drift.
            if (speed < 3f) speed = 0f;
            if (gauge != null) gauge.setSpeed(speed, location.hasAccuracy() ? location.getAccuracy() : -1f);
        } catch (Throwable error) { showFatalError("GPS UPDATE", error); }
    }

    @Override public void onProviderDisabled(String provider) { gauge.setGpsStatus("ENABLE GPS"); }
    @Override public void onProviderEnabled(String provider) { gauge.setGpsStatus("GPS SEARCHING"); }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override public void onSensorChanged(SensorEvent event) {
        try {
        float x=event.values[0], y=event.values[1], z=event.values[2];
        float w=event.values.length >= 4 ? event.values[3] :
                (float)Math.sqrt(Math.max(0f,1f-x*x-y*y-z*z));
        float norm=(float)Math.sqrt(x*x+y*y+z*z+w*w);
        if (norm == 0f) return;
        x/=norm; y/=norm; z/=norm; w/=norm;
        if (!calibrated) {
            baseQuaternion[0]=x; baseQuaternion[1]=y;
            baseQuaternion[2]=z; baseQuaternion[3]=w;
            calibrated=true; filteredLean=0f; return;
        }
        // Relative quaternion inverse(base) * current. Its twist around device
        // Z is lean in the screen plane, independent of portrait/landscape.
        float bx=baseQuaternion[0], by=baseQuaternion[1];
        float bz=baseQuaternion[2], bw=baseQuaternion[3];
        float relativeZ=bw*z-bx*y+by*x-bz*w;
        float relativeW=bw*w+bx*x+by*y+bz*z;
        if (relativeW < 0f) { relativeW=-relativeW; relativeZ=-relativeZ; }
        float lean=(float)Math.toDegrees(2.0*Math.atan2(relativeZ,relativeW));
        lean=wrapDegrees(lean);
        if (Math.abs(lean) < 0.5f) lean = 0f;
        filteredLean += 0.16f * (lean - filteredLean);
        gauge.setLean(filteredLean);
        } catch (Throwable error) { showFatalError("SENSOR UPDATE", error); }
    }

    private static float wrapDegrees(float angle) {
        while (angle > 180f) angle -= 360f;
        while (angle < -180f) angle += 360f;
        return angle;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Rotating the phone itself is a 90-degree Z rotation. Re-zero on the
        // first sample after the UI changes orientation so it is not mistaken
        // for motorcycle lean.
        calibrated=false;
        filteredLean=0f;
        if (gauge != null) gauge.resetLean();
    }

    private void showFatalError(String stage, Throwable error) {
        initialized = false;
        TextView message = new TextView(this);
        message.setBackgroundColor(Color.rgb(7, 10, 15));
        message.setTextColor(Color.WHITE);
        message.setTextSize(17f);
        message.setPadding(36, 36, 36, 36);
        message.setText("MotoGauge diagnostic screen\n\nStage: " + stage + "\n" +
                error.getClass().getName() + "\n\n" + String.valueOf(error.getMessage()) +
                "\n\nPlease take a screenshot of this screen.");
        setContentView(message);
    }
}
