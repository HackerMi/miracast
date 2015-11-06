
package com.milink.bonjour.serviceinfo;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MiLinkServiceInfo implements BonjourServiceInfo {

    private static final String TAG = AirPlayServiceInfo.class.getSimpleName();

    public static final String SERVICE_TYPE = "_milink._tcp.local.";
    private static final float SERVICE_VERSION = 1.0f;
    private static final int SUPPORT_MIRACAST = 0x0001;
    private static final int DEFAULT_FEATURE = SUPPORT_MIRACAST;

    private String mDeviceName = null;
    private String mDeviceId = null;
    private int mPort = 0;

    private Map<String, String> mProperties = new HashMap<String, String>();

    public MiLinkServiceInfo(byte[] deviceId, String name, int port) {
        UUID uuid = UUID.nameUUIDFromBytes(deviceId);
        mDeviceId = uuid.toString();
        Log.d(TAG, mDeviceId);

        mDeviceName = name;
        mPort = port;

        mProperties.put("version", String.format("%f", SERVICE_VERSION));
        mProperties.put("deviceid", mDeviceId);
        mProperties.put("features", String.format("%x", DEFAULT_FEATURE));
    }

    @Override
    public String getServiceName() {
        return mDeviceName;
    }

    @Override
    public int getServicePort() {
        return mPort;
    }

    @Override
    public String getServiceType() {
        return SERVICE_TYPE;
    }

    @Override
    public Map<String, String> getProperties() {
        return mProperties;
    }
}
