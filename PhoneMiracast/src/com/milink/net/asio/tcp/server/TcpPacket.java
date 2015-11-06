package com.milink.net.asio.tcp.server;

public class TcpPacket {

    public enum Type {
        Unknown,
        Exit,
        Accept,
        Closed,
        Receive,
        Send,
    };

    public Type type = Type.Unknown;
    public byte[] data = null;
    public TcpConn conn = null;
}