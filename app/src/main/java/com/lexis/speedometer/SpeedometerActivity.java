package com.lexis.speedometer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;

public class SpeedometerActivity extends Activity {

    public static class FlipPageViewTransformer implements ViewPager.PageTransformer {
        @Override
        public void transformPage(View page, float progress) {
            float regression = 1 - Math.abs(progress);
            ViewPager viewPager = (ViewPager) page.getParent();
            page.setCameraDistance(15000);

            page.setAlpha(regression);
            page.setVisibility(regression > 0.5 ? View.VISIBLE : View.INVISIBLE);
            int scroll = viewPager.getScrollX() - page.getLeft();
            page.setTranslationX(scroll);
            page.setScaleX((regression != 0 && regression != 1) ? regression : 1);
            page.setScaleY((regression != 0 && regression != 1) ? regression : 1);
            float rotation = 180 * (regression + 1);
            if (progress > 0) {
                rotation = -rotation;
            }
            page.setRotationY(rotation);
        }
    }

    private class DataServiceConnection implements ServiceConnection {
        private DataCollectionService mService;
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            DataCollectionService.LocalBinder binder = (DataCollectionService.LocalBinder)iBinder;
            mService = binder.registerActivity(SpeedometerActivity.this);

            ViewPager pager = findViewById(R.id.speedomerter_pager);
            pager.setAdapter(new SpeedometerAdapter(SpeedometerActivity.this, mService));
            pager.setPageTransformer(false, new FlipPageViewTransformer());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }

        DataCollectionService getService() {
            return mService;
        }
    }

    private DataServiceConnection serviceConnection = new DataServiceConnection();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.speedometer_pager);

        startService(new Intent(this, DataCollectionService.class));

        bindService(new Intent(this, DataCollectionService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);

        alertBuilder.setMessage(R.string.datagen_running);
        alertBuilder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            if (serviceConnection.getService() != null) {
                serviceConnection.getService().stopService(new Intent(this, DataCollectionService.class));
            }
            SpeedometerActivity.super.onBackPressed();
        });
        alertBuilder.setNegativeButton(R.string.no, (dialogInterface, i) -> SpeedometerActivity.super.onBackPressed());

        alertBuilder.create().show();
    }
}
