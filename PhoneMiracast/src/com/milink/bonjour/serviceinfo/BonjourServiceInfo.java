
package com.milink.bonjour.serviceinfo;

import java.util.Map;

public interface BonjourServiceInfo {

    String getServiceName();

    int getServicePort();

    String getServiceType();

    Map<String, String> getProperties();
}
