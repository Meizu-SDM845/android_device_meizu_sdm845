/*
 * Copyright (C) 2020 The MoKee Open Source Project
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.lineageos.device.parts;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AODService extends Service {

    private static final String TAG = "AODService";
    private static final boolean DEBUG = false;

    private static final long AOD_DELAY_MS = 1000;

    private ExecutorService mExecutorService;
    private SettingObserver mSettingObserver;
    private ScreenReceiver mScreenReceiver;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mInteractive = true;

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "Creating service");

        mSettingObserver = new SettingObserver(this);
        mScreenReceiver = new ScreenReceiver(this);

        mSettingObserver.enable();

        if (Utils.isAODEnabled(this)) {
            mScreenReceiver.enable();
            mExecutorService = Executors.newSingleThreadExecutor();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(TAG, "Destroying service");

        mSettingObserver.disable();
        mScreenReceiver.disable();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void onSettingChange() {
        if (Utils.isAODEnabled(this)) {
            Log.d(TAG, "AOD enabled");
            mScreenReceiver.enable();
        } else {
            Log.d(TAG, "AOD disabled");
            mScreenReceiver.disable();
        }
    }

    void onDisplayOn() {
        Log.d(TAG, "Device interactive");
        mInteractive = true;
        mHandler.removeCallbacksAndMessages(null);
    }

    void onDisplayOff() {
        Log.d(TAG, "Device non-interactive");
        mHandler.postDelayed(() -> {
            mInteractive = false;
            mExecutorService.execute(someRunnable);
        }, AOD_DELAY_MS);
    }


    Runnable someRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Trigger AOD");
            while (!mInteractive) {
                Utils.enterAOD();
                Utils.boostAOD();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

}