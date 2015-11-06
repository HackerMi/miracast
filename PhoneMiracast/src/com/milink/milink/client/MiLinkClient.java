
package com.milink.milink.client;

import com.milink.milink.common.IQ;
import com.milink.net.asio.tcp.client.TcpClient;
import com.milink.net.asio.tcp.client.TcpClientListener;

public class MiLinkClient implements TcpClientListener {

    private static final String TAG = MiLinkClient.class.getSimpleName();

    private TcpClient mClient = null;
    private MiLinkClientListener mListener = null;

    public MiLinkClient(MiLinkClientListener listener) {
        mClient = new TcpClient(this);
        mListener = listener;
    }

    public boolean connect(String ip, int port, int millisecond) {
        return mClient.connect(ip, port, millisecond);
    }

    public boolean disconnect() {
        return mClient.disconnect();
    }

    public boolean isConnected() {
        return mClient.isConnected();
    }
    
    public String getSelfIp() {
        return mClient.getSelfIp();
    }
    
    public boolean send(IQ iq) {
        boolean result = false;

        do
        {
            String msg = iq.toString();
            if (msg == null)
                break;

            result = mClient.send(msg.getBytes());
        } while (false);

        return result;
    }

    @Override
    public void onConnected(TcpClient client) {
        mListener.onConnected(this);
    }

    @Override
    public void onConnectedFailed(TcpClient client) {
        mListener.onConnectedFailed(this);
    }

    @Override
    public void onDisconnect(TcpClient client) {
        mListener.onDisconnect(this);
    }

    @Override
    public void onReceived(TcpClient client, byte[] data) {
        IQ iq = IQ.create(data);
        if (iq == null)
            return;

        if (iq.getType() == IQ.Type.Event) {
            mListener.onEvent(this, iq);
        }
        else {
            mListener.onReceived(this, iq);
        }
    }
}