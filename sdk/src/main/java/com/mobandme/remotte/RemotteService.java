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

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.content.Intent;
import android.content.Context;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.pm.PackageManager;
import android.os.Message;
import android.os.Messenger;

import com.mobandme.remotte.helper.LogsHelper;
import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This service it's the mayor of Bluetooth management. Her job it's encapsulate all of the logic and management of Bluetooth LE and GATT.
 * @author Txus Ballesteros
 * @version 1
 * @since 1
 */
public class RemotteService extends Service {

    static final int MSG_REGISTER_CLIENT    = 1;
    static final int MSG_COMMAND_CONNECT    = 2;
    static final int MSG_COMMAND_DISCONNECT = 3;
    static final int MSG_CONFIGURE_GATT     = 4;
    static final int MSG_ENABLE_HAPTIC      = 5;
    static final int MSG_READ_CHARACTERISTC = 6;

    static final String EXTRA_GATT_CONFIGURATION     = "GATT_CONFIGURATION";
    static final String EXTRA_HAPTIC_CONFIGURATION   = "HAPTIC_CONFIGURATION";
    static final String EXTRA_CHARACTERISTIC         = "GATT_CHARACTERISTIC";

    private       BluetoothManager          mBluetoothManager;
    private       BluetoothAdapter          mBluetoothAdapter;
    private       BluetoothDevice           mBluetoothDevice;
    private       BluetoothGatt             mBluetoothGatt;
    private       Messenger                 mClientMessenger;
    private       Boolean                   mBusy = false;
    private       Queue<RemotteGattCommand> mBluetoothStack = new ConcurrentLinkedQueue<RemotteGattCommand>();
    private final IncomingHandler           mHandler;
    private final Messenger                 mMessenger;
    private       Remotte.Configuration     mGattConfiguration;

    /**
     * Primary constructor of the service.
     */
    public RemotteService() {
        mHandler = new IncomingHandler(this);
        mMessenger = new Messenger(mHandler);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return Service.START_STICKY; }

    /**
     * Use this method to start connection process.
     * @param deviceAddress Bluetooth device address.
     */
    protected void connect(String deviceAddress) {
        LogsHelper.log(LogsHelper.VERBOSE, "Remotte service connect.");
        if (deviceAddress == null || deviceAddress.trim().equals("")) {
            throw new IllegalArgumentException("Invalid arguments exception.");
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            LogsHelper.log(LogsHelper.ERROR, "This device does not support Bluetooth 4.0.");
            throw new RuntimeException("This device does not support Bluetooth 4.0.");
        }

        mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        //Check if the Bluetooth adapter it's enabled into device.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            LogsHelper.log(LogsHelper.ERROR, "The device have the bluetooth connection disabled.");
            throw new RuntimeException("The device have the bluetooth connection disabled.");
        }

        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        if (mBluetoothDevice == null) {
            LogsHelper.log(LogsHelper.ERROR, "Unable access to Remotte device.");
            throw new RuntimeException("Unable access to Remotte device.");
        }


        if (mBluetoothGatt != null)
            mBluetoothGatt.disconnect();

        mBluetoothGatt = mBluetoothDevice.connectGatt(this, false, mBluetoothGattCallback);
        if (mBluetoothGatt == null) {
            LogsHelper.log(LogsHelper.ERROR, "Unable access to Remotte device.");
            throw new RuntimeException("Unable access to Remotte device.");
        }
    }

    /**
     * Use this method to close connection with the Remotte device.
     * @param deviceAddress Bluetooth device address.
     */
    protected void disconect(String deviceAddress) {
        LogsHelper.log(LogsHelper.DEBUG, "Disconnecting from Remotte.");
        if (isConnected()) {
            disableSensorsNotifications();
            disableSensors();

            if (mBluetoothGatt != null)
                mBluetoothGatt.disconnect();

            mBluetoothGatt    = null;
            mBluetoothDevice  = null;
            mBluetoothManager = null;
            mBluetoothAdapter = null;
            mBluetoothStack   = null;
        }
    }

    /**
     * Use this method to retrieve the device type.
     * @return Returns the device type.
     */
    private int getDeviceType() {
        return getGattConfiguration().getDevice();
    }

    /**
     * This method check the connectio state with the Renotte device.
     * @return true if conection state it's connected.
     */
    private boolean isConnected() {
        if (!isGattReady())
            return false;

        int connectionState = mBluetoothManager.getConnectionState(mBluetoothDevice, BluetoothProfile.GATT);
        if (connectionState == BluetoothProfile.STATE_CONNECTED)
            return true;
        else
            return false;
    }

    /**
     * This method check all basic bluetooth elemets needed to manage GATT services.
     * @return true if all it's ok.
     */
    private boolean isGattReady() {
        if (mBluetoothManager == null)
            return false;

        if (mBluetoothAdapter == null)
            return false;

        if (mBluetoothGatt == null)
            return false;

        return true;
    }

    private void setGattConfiguration(Remotte.Configuration configuration) { this.mGattConfiguration = configuration; }

    private Remotte.Configuration getGattConfiguration() { return this.mGattConfiguration; }

    private void enableSensors() {
        LogsHelper.log(LogsHelper.DEBUG, "Enabling Remotte sensors.");

        if (getDeviceType() == Remotte.Devices.REMOTTE) {
            if (getGattConfiguration().getTemperatureSensorEnabled())
                configureSensor(GattDeviceRemotte.TEMPERATURE_SERVICE, GattDeviceRemotte.TEMPERATURE_CONFIG_CHARACTERISTIC, true);
            if (getGattConfiguration().getAccelerometerSensorEnabled())
                configureSensor(GattDeviceRemotte.ACCELEROMETER_SERVICE, GattDeviceRemotte.ACCELEROMETER_CONFIG_CHARACTERISTIC, true);
            if (getGattConfiguration().getGyroscopeSensorEnabled())
                configureSensor(GattDeviceSensorTag.GYROSCOPE_SERVICE, GattDeviceSensorTag.GYROSCOPE_CONFIG_CHARACTERISTIC, GattDevice.ENABLE_GYROSCOPE_3_AXIS_SENSOR); //Enable the 3 axis of the sensor.
            if (getGattConfiguration().getAltimeterSensorEnabled()) {
                configureSensor(GattDeviceSensorTag.ALTIMETER_SERVICE, GattDeviceSensorTag.ALTIMETER_CONFIG_CHARACTERISTIC, GattDevice.SENSOR_CALIBRATION);
                readCharacteristic(GattDeviceRemotte.ALTIMETER_SERVICE, GattDeviceRemotte.ALTIMETER_CALIBRATION_CHARACTERITIC);
            }
        } else {
            if (getGattConfiguration().getTemperatureSensorEnabled())
                configureSensor(GattDeviceSensorTag.TEMPERATURE_SERVICE, GattDeviceSensorTag.TEMPERATURE_CONFIG_CHARACTERISTIC, true);
            if (getGattConfiguration().getAccelerometerSensorEnabled())
                configureSensor(GattDeviceSensorTag.ACCELEROMETER_SERVICE, GattDeviceSensorTag.ACCELEROMETER_CONFIG_CHARACTERISTIC, true);
            if (getGattConfiguration().getGyroscopeSensorEnabled())
                configureSensor(GattDeviceSensorTag.GYROSCOPE_SERVICE, GattDeviceSensorTag.GYROSCOPE_CONFIG_CHARACTERISTIC, GattDevice.ENABLE_GYROSCOPE_3_AXIS_SENSOR); //Enable the 3 axis of the sensor.
            if (getGattConfiguration().getAltimeterSensorEnabled()) {
                configureSensor(GattDeviceSensorTag.ALTIMETER_SERVICE, GattDeviceSensorTag.ALTIMETER_CONFIG_CHARACTERISTIC, GattDevice.SENSOR_CALIBRATION);
                readCharacteristic(GattDeviceSensorTag.ALTIMETER_SERVICE, GattDeviceSensorTag.ALTIMETER_CALIBRATION_CHARACTERITIC);
            }
        }
    }

    private void disableSensors() {
        LogsHelper.log(LogsHelper.DEBUG, "Disabling Remotte sensors.");

        if (getDeviceType() == Remotte.Devices.REMOTTE) {
            if (getGattConfiguration().getTemperatureSensorEnabled())
                configureSensor(GattDeviceRemotte.TEMPERATURE_SERVICE, GattDeviceRemotte.TEMPERATURE_CONFIG_CHARACTERISTIC, false);
            if (getGattConfiguration().getAccelerometerSensorEnabled())
                configureSensor(GattDeviceRemotte.ACCELEROMETER_SERVICE, GattDeviceRemotte.ACCELEROMETER_CONFIG_CHARACTERISTIC, false);
            if (getGattConfiguration().getGyroscopeSensorEnabled())
                configureSensor(GattDeviceRemotte.GYROSCOPE_SERVICE, GattDeviceRemotte.GYROSCOPE_CONFIG_CHARACTERISTIC, false);
            if (getGattConfiguration().getAltimeterSensorEnabled())
                configureSensor(GattDeviceRemotte.ALTIMETER_SERVICE, GattDeviceRemotte.ALTIMETER_CONFIG_CHARACTERISTIC, false);
        } else {
            if (getGattConfiguration().getTemperatureSensorEnabled())
                configureSensor(GattDeviceSensorTag.TEMPERATURE_SERVICE, GattDeviceSensorTag.TEMPERATURE_CONFIG_CHARACTERISTIC, false);
            if (getGattConfiguration().getAccelerometerSensorEnabled())
                configureSensor(GattDeviceSensorTag.ACCELEROMETER_SERVICE, GattDeviceSensorTag.ACCELEROMETER_CONFIG_CHARACTERISTIC, false);
            if (getGattConfiguration().getGyroscopeSensorEnabled())
                configureSensor(GattDeviceSensorTag.GYROSCOPE_SERVICE, GattDeviceSensorTag.GYROSCOPE_CONFIG_CHARACTERISTIC, false);
            if (getGattConfiguration().getAltimeterSensorEnabled())
                configureSensor(GattDeviceSensorTag.ALTIMETER_SERVICE, GattDeviceSensorTag.ALTIMETER_CONFIG_CHARACTERISTIC, false);
        }
    }

    private void enableSensorsNotifications() {
        LogsHelper.log(LogsHelper.DEBUG, "Enabling Remotte sensors to on change notifications.");
        if (getDeviceType() == Remotte.Devices.REMOTTE) {
            if (getGattConfiguration().getTemperatureCallbackEnabled()) {
                configureSensorNotification(GattDeviceRemotte.TEMPERATURE_SERVICE, GattDeviceRemotte.TEMPERATURE_DATA_CHARACTERISTIC, true);
                configureSensor(GattDeviceRemotte.TEMPERATURE_SERVICE, GattDeviceRemotte.TEMPERATURE_PERIOD_CHARACTERISTIC, getGattConfiguration().getTemperaturePeriod());
            }
            if (getGattConfiguration().getAccelerometerCallbackEnabled()) {
                configureSensorNotification(GattDeviceRemotte.ACCELEROMETER_SERVICE, GattDeviceRemotte.ACCELEROMETER_DATA_CHARACTERISTIC, true);
                configureSensor(GattDeviceRemotte.ACCELEROMETER_SERVICE, GattDeviceRemotte.ACCELEROMETER_PERIOD_CHARACTERISTIC, getGattConfiguration().getAccelerometerPeriod());
            }
            if (getGattConfiguration().getGyroscopeCallbackEnabled()) {
                configureSensorNotification(GattDeviceRemotte.GYROSCOPE_SERVICE, GattDeviceRemotte.GYROSCOPE_DATA_CHARACTERISTIC, true);
                configureSensor(GattDeviceRemotte.GYROSCOPE_SERVICE, GattDeviceRemotte.GYROSCOPE_PERIOD_CHARACTERISTIC, getGattConfiguration().getGyroscopePeriod());
            }
            if (getGattConfiguration().getAltimeterCallbackEnabled()) {
                configureSensorNotification(GattDeviceRemotte.ALTIMETER_SERVICE, GattDeviceRemotte.ALTIMETER_DATA_CHARACTERISTIC, true);
                configureSensor(GattDeviceRemotte.ALTIMETER_SERVICE, GattDeviceRemotte.ALTIMETER_PERIOD_CHARACTERISTIC, getGattConfiguration().getAltimeterPeriod());
            }
            if (getGattConfiguration().getKeysPressedCallbackEnabled())
                configureSensorNotification(GattDeviceRemotte.KEY_SERVICE, GattDeviceRemotte.KEY_DATA_CHARACTERISTIC, true);
        } else {
            if (getGattConfiguration().getTemperatureCallbackEnabled()) {
                configureSensorNotification(GattDeviceSensorTag.TEMPERATURE_SERVICE, GattDeviceSensorTag.TEMPERATURE_DATA_CHARACTERISTIC, true);
                configureSensor(GattDeviceSensorTag.TEMPERATURE_SERVICE, GattDeviceSensorTag.TEMPERATURE_PERIOD_CHARACTERISTIC, getGattConfiguration().getTemperaturePeriod());
            }
            if (getGattConfiguration().getAccelerometerCallbackEnabled()) {
                configureSensorNotification(GattDeviceSensorTag.ACCELEROMETER_SERVICE, GattDeviceSensorTag.ACCELEROMETER_DATA_CHARACTERISTIC, true);
                configureSensor(GattDeviceSensorTag.ACCELEROMETER_SERVICE, GattDeviceSensorTag.ACCELEROMETER_PERIOD_CHARACTERISTIC, getGattConfiguration().getAccelerometerPeriod());
            }
            if (getGattConfiguration().getGyroscopeCallbackEnabled()) {
                configureSensorNotification(GattDeviceSensorTag.GYROSCOPE_SERVICE, GattDeviceSensorTag.GYROSCOPE_DATA_CHARACTERISTIC, true);
                configureSensor(GattDeviceSensorTag.GYROSCOPE_SERVICE, GattDeviceSensorTag.GYROSCOPE_PERIOD_CHARACTERISTIC, getGattConfiguration().getGyroscopePeriod());
            }
            if (getGattConfiguration().getAltimeterCallbackEnabled()) {
                configureSensorNotification(GattDeviceSensorTag.ALTIMETER_SERVICE, GattDeviceSensorTag.ALTIMETER_DATA_CHARACTERISTIC, true);
                configureSensor(GattDeviceSensorTag.ALTIMETER_SERVICE, GattDeviceSensorTag.ALTIMETER_PERIOD_CHARACTERISTIC, getGattConfiguration().getAltimeterPeriod());
            }
            if (getGattConfiguration().getKeysPressedCallbackEnabled())
                configureSensorNotification(GattDeviceSensorTag.KEY_SERVICE, GattDeviceSensorTag.KEY_DATA_CHARACTERISTIC, true);
        }
    }

    private void disableSensorsNotifications() {
        LogsHelper.log(LogsHelper.DEBUG, "Disabling Remotte sensors to on change notifications.");
        if (getDeviceType() == Remotte.Devices.REMOTTE) {
            if (getGattConfiguration().getTemperatureCallbackEnabled())
                configureSensorNotification(GattDeviceRemotte.TEMPERATURE_SERVICE, GattDeviceRemotte.TEMPERATURE_DATA_CHARACTERISTIC, false);
            if (getGattConfiguration().getAccelerometerCallbackEnabled())
                configureSensorNotification(GattDeviceRemotte.ACCELEROMETER_SERVICE, GattDeviceRemotte.ACCELEROMETER_DATA_CHARACTERISTIC, false);
            if (getGattConfiguration().getGyroscopeCallbackEnabled())
                configureSensorNotification(GattDeviceRemotte.GYROSCOPE_SERVICE, GattDeviceRemotte.GYROSCOPE_DATA_CHARACTERISTIC, false);
            if (getGattConfiguration().getAltimeterCallbackEnabled())
                configureSensorNotification(GattDeviceRemotte.ALTIMETER_SERVICE, GattDeviceRemotte.ALTIMETER_DATA_CHARACTERISTIC, false);
            if (getGattConfiguration().getKeysPressedCallbackEnabled())
                configureSensorNotification(GattDeviceRemotte.KEY_SERVICE, GattDeviceRemotte.KEY_DATA_CHARACTERISTIC, false);
        } else {
            if (getGattConfiguration().getTemperatureCallbackEnabled())
                configureSensorNotification(GattDeviceSensorTag.TEMPERATURE_SERVICE, GattDeviceSensorTag.TEMPERATURE_DATA_CHARACTERISTIC, false);
            if (getGattConfiguration().getAccelerometerCallbackEnabled())
                configureSensorNotification(GattDeviceSensorTag.ACCELEROMETER_SERVICE, GattDeviceSensorTag.ACCELEROMETER_DATA_CHARACTERISTIC, false);
            if (getGattConfiguration().getGyroscopeCallbackEnabled())
                configureSensorNotification(GattDeviceSensorTag.GYROSCOPE_SERVICE, GattDeviceSensorTag.GYROSCOPE_DATA_CHARACTERISTIC, false);
            if (getGattConfiguration().getAltimeterCallbackEnabled())
                configureSensorNotification(GattDeviceSensorTag.ALTIMETER_SERVICE, GattDeviceSensorTag.ALTIMETER_DATA_CHARACTERISTIC, false);
            if (getGattConfiguration().getKeysPressedCallbackEnabled())
                configureSensorNotification(GattDeviceSensorTag.KEY_SERVICE, GattDeviceSensorTag.KEY_DATA_CHARACTERISTIC, false);
        }
    }

    private void configureSensor(UUID gattServiceID, UUID gattConfigurationCharacteristicID, boolean enabled) {
        if (enabled)
            configureSensor(gattServiceID, gattConfigurationCharacteristicID, GattDevice.ENABLE_SENSOR);
        else
            configureSensor(gattServiceID, gattConfigurationCharacteristicID, GattDevice.DISABLE_SENSOR);
    }

    private boolean configureSensor(UUID gattServiceID, UUID gattCharacteristicID, byte[] value) {
        boolean returnedValue = false;

        BluetoothGattService         service = getGattService(mBluetoothGatt, gattServiceID);
        if (service != null) {
            BluetoothGattCharacteristic  configurationCharacteristic = getGattCharacteristic(service, gattCharacteristicID);

            if (configurationCharacteristic != null) {
                writeCharacteristic(configurationCharacteristic, value);
                returnedValue = true;
            }
        }

        return returnedValue;
    }

    private void configureSensorNotification(UUID gattServiceID, UUID gattCharacteristicID, boolean enabled) {
        BluetoothGattService         service = getGattService(mBluetoothGatt, gattServiceID);
        if (service != null) {
            BluetoothGattCharacteristic dataCharacteristic = getGattCharacteristic(service, gattCharacteristicID);

            if (dataCharacteristic != null)
                setGattCharacteristicNotification(dataCharacteristic, enabled);
        }
    }

    private void setGattCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothGatt.setCharacteristicNotification(characteristic, enabled)) {

            //Get Standar Client Configuration Descriptor from the Characteristic.
            BluetoothGattDescriptor clientConfigurationDescriptor = characteristic.getDescriptor(GattDeviceRemotte.CLIENT_CHARACTERISTIC_CONFIG);
            if (clientConfigurationDescriptor != null) {
                if (enabled) {
                    clientConfigurationDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    clientConfigurationDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }

                writeDescriptor(clientConfigurationDescriptor);
            }
        }
    }

    private synchronized void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        characteristic.setValue(value);
        executeCommand(new RemotteGattCommand(characteristic, RemotteGattCommand.TYPE_WRITE));
    }

    private synchronized void readCharacteristic(UUID gattServiceID, UUID gattCharacteristicID) {
        BluetoothGattService gattService = getGattService(mBluetoothGatt, gattServiceID);

        if (gattService != null) {
            BluetoothGattCharacteristic  gattCharacteristic = getGattCharacteristic(gattService, gattCharacteristicID);

            if (gattCharacteristic != null)
                readCharacteristic(gattCharacteristic);
        }
    }

    private synchronized void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        executeCommand(new RemotteGattCommand(characteristic, RemotteGattCommand.TYPE_READ));
    }

    private synchronized void writeDescriptor(BluetoothGattDescriptor descriptor) {
        executeCommand(new RemotteGattCommand(descriptor, RemotteGattCommand.TYPE_WRITE));
    }

    private BluetoothGattService getGattService(BluetoothGatt gatt, UUID id) {
        LogsHelper.log(LogsHelper.DEBUG, String.format("Getting {%s} GATT Service from Remotte device.", id.toString()));
        BluetoothGattService service = gatt.getService(id);

        if (service == null)
            LogsHelper.log(LogsHelper.ERROR, String.format("Impossible to get GATT Service {%s} from Remotte device.", id.toString()));


        return service;
    }

    private BluetoothGattCharacteristic getGattCharacteristic(BluetoothGattService service, UUID id) {
        LogsHelper.log(LogsHelper.DEBUG, String.format("Getting {%s} GATT Characteristic from Remotte device.", id.toString()));
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(id);

        if (characteristic == null)
            LogsHelper.log(LogsHelper.ERROR, String.format("Impossible to get GATT Characteristic {%s} from Remotte device.", id.toString()));

        return characteristic;
    }

    private void notifyTemperatureChange(byte[] value) {
        double temperature = RemotteSensorTemperature.convert(getDeviceType(), value);

        Bundle data = new Bundle();
        data.putDouble(Remotte.EXTRA_TEMPERATURE_VALUE, temperature);

        Message message = Message.obtain(null, Remotte.MSG_TEMPERATURE_CHANGED);
        if (message != null) {
            message.setData(data);
            notify(message);
        }
    }

    private void notifyAccelerometerChange(byte[] value) {
        RemotteSensorAccelerometer.AccelerometerValue accelerometerValue = RemotteSensorAccelerometer.convert(getDeviceType(), value);

        Bundle data = new Bundle();
        data.putDouble(Remotte.EXTRA_ACCELEROMETER_VALUE_X, accelerometerValue.x);
        data.putDouble(Remotte.EXTRA_ACCELEROMETER_VALUE_Y, accelerometerValue.y);
        data.putDouble(Remotte.EXTRA_ACCELEROMETER_VALUE_Z, accelerometerValue.z);

        Message message = Message.obtain(null, Remotte.MSG_ACCELEROMETER_CHANGED);
        if (message != null) {
            message.setData(data);
            notify(message);
        }
    }

    private void notifyGyroscopeChange(byte[] value) {
        RemotteSensorGyroscope.GyroscopeValue gyroscopeValue = RemotteSensorGyroscope.convert(getDeviceType(), value);

        Bundle data = new Bundle();
        data.putDouble(Remotte.EXTRA_GYROSCOPE_VALUE_X, gyroscopeValue.x);
        data.putDouble(Remotte.EXTRA_GYROSCOPE_VALUE_Y, gyroscopeValue.y);
        data.putDouble(Remotte.EXTRA_GYROSCOPE_VALUE_Z, gyroscopeValue.z);

        Message message = Message.obtain(null, Remotte.MSG_GYROSCOPE_CHANGED);
        if (message != null) {
            message.setData(data);
            notify(message);
        }
    }

    private void notifyAltimeterChange(byte[] value) {
        RemotteSensorAltimeter.AltimeterValue altimeterValue = RemotteSensorAltimeter.convert(getDeviceType(), value);

        Bundle data = new Bundle();
        data.putDouble(Remotte.EXTRA_ALTIMETER_PRESSURE_VALUE, altimeterValue.pressure);
        data.putDouble(Remotte.EXTRA_ALTIMETER_ALTITUDE_VALUE, altimeterValue.altitude);

        Message message = Message.obtain(null, Remotte.MSG_BAROMETER_CHANGED);
        if (message != null) {
            message.setData(data);
            notify(message);
        }
    }

    private void notifyAltimeterCalibration(byte[] value) {
        RemotteSensorAltimeter.setCalibration(value);

        if (getDeviceType() == Remotte.Devices.REMOTTE)
            configureSensor(GattDeviceRemotte.ALTIMETER_SERVICE, GattDeviceRemotte.ALTIMETER_CONFIG_CHARACTERISTIC, GattDevice.ENABLE_SENSOR);
        else
            configureSensor(GattDeviceSensorTag.ALTIMETER_SERVICE, GattDeviceSensorTag.ALTIMETER_CONFIG_CHARACTERISTIC, GattDevice.ENABLE_SENSOR);
    }

    private void notifyDesciptorRead(BluetoothGattDescriptor descriptor) { }

    private void notifyCharacteristicRead(BluetoothGattCharacteristic characteristic) {
        Bundle data = new Bundle();
        data.putInt(Remotte.EXTRA_CHARACTERISTIC, getCharactericticType(characteristic.getUuid()));
        data.putByteArray(Remotte.EXTRA_CHARACTERISTIC_VALUE, characteristic.getValue());

        Message message = Message.obtain(null, Remotte.MSG_CHARACTERISTIC_READED);
        if (message != null) {
            message.setData(data);
            notify(message);
        }
    }

    private int getCharactericticType(UUID characteristic) {
        return Remotte.Characteristics.getCharacteristicTypeFromGattCharacteristicUUID(getDeviceType(), characteristic);
    }

    private void notifyKeysPressedChange(byte[] value) {
        RemotteSensorKeys.KeysStatus status = RemotteSensorKeys.convert(getDeviceType(), value);

        Bundle data = new Bundle();
        data.putInt(Remotte.EXTRA_KEY_POWER_STATE, status.powerKeyStatus);
        data.putInt(Remotte.EXTRA_KEY_CENTER_STATE, status.centerKeyStatus);

        Message message = Message.obtain(null, Remotte.MSG_KEY_PRESSED);
        if (message != null) {
            message.setData(data);
            notify(message);
        }
    }

    private void notifyConnectionChange(int state) {
        if (mClientMessenger != null) {
            Bundle data = new Bundle();
            data.putInt(Remotte.EXTRA_CONNECTION_STATE, state);

            Message message = Message.obtain(null, Remotte.MSG_STATE_CHANGED);
            if (message != null) {
                message.setData(data);
                notify(message);
            }
        }
    }

    private void notify(Message message) {
        try {
            if (mClientMessenger != null && message != null)
                mClientMessenger.send(message);
        } catch (Exception e) {
            LogsHelper.log(LogsHelper.ERROR, "Error sending message to the client.", e);
        }
    }

    private void nextCommand() {
        if (mBluetoothStack != null && mBluetoothStack.size() > 0)
            executeCommand(mBluetoothStack.poll());
    }

    private void executeCommand(RemotteGattCommand gattCommand) {
        if (isGattReady()) {
            if (!mBusy) {
                if (gattCommand.type == RemotteGattCommand.TYPE_WRITE) {
                    if (gattCommand.command instanceof BluetoothGattCharacteristic) {
                        mBusy = true;
                        mBluetoothGatt.writeCharacteristic((BluetoothGattCharacteristic) gattCommand.command);
                    } else if (gattCommand.command instanceof BluetoothGattDescriptor) {
                        mBusy = true;
                        mBluetoothGatt.writeDescriptor((BluetoothGattDescriptor) gattCommand.command);
                    }
                } else if (gattCommand.type == RemotteGattCommand.TYPE_READ) {
                    if (gattCommand.command instanceof BluetoothGattCharacteristic) {
                        mBusy = true;
                        mBluetoothGatt.readCharacteristic((BluetoothGattCharacteristic) gattCommand.command);
                    } else if (gattCommand.command instanceof BluetoothGattDescriptor) {
                        mBusy = true;
                        mBluetoothGatt.readDescriptor((BluetoothGattDescriptor) gattCommand.command);
                    }
                } else {
                    LogsHelper.log(LogsHelper.WARN, "Invalid Remotte GATT command.");
                }
            } else {
                if (mBluetoothStack == null)
                    mBluetoothStack = new ConcurrentLinkedQueue<RemotteGattCommand>();

                mBluetoothStack.add(gattCommand);
            }
        }
    }

    /**
     * This callback manage all messages from Remotte device.
     */
    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.STATE_CONNECTED)
                LogsHelper.log(LogsHelper.DEBUG, "GATT Connection State Change, CONNECTED.");
            else if (status == BluetoothGatt.STATE_CONNECTING)
                LogsHelper.log(LogsHelper.DEBUG, "GATT Connection State Change, CONNECTING.");
            else if (status == BluetoothGatt.STATE_DISCONNECTING)
                LogsHelper.log(LogsHelper.DEBUG, "GATT Connection State Change, DISCONNECTING.");
            else if (status == BluetoothGatt.STATE_DISCONNECTED)
                LogsHelper.log(LogsHelper.DEBUG, "GATT Connection State Change, DISCONNECTED.");

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                LogsHelper.log(LogsHelper.DEBUG, "Discovering supported GATT Services from Remotte Device.");
                mBluetoothGatt.discoverServices();
                notifyConnectionChange(BluetoothGatt.STATE_CONNECTING);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                mBluetoothGatt = null;
                notifyConnectionChange(newState);
            } else {
                notifyConnectionChange(newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            LogsHelper.log(LogsHelper.DEBUG, "Discovered supported GATT Services from Remotte Device.");
            if (gatt.getServices() != null && gatt.getServices().size() > 0)
                notifyConnectionChange(BluetoothGatt.STATE_CONNECTED);
            else
                LogsHelper.log(LogsHelper.ERROR, "Problem discovering GATT Services from Remotte Device.");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //TEMPERATURE
            if (characteristic.getUuid().equals(GattDeviceRemotte.TEMPERATURE_DATA_CHARACTERISTIC) ||
                characteristic.getUuid().equals(GattDeviceSensorTag.TEMPERATURE_DATA_CHARACTERISTIC))
                notifyTemperatureChange(characteristic.getValue());
            //ACCELEROMETER
            else if (characteristic.getUuid().equals(GattDeviceRemotte.ACCELEROMETER_DATA_CHARACTERISTIC) ||
                     characteristic.getUuid().equals(GattDeviceSensorTag.ACCELEROMETER_DATA_CHARACTERISTIC))
                notifyAccelerometerChange(characteristic.getValue());
            //GYROSCOPE
            else if (characteristic.getUuid().equals(GattDeviceRemotte.GYROSCOPE_DATA_CHARACTERISTIC) ||
                     characteristic.getUuid().equals(GattDeviceSensorTag.GYROSCOPE_DATA_CHARACTERISTIC))
                notifyGyroscopeChange(characteristic.getValue());
            //BAROMETER
            else if (characteristic.getUuid().equals(GattDeviceRemotte.ALTIMETER_DATA_CHARACTERISTIC) ||
                     characteristic.getUuid().equals(GattDeviceSensorTag.ALTIMETER_DATA_CHARACTERISTIC))
                notifyAltimeterChange(characteristic.getValue());
            //KEYS
            else if (characteristic.getUuid().equals(GattDeviceRemotte.KEY_DATA_CHARACTERISTIC) ||
                     characteristic.getUuid().equals(GattDeviceSensorTag.KEY_DATA_CHARACTERISTIC))
                notifyKeysPressedChange(characteristic.getValue());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                if (characteristic.getUuid().equals(GattDeviceSensorTag.ALTIMETER_CALIBRATION_CHARACTERITIC))
                    notifyAltimeterCalibration(characteristic.getValue());
                else
                    notifyCharacteristicRead(characteristic);


            mBusy = false;
            nextCommand();
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                notifyDesciptorRead(descriptor);

            mBusy = false;
            nextCommand();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            mBusy = false;
            nextCommand();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            mBusy = false;
            nextCommand();
        }
    };

    /**
     * This handler manage all the request sent to the service from outside.
     */
    private final class IncomingHandler extends Handler {
        private final WeakReference<RemotteService> mService;

        public IncomingHandler(RemotteService service) {
            mService = new WeakReference<RemotteService>(service);
        }

        @Override
        public void handleMessage(Message message) {
            RemotteService remotteService = mService.get();

            if (remotteService != null) {
                switch (message.what) {
                    case MSG_REGISTER_CLIENT:
                        mClientMessenger = message.replyTo;
                        if (mClientMessenger != null)
                            executeRegistedCommand(mClientMessenger);
                        break;
                    case MSG_COMMAND_CONNECT:
                        executeConnectCommand(remotteService, message);
                        break;
                    case MSG_CONFIGURE_GATT:
                        executeConfigureGattCommand(remotteService, message);
                        break;
                    case MSG_COMMAND_DISCONNECT:
                        executeDisconnectCommand(remotteService, message);
                        break;
                    case MSG_ENABLE_HAPTIC:
                        executeHapticCommand(remotteService, message);
                        break;
                    case MSG_READ_CHARACTERISTC:
                        executeReadCharacteristicCommand(remotteService, message);
                        break;
                    default:
                        super.handleMessage(message);
                        break;
                }
            } else {
                super.handleMessage(message);
            }
        }

        private void executeRegistedCommand(Messenger client) {
            try {
                Message message = Message.obtain(null, Remotte.MSG_CLIENT_REGISTERED);
                client.send(message);
            } catch (Exception e) {
                LogsHelper.log(LogsHelper.DEBUG, "Error sending registered message to client.", e);
            }
        }

        private void executeConnectCommand(RemotteService service, Message message) {
            service.connect(
                    message.getData().getString(Remotte.EXTRA_DEVICE_ADDRESS));
        }

        private void executeDisconnectCommand(RemotteService service, Message message) {
            service.disconect(
                    message.getData().getString(Remotte.EXTRA_DEVICE_ADDRESS));
        }

        private void executeHapticCommand(RemotteService service, Message message) {
            //This functionality it's only available on Remotte device.
            if (getDeviceType() == Remotte.Devices.REMOTTE) {
                BluetoothGattService gattService = getGattService(mBluetoothGatt, GattDeviceRemotte.HAPTIC_SERVICE);
                if (gattService != null) {
                    BluetoothGattCharacteristic dataCharacteristic = getGattCharacteristic(gattService, GattDeviceRemotte.HAPTIC_DATA_CHARACTERISTIC);

                    if (dataCharacteristic != null)
                        writeCharacteristic(dataCharacteristic, new byte[]{message.getData().getByte(EXTRA_HAPTIC_CONFIGURATION)});
                }
            }
        }

        private void executeReadCharacteristicCommand(RemotteService service, Message message) {
            int characteristic = message.getData().getInt(EXTRA_CHARACTERISTIC);

            UUID serviceID = Remotte.Characteristics.getGattServiceUUID(getDeviceType(), characteristic);
            UUID dataCharacteriticID = Remotte.Characteristics.getGattCharacteristicUUID(getDeviceType(), characteristic);

            BluetoothGattService         gattService = getGattService(mBluetoothGatt, serviceID);
            if (gattService != null) {
                BluetoothGattCharacteristic dataCharacteristic = getGattCharacteristic(gattService, dataCharacteriticID);

                if (dataCharacteristic != null)
                    readCharacteristic(dataCharacteristic);
            }
        }

        private void executeConfigureGattCommand(RemotteService service, Message message) {
            Bundle data = message.getData();
            data.setClassLoader(Remotte.Configuration.class.getClassLoader());

            service.setGattConfiguration((Remotte.Configuration)data.get(EXTRA_GATT_CONFIGURATION));
            service.enableSensors();
            service.enableSensorsNotifications();
        }
    }

    /**
     * This class define the Objects to the GATT Stack commands.
     */
    private class RemotteGattCommand {

        public final static int TYPE_READ  = 1;
        public final static int TYPE_WRITE = 2;

        public int    type;
        public Object command;

        public RemotteGattCommand(Object command, Integer type) {
            this.type = type;
            this.command = command;
        }
    }
}
