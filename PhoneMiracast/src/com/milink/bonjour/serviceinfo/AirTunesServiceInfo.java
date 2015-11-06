
package com.milink.bonjour.serviceinfo;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class AirTunesServiceInfo implements BonjourServiceInfo {
    private static final String TAG = AirTunesServiceInfo.class.getSimpleName();

    public static final String SERVICE_TYPE = "_raop._tcp.local.";
    private static final int DEFAULT_PORT = 5000;

    private static final String DEFAULT_MODEL = "AppleTV3,2";
    private static final String DEFAULT_SRCVERS = "150.33";

    private String mDeviceId = null;
    private String mDeviceName = null;
    private int mPort = DEFAULT_PORT;

    private Map<String, String> mProperties = new HashMap<String, String>();

    public AirTunesServiceInfo(byte[] deviceId, String name, int port) {
        if (deviceId.length != 6)
            return;

        mDeviceId = String.format("%02X%02X%02X%02X%02X%02X",
                deviceId[0],
                deviceId[1],
                deviceId[2],
                deviceId[3],
                deviceId[4],
                deviceId[5]);
        Log.d(TAG, String.format("deviceId: %s", mDeviceId));

        mDeviceName = name;
        mPort = port;

        mProperties.put("am", DEFAULT_MODEL);
        mProperties.put("ch", "2");
        mProperties.put("cn", "0,1,3");
        mProperties.put("da", "true");

        mProperties.put("et", "0,3,5");
        mProperties.put("md", "0,1,2");
        mProperties.put("pw", "false");
        mProperties.put("rhd", "1.9.8");

        mProperties.put("sf", "0x4");
        mProperties.put("sr", "44100");
        mProperties.put("ss", "16");
        mProperties.put("sv", "false");

        mProperties.put("tp", "UDP");
        mProperties.put("txtvers", "1");
        mProperties.put("vn", "65537");
        mProperties.put("vs", DEFAULT_SRCVERS);
        mProperties.put("vv", "1");
    }

    @Override
    public String getServiceName() {
        return String.format("%s@%s", mDeviceId, mDeviceName);
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
