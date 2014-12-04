package com.mobandme.remotte.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.mobandme.remotte.Remotte;
import com.mobandme.remotte.app.bus.DeviceBus;
import com.mobandme.remotte.listener.AccelerometerSensorCallback;


public class ShakeActivity extends Activity {

    private static double SHAKE_THRESHOLD = 0.8;
    private static double SHAKE_TIME      = 500;

    private Remotte     mRemotte;
    private long        mLastShakeTime = 0;

    private TextView    mAccelerometerView;
    private TextView    mSpeedView;
    private TextView    mForceView;

    private AccelerometerSensorCallback accelerometerCallback = new AccelerometerSensorCallback() {
        @Override
        public void onAccelerometerChange(Remotte remotte, double x, double y, double z) {

            mAccelerometerView.setText(String.format("X: %f Y: %f Z: %f", x, y, z));

            float gX = (float)x;
            float gY = (float)y;
            float gZ = (float)z;

            double gForce = FloatMath.sqrt(gX * gX + gY * gY + gZ * gZ);
            mForceView.setText(String.format("Force: %s", gForce));

            if (gForce > SHAKE_THRESHOLD) {
                final long now = System.currentTimeMillis();
                if (mLastShakeTime + SHAKE_TIME > now)
                    return;

                Toast.makeText(ShakeActivity.this, "Shake!!!", Toast.LENGTH_SHORT).show();
                mLastShakeTime = now;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shake);

        mAccelerometerView  = (TextView)findViewById(R.id.Accelerometer);
        mSpeedView          = (TextView)findViewById(R.id.Speed);
        mForceView          = (TextView)findViewById(R.id.Force);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mRemotte == null) {
            mRemotte = new Remotte.Builder()
                    .setDeviceAddress(DeviceBus.deviceAddress)
                    .setConfiguration(new Remotte.Configuration()
                        .enableAccelerometerSensor(true, accelerometerCallback)
                    )
                    .build(this);
        }

        mRemotte.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRemotte.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRemotte = null;
    }

}
