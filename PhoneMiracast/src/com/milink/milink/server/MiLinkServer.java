
package com.milink.milink.server;

import android.util.Log;

import com.milink.milink.common.IQ;
import com.milink.net.asio.tcp.server.TcpConn;
import com.milink.net.asio.tcp.server.TcpServer;
import com.milink.net.asio.tcp.server.TcpServerListener;

public class MiLinkServer implements TcpServerListener {

    private static final String TAG = MiLinkServer.class.getSimpleName();

    private TcpServer mServer = null;
    private MiLinkServerListener mListener = null;
    private TcpConn mLastConnection = null;

    public MiLinkServer(MiLinkServerListener listener) {
        mServer = new TcpServer(0, this);
        mListener = listener;
    }

    public boolean start() {
        return mServer.start();
    }

    public boolean stop() {
        return mServer.stop();
    }

    public int getListenPort() {
        return mServer.getListenPort();
    }

    public boolean publishEvent(IQ iq) {
        boolean result = false;

        do
        {
            if (mLastConnection == null)
                break;

            if (iq.getType() != IQ.Type.Event)
                break;

            String msg = iq.toString();
            if (msg == null)
                break;

            result = mServer.send(mLastConnection, msg.getBytes());
        } while (false);

        return result;
    }

    public boolean send(String ip, int port, IQ iq) {
        boolean result = false;

        do
        {
            String msg = iq.toString();
            if (msg == null)
                break;

            TcpConn conn = mServer.getConnPool().getConn(ip, port);
            if (conn == null)
                break;

            result = mServer.send(conn, msg.getBytes());
        } while (false);

        return result;
    }

    @Override
    public void onAccept(TcpServer server, TcpConn conn) {
        Log.d(TAG, String.format("onAccept: %s:%d", conn.getIp(), conn.getPort()));

        mListener.onAccept(this, conn.getIp(), conn.getPort());

        if (mLastConnection != null) {
            mServer.closeConnection(mLastConnection);
        }

        mLastConnection = conn;
    }

    @Override
    public void onReceived(TcpServer server, TcpConn conn, byte[] data) {
        Log.d(TAG, String.format("onReceived: %s:%d", conn.getIp(), conn.getPort()));

        IQ iq = IQ.create(data);
        if (iq != null) {
            mListener.onReceived(this, conn.getIp(), conn.getPort(), iq);
        }
    }

    @Override
    public void onConnectionClosed(TcpServer server, TcpConn conn) {
        Log.d(TAG, String.format("onConnectionClosed: %s:%d", conn.getIp(), conn.getPort()));

        mListener.onConnectionClosed(this, conn.getIp(), conn.getPort());

        if (mLastConnection.equals(conn)) {
            mLastConnection = null;
        }
    }
}
