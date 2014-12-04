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
 * This class is the Temperature sensor adapter for Remotte, use it to convert the GATT Values to Callbacks values.
 * @author Txus Ballesteros
 * @version 1
 * @since 1
 */
final class RemotteSensorTemperature extends RemotteSensor {

    public static double convert(int deviceType, final byte[] data) {
        int offset = 2;
        return shortUnsignedAtOffset(data, offset) / 128.0;
    }
}
