/**
 * Copyright 2013 Tommi S.E. Laukkanen
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
 */
package org.agocontrol.client;

/**
 * AgoControl device POJO.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class AgoDevice {
    /** The device ID. */
    private final String deviceId;
    /** The device type. */
    private final String deviceType;
    /** the device name. */
    private final String deviceName;

    /**
     * Constructor for setting device parameters.
     * @param deviceId the deviceId
     * @param deviceType the deviceType
     * @param deviceName the deviceName
     */
    public AgoDevice(final String deviceId, final String deviceType, final String deviceName) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.deviceName = deviceName;
    }

    /**
     * @return the deviceId
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * @return the deviceName
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * @return the deviceType
     */
    public String getDeviceType() {
        return deviceType;
    }

}
