
package com.milink.miracast;

public class ScreenMirroring {

    private static final String TAG = ScreenMirroring.class.getSimpleName();

    static {
        System.loadLibrary("miracast");
    }

    private static final ScreenMirroring single = new ScreenMirroring();

    public static ScreenMirroring getInstance() {
        return single;
    }

    private ScreenMirroring() {
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public native int start(String ip, int port);

    public native int stop();
}
