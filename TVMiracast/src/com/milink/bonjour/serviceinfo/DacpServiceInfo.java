
package com.milink.bonjour.serviceinfo;

import java.util.HashMap;
import java.util.Map;

public class DacpServiceInfo implements BonjourServiceInfo {

    public static final String SERVICE_TYPE = "_dacp._tcp.local.";

    private String mName = null;
    private int mPort = 0;
    private Map<String, String> mProperties = new HashMap<String, String>();

    public DacpServiceInfo(String name, int port) {

        mName = name;
        mPort = port;

        mProperties.put("txtvers", "1");
        mProperties.put("Ver", "131075");
        mProperties.put("DbId", "63B5E5C0C201542E");
        mProperties.put("OSsi", "0x1F5");
    }

    @Override
    public String getServiceName() {
        return mName;
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
