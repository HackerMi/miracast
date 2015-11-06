
package com.milink.net.asio.tcp.client;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TcpClient {

    private static final String TAG = TcpClient.class.getSimpleName();
    private static final int DEFAULT_CONNECT_TIMEOUT = 1000 * 5;
    private TcpClientListener mListener = null;
    private boolean mIsConnected = false;
    private SocketChannel mChannel = null;
    private SelectWorker mSelectWorker = null;
    private RecvWorker mRecvWorker = null;
    private SendWorker mSendWorker = null;
    private String mIp = null;
    private int mPort = 0;
    private int mTimeout = 0;

    public TcpClient(TcpClientListener listener) {
        mListener = listener;
    }

    public boolean connect(String ip, int port, int millisecond) {
        boolean doing = false;

        if (!mIsConnected) {
            mIp = ip;
            mPort = port;
            mTimeout = (millisecond > 0) ? millisecond : DEFAULT_CONNECT_TIMEOUT;

            mRecvWorker = new RecvWorker();
            mSendWorker = new SendWorker();
            mSelectWorker = new SelectWorker();

            doing = true;
        }

        return doing;
    }

    public boolean disconnect() {
        boolean done = false;

        if (mIsConnected) {
            Log.d(TAG, String.format("disconnect: %s:%d", mIp, mPort));

            mSelectWorker.close();
            mSendWorker.close();
            mRecvWorker.close();

            done = true;
        }

        return done;
    }

    public boolean isConnected() {
        synchronized (this) {
            return mIsConnected;
        }
    }

    public String getSelfIp() {
        return mChannel.socket().getLocalAddress().getHostAddress().toString();
    }

    public int getSelfPort() {
        return mChannel.socket().getPort();
    }

    public String getPeerIp() {
        return mIp;
    }

    public int getPeerPort() {
        return mPort;
    }

    public boolean send(byte[] bytes) {
        boolean done = false;

        if (mIsConnected) {
            mSendWorker.putData(bytes);
            done = true;
        }

        return done;
    }

    private static enum ConnectRet {
        ConnectException,
        ConnectOk,
        ConnectTimeout,
    }

    public class SelectWorker implements Runnable {
        private Selector mSelector = null;
        private Thread mThread = null;
        private Boolean mLoop = true;

        public SelectWorker() {
            mThread = new Thread(this);
            mThread.start();
        }

        public void close() {
            try {
                mSelector.close();
                mChannel.close();
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                mChannel = SocketChannel.open();
                mChannel.configureBlocking(false);
                mSelector = Selector.open();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            ConnectRet ret = this.connect();
            if (ret != ConnectRet.ConnectOk) {
                Log.d(TAG, String.format("connect to %s:%d failed", mIp, mPort));

                try {
                    mChannel.close();
                    mSelector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mListener.onConnectedFailed(TcpClient.this);
                return;
            }

            Log.d(TAG, String.format("connect to %s:%d is ok!", mIp, mPort));
            mIsConnected = true;
            mListener.onConnected(TcpClient.this);

            try {
                mChannel.register(mSelector, SelectionKey.OP_READ);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
                return;
            }

            mLoop = true;
            while (mLoop) {
                try {
                    mSelector.select();
                } catch (ClosedSelectorException e) {
                    break;
                } catch (IOException e) {
                    break;
                }

                try {
                    Set<SelectionKey> readyKeys = mSelector.selectedKeys();
                    Iterator<SelectionKey> iter = readyKeys.iterator();

                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        postSelect(key);
                    }
                } catch (ClosedSelectorException e) {
                    break;
                }
            }

            Log.d(TAG, "client is disconnect");

            try {
                mChannel.close();
                mSelector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mIsConnected = false;
            mListener.onDisconnect(TcpClient.this);
        }

        private ConnectRet connect() {
            int count = 0;
            int SLEEP_INTERVAL = 100;

            try {
                Log.d(TAG, String.format("connect to %s:%d", mIp, mPort));

                mChannel.connect(new InetSocketAddress(mIp, mPort));

                while (!mChannel.finishConnect()) {
                    count += SLEEP_INTERVAL;
                    if (count > mTimeout) {
                        return ConnectRet.ConnectTimeout;
                    }
                    else
                    {
                        Log.i(TAG, String.format("waiting for connection establish (%s:%d)", mIp,
                                mPort));
                    }

                    Thread.sleep(SLEEP_INTERVAL);
                }

                return ConnectRet.ConnectOk;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return ConnectRet.ConnectException;
            } catch (IOException e) {
                e.printStackTrace();
                return ConnectRet.ConnectException;
            }
        }

        private void postSelect(SelectionKey key) {
            Log.d(TAG, String.format("postSelect"));

            if (key.isValid() && key.isReadable()) {
                SocketChannel channel = (SocketChannel) key.channel();

                ByteBuffer buf = ByteBuffer.allocateDirect(1024);
                int numBytesRead = 0;
                try {
                    numBytesRead = channel.read(buf);
                } catch (IOException e) {
                    mLoop = false;
                    return;
                }

                if (numBytesRead > 0) {
                    buf.flip();

                    byte[] data = new byte[numBytesRead];
                    buf.get(data, 0, numBytesRead);

                    mRecvWorker.putData(data);

                    buf.clear();
                }
                else {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mLoop = false;
                }
            }
        }
    }

    public class RecvWorker implements Runnable {

        private static final int MAX_RECV_QUEUE_LENGTH = 128;
        private BlockingQueue<TcpPacket> mQueue = null;
        private Thread mThread = null;

        public RecvWorker() {
            mQueue = new ArrayBlockingQueue<TcpPacket>(MAX_RECV_QUEUE_LENGTH);
            mThread = new Thread(this);
            mThread.start();
        }

        public void close() {
            TcpPacket packet = new TcpPacket();
            packet.type = TcpPacket.Type.Exit;

            synchronized (this) {
                mQueue.clear();
                try {
                    mQueue.put(packet);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void putData(byte[] data) {
            TcpPacket packet = new TcpPacket();
            packet.type = TcpPacket.Type.Receive;
            packet.data = data.clone();

            synchronized (this) {
                try {
                    mQueue.put(packet);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            while (true) {
                TcpPacket packet;

                try {
                    packet = mQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                if (packet.type == TcpPacket.Type.Exit) {
                    break;
                }

                else if (packet.type == TcpPacket.Type.Receive) {
                    mListener.onReceived(TcpClient.this, packet.data);
                }
            }

            mQueue.clear();
        }
    }

    public class SendWorker implements Runnable {

        private static final int MAX_SEND_QUEUE_LENGTH = 128;
        private BlockingQueue<TcpPacket> mQueue = null;
        private Thread mThread = null;

        public SendWorker() {
            mQueue = new ArrayBlockingQueue<TcpPacket>(MAX_SEND_QUEUE_LENGTH);
            mThread = new Thread(this);
            mThread.start();
        }

        public void close() {
            synchronized (this) {
                TcpPacket packet = new TcpPacket();
                packet.type = TcpPacket.Type.Exit;

                synchronized (this) {
                    try {
                        mQueue.clear();
                        mQueue.put(packet);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void putData(byte[] data) {
            synchronized (this) {
                TcpPacket packet = new TcpPacket();
                packet.type = TcpPacket.Type.Send;
                packet.data = data.clone();

                synchronized (this) {
                    try {
                        mQueue.put(packet);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void run() {
            while (true) {
                TcpPacket packet = null;

                try {
                    packet = mQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                if (packet.type == TcpPacket.Type.Exit) {
                    break;
                }
                else if (packet.type == TcpPacket.Type.Send) {
                    if (!mChannel.isConnected()) {
                        Log.d(TAG, "channel is not connected!");
                        continue;
                    }

                    ByteBuffer buffer = ByteBuffer.wrap(packet.data);
                    buffer.clear();

                    int writeSize = 0;
                    while (true) {
                        int size = 0;
                        try {
                            size = mChannel.write(buffer);
                        } catch (NotYetConnectedException e) {
                            e.printStackTrace();
                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
                        }

                        writeSize += size;
                        if (writeSize < packet.data.length) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        else {
                            break;
                        }
                    }
                }
            }

            mQueue.clear();
        }
    }
}
