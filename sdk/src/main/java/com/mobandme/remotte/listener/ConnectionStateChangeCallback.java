package com.mobandme.remotte.listener;

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

import com.mobandme.remotte.Remotte;
import android.bluetooth.BluetoothGatt;

/**
 * Use this listener to listen connection events from Remotte device.
 * @author Txus Ballesteros
 * @version 1
 * @since 1
 */
public interface ConnectionStateChangeCallback {

    public static final int STATE_CONNECTING = BluetoothGatt.STATE_CONNECTING;
    public static final int STATE_CONNECTED = BluetoothGatt.STATE_CONNECTED;
    public static final int STATE_DISCONNECTING = BluetoothGatt.STATE_DISCONNECTING;
    public static final int STATE_DISCONNECTED = BluetoothGatt.STATE_DISCONNECTED;

    /**
     * This event is sent when a change is detected in the state of the connection to the device.
     * @param remotte The Remotte device that is the origin of the event.
     * @param newState The new state of the connection. This value can take four choices, STATE_CONNECTING, STATE_CONNECTED, STATE_DISCONNECTING, STATE_DISCONNECTED
     */
    void onConnectionStateChange(Remotte remotte, int newState);
}
