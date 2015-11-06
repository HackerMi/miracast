
package com.test.miracast;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.MediaController;
import android.widget.VideoView;

import com.milink.bonjour.Bonjour;
import com.milink.bonjour.BonjourListener;
import com.milink.bonjour.serviceinfo.MiLinkServiceInfo;
import com.milink.milink.common.IQ;
import com.milink.milink.contants.*;
import com.milink.milink.contants.miracast.*;
import com.milink.milink.server.MiLinkServer;
import com.milink.milink.server.MiLinkServerListener;
import com.milink.net.util.NetWork;

import java.util.Map;

public class MainActivity extends Activity implements MiLinkServerListener, BonjourListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private MiLinkServer mServer = null;
    private Bonjour mBonjour = null;
    private int eventId = 0;
    private VideoView mVideoView = null;
    private MediaController mMc = null;
    private Handler mHandler = new Handler();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideoView = (VideoView)findViewById(R.id.videoView);
        mMc = new MediaController(this);
        mVideoView.setMediaController(mMc);
        mVideoView.requestFocus();

        // start Server
        mServer = new MiLinkServer(this);
        mServer.start();

        // start Bonjour
        mBonjour = Bonjour.getInstance();
        mBonjour.setContent(this);
        mBonjour.setListener(this);
        mBonjour.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        this.publishEvent(com.milink.milink.contants.miracast.Events.STOPPED);
        
        mBonjour.stop();
        mServer.stop();
        super.onDestroy();
    }

    public void publishEvent(String event) {
        String param = "<root/>";
        IQ iq = new IQ(IQ.Type.Event,
                eventId++,
                com.milink.milink.contants.Xmlns.MIRACAST,
                event,
                param.getBytes());

        mServer.publishEvent(iq);
    }

    @Override
    public void onAccept(MiLinkServer server, String ip, int port) {
        Log.d(TAG, String.format("onAccept: %s:%d", ip, port));
    }

    @Override
    public void onReceived(MiLinkServer server, String ip, int port, IQ iq) {
        Log.d(TAG, String.format("onReceived: %s:%d", ip, port));
        Log.d(TAG, iq.toString());

        if (! iq.getXmlns().equalsIgnoreCase(Xmlns.MIRACAST)) {
            iq.setType(IQ.Type.Error);
            server.send(ip, port, iq);
            return;
        }
        
        if (iq.getType() != IQ.Type.Set) {
            iq.setType(IQ.Type.Error);
            server.send(ip, port, iq);
            return;
        }
        
        if (iq.getAction().equalsIgnoreCase(Actions.START)){

            final ParamStart param = ParamStart.create(iq.getParam());
            if (param == null){
                iq.setType(IQ.Type.Error);
                server.send(ip, port, iq);
                return;
            }
            
            mHandler.post(new Runnable() {
				@Override
				public void run() {
		            String url = String.format("wfd://%s:%d", param.getIp(), param.getPort());
		            mVideoView.setVideoPath(url);
		            mVideoView.start();
				}
            });
            
            this.publishEvent(Events.PLAYING);
        }
        else if (iq.getAction().equalsIgnoreCase(Actions.STOP)){
        	
            mHandler.post(new Runnable() {
				@Override
				public void run() {
		            mVideoView.stopPlayback();
				}
            });
            
            this.publishEvent(Events.STOPPED);
        }
    }

    @Override
    public void onConnectionClosed(MiLinkServer server, String ip, int port) {
        Log.d(TAG, String.format("onConnectionClosed: %s:%d", ip, port));
    }

    @Override
    public void onServiceFound(String name, String type, String ip, int port,
            Map<String, String> properties) {
        Log.d(TAG, String.format("onServiceFound: %s %s %s:%d", name, type, ip, port));
    }

    @Override
    public void onServiceLost(String name, String type, String ip) {
        Log.d(TAG, String.format("onServiceLost: %s %s %s:%d", name, type, ip));
    }

    @Override
    public void onStarted() {
        Log.d(TAG, String.format("onStarted"));

        // publish Service
        byte[] deviceId = NetWork.getMacAddress();
        String name = "MiTV";
        int port = mServer.getListenPort();
        MiLinkServiceInfo svcInfo = new MiLinkServiceInfo(deviceId, name, port);
        mBonjour.publishService(svcInfo);
    }

    @Override
    public void onStartFailed() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStopped() {
        // TODO Auto-generated method stub
    }
}