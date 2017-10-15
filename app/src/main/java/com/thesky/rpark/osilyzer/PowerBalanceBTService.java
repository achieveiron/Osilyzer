/*
 * Copyright (C) 2014 Bluetooth Connection Template
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thesky.rpark.osilyzer;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.IntDef;
import android.util.Log;
import android.widget.Toast;

public class PowerBalanceBTService extends Service {
    private static final String TAG = "PowerBalanceBTService";

    // Context, System
    private Context mContext = null;
    private static Handler mActivityHandler = null;
    private ServiceHandler mServiceHandler = new ServiceHandler();
    private final IBinder mBinder = new ServiceBinder();

    //Bluetooth
    private BluetoothAdapter mBluetoothAdapter = null;
    private BleManager mBleManager = null;
    private ConnectionInfo mConnectionInfo = null;
    private TransactionBuilder mTransactionBuilder = null;
    //private TransactionReceiver mTransactionReceiver = null;

    @Override
    public void onCreate() {
        Log.d(TAG, "# Service - onCreate() starts here");

        mContext = getApplicationContext();
        initialize();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "# Service - onStartCommand() starts here");

        // If service returns START_STICKY, android restarts service automatically after forced close.
        // At this time, onStartCommand() method in service must handle null intent.
        return Service.START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "# Service - onBind()");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "# Service - onUnbind()");
        return true;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "# Service - onDestroy()");
        finalizeService();
    }

    @Override
    public void onLowMemory() {
        Log.d(TAG, "# Service - onLowMemory()");
        finalizeService();
    }

    /*****************************************************
     *	Private methods
     ******************************************************/
    private void initialize() {
        boolean isBleSupported = true;
        Log.d(TAG, "# Service : initialize ---");

        AppSettings.initializeAppSettings(mContext);
        startServiceMonitoring();

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.bt_ble_not_supported, Toast.LENGTH_SHORT).show();
            isBleSupported = false;
        }

        // Make instances
        mConnectionInfo = ConnectionInfo.getInstance(mContext);
        //mCommandParser = new CommandParser();

        // Get local Bluetooth adapter
        if(mBluetoothAdapter == null)
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            // BT is not on, need to turn on manually.
            // Activity will do this.
        } else {
            if(mBleManager == null && isBleSupported) {
                setupBLE();
            }
        }
    }

    /**
     * Send message to device.
     * @param message		message to send
     */
    private void sendMessageToDevice(String message) {
        if(message == null || message.length() < 1)
            return;

        TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
        transaction.begin();
        transaction.setMessage(message);
        transaction.settingFinished();
        transaction.sendTransaction();
    }
    /*****************************************************
     *	Public methods
     ******************************************************/
    public void finalizeService() {
        Log.d(TAG, "# Service : finalize ---");

        // Stop the bluetooth session
        mBluetoothAdapter = null;
        if (mBleManager != null) {
            mBleManager.finalize();
        }
        mBleManager = null;
    }

    /**
     * Setting up bluetooth connection
     * @param h
     */
    public void setupService(Handler h) {
        mActivityHandler = h;

        // Double check BT manager instance
        if(mBleManager == null)
            setupBLE();

        // Initialize transaction builder & receiver
        if(mTransactionBuilder == null)
            mTransactionBuilder = new TransactionBuilder(mBleManager, mActivityHandler);
        //if(mTransactionReceiver == null)
        //    mTransactionReceiver = new TransactionReceiver(mActivityHandler);

        // TODO: If ConnectionInfo holds previous connection info,
        // try to connect using it.
        if(mConnectionInfo.getDeviceAddress() != null && mConnectionInfo.getDeviceName() != null) {
            //connectDevice(mConnectionInfo.getDeviceAddress());
        }
        else {
            if (mBleManager.getState() == BleManager.STATE_NONE) {
                // Do nothing
            }
        }
    }

    /**
     * Setup and initialize BLE manager
     */
    public void setupBLE() {
        Log.d(TAG, "Service - setupBLE()");

        // Initialize the BluetoothManager to perform bluetooth le scanning
        if(mBleManager == null)
            mBleManager = BleManager.getInstance(mContext, mServiceHandler);
    }

    /**
     * Check bluetooth is enabled or not.
     */
    public boolean isBluetoothEnabled() {
        if(mBluetoothAdapter==null) {
            Log.e(TAG, "# Service - cannot find bluetooth adapter. Restart app.");
            return false;
        }
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * Get scan mode
     */
    public int getBluetoothScanMode() {
        int scanMode = -1;
        if(mBluetoothAdapter != null)
            scanMode = mBluetoothAdapter.getScanMode();

        return scanMode;
    }


    /**
     * Connect to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public void connectDevice(String address) {
        if(address != null && mBleManager != null) {
            //mBleManager.disconnect();

            if(mBleManager.connectGatt(mContext, true, address)) {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mConnectionInfo.setDeviceAddress(address);
                mConnectionInfo.setDeviceName(device.getName());
            }
        }
    }

    /**
     * Connect to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public void connectDevice(BluetoothDevice device) {
        if(device != null && mBleManager != null) {
            //mBleManager.disconnect();

            if(mBleManager.connectGatt(mContext, true, device)) {
                mConnectionInfo.setDeviceAddress(device.getAddress());
                mConnectionInfo.setDeviceName(device.getName());
            }
        }
    }

    /**
     * Get connected device name
     */
    public String getDeviceName() {
        return mConnectionInfo.getDeviceName();
    }

    /**
     * Send message to remote device using Bluetooth
     */
    public void sendMessageToRemote(String message) {
        sendMessageToDevice(message);
    }

    /**
     * Start service monitoring. Service monitoring prevents
     * unintended close of service.
     */
    public void startServiceMonitoring() {
        if(AppSettings.getBgService()) {
            ServiceMonitoring.startMonitoring(mContext);
        } else {
            ServiceMonitoring.stopMonitoring(mContext);
        }
    }


    /*****************************************************
     *	Handler, Listener, Timer, Sub classes
     ******************************************************/
    public class ServiceBinder extends Binder {
        public PowerBalanceBTService getService() {
            return PowerBalanceBTService.this;
        }
    }

    /**
     * Receives messages from bluetooth manager
     */
    class ServiceHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {

            switch(msg.what) {
                // Bluetooth state changed
                case BleManager.MESSAGE_STATE_CHANGE:
                    // Bluetooth state Changed
                    Log.d(TAG, "Service - MESSAGE_STATE_CHANGE: " + msg.arg1);

                    switch (msg.arg1) {
                        case BleManager.STATE_NONE:
                            mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_INITIALIZED).sendToTarget();
                            break;

                        case BleManager.STATE_CONNECTING:
                            mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_CONNECTING).sendToTarget();
                            break;

                        case BleManager.STATE_CONNECTED:
                            mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_CONNECTED).sendToTarget();
                            break;

                        case BleManager.STATE_IDLE:
                            mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_INITIALIZED).sendToTarget();
                            break;
                    }
                    break;

                // If you want to send data to remote
                case BleManager.MESSAGE_WRITE:
                    Log.d(TAG, "Service - MESSAGE_WRITE: ");
                    String message = (String) msg.obj;
                    if(message != null && message.length() > 0)
                        sendMessageToDevice(message);
                    break;

                // Received packets from remote
                case BleManager.MESSAGE_READ:
                    Log.d(TAG, "Service - MESSAGE_READ: ");
                    byte[] readBuf = (byte[]) msg.obj;
                    int data_length = msg.arg1;
                    // send bytes in the buffer to activity
                    if(readBuf != null && readBuf.length > 0) {
                        mActivityHandler.obtainMessage(Constants.MESSAGE_READ_CHAT_DATA, data_length, -1, readBuf)
                                .sendToTarget();
                        /*
                        int command = mCommandParser.setString(new String(readBuf));
                        if(command == CommandParser.COMMAND_THINGSPEAK) {
                            String parameters = mCommandParser.getParameterString();
                            StringBuilder requestUrl = new StringBuilder("http://184.106.153.149/update?");
                            if(parameters != null && parameters.length() > 0)
                                requestUrl.append(parameters);

                            //Logs.d("# Find thingspeak command. URL = "+requestUrl);

                            HttpAsyncTask task = new HttpAsyncTask(mHTTPListener, 0, requestUrl.toString(), HttpInterface.REQUEST_TYPE_GET);
                            task.execute();
                            mCommandParser.resetParser();

                        }
                        */
                    }
                    break;

                case BleManager.MESSAGE_DEVICE_NAME:
                    Log.d(TAG, "Service - MESSAGE_DEVICE_NAME: ");

                    // save connected device's name and notify using toast
                    String deviceAddress = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS);
                    String deviceName = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_NAME);

                    if(deviceName != null && deviceAddress != null) {
                        // Remember device's address and name
                        mConnectionInfo.setDeviceAddress(deviceAddress);
                        mConnectionInfo.setDeviceName(deviceName);

                        Toast.makeText(getApplicationContext(),
                                "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;

                case BleManager.MESSAGE_TOAST:
                    Log.d(TAG, "Service - MESSAGE_TOAST: ");

                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;

            }	// End of switch(msg.what)

            super.handleMessage(msg);
        }
    }	// End of class MainHandler
}
