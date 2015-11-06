
package com.milink.milink.client;

import com.milink.milink.common.IQ;

public interface MiLinkClientListener {

    void onConnected(MiLinkClient client);

    void onConnectedFailed(MiLinkClient client);

    void onDisconnect(MiLinkClient client);

    void onReceived(MiLinkClient client, IQ iq);

    void onEvent(MiLinkClient client, IQ iq);
}