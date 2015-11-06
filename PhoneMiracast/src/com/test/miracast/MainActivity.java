
package com.test.miracast;

import android.content.Context;
import android.content.IntentFilter;
import android.media.RemoteDisplay;
import android.net.NetworkUtils;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.hardware.display.VirtualDisplay;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceControl;

import com.milink.bonjour.Bonjour;
import com.milink.bonjour.BonjourListener;
import com.milink.milink.client.MiLinkClient;
import com.milink.milink.client.MiLinkClientListener;
import com.milink.milink.common.IQ;
import com.milink.miracast.ScreenMirroring;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends Activity implements MiLinkClientListener, BonjourListener {

    private static final String MILINK = "_milink._tcp.local.";

    private static final String TAG = "@@@@";
    private static MiLinkClient mClient = null;
    private static Bonjour mBonjour = null;
    private ArrayList<Device> mDevices = new ArrayList<Device>();

    private class Device {
        public String ip;
        public int port;
        public String name;
        public String type;
    }

    private int mDeviceCurrentPosition = 0;
    private int mActionId = 0;
    private boolean mMiracast = false;
    private Handler mHandler;
    private RemoteDisplay mRemoteDisplay;
    private static final int DEFAULT_CONTROL_PORT = 7236;
    private WifiManager mWifiManager;
    private RemoteDisplay.Listener mRemoteDisplayListener;
    private String mDeviceAddr;
    private DisplayManager mDisplayManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate");

        mBonjour = Bonjour.getInstance();
        mBonjour.setContent(this);
        mBonjour.setListener(this);
        mBonjour.start();

        mClient = new MiLinkClient(this);

        Device device = new Device();
        device.ip = "127.0.0.1";
        device.type = "_milink._tcp";
        device.name = "我的手机";
        device.port = 0;
        mDevices.add(device);
        
        mHandler = new Handler () {
        	
        };

        mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        
        mDisplayManager = (DisplayManager)this.getSystemService(Context.DISPLAY_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem mi = menu.add(R.string.push);
        mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mi.setIcon(android.R.drawable.ic_menu_share);

        return true;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        mBonjour.stop();
        mClient.disconnect();
        super.onDestroy();
    }

    private void startServer(String ip, int port) {
        String iface = ip + ":" + port;
        mDeviceAddr = ip;
        //RemoteDisplay is hide class, we need change framework code.
        RemoteDisplay.Listener listener = new RemoteDisplay.Listener() {
            @Override
            public void onDisplayConnected(Surface surface,
                    int width, int height, int flags, int session) {
                Log.i(TAG, "Connected RTSP connection with Wifi display: ");
                VirtualDisplay display = mDisplayManager.createVirtualDisplay(
                        "aa", width, height, (int)1.5, surface, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR |
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION);
                if (display != null) {
                    Log.i(TAG, "virtual display create success!");
                }
            }

            @Override
            public void onDisplayDisconnected() {
                Log.i(TAG, "Closed RTSP connection with Wifi display: ");
                mDisplayManager.removeDisplayDeviceLocked();
            }

            @Override
            public void onDisplayError(int error) {
                Log.i(TAG, "Lost RTSP connection with Wifi display due to error ");
                mDisplayManager.removeDisplayDeviceLocked();
            }
        };

        mRemoteDisplay = RemoteDisplay.listen(iface, listener,
                mHandler);
    }

    private void stopServer() {
        mDisplayManager.removeDisplayDeviceLocked();
        mRemoteDisplay.dispose();
    }

    private void startMiracast(String ip, int port) {
        if (!mMiracast) {
            mMiracast = true;
            mClient.connect(ip, port, 1000 * 5);
//            WifiInfo info = mWifiManager.getConnectionInfo();
//            if (info == null) {
//            	Log.d(TAG, "wifi network do not connect!");
//            	return;
//            }
//            	
//            String iface = Formatter.formatIpAddress(info.getIpAddress()) + ":" + DEFAULT_CONTROL_PORT;
           
        }
    }

    private void stopMiracast() {
        if (mMiracast) {
            mMiracast = false;

            String param = "<root/>";
            IQ iq = new IQ(IQ.Type.Set,
                    mActionId++,
                    com.milink.milink.contants.Xmlns.MIRACAST,
                    com.milink.milink.contants.miracast.Actions.STOP,
                    param.getBytes());

            mClient.send(iq);
            mClient.disconnect();
            
//            int ret = ScreenMirroring.getInstance().stop();
            stopServer();
            Log.d(TAG, String.format("ScreenMirroring.stop: %d", 0));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        synchronized (mDevices) {
            final ArrayList<Device> deviceList = new ArrayList<Device>();
            for (Device device : mDevices) {
                deviceList.add(device);
            }

            final ArrayList<String> names = new ArrayList<String>();
            for (Device device : mDevices) {
                names.add(device.name);
            }

            String[] deviceNames = new String[names.size()];
            names.toArray(deviceNames);

            new AlertDialog.Builder(this).setTitle("Device List").setItems(
                    deviceNames,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int pos) {
                            if (pos == 0) {
                                mDeviceCurrentPosition = 0;
                                stopMiracast();
                                getActionBar().setTitle("Stop Miracast");
                            } else if (pos != mDeviceCurrentPosition) {
                                if (mDeviceCurrentPosition != 0) {
                                    stopMiracast();
                                }

                                mDeviceCurrentPosition = pos;
                                Device d = deviceList.get(pos);
                                getActionBar().setTitle(
                                        String.format("Start Miracast to : %s", d.name));

                                startMiracast(d.ip, d.port);
                            } else {
                                mDeviceCurrentPosition = pos;
                                Device d = deviceList.get(pos);
                                getActionBar().setTitle(
                                        String.format("Start Miracast to : %s", d.name));

                                startMiracast(d.ip, d.port);
                            }
                        }

                    })
                    .create().show();
        }

        return true;
    }

    @Override
    public void onConnected(MiLinkClient client) {
        Log.d(TAG, "onConnected");

        String ip = client.getSelfIp();
        int port = DEFAULT_CONTROL_PORT;
//        int ret = ScreenMirroring.getInstance().start(ip, port);
        startServer(ip, port);

        Log.d(TAG, "start rtsp server, ip: " + ip + ", port: " + port);
        
        ParamStart param = ParamStart.create(ip, port);
        //String param = String.format("<root><ip>%s</ip><port>%d</port></root>", ip, port);

        IQ iq = new IQ(IQ.Type.Set,
                mActionId++,
                com.milink.milink.contants.Xmlns.MIRACAST,
                com.milink.milink.contants.miracast.Actions.START,
                param.toString().getBytes());

        mClient.send(iq);
    }

    @Override
    public void onConnectedFailed(MiLinkClient client) {
        Log.d(TAG, "onConnectedFailed");

        //getActionBar().setTitle("Start Miracast to : %s failed!");
        
//        int ret = ScreenMirroring.getInstance().stop();
        int ret = 0;
        stopServer();
        Log.d(TAG, String.format("ScreenMirroring.stop: %d", ret));
    }

    @Override
    public void onDisconnect(MiLinkClient client) {
        Log.d(TAG, "onDisconnect");
    }

    @Override
    public void onReceived(MiLinkClient client, IQ iq) {
        Log.d(TAG, "onReceived");
        Log.d(TAG, iq.toString());
    }

    @Override
    public void onEvent(MiLinkClient client, IQ iq) {
        Log.d(TAG, "onEvent");
        
        if (iq.getType() != IQ.Type.Event)
            return;
        
        if (! iq.getXmlns().equalsIgnoreCase(com.milink.milink.contants.Xmlns.MIRACAST))
            return;
        
        if (iq.getEvent().equalsIgnoreCase(com.milink.milink.contants.miracast.Events.STOPPED)) {
            Log.d(TAG, "TV stopped!");
//            ScreenMirroring.getInstance().stop();
            stopServer();
            return;
        }
    }

    @Override
    public void onServiceFound(String name, String type, String ip, int port,
            Map<String, String> properties) {
        Log.d(TAG, String.format("onServiceFound: %s %s %s:%d", name, type, ip, port));

        Device device = new Device();
        device.ip = ip;
        device.type = type;
        device.name = name;
        device.port = port;

        mDevices.add(device);
    }

    @Override
    public void onServiceLost(String name, String type, String ip) {
        Log.d(TAG, String.format("onServiceLost: %s %s %s:%d", name, type, ip));

        for (Device device: mDevices) {
            if (device.ip.equalsIgnoreCase(ip)) {
                mDevices.remove(device);
                break;
            }
        }
    }

    @Override
    public void onStarted() {
        mBonjour.discoveryService(MILINK);
    }

    @Override
    public void onStartFailed() {
    }

    @Override
    public void onStopped() {
    }
}
