
package com.milink.net.asio.tcp.server;

public interface TcpServerListener {

    void onAccept(TcpServer server, TcpConn conn);

    void onReceived(TcpServer server, TcpConn conn, byte[] data);

    void onConnectionClosed(TcpServer server, TcpConn conn);
}
