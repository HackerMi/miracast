
package com.milink.net.asio.tcp.client;

public class TcpPacket {

    public enum Type {
        Unknown,
        Exit,
        Receive,
        Send,
    };

    public Type type = Type.Unknown;
    public byte[] data = null;
}
