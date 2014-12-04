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
 * This class is the Gyroscope sensor adapter for Remotte, use it to convert the GATT Values to Callbacks values.
 * @author Txus Ballesteros
 * @version 1
 * @since 1
 */
final class RemotteSensorGyroscope extends RemotteSensor {

    public static class GyroscopeValue {
        public double x;
        public double y;
        public double z;

        public GyroscopeValue(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static GyroscopeValue convert(int deviceType, final byte[] value) {
        if (deviceType == Remotte.Devices.REMOTTE) {
            int x = ((value[0] << 8) + value[1]);
            int y = ((value[2] << 8) + value[3]);
            int z = ((value[4] << 8) + value[4]);

            return new GyroscopeValue(x, y, z);
        } else {
            float y = shortSignedAtOffset(value, 0) * (500f / 65536f) * -1;
            float x = shortSignedAtOffset(value, 2) * (500f / 65536f);
            float z = shortSignedAtOffset(value, 4) * (500f / 65536f);

            return new GyroscopeValue(x, y, z);
        }
    }
}
