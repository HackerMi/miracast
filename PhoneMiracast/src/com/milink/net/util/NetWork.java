
package com.milink.net.util;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetWork {
    
    private static byte[] mMacAddress = null;

    public static byte[] getMacAddress() {
        final int hwAddrLengthInBytes = 6;
        if (mMacAddress == null) {
            try {
                Enumeration<NetworkInterface> netInterfaces = NetworkInterface
                        .getNetworkInterfaces();

                while (netInterfaces.hasMoreElements()) {
                    NetworkInterface ni = netInterfaces.nextElement();
                    byte[] addrBytes = ni.getHardwareAddress();
                    if (!ni.isLoopback() && addrBytes != null
                            && hwAddrLengthInBytes == addrBytes.length) {
                        mMacAddress = addrBytes;
                        break;
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }

            if (null == mMacAddress) {
                mMacAddress = new byte[hwAddrLengthInBytes];
                for (int i = 0; i < hwAddrLengthInBytes; ++i) {
                    mMacAddress[i] = (byte) (Math.random() * 0xff);
                }
            }
        }

        return mMacAddress;
    }
    
    public static byte[] getLocalIpInt(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!wm.isWifiEnabled()) {
            return null;
        }

        WifiInfo wi = wm.getConnectionInfo();
        return intToBytes(wi.getIpAddress());
    }
    
    public static String getLocalIpString(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!wm.isWifiEnabled()) {
            return null;
        }

        WifiInfo wi = wm.getConnectionInfo();
        return intToString(wi.getIpAddress());
    }

    private static String intToString(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
                + ((i >> 24) & 0xFF);
    }
    
    private static byte[] intToBytes(int i) {
        byte[] ip = new byte[4];
        ip[0] = (byte)(i & 0xFF);
        ip[1] = (byte)((i >> 8) & 0xFF);
        ip[2] = (byte)((i >> 16) & 0xFF);
        ip[3] = (byte)((i >> 24) & 0xFF);
        return ip;
    }

    public static String getLocalIpAddress(String remoteIpAddress) {

        Enumeration<NetworkInterface> en = null;

        try {
            en = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }

        while (en.hasMoreElements()) {

            NetworkInterface nif = en.nextElement();
            Enumeration<InetAddress> enumIpAddr = nif.getInetAddresses();

            while (enumIpAddr.hasMoreElements()) {

                InetAddress inetAddress = enumIpAddr.nextElement();

                if (inetAddress.isLoopbackAddress())
                    continue;

                if (!(inetAddress instanceof Inet4Address))
                    continue;

                String ip = inetAddress.getHostAddress().toString();

                if (remoteIpAddress == null)
                    return ip;

                String[] localIp = ip.split("\\.");
                String[] remoteIp = remoteIpAddress.split("\\.");
                if (localIp.length != 4 || remoteIp.length != 4)
                    return ip;

                if (localIp[0].equals(remoteIp[0])
                        && localIp[1].equals(remoteIp[1])
                        && localIp[2].equals(remoteIp[2]))
                    return ip;
            }
        }

        return null;
    }

    /**
     * Try to extract a hardware MAC address from a given IP address using the
     * ARP cache (/proc/net/arp).<br>
     * <br>
     * We assume that the file has this structure:<br>
     * <br>
     * IP address HW type Flags HW address Mask Device 192.168.18.11 0x1 0x2
     * 00:04:20:06:55:1a * eth0 192.168.18.36 0x1 0x2 00:22:43:ab:2a:5b * eth0
     * 
     * @param ip
     * @return the MAC from the ARP cache
     */
    public static String getMacFromArpCache(String ip) {
        if (ip == null)
            return null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4 && ip.equals(splitted[0])) {
                    // Basic sanity check
                    String mac = splitted[3];
                    if (mac.matches("..:..:..:..:..:..")) {
                        return mac;
                    } else {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getMacFromArpCache(int ip) {
        byte[] i = new byte[4];
        i[0] = (byte) ((byte) ip & 0xff);
        i[1] = (byte) ((byte) (ip >> 8) & 0xff);
        i[2] = (byte) ((byte) (ip >> 16) & 0xff);
        i[3] = (byte) ((byte) (ip >> 24) & 0xff);
        String ipString = String.format("%d.%d.%d.%d", i[0], i[1], i[2], i[3]);
        return getMacFromArpCache(ipString);
    }

    public static String getManufactory(String mac) {
        if (mac == null)
            return null;

        String[] m = mac.split(":");
        if (m.length < 3)
            return null;

        String manufactory = String.format("%s-%s-%s", m[0], m[1], m[2]);
        return manufactory;
    }
}
