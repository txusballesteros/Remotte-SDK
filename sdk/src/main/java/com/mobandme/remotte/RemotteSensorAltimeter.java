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

import java.util.List;
import java.util.ArrayList;
import static java.lang.Math.pow;

/**
 * This class is the Altimeter sensor adapter for Remotte, use it to convert the GATT values to callback values.
 * @author Txus Ballesteros
 * @version 1
 * @since 1
 */
final class RemotteSensorAltimeter extends RemotteSensor {

    private static final double  PA_PER_METER = 12.0;

    private static Double        mHeightCalibration = null;
    private static List<Integer> mCalibration;

    private static List<Integer> getCalibration() { return mCalibration; }

    public static class AltimeterValue {
        public double pressure;
        public double altitude;

        public AltimeterValue(double pressure, double altitude) {
            this.pressure = pressure;
            this.altitude = altitude;
        }
    }

    public static void setCalibration(byte[] value) {
        if (value.length != 16)
            return;

        List<Integer> calibrationValues = new ArrayList<Integer>();
        for (int offset = 0; offset < 8; offset += 2) {
            Integer lowerByte = (int)value[offset] & 0xFF;
            Integer upperByte = (int)value[offset + 1] & 0xFF;
            calibrationValues.add((upperByte << 8) + lowerByte);
        }

        for (int offset = 8; offset < 16; offset += 2) {
            Integer lowerByte = (int)value[offset] & 0xFF;
            Integer upperByte = (int)value[offset + 1];
            calibrationValues.add((upperByte << 8) + lowerByte);
        }

        mCalibration = calibrationValues;
    }

    public static AltimeterValue convert(int deviceType, final byte[] value) {
        Double pressure = 0.0d;
        Double altitude = 0.0d;

        if (getCalibration() != null && getCalibration().size() > 0)
            if (mHeightCalibration == null) {
                mHeightCalibration = calculatePressure(value);
            } else {
                double p_a = calculatePressure(value);

                altitude = ((p_a - mHeightCalibration) / PA_PER_METER);
                pressure = (p_a / 100.0);
            }

        return new AltimeterValue(pressure, altitude);
    }

    private static Double calculatePressure(final byte[] value) {
        final int[] coefficients; // Calibration coefficients
        final Integer t_r; // Temperature raw value from sensor
        final Integer p_r; // Pressure raw value from sensor
        final Double S; // Interim value in calculation
        final Double O; // Interim value in calculation
        final Double p_a; // Pressure actual value in unit Pascal.

        coefficients = new int[getCalibration().size()];
        for (int i = 0; i < getCalibration().size(); i++) {
            coefficients[i] = getCalibration().get(i);
        }

        t_r = shortSignedAtOffset(value, 0);
        p_r = shortUnsignedAtOffset(value, 2);

        S = coefficients[2] + coefficients[3] * t_r / pow(2, 17) + ((coefficients[4] * t_r / pow(2, 15)) * t_r) / pow(2, 19);
        O = coefficients[5] * pow(2, 14) + coefficients[6] * t_r / pow(2, 3) + ((coefficients[7] * t_r / pow(2, 15)) * t_r) / pow(2, 4);
        p_a = (S * p_r + O) / pow(2, 14);

        return p_a;
    }
}
