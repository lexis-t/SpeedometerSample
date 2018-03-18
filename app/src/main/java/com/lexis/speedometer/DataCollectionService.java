package com.lexis.speedometer;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.ArraySet;
import android.util.Log;

import java.util.Set;

public class DataCollectionService extends Service implements IDataProvider {

    static final String LOG_TAG = "SpeedometerService";

    static {
        System.loadLibrary("speedometer-lib");
    }

    class LocalBinder extends Binder {
        Activity activity;
        synchronized DataCollectionService registerActivity(Activity activity) {
            this.activity = activity;
            return DataCollectionService.this;
        }
        synchronized Activity getActivity() {
            return activity;
        }
        synchronized void resetActivity() {
            this.activity = null;
        }

    }

    private class NativeListener implements IDataListener {

        private final Set<IDataListener> javaListeners = new ArraySet<>();
        private int channel;

        NativeListener(int channel) {
            this.channel = channel;
        }

        // Called from UI thread
        void addJavaListener(IDataListener l) {
            javaListeners.add(l);
        }

        // Called from UI thread
        void removeJavaListener(IDataListener l) {
            javaListeners.remove(l);
        }

        @Override
        public void onNewValue(double value) {
            Activity activity = binder.getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    for (IDataListener l : javaListeners) {
                        l.onNewValue(value);
                    }
                });
            }
        }
    }

    private LocalBinder binder = new LocalBinder();
    NativeListener[] nativeListeners;

    @Override
    public void onCreate() {
        super.onCreate();

        nativeListeners = new NativeListener[] {new NativeListener(0), new NativeListener(1)};
        nativeStart(nativeListeners);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        binder.resetActivity();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        nativeCleanup();
        super.onDestroy();
    }

    @Override
    public void registerListener(int channel, IDataListener listener) {
        nativeListeners[channel].addJavaListener(listener);
    }

    @Override
    public void unregisterListener(int channel, IDataListener listener) {
        nativeListeners[channel].removeJavaListener(listener);
    }

    private static native void nativeStart(IDataListener[] listeners);
    private static native void nativeCleanup();
}
