package com.mobandme.remotte.app;

import android.os.Bundle;
import android.view.Menu;
import android.app.Activity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mobandme.remotte.Remotte;
import com.mobandme.remotte.app.bus.DeviceBus;
import com.mobandme.remotte.listener.AccelerometerSensorCallback;
import com.mobandme.remotte.listener.AltimeterSensorCallback;
import com.mobandme.remotte.listener.CharacteristicReadCallback;
import com.mobandme.remotte.listener.ConnectionStateChangeCallback;
import com.mobandme.remotte.listener.GyroscopeSensorCallback;
import com.mobandme.remotte.listener.KeysPressedCallback;
import com.mobandme.remotte.listener.TemperatureSensorCallback;

import java.text.DecimalFormat;

public class SensorsActivity extends Activity implements View.OnClickListener {

    private Remotte  remotte;
    private TextView temperatureView;
    private TextView connectionStateView;
    private TextView accelerometerView;
    private TextView gyroscopeView;
    private TextView altimeterView;

    private DecimalFormat decimal = new DecimalFormat("0.00");

    private ConnectionStateChangeCallback connectionStateCallback = new ConnectionStateChangeCallback() {
        @Override
        public void onConnectionStateChange(Remotte remotte, int newState) {
            switch (newState) {
                case ConnectionStateChangeCallback.STATE_CONNECTING:
                    connectionStateView.setText("Connecting");
                    break;
                case ConnectionStateChangeCallback.STATE_CONNECTED:
                    connectionStateView.setText("Connected");
                    break;
                case ConnectionStateChangeCallback.STATE_DISCONNECTING:
                    connectionStateView.setText("Disconnecting");
                    break;
                case ConnectionStateChangeCallback.STATE_DISCONNECTED:
                    connectionStateView.setText("Disconnected");
                    temperatureView.setText("");
                    break;
            }
        }
    };

    private TemperatureSensorCallback temperatureSensorCallback = new TemperatureSensorCallback() {
        @Override
        public void onTemperatureChange(Remotte remotte, double ambientTemperature) {
            temperatureView.setText(String.format("%.2f ºC", ambientTemperature));
        }
    };

    private KeysPressedCallback keysPressedCallback = new KeysPressedCallback() {
        @Override
        public void onKeyPress(Remotte remotte, int powerKeyPressed, int centerKeyPressed) {
            if (powerKeyPressed == 1)
                Toast.makeText(SensorsActivity.this, "Power Button Pressed", Toast.LENGTH_SHORT).show();
            if (centerKeyPressed == 1)
                Toast.makeText(SensorsActivity.this, "Enter Button Pressed", Toast.LENGTH_SHORT).show();
        }
    };

    private AccelerometerSensorCallback accelerometerCallback = new AccelerometerSensorCallback() {
        @Override
        public void onAccelerometerChange(Remotte remotte, double x, double y, double z) {
            accelerometerView.setText(String.format("Accel. X: %.4f Y: %.4f Z: %.4f", x, y, z));
        }
    };

    private GyroscopeSensorCallback gyroscopeSensorCallback = new GyroscopeSensorCallback() {
        @Override
        public void onGyroscopeChange(Remotte remotte, double x, double y, double z) {
            gyroscopeView.setText(String.format("Gyro. X: %.0f Y: %.0f Z: %.0f", x, y, z));
        }
    };

    private AltimeterSensorCallback altimeterSensorCallback = new AltimeterSensorCallback() {
        @Override
        public void onAltimeterChange(Remotte remotte, double pressure, double altitude) {
            altimeterView.setText(String.format("Pressure: %s nPA Altitude: %s m", decimal.format(pressure), decimal.format(altitude)));
        }
    };

    private CharacteristicReadCallback characteristicReadCallback = new CharacteristicReadCallback() {
        @Override
        public void onCharacteristicRead(Remotte remotte, int characteristic, byte[] value) {
            try {
                String message = "";
                switch (characteristic) {
                    case Remotte.Characteristics.MANUFACTURER_NAME:
                        String manufacturerName = new String(value, "utf-8");
                        message = String.format("Manufacturer Name: %s", manufacturerName);
                        break;
                    case Remotte.Characteristics.BATTERY_LEVEL:
                        int batteryLevel = value[0];
                        message = String.format("Battery Level %d%.", batteryLevel);
                        break;
                    case Remotte.Characteristics.FIRMWARE_VERSION:
                        String firmwareVersion = new String(value, "utf-8");
                        message = String.format("Firmware Version: %s", firmwareVersion);
                        break;
                }

                Toast.makeText(SensorsActivity.this, message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(SensorsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensors);

        findViewById(R.id.VibrateCommand).setOnClickListener(this);
        findViewById(R.id.BuzzerCommand).setOnClickListener(this);
        findViewById(R.id.BatteryCommand).setOnClickListener(this);

        temperatureView     = (TextView)findViewById(R.id.Temperature);
        connectionStateView = (TextView)findViewById(R.id.ConnectionState);
        accelerometerView = (TextView)findViewById(R.id.Accelerometer);
        gyroscopeView = (TextView)findViewById(R.id.Gyro);
        altimeterView= (TextView)findViewById(R.id.Altimeter);

        connectionStateView.setText("Disconnected");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (remotte == null) {
            remotte = new Remotte.Builder()
                    .setDeviceAddress(DeviceBus.deviceAddress)
                    .setOnCharacteristicReadCallback(characteristicReadCallback)
                    .setConfiguration(new Remotte.Configuration()
                                    .enableConnectionStateChange(connectionStateCallback)
                                    .enableTemperatureSensor(true, temperatureSensorCallback)
                                    .enableAccelerometerSensor(true, accelerometerCallback)
                                    .enableGyroscopeSensor(true, gyroscopeSensorCallback)
                                    .enableAltimeterSensor(true, altimeterSensorCallback)
                                    .enableKeysPressedCallback(keysPressedCallback)
                    )
                    .build(this);
        }

        remotte.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        remotte.disconnect();
    }

    @Override
    public void onClick(View view) {
       if (view.getId() == R.id.VibrateCommand) {
            if (remotte != null)
                remotte.enableHaptic(true, false);
        } else if (view.getId() == R.id.BuzzerCommand) {
            if (remotte != null)
                remotte.enableHaptic(false, true);
        } else if (view.getId() == R.id.BatteryCommand) {
            if (remotte != null)
                remotte.readCharacteristic(Remotte.Characteristics.FIRMWARE_VERSION);
        }
    }
}
