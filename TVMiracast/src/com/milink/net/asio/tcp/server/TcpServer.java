
package com.milink.net.asio.tcp.server;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TcpServer {

    private static final String TAG = TcpServer.class.getSimpleName();

    private TcpServerListener mListener = null;
    private ServerSocketChannel mServerChannel = null;
    private int mListenPort = 0;
    private boolean mStarted = false;
    private TcpConnPool mConnPool = new TcpConnPool();
    private SelectWorker mSelectWorker = null;
    private RecvWorker mRecvWorker = null;
    private SendWorker mSendWorker = null;

    public TcpServer(int port, TcpServerListener listener) {
        mListener = listener;

        try {
            mServerChannel = ServerSocketChannel.open();
            InetSocketAddress localAddress = new InetSocketAddress(port);
            mServerChannel.socket().bind(localAddress);
            mListenPort = mServerChannel.socket().getLocalPort();
            mServerChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean start() {
        boolean doing = false;

        if (!mStarted) {
            mStarted = true;
            mRecvWorker = new RecvWorker();
            mSendWorker = new SendWorker();
            mSelectWorker = new SelectWorker();

            doing = true;
        }

        return doing;
    }

    public boolean stop() {
        boolean done = false;

        if (mStarted) {
            mStarted = false;
            mSelectWorker.close();
            mRecvWorker.close();
            mSendWorker.close();

            done = true;
        }

        return done;
    }

    public int getListenPort() {
        return mListenPort;
    }

    public TcpConnPool getConnPool() {
        return mConnPool;
    }

    public void closeConnection(TcpConn conn) {
        if (mStarted) {
            try {
                conn.getChannel().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean send(TcpConn conn, byte[] bytes) {
        boolean doing = false;

        if (mStarted) {
            mSendWorker.putData(conn, bytes);
            doing = true;
        }

        return doing;
    }

    public class SelectWorker implements Runnable {

        private Selector mSelector = null;
        private Thread mThread = null;

        public SelectWorker() {
            mThread = new Thread(this);
            mThread.start();
        }

        public void close() {
            try {
                mSelector.close();
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
                mSelector = Selector.open();
            } catch (IOException e1) {
                e1.printStackTrace();
                return;
            }

            Log.d(TAG, String.format("listen port: %d", mListenPort));

            while (true) {
                if (!preSelect())
                    break;

                try {
                    mSelector.select();
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

            Log.d(TAG, "listen stopped");
        }

        private boolean preSelect() {
            try {
                mServerChannel.register(mSelector, SelectionKey.OP_ACCEPT);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
                return false;
            }

            for (TcpConn conn : mConnPool.getConns().values()) {
                try {
                    conn.getChannel().register(mSelector, SelectionKey.OP_READ);
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            }

            return true;
        }

        private void postSelect(SelectionKey key) {
            // accept a new connection
            if (key.isValid() && key.isAcceptable()) {
                try {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel connection = server.accept();
                    connection.configureBlocking(false);

                    String ip = connection.socket().getInetAddress().getHostAddress();
                    int port = connection.socket().getPort();

                    TcpConn conn = new TcpConn(ip, port, connection);
                    mRecvWorker.putNewConnection(conn);

                    mConnPool.add(conn);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                return;
            }

            // read data
            if (key.isValid() && key.isReadable()) {
                SocketChannel channel = (SocketChannel) key.channel();

                ByteBuffer buf = ByteBuffer.allocateDirect(1024);
                int numBytesRead = 0;
                try {
                    numBytesRead = channel.read(buf);
                } catch (IOException e) {
                    TcpConn conn = mConnPool.getConn(channel);
                    mRecvWorker.putClosedConnection(conn);
                    mConnPool.remove(conn);
                    return;
                }

                if (numBytesRead > 0) {
                    buf.flip();

                    byte[] data = new byte[numBytesRead];
                    buf.get(data, 0, numBytesRead);

                    TcpConn conn = mConnPool.getConn(channel);
                    mRecvWorker.putData(conn, data);

                    buf.clear();
                }
                else {
                    try {
                        channel.close();
                    } catch (IOException e) {
                    }

                    TcpConn conn = mConnPool.getConn(channel);
                    mRecvWorker.putClosedConnection(conn);
                    mConnPool.remove(conn);
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

        public void putNewConnection(TcpConn conn) {
            TcpPacket packet = new TcpPacket();
            packet.type = TcpPacket.Type.Accept;
            packet.conn = conn;

            synchronized (this) {
                try {
                    mQueue.put(packet);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void putClosedConnection(TcpConn conn) {
            TcpPacket packet = new TcpPacket();
            packet.type = TcpPacket.Type.Closed;
            packet.conn = conn;

            synchronized (this) {
                try {
                    mQueue.put(packet);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void putData(TcpConn conn, byte[] data) {
            TcpPacket packet = new TcpPacket();
            packet.type = TcpPacket.Type.Receive;
            packet.data = data.clone();
            packet.conn = conn;

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

                else if (packet.type == TcpPacket.Type.Accept) {
                    mListener.onAccept(TcpServer.this, packet.conn);
                }

                else if (packet.type == TcpPacket.Type.Closed) {
                    mListener.onConnectionClosed(TcpServer.this, packet.conn);
                }
                else if (packet.type == TcpPacket.Type.Receive) {
                    mListener.onReceived(TcpServer.this, packet.conn, packet.data);
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

        public void putData(TcpConn conn, byte[] data) {
            synchronized (this) {
                TcpPacket packet = new TcpPacket();
                packet.type = TcpPacket.Type.Send;
                packet.conn = conn;
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
                    SocketChannel channel = packet.conn.getChannel();

                    ByteBuffer buffer = ByteBuffer.wrap(packet.data);
                    buffer.clear();

                    int writeSize = 0;
                    while (true) {
                        int size = 0;
                        try {
                            size = channel.write(buffer);
                        } catch (IOException e) {
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
