package com.mobandme.remotte;

/**
 * Copyright Mob&Me 2014 (@MobAndMe)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Website: http://mobandme.com
 * Contact: Txus Ballesteros <txus.ballesteros@mobandme.com>
 */

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import com.mobandme.remotte.helper.LogsHelper;
import com.mobandme.remotte.listener.AccelerometerSensorCallback;
import com.mobandme.remotte.listener.AltimeterSensorCallback;
import com.mobandme.remotte.listener.CharacteristicReadCallback;
import com.mobandme.remotte.listener.ConnectionStateChangeCallback;
import com.mobandme.remotte.listener.GyroscopeSensorCallback;
import com.mobandme.remotte.listener.KeysPressedCallback;
import com.mobandme.remotte.listener.TemperatureSensorCallback;
import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * This is the principal class to manage your Remotte device. Use it for example to connect and disconnecto from the Remotte or to read the sensors values.
 * Remember that to obtain an instance ad configure you connection do you need use a built-in Builder.
 */
public final class Remotte {

    public final static int BUTTON_PRESSED  = 1;
    public final static int BUTTON_RELEASED = 0;

    static final int MSG_CLIENT_REGISTERED     = 1;
    static final int MSG_STATE_CHANGED         = 2;
    static final int MSG_TEMPERATURE_CHANGED   = 3;
    static final int MSG_ACCELEROMETER_CHANGED = 4;
    static final int MSG_GYROSCOPE_CHANGED     = 5;
    static final int MSG_BAROMETER_CHANGED     = 6;
    static final int MSG_KEY_PRESSED           = 7;
    static final int MSG_CHARACTERISTIC_READED = 8;

    static final String EXTRA_CONNECTION_STATE            = "CONNECTION_STATE";
    static final String EXTRA_TEMPERATURE_VALUE           = "TEMPERATURE_VALUE";
    static final String EXTRA_ACCELEROMETER_VALUE_X       = "ACCELEROMETER_VALUE_X";
    static final String EXTRA_ACCELEROMETER_VALUE_Y       = "ACCELEROMETER_VALUE_Y";
    static final String EXTRA_ACCELEROMETER_VALUE_Z       = "ACCELEROMETER_VALUE_Z";
    static final String EXTRA_GYROSCOPE_VALUE_X           = "GYROSCOPE_VALUE_X";
    static final String EXTRA_GYROSCOPE_VALUE_Y           = "GYROSCOPE_VALUE_Y";
    static final String EXTRA_GYROSCOPE_VALUE_Z           = "GYROSCOPE_VALUE_Z";
    static final String EXTRA_ALTIMETER_PRESSURE_VALUE    = "ALTIMETER_PRESSURE_VALUE";
    static final String EXTRA_ALTIMETER_ALTITUDE_VALUE    = "ALTIMETER_ALTITUDE_VALUE";
    static final String EXTRA_KEY_POWER_STATE             = "KEY_POWER_STATE";
    static final String EXTRA_KEY_CENTER_STATE            = "KEY_CENTER_STATE";
    static final String EXTRA_CHARACTERISTIC              = "GATT_CHARACTERISTIC";
    static final String EXTRA_CHARACTERISTIC_VALUE        = "CHARACTERISTIC_VALUE";
    static final String EXTRA_DEVICE_ADDRESS              = "BT_DEVICE_ADDRESS";

    /**
     * Use this class to set the device type to the you want connect.
     */
    public static class Devices {

        /**
         * Use this constant if you want connect to Remotte device.
         */
        public static int REMOTTE   = 1;

        /**
         * Use this constant if you want connect to Sensor Tag device.
         */
        public static int SENSOR_TAG = 2;
    }

    private Builder                       mBuilder;
    private Messenger                     mRemotteService;
    private Messenger                     mMessenger;

    private void                          setBuilder(Builder builder) { this.mBuilder = builder; }
    private Builder                       getBuilder() { return this.mBuilder; }
    public  Context                       getContext() { return getBuilder().getContext(); }

    /**
     * Primary constructor of the class.
      * @param builder Builder to be used by the instance.
     */
    private Remotte(Builder builder) {
        setBuilder(builder);
        LogsHelper.setContext(getContext());
        mMessenger = new Messenger(new IncomingHandler(this));
    }

    /**
     * Use this method to read a specific Remotte characteristic, remember that this call is asynchronous and you will receive the result into
     * ReadCharacteristic Callback previously configured on your Remote Builder.
     * @param remotteCharacteristic Use this parameter to determine the Remotte characteristic that you want read. You can use any constants defined by the {@link com.mobandme.remotte.Remotte.Characteristics} class.
     */
    public void readCharacteristic(int remotteCharacteristic) {
        try {

            if (!Characteristics.isValid(remotteCharacteristic))
                throw new RuntimeException("Invalid Remotte characteristic, please use one of constants defined by RemotteCharacteristic class");


            LogsHelper.log(LogsHelper.DEBUG, "Sending read characteristic command.");
            Message message = Message.obtain(null, RemotteService.MSG_READ_CHARACTERISTC);
            if (message != null) {
                Bundle parameters = new Bundle();
                parameters.putInt(RemotteService.EXTRA_CHARACTERISTIC, remotteCharacteristic);
                message.setData(parameters);
                mRemotteService.send(message);
            }

        } catch (Exception e) {
            LogsHelper.log(LogsHelper.ERROR, "Error sending read characteristic command.", e);
        }
    }

    /**
     * Use this method to connect to Remotte device.
     */
    public void connect() {
        LogsHelper.log(LogsHelper.DEBUG, "Sending Connect command to Remotte.");
        bindToRemotteService(getContext());
    }

    /**
     * Use this method to start the connection with the Remotte device.
     */
    private void connectoToDevice() {

        try {

            LogsHelper.log(LogsHelper.DEBUG, "Starting connection process to Remotte Device.");
            Message message = Message.obtain(null, RemotteService.MSG_COMMAND_CONNECT);
            if (message != null) {
                Bundle parameters = new Bundle();
                parameters.putString(EXTRA_DEVICE_ADDRESS, getBuilder().getDeviceAddress());
                message.setData(parameters);
                mRemotteService.send(message);
            }

        } catch (Exception e) {
            LogsHelper.log(LogsHelper.ERROR, "Error sending connect command to Remotte Service.", e);
        }
    }

    /**
     * Use this method to disconnect from Remotte device.
     */
    public void disconnect() {
        try {

            LogsHelper.log(LogsHelper.DEBUG, "Sending Disconnect command to Remotte.");
            Message message = Message.obtain(null, RemotteService.MSG_COMMAND_DISCONNECT);
            if (message != null) {
                Bundle parameters = new Bundle();
                parameters.putString(EXTRA_DEVICE_ADDRESS, getBuilder().getDeviceAddress());
                message.setData(parameters);
                mRemotteService.send(message);
            } else {
                LogsHelper.log(LogsHelper.ERROR, "Error sending disconnect command to Remotte Service.");
            }

        } catch (Exception e) {
            LogsHelper.log(LogsHelper.ERROR, "Error sending disconnect command to Remotte Service.", e);
        }
    }

    /**
     * Use this method to enable a haptic reaction in Remotte.
     * @param vibrator Pass true if you want enable the built in vibrator.
     * @param buzzer Pass true if you want play the built in buzzer.
     */
    public void enableHaptic(boolean vibrator, boolean buzzer) {
        LogsHelper.log(LogsHelper.DEBUG, "Sending Haptic command to Remotte.");
        try {

            if (vibrator || buzzer) {
                byte configuration = 0x00;
                if (vibrator && !buzzer) {
                    configuration = 0x01;
                } else if (!vibrator && buzzer) {
                    configuration = 0x02;
                } else if (vibrator && buzzer) {
                    configuration = 0x03;
                }

                Message message = Message.obtain(null, RemotteService.MSG_ENABLE_HAPTIC);
                if (message != null) {
                    Bundle parameters = new Bundle();
                    parameters.putByte(RemotteService.EXTRA_HAPTIC_CONFIGURATION, configuration);
                    message.setData(parameters);

                    mRemotteService.send(message);
                } else {
                    LogsHelper.log(LogsHelper.ERROR, "Error sending haptic command to Remotte Service.");
                }

            }

        } catch (Exception e) {
            LogsHelper.log(LogsHelper.ERROR, "Error sending haptic command to Remotte Service.", e);
        }
    }

    private void bindToRemotteService(Context context) {
        LogsHelper.log(LogsHelper.DEBUG, "Connecting to Remotte Service.");
        Intent remotteServiceIntent = new Intent(context, RemotteService.class);
        context.bindService(remotteServiceIntent, mRemotteServiceConnection, Service.BIND_AUTO_CREATE);
    }

    private void unbindFromRemotteService() {
        getContext().unbindService(mRemotteServiceConnection);
    }

    private void configureGatt() {
        try {
            LogsHelper.log(LogsHelper.DEBUG, "Sending GATT Configuration.");
            if (mRemotteService != null) {
                Message message = Message.obtain(null, RemotteService.MSG_CONFIGURE_GATT);
                if (message != null) {
                    Bundle parameters = new Bundle();
                    parameters.putString(Remotte.EXTRA_DEVICE_ADDRESS, getBuilder().getDeviceAddress());
                    parameters.putParcelable(RemotteService.EXTRA_GATT_CONFIGURATION, getBuilder().getConfiguration());
                    message.setData(parameters);

                    mRemotteService.send(message);
                }
            }

        } catch (Exception e) {
            LogsHelper.log(LogsHelper.ERROR, "Error sending GATT configuration to the service.");
        }
    }

    /**
     * This class manage the connection with the Remotte service.
     */
    private ServiceConnection mRemotteServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogsHelper.log(LogsHelper.DEBUG, "Connected to Remotte Service.");
            mRemotteService = new Messenger(service);

            try {
                if (mRemotteService != null) {
                    Message message = Message.obtain(null, RemotteService.MSG_REGISTER_CLIENT);
                    if (message != null) {
                        message.replyTo = mMessenger;
                        mRemotteService.send(message);
                    }
                }
            } catch (Exception e) {
                LogsHelper.log(LogsHelper.ERROR, "Error during client registration process.", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogsHelper.log(LogsHelper.DEBUG, "Disconnected from Remotte Service.");
            mRemotteService = null;
        }
    };

    /**
     * This class manage all messages receibed from the Bluetooth Service.
     */
    private final class IncomingHandler extends Handler {
        private final WeakReference<Remotte> mRemotte;

        public IncomingHandler(Remotte remotte) {
            mRemotte = new WeakReference<Remotte>(remotte);
        }

        @Override
        public void handleMessage(Message message) {
            Remotte remotte = mRemotte.get();
            if (remotte != null) {
                switch (message.what) {
                    case MSG_CLIENT_REGISTERED:
                        remotte.connectoToDevice();
                        break;
                    case MSG_STATE_CHANGED:
                        int gattConnectionState = message.getData().getInt(EXTRA_CONNECTION_STATE);
                        if (gattConnectionState == BluetoothGatt.STATE_CONNECTED)
                            configureGatt();

                        if (gattConnectionState == BluetoothGatt.STATE_DISCONNECTED)
                            remotte.unbindFromRemotteService();

                        if (getBuilder().getConfiguration().getConnectionStateChangeCallback() != null) {
                            getBuilder().getConfiguration().getConnectionStateChangeCallback().onConnectionStateChange(
                                    remotte,
                                    gattConnectionState);
                        }
                        break;
                    case MSG_TEMPERATURE_CHANGED:
                        if (getBuilder().getConfiguration().getTemperatureSensorCallback() != null) {
                            getBuilder().getConfiguration().getTemperatureSensorCallback().onTemperatureChange(
                                    remotte,
                                    message.getData().getDouble(EXTRA_TEMPERATURE_VALUE));
                        }
                        break;
                    case MSG_ACCELEROMETER_CHANGED:
                        if (getBuilder().getConfiguration().getAccelerometerSensorCallback() != null) {
                            getBuilder().getConfiguration().getAccelerometerSensorCallback().onAccelerometerChange(
                                remotte,
                                message.getData().getDouble(EXTRA_ACCELEROMETER_VALUE_X),
                                message.getData().getDouble(EXTRA_ACCELEROMETER_VALUE_Y),
                                message.getData().getDouble(EXTRA_ACCELEROMETER_VALUE_Z)
                            );
                        }
                        break;
                    case MSG_GYROSCOPE_CHANGED:
                        if (getBuilder().getConfiguration().getGyroscopeSensorCallback() != null) {
                            getBuilder().getConfiguration().getGyroscopeSensorCallback().onGyroscopeChange(
                                    remotte,
                                    message.getData().getDouble(EXTRA_GYROSCOPE_VALUE_X),
                                    message.getData().getDouble(EXTRA_GYROSCOPE_VALUE_Y),
                                    message.getData().getDouble(EXTRA_GYROSCOPE_VALUE_Z)
                            );
                        }
                        break;
                    case MSG_BAROMETER_CHANGED:
                        if (getBuilder().getConfiguration().getAltimeterSensorCallback() != null) {
                            getBuilder().getConfiguration().getAltimeterSensorCallback().onAltimeterChange(
                                    remotte,
                                    message.getData().getDouble(EXTRA_ALTIMETER_PRESSURE_VALUE),
                                    message.getData().getDouble(EXTRA_ALTIMETER_ALTITUDE_VALUE)
                            );
                        }
                        break;
                    case MSG_KEY_PRESSED:
                        if (getBuilder().getConfiguration().getKeysPressedCallback() != null) {
                            getBuilder().getConfiguration().getKeysPressedCallback().onKeyPress(
                                    remotte,
                                    message.getData().getInt(EXTRA_KEY_POWER_STATE),
                                    message.getData().getInt(EXTRA_KEY_CENTER_STATE)
                            );
                        }
                        break;
                    case MSG_CHARACTERISTIC_READED:
                        if (getBuilder().getOnCharacteristicReadCallback() != null) {
                            getBuilder().getOnCharacteristicReadCallback().onCharacteristicRead(
                                    remotte,
                                    message.getData().getInt(EXTRA_CHARACTERISTIC),
                                    message.getData().getByteArray(EXTRA_CHARACTERISTIC_VALUE));
                        }
                        break;
                    default:
                        super.handleMessage(message);
                        break;
                }
            } else {
                super.handleMessage(message);
            }
        }
    }

    /**
     * Builder class for Remotte management object.
     */
    public static final class Builder {

        private Context                       mContext;
        private String                        mDeviceAddress;
        private Configuration                 mConfiguration;
        private CharacteristicReadCallback    mCharacteristicReadCallback;

        /**
         * Use this method to get your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration getConfiguration() { return this.mConfiguration; }

        /**
         * Use this method to set your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         * @param configuration Pass your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Builder} instance.
         */
        public Builder       setConfiguration(Configuration configuration) {
            this.mConfiguration = configuration;
            return this;
        }

        /**
         * Use this method to set your {@link com.mobandme.remotte.listener.CharacteristicReadCallback} instance, this callback will be called when you call to {@link #readCharacteristic(int)} method.
         * @param callback Pass your {@link com.mobandme.remotte.listener.CharacteristicReadCallback} instance.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Builder} instance.
         */
        public  Builder setOnCharacteristicReadCallback(CharacteristicReadCallback callback) { this.mCharacteristicReadCallback = callback; return this; }
        private CharacteristicReadCallback getOnCharacteristicReadCallback() { return this.mCharacteristicReadCallback; }

        /**
         * Use this method to configure the Remotte device addrress.
         * @param address Pass the Bluetooth Device address, for example 00:00:00:00:00:00
         * @return Returns your {@link com.mobandme.remotte.Remotte.Builder} instance.
         */
        public  Builder setDeviceAddress(String address) {
            this.mDeviceAddress = address;
            return this;
        }
        private String  getDeviceAddress() { return this.mDeviceAddress; }

        private Context getContext() { return this.mContext.getApplicationContext(); }
        private void    setContext(Context context) { this.mContext = context; }

        /**
         * Use this method to build and obtain your {@link com.mobandme.remotte.Remotte} instance.
         * @param context Pass a {@link android.content.Context}
         * @return Returns a {@link com.mobandme.remotte.Remotte} instance.
         */
        public Remotte build(Context context) {
            if (mConfiguration == null)
                throw new RuntimeException("Not configuration set, please use setConfiguration method to set it.");

            setContext(context);
            return new Remotte(this);
        }
    }

    /**
     * Use this class to configure all referred aspects of the Remotte Device.
     */
    public static class Configuration implements Parcelable {

        private AltimeterSensorCallback         mAltimeterSensorCallback;
        private GyroscopeSensorCallback         mGyroscopeSensorCallback;
        private AccelerometerSensorCallback     mAccelerometerSensorCallback;
        private TemperatureSensorCallback       mTemperatureSensorCallback;
        private ConnectionStateChangeCallback   mConnectionStateChangeCallback;
        private KeysPressedCallback             mKeysPressedCallback;

        private boolean mConnectionStateCallbackEnabled     = false;
        private boolean mTemperatureSensorCallbackEnabled   = false;
        private boolean mAccelerometerSensorCallbackEnabled = false;
        private boolean mGyroscopeSensorCallbackEnabled     = false;
        private boolean mAltimeterSensorCallbackEnabled     = false;
        private boolean mKeysPressedCallbackEnabled         = false;
        private boolean mTemperatureSensorEnabled           = false;
        private boolean mAccelerometerSensorEnabled         = false;
        private boolean mGyroscopeSensorEnabled             = false;
        private boolean mAltimeterSensorEnabled             = false;

        private byte    mAccelerometerPeriod = 0x64; //This value is equivalent to 1 second period.
        private byte    mGyroscopePeriod     = 0x64; //This value is equivalent to 1 second period.
        private byte    mAltimeterPeriod     = 0x64; //This value is equivalent to 1 second period.
        private byte    mTemperaturePeriod   = 0x64; //This value is equivalent to 1 second period.

        private int     mDevice = Remotte.Devices.REMOTTE;

        public  Configuration() { }
        private Configuration(Parcel in) { readFromParcel(in); }

        /**
         * Use this method to set the device type, remember use anyone of this constants, Remotte.Devices.REMOTTE or Remotte.Devices.SENSOR_TAG
         * @param deviceType Use to set the device type.
         */
        public Configuration setDevice(int deviceType) {
            if (deviceType != Devices.REMOTTE && deviceType != Devices.SENSOR_TAG)
                throw new RuntimeException("Invalid device type. Please use anyone of this constants, Remotte.Devices.REMOTTE or Remotte.Devices.SENSOR_TAG");

            this.mDevice = deviceType;
            return this;
        }
        public int getDevice() { return this.mDevice; }

        /**
         * This method provide configuration to notify connection state changes between de Smartphone and the Remotte.
         * @param connectionChangeCallback Use this callback to retrieve when the connection state changed.
         * @return
         */
        public Configuration enableConnectionStateChange(ConnectionStateChangeCallback connectionChangeCallback) {
            this.mConnectionStateChangeCallback = connectionChangeCallback;
            this.mConnectionStateCallbackEnabled = (connectionChangeCallback != null);
            return this;
        }

        /**
         * Use this method to configure your On Key Press callback. Use it when you want manage physical buttons events.
         * @param keysPressedCallback Pass your {@link com.mobandme.remotte.listener.KeysPressedCallback} implementation.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableKeysPressedCallback(KeysPressedCallback keysPressedCallback) {
            this.mKeysPressedCallback = keysPressedCallback;
            this.mKeysPressedCallbackEnabled = (keysPressedCallback != null);
            return this;
        }

        /**
         * Use this method to enable the Altimeter built in sensor.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableAltimeterSensor(boolean enabled) { mAltimeterSensorEnabled = enabled; return this; }

        /**
         * Use this method to enable the Altimeter built in sensor and subscribe to their notifications of changes.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @param sensorChangeListener Pass your {@link com.mobandme.remotte.listener.AltimeterSensorCallback} implementation.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableAltimeterSensor(boolean enabled, AltimeterSensorCallback sensorChangeListener) {
            this.mAltimeterSensorCallback = sensorChangeListener;
            this.mAltimeterSensorCallbackEnabled = (sensorChangeListener != null);
            return enableAltimeterSensor(enabled);
        }

        /**
         * Use this method to enable the Altimeter built in sensor and subscribe to their notifications of changes and set the notifications time period.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @param period Pass the time in milliseconds that you will like receive the notifications of changes. Remember that the minimum value it's 100 and the maximum it's 2550.
         * @param sensorChangeListener Pass your {@link com.mobandme.remotte.listener.AltimeterSensorCallback} implementation.
         * @throws {@link java.lang.RuntimeException} when the period not is valid.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableAltimeterSensor(boolean enabled, int period, AltimeterSensorCallback sensorChangeListener) {
            if (period < 100)
                throw new RuntimeException("Invalid period, the minimum value is 100 milliseconds.");
            mAltimeterPeriod = (byte)(period / 10);
            return enableAltimeterSensor(enabled, sensorChangeListener);
        }

        /**
         * Use this method to enable the Gyroscope built in sensor.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableGyroscopeSensor(boolean enabled) { mGyroscopeSensorEnabled = enabled; return this; }

        /**
         * Use this method to enable the Gyroscope built in sensor and subscribe to their notifications of changes.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @param sensorChangeListener Pass your {@link com.mobandme.remotte.listener.GyroscopeSensorCallback} implementation.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableGyroscopeSensor(boolean enabled, GyroscopeSensorCallback sensorChangeListener) {
            this.mGyroscopeSensorCallback = sensorChangeListener;
            this.mGyroscopeSensorCallbackEnabled = (sensorChangeListener != null);
            return enableGyroscopeSensor(enabled);
        }

        /**
         * Use this method to enable the Gyroscope built in sensor and subscribe to their notifications of changes and set the notifications time period.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @param period Pass the time in milliseconds that you will like receive the notifications of changes. Remember that the minimum value it's 100 and the maximum it's 2550.
         * @param sensorChangeListener Pass your {@link com.mobandme.remotte.listener.GyroscopeSensorCallback} implementation.
         * @throws {@link java.lang.RuntimeException} when the period not is valid.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableGyroscopeSensor(boolean enabled, int period, GyroscopeSensorCallback sensorChangeListener) {
            if (period < 100)
                throw new RuntimeException("Invalid period, the minimum value is 100 milliseconds.");
            mGyroscopePeriod = (byte)(period / 10);
            return enableGyroscopeSensor(enabled, sensorChangeListener);
        }

        /**
         * Use this method to enable the Accelerometer built in sensor.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableAccelerometerSensor(boolean enabled) { mAccelerometerSensorEnabled = enabled; return this; }

        /**
         * Use this method to enable the Accelerometer built in sensor and subscribe to their notifications of changes.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @param sensorChangeListener Pass your {@link com.mobandme.remotte.listener.AccelerometerSensorCallback} implementation.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableAccelerometerSensor(boolean enabled, AccelerometerSensorCallback sensorChangeListener) {
            this.mAccelerometerSensorCallback = sensorChangeListener;
            this.mAccelerometerSensorCallbackEnabled = (sensorChangeListener != null);
            return enableAccelerometerSensor(enabled);
        }

        /**
         * Use this method to enable the Accelerometer built in sensor and subscribe to their notifications of changes and set the notifications time period.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @param period Pass the time in milliseconds that you will like receive the notifications of changes. Remember that the minimum value it's 100 and the maximum it's 2550.
         * @param sensorChangeListener Pass your {@link com.mobandme.remotte.listener.AccelerometerSensorCallback} implementation.
         * @throws {@link java.lang.RuntimeException} when the period not is valid.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableAccelerometerSensor(boolean enabled, int period, AccelerometerSensorCallback sensorChangeListener) {
            if (period < 100)
                throw new RuntimeException("Invalid period, the minimum value is 100 milliseconds.");
            mAccelerometerPeriod = (byte)(period / 10);
            return enableAccelerometerSensor(enabled, sensorChangeListener);
        }

        /**
         * Use this method to enable the Temperature built in sensor.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableTemperatureSensor(boolean enabled) { mTemperatureSensorEnabled = enabled; return this; }

        /**
         * Use this method to enable the Temperature built in sensor and subscribe to their notifications of changes.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @param sensorChangeListener Pass your {@link com.mobandme.remotte.listener.TemperatureSensorCallback} implementation.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableTemperatureSensor(boolean enabled, TemperatureSensorCallback sensorChangeListener) {
            this.mTemperatureSensorCallback = sensorChangeListener;
            this.mTemperatureSensorCallbackEnabled = (sensorChangeListener != null);
            return enableTemperatureSensor(enabled);
        }

        /**
         * Use this method to enable the Temperature built in sensor and subscribe to their notifications of changes and set the notifications time period.
         * @param enabled If you want enable the sensor, pass true or otherwise pass false.
         * @param period Pass the time in milliseconds that you will like receive the notifications of changes. Remember that the minimum value it's 100 and the maximum it's 2550.
         * @param sensorChangeListener Pass your {@link com.mobandme.remotte.listener.TemperatureSensorCallback} implementation.
         * @throws {@link java.lang.RuntimeException} when the period not is valid.
         * @return Returns your {@link com.mobandme.remotte.Remotte.Configuration} instance.
         */
        public Configuration enableTemperatureSensor(boolean enabled, int period, TemperatureSensorCallback sensorChangeListener) {
            if (period < 100)
                throw new RuntimeException("Invalid period, the minimum value is 100 milliseconds.");
            mTemperaturePeriod = (byte)(period / 10);
            return enableTemperatureSensor(enabled, sensorChangeListener);
        }

        public boolean  getTemperatureSensorEnabled()       { return this.mTemperatureSensorEnabled; }
        public boolean  getAccelerometerSensorEnabled()     { return this.mAccelerometerSensorEnabled; }
        public boolean  getGyroscopeSensorEnabled()         { return this.mGyroscopeSensorEnabled; }
        public boolean  getAltimeterSensorEnabled()         { return this.mAltimeterSensorEnabled; }

        public boolean  getConnectionStateCallbackEnabled(){ return this.mConnectionStateCallbackEnabled; }
        public boolean  getTemperatureCallbackEnabled()    { return this.mTemperatureSensorCallbackEnabled; }
        public boolean  getAltimeterCallbackEnabled()      { return this.mAltimeterSensorCallbackEnabled; }
        public boolean  getAccelerometerCallbackEnabled()  { return this.mAccelerometerSensorCallbackEnabled; }
        public boolean  getGyroscopeCallbackEnabled()      { return this.mGyroscopeSensorCallbackEnabled; }
        public boolean  getKeysPressedCallbackEnabled()      { return this.mKeysPressedCallbackEnabled; }

        protected AltimeterSensorCallback       getAltimeterSensorCallback() { return this.mAltimeterSensorCallback; }
        protected GyroscopeSensorCallback       getGyroscopeSensorCallback() { return this.mGyroscopeSensorCallback; }
        protected AccelerometerSensorCallback   getAccelerometerSensorCallback() { return this.mAccelerometerSensorCallback; }
        protected TemperatureSensorCallback     getTemperatureSensorCallback() { return this.mTemperatureSensorCallback; }
        protected ConnectionStateChangeCallback getConnectionStateChangeCallback() { return this.mConnectionStateChangeCallback; }
        protected KeysPressedCallback           getKeysPressedCallback() { return this.mKeysPressedCallback; }

        public byte[] getAltimeterPeriod() { return new byte[] { this.mAltimeterPeriod }; }
        public byte[] getAccelerometerPeriod() { return new byte[] { this.mAccelerometerPeriod }; }
        public byte[] getGyroscopePeriod() { return new byte[] { this.mGyroscopePeriod }; }
        public byte[] getTemperaturePeriod() { return new byte[] { this.mTemperaturePeriod }; }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(Boolean.toString(this.mTemperatureSensorEnabled));
            out.writeString(Boolean.toString(this.mAccelerometerSensorEnabled));
            out.writeString(Boolean.toString(this.mGyroscopeSensorEnabled));
            out.writeString(Boolean.toString(this.mAltimeterSensorEnabled));

            out.writeString(Boolean.toString(getConnectionStateCallbackEnabled()));
            out.writeString(Boolean.toString(getTemperatureCallbackEnabled()));
            out.writeString(Boolean.toString(getAccelerometerCallbackEnabled()));
            out.writeString(Boolean.toString(getGyroscopeCallbackEnabled()));
            out.writeString(Boolean.toString(getAltimeterCallbackEnabled()));
            out.writeString(Boolean.toString(getKeysPressedCallbackEnabled()));

            out.writeInt(getDevice());

            out.writeByte(this.mAltimeterPeriod);
            out.writeByte(this.mAccelerometerPeriod);
            out.writeByte(this.mGyroscopePeriod);
            out.writeByte(this.mTemperaturePeriod);
        }

        private void readFromParcel(Parcel in) {
            this.mTemperatureSensorEnabled = Boolean.valueOf(in.readString());
            this.mAccelerometerSensorEnabled = Boolean.valueOf(in.readString());
            this.mGyroscopeSensorEnabled = Boolean.valueOf(in.readString());
            this.mAltimeterSensorEnabled = Boolean.valueOf(in.readString());

            this.mConnectionStateCallbackEnabled = Boolean.valueOf(in.readString());
            this.mTemperatureSensorCallbackEnabled = Boolean.valueOf(in.readString());
            this.mAccelerometerSensorCallbackEnabled = Boolean.valueOf(in.readString());
            this.mGyroscopeSensorCallbackEnabled = Boolean.valueOf(in.readString());
            this.mAltimeterSensorCallbackEnabled = Boolean.valueOf(in.readString());
            this.mKeysPressedCallbackEnabled = Boolean.valueOf(in.readString());

            this.mDevice = in.readInt();

            this.mAltimeterPeriod = in.readByte();
            this.mAccelerometerPeriod = in.readByte();
            this.mGyroscopePeriod = in.readByte();
            this.mTemperaturePeriod = in.readByte();
        }

        public static final Parcelable.Creator<Configuration> CREATOR = new Parcelable.Creator<Configuration>() {
            public Configuration createFromParcel(Parcel in) {
                return new Configuration(in);
            }

            public Configuration[] newArray(int size) {
                return new Configuration[size];
            }
        };
    }

    /**
     * This class define the Remotte characteristics that you can read asynchronously.
     */
    public static class Characteristics {
        static final int    UNKNOW = 0;

        /**
         * Use this constant to read the Battery Level of Remotte.
         */
        public static final int    BATTERY_LEVEL = 1;

        /**
         * Use this constant to read the Manufacturer Name from Remotte.
         */
        public static final int    MANUFACTURER_NAME = 2;

        /**
         * Use this constant to read the Firmware Version from Remotte.
         */
        public static final int    FIRMWARE_VERSION = 3;

        /**
         * Use this method to obtain the UUID of the GATT service for the specified Characteristic.
         * @param characteristic Pass the identifier constant of the characteristic. You can use one of the values defined with RemotteCharacteristic class.
         * @return Return a GATT Servide UUID o throws an exception if it does not exist.
         */
        static UUID getGattServiceUUID(int device, int characteristic) {
            if (device == Devices.REMOTTE) {
                switch (characteristic) {
                    case BATTERY_LEVEL:
                        return GattDeviceRemotte.BATTERY_SERVICE;
                    case MANUFACTURER_NAME:
                    case FIRMWARE_VERSION:
                        return GattDeviceSensorTag.DEVICE_INFO_SERVICE;
                    default:
                        throw new RuntimeException("Invalid Remotte characteristic.");
                }
            } else {
                switch (characteristic) {
                    case BATTERY_LEVEL:
                        return GattDeviceSensorTag.BATTERY_SERVICE;
                    case MANUFACTURER_NAME:
                    case FIRMWARE_VERSION:
                       return GattDeviceSensorTag.DEVICE_INFO_SERVICE;
                    default:
                        throw new RuntimeException("Invalid Remotte characteristic.");
                }
            }
        }

        /**
         * Use this method to obtain the UUID of the GATT Characteristic for the specified value.
         * @param characteristic Pass the identifier constant of the characteristic. You can use one of the values defined with RemotteCharacteristic class.
         * @return Return a GATT Servide UUID o throws an exception if it does not exist.
         */
        static UUID getGattCharacteristicUUID(int device, int characteristic) {
            if (device == Devices.REMOTTE) {
                switch (characteristic) {
                    case BATTERY_LEVEL:
                        return GattDeviceRemotte.BATTERY_LEVEL_CHARACTERISTIC;
                    case MANUFACTURER_NAME:
                        return GattDeviceRemotte.DEVICE_INFO_MANUFACTURER_NAME_CHARACTERISTIC;
                    case FIRMWARE_VERSION:
                        return GattDeviceRemotte.DEVICE_INFO_FIRMWARE_REVISION_CHARACTERISTIC;
                    default:
                        throw new RuntimeException("Invalid Remotte characteristic.");
                }
            } else {
                switch (characteristic) {
                    case BATTERY_LEVEL:
                        return GattDeviceSensorTag.BATTERY_LEVEL_CHARACTERISTIC;
                    case MANUFACTURER_NAME:
                       return GattDeviceSensorTag.DEVICE_INFO_MANUFACTURER_NAME_CHARACTERISTIC;
                    case FIRMWARE_VERSION:
                        return GattDeviceSensorTag.DEVICE_INFO_FIRMWARE_REVISION_CHARACTERISTIC;
                    default:
                        throw new RuntimeException("Invalid Remotte characteristic.");
                }
            }
        }

        /**
         * Use this method to get the correlation value between GATT Characteristic UUID and type of Remotte characteristics.
         * @param gattCharacteristic UUID of the GATT Characteristic
         * @return Returns the value of the Remotte type.
         */
        static int getCharacteristicTypeFromGattCharacteristicUUID(int device, UUID gattCharacteristic) {
            int returnedValue = UNKNOW;

            if (device == Devices.REMOTTE) {
                if (GattDeviceRemotte.BATTERY_LEVEL_CHARACTERISTIC.equals(gattCharacteristic)) {
                    returnedValue = BATTERY_LEVEL;
                } else if (GattDeviceRemotte.DEVICE_INFO_MANUFACTURER_NAME_CHARACTERISTIC.equals(gattCharacteristic)) {
                    returnedValue = MANUFACTURER_NAME;
                } else if (GattDeviceRemotte.DEVICE_INFO_FIRMWARE_REVISION_CHARACTERISTIC.equals(gattCharacteristic)) {
                    returnedValue = FIRMWARE_VERSION;
                }
            } else {
                if (GattDeviceSensorTag.BATTERY_LEVEL_CHARACTERISTIC.equals(gattCharacteristic)) {
                    returnedValue = BATTERY_LEVEL;
                } else if (GattDeviceSensorTag.DEVICE_INFO_MANUFACTURER_NAME_CHARACTERISTIC.equals(gattCharacteristic)) {
                    returnedValue = MANUFACTURER_NAME;
                } else if (GattDeviceSensorTag.DEVICE_INFO_FIRMWARE_REVISION_CHARACTERISTIC.equals(gattCharacteristic)) {
                    returnedValue = FIRMWARE_VERSION;
                }
            }

            return returnedValue;
        }

        /**
         * Use this method to validate if the characteristic is a valid Remotte characteristic.
         * @param characteristic Pass the characteristic type based on Remotte.Characteristics constants.
         * @return Returns true if the characteristic type is valid or false if not.
         */
        static boolean isValid(int characteristic) {
            boolean returnedValue = true;

            if (characteristic != BATTERY_LEVEL &&
                characteristic != MANUFACTURER_NAME &&
                characteristic != FIRMWARE_VERSION)
                returnedValue = false;

            return returnedValue;
        }
    }
}
