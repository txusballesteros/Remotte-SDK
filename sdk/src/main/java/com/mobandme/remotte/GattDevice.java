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

class GattDevice {
    static final byte[] ENABLE_SENSOR                  = { 0x01 };
    static final byte[] DISABLE_SENSOR                 = { 0x00 };
    static final byte[] ENABLE_GYROSCOPE_3_AXIS_SENSOR = { 0x07 };
    static final byte[] SENSOR_CALIBRATION             = { 0x02 };

    static UUID CLIENT_CHARACTERISTIC_CONFIG      = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); //Client Configuration
}
