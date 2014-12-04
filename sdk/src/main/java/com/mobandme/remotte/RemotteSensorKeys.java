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
 * This class is the Physical Buttons adapter for Remotte, use it to convert the GATT Values to Callbacks values.
 * @author Txus Ballesteros
 * @version 1
 * @since 1
 */
final class RemotteSensorKeys extends RemotteSensor {

    public static class KeysStatus {
        public int powerKeyStatus;
        public int centerKeyStatus;

        public KeysStatus(int powerKeyStatus, int centerKeyStatus) {
            this.powerKeyStatus = powerKeyStatus;
            this.centerKeyStatus = centerKeyStatus;
        }
    }

    public static KeysStatus convert(int deviceType, byte[] value) {
        int keyValue = (int)value[0];

        switch (keyValue) {
            case 1:
                return new KeysStatus(0, 1);
            case 2:
                return new KeysStatus(1, 0);
            default:
                return new KeysStatus(0, 0);
        }
    }
}
