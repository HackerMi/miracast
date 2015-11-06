
package com.milink.net.asio.tcp.server;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class TcpConnPool {

    private Map<String, TcpConn> mConns = new HashMap<String, TcpConn>();

    public Map<String, TcpConn> getConns() {
        return mConns;
    }

    public void add(TcpConn conn) {
        String id = String.format("%s:%d", conn.getIp(), conn.getPort());
        mConns.put(id, conn);
    }
    
    public void remove(TcpConn conn) {
        String id = String.format("%s:%d", conn.getIp(), conn.getPort());
        mConns.remove(id);
    }

    public TcpConn getConn(SocketChannel channel) {
        for (TcpConn conn : mConns.values()) {
            if (conn.getChannel().equals(channel))
                return conn;
        }

        return null;
    }

    public TcpConn getConn(String ip, int port) {
        String id = String.format("%s:%d", ip, port);
        return mConns.get(id);
    }
}