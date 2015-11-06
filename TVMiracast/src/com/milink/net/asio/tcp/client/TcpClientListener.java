
package com.milink.net.asio.tcp.client;

public interface TcpClientListener {

    void onConnected(TcpClient client);

    void onConnectedFailed(TcpClient client);

    void onDisconnect(TcpClient client);

    void onReceived(TcpClient client, byte[] data);
}