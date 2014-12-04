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

import java.util.UUID;

/**
 * This class contains the GATT Specification of the Remotte Device.
 * @author Txus Ballesteros
 * @version 1
 */
class GattDeviceRemotte extends GattDevice {

    static UUID BATTERY_SERVICE                   = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"); //Battery Information Service
    static UUID BATTERY_LEVEL_CHARACTERISTIC      = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"); //Battery Level

    static UUID DEVICE_INFO_SERVICE                          = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb"); //Device Information Service
    static UUID DEVICE_INFO_MANUFACTURER_NAME_CHARACTERISTIC = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb"); //Manufacturer Name
    static UUID DEVICE_INFO_MODEL_NAME_CHARACTERISTIC        = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb"); //Model Number
    static UUID DEVICE_INFO_SERIAL_CHARACTERISTIC            = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb"); //Serial Number
    static UUID DEVICE_INFO_FIRMWARE_REVISION_CHARACTERISTIC = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb"); //Firmware Revision
    static UUID DEVICE_INFO_HARDWARE_REVISON_CHARACTERISTIC  = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb"); //Hardware Revision
    static UUID DEVICE_INFO_SOFTWARE_REVISION_CHARACTERISTIC = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb"); //Software Revision

    static UUID TEMPERATURE_SERVICE               = UUID.fromString("f000aa00-0451-4000-B000-000000000000");
    static UUID TEMPERATURE_DATA_CHARACTERISTIC   = UUID.fromString("f000aa01-0451-4000-B000-000000000000");
    static UUID TEMPERATURE_CONFIG_CHARACTERISTIC = UUID.fromString("f000aa02-0451-4000-B000-000000000000");
    static UUID TEMPERATURE_PERIOD_CHARACTERISTIC = UUID.fromString("f000aa03-0451-4000-B000-000000000000");

    static UUID ACCELEROMETER_SERVICE               = UUID.fromString("f000aa10-0451-4000-b000-000000000000");
    static UUID ACCELEROMETER_DATA_CHARACTERISTIC   = UUID.fromString("f000aa11-0451-4000-b000-000000000000");
    static UUID ACCELEROMETER_CONFIG_CHARACTERISTIC = UUID.fromString("f000aa12-0451-4000-b000-000000000000");
    static UUID ACCELEROMETER_PERIOD_CHARACTERISTIC = UUID.fromString("f000aa13-0451-4000-b000-000000000000");

    static UUID GYROSCOPE_SERVICE                = UUID.fromString("f000aa50-0451-4000-b000-000000000000");
    static UUID GYROSCOPE_DATA_CHARACTERISTIC    = UUID.fromString("f000aa51-0451-4000-b000-000000000000");
    static UUID GYROSCOPE_CONFIG_CHARACTERISTIC  = UUID.fromString("f000aa52-0451-4000-b000-000000000000");
    static UUID GYROSCOPE_PERIOD_CHARACTERISTIC  = UUID.fromString("f000aa53-0451-4000-b000-000000000000");

    static UUID ALTIMETER_SERVICE                   = UUID.fromString("f000aa40-0451-4000-b000-000000000000");
    static UUID ALTIMETER_DATA_CHARACTERISTIC       = UUID.fromString("f000aa41-0451-4000-b000-000000000000");
    static UUID ALTIMETER_CONFIG_CHARACTERISTIC     = UUID.fromString("f000aa42-0451-4000-b000-000000000000");
    static UUID ALTIMETER_CALIBRATION_CHARACTERITIC = UUID.fromString("f000aa43-0451-4000-b000-000000000000"); // Calibration characteristic
    static UUID ALTIMETER_PERIOD_CHARACTERISTIC     = UUID.fromString("f000aa44-0451-4000-b000-000000000000");

    static UUID HAPTIC_SERVICE                   = UUID.fromString("f000aa80-0451-4000-b000-000000000000");
    static UUID HAPTIC_DATA_CHARACTERISTIC       = UUID.fromString("f000aa81-0451-4000-b000-000000000000");

    static UUID KEY_SERVICE                      = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    static UUID KEY_DATA_CHARACTERISTIC          = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
}
