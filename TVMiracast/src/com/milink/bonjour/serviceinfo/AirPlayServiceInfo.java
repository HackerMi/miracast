
package com.milink.bonjour.serviceinfo;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class AirPlayServiceInfo implements BonjourServiceInfo {

    private static final String TAG = AirPlayServiceInfo.class.getSimpleName();

    public static final String SERVICE_TYPE = "_airplay._tcp.local.";

    private static final int DEFAULT_PORT = 7000;

    private static final String DEFAULT_FEATURE = "0x100029ff";
    private static final String DEFAULT_MODEL = "AppleTV3,2";
    private static final String DEFAULT_SRCVERS = "150.33";

    private String mDeviceName = null;
    private String mDeviceId = null;
    private int mPort = DEFAULT_PORT;

    private Map<String, String> mProperties = new HashMap<String, String>();

    public AirPlayServiceInfo(byte[] deviceId, String name, int port) {
        if (deviceId.length != 6)
            return;

        mDeviceId = String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                deviceId[0],
                deviceId[1],
                deviceId[2],
                deviceId[3],
                deviceId[4],
                deviceId[5]);

        Log.d(TAG, String.format("deviceId: %s", mDeviceId));

        mDeviceName = name;
        mPort = port;

        mProperties.put("deviceid", mDeviceId);
        mProperties.put("features", DEFAULT_FEATURE);
        mProperties.put("model", DEFAULT_MODEL);
        mProperties.put("rhd", "1.9.8");
        mProperties.put("srcvers", DEFAULT_SRCVERS);
        mProperties.put("vv", "1");
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
