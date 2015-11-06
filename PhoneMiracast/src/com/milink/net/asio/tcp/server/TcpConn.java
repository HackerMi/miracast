
package com.milink.net.asio.tcp.server;

import java.nio.channels.SocketChannel;

public class TcpConn {

    private SocketChannel mChannel = null;
    private String mIp = null;
    private int mPort = 0;

    public TcpConn(String ip, int port, SocketChannel channel) {
        mIp = ip;
        mPort = port;
        mChannel = channel;
    }

    public SocketChannel getChannel() {
        return mChannel;
    }

    public String getIp() {
        return mIp;
    }

    public int getPort() {
        return mPort;
    }
}