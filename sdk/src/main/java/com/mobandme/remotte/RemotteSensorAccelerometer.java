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

/**
 * This class is the Accelerometer sensor adapter for Remotte, use it to convert the GATT Values to Callbacks values.
 * @author Txus Ballesteros
 * @version 1
 * @since 1
 */
final class RemotteSensorAccelerometer extends RemotteSensor {
    public static class AccelerometerValue {
        public double x;
        public double y;
        public double z;

        public AccelerometerValue(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static AccelerometerValue convert(int deviceType, final byte[] value) {
        if (deviceType == Remotte.Devices.REMOTTE) {
            Integer x = (int)value[0];
            Integer y = (int)value[1];
            Integer z = (int)value[2];

            return new AccelerometerValue(x, y, z);
        } else {
            Integer x = (int)value[0];
            Integer y = (int)value[1];
            Integer z = (int)value[2] * -1;

            final float SCALE = (float) 64.0;
            return new AccelerometerValue(x / SCALE, y / SCALE, z / SCALE);
        }
    }
}
