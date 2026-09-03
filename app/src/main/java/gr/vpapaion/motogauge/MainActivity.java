package gr.vpapaion.motogauge;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.graphics.Color;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity implements LocationListener {
    private static final int LOCATION_REQUEST = 40;
    private SpeedView speedView;
    private LocationManager locations;
    private boolean initialized;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
            speedView = new SpeedView(this);
            setContentView(speedView);
            locations = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            initialized = true;
            speedView.postDelayed(this::requestGps, 700L);
        } catch (Throwable error) { showFatalError("STARTUP", error); }
    }

    @Override protected void onResume() {
        super.onResume();
        if (initialized) startGps();
    }

    @Override protected void onPause() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try { if (locations != null) locations.removeUpdates(this); }
            catch (SecurityException ignored) { }
        }
        super.onPause();
    }

    private void requestGps() {
        if (isFinishing() || speedView == null) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        else startGps();
    }

    private void startGps() {
        if (locations == null || speedView == null) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200L, 0f, this);
                speedView.setGpsStatus("GPS SEARCHING");
            } catch (Exception error) { speedView.setGpsStatus("ENABLE GPS"); }
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == LOCATION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startGps();
        else if (speedView != null) speedView.setGpsStatus("GPS PERMISSION NEEDED");
    }

    @Override public void onLocationChanged(Location location) {
        try {
            float speed = location.hasSpeed() ? Math.max(0f, location.getSpeed() * 3.6f) : 0f;
            if (speed < 3f) speed = 0f;
            if (speedView != null) speedView.setSpeed(speed, location.hasAccuracy() ? location.getAccuracy() : -1f);
        } catch (Throwable error) { showFatalError("GPS UPDATE", error); }
    }

    @Override public void onProviderDisabled(String provider) { if (speedView != null) speedView.setGpsStatus("ENABLE GPS"); }
    @Override public void onProviderEnabled(String provider) { if (speedView != null) speedView.setGpsStatus("GPS SEARCHING"); }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }

    private void showFatalError(String stage, Throwable error) {
        initialized = false;
        TextView message = new TextView(this);
        message.setBackgroundColor(Color.rgb(7,10,15));
        message.setTextColor(Color.WHITE);
        message.setTextSize(17f);
        message.setPadding(36,36,36,36);
        message.setText("MotoSpeed diagnostic screen\n\nStage: "+stage+"\n"+error.getClass().getName()+"\n\n"+String.valueOf(error.getMessage()));
        setContentView(message);
    }
}
