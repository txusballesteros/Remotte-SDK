package com.mobandme.remotte.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.mobandme.remotte.Remotte;
import com.mobandme.remotte.app.bus.DeviceBus;
import com.mobandme.remotte.listener.AccelerometerSensorCallback;
import com.mobandme.remotte.listener.KeysPressedCallback;


public class RotationActivity extends Activity {

    private View     mRotationView;
    private Remotte  mRemotte;
    private boolean  mLeftButtonPressed = false;
    private boolean  mRightButtonPressed = false;

    private KeysPressedCallback keysPressedCallback = new KeysPressedCallback() {
        @Override
        public void onKeyPress(Remotte remotte, int powerKeyPressed, int centerKeyPressed) {
            if (powerKeyPressed == Remotte.BUTTON_PRESSED && centerKeyPressed == Remotte.BUTTON_RELEASED) {
                mLeftButtonPressed = true;
                mRightButtonPressed = false;
                mRotationView.setBackgroundColor(Color.RED);
            } else if (powerKeyPressed == Remotte.BUTTON_RELEASED && centerKeyPressed ==  Remotte.BUTTON_PRESSED) {
                mLeftButtonPressed = false;
                mRightButtonPressed = true;
                mRotationView.setBackgroundColor(Color.BLUE);
            } else if (powerKeyPressed == Remotte.BUTTON_RELEASED && centerKeyPressed == Remotte.BUTTON_RELEASED) {
                mLeftButtonPressed = false;
                mRightButtonPressed = false;
                mRotationView.setRotationX(0);
                mRotationView.setBackgroundColor(Color.GREEN);
            }
        }
    };

    private AccelerometerSensorCallback accelerometerCallback = new AccelerometerSensorCallback() {
        @Override
        public void onAccelerometerChange(Remotte remotte, double x, double y, double z) {
            float rotation = calculateRotation(x, y);
            float flip     = calculateRotation(z, y);

            mRotationView.setRotation(rotation);

            if (mLeftButtonPressed)
                mRotationView.setRotationX(flip);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation);

        mRotationView = findViewById(R.id.RotationView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mRemotte == null) {
            mRemotte = new Remotte.Builder()
                    .setDeviceAddress(DeviceBus.deviceAddress)
                    .setConfiguration(new Remotte.Configuration()
                        .enableAccelerometerSensor(true, 100, accelerometerCallback)
                        .enableKeysPressedCallback(keysPressedCallback)
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

    public float calculateRotation(double a, double b) {
        return (float)(180 - (Math.atan2(a, b) / (Math.PI/180)));
    }
}
