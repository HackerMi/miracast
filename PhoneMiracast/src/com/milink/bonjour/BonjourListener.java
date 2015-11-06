
package com.milink.bonjour;

import java.util.Map;

public interface BonjourListener {

    void onStarted();

    void onStartFailed();

    void onStopped();

    void onServiceFound(
            String name,
            String type,
            String ip,
            int port,
            Map<String, String> properties);

    void onServiceLost(String name, String type, String ip);
}
