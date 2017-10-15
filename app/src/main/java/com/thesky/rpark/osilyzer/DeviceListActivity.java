package com.thesky.rpark.osilyzer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    public static final long SCAN_PERIOD = 8*1000;

    public static final int SCAN_BLE = 0;
    public static final int SCAN_BLUETOOTH = 1;
    public static final String SCANING_DEVICE_TYPE = "device_type";

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private int whichDevice;
    private  BleManager mBleManager;
    private ActivityHandler mActivityHandler;
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    private ArrayList<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();

    // UI stuff
    Button mScanButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        mScanButton = (Button) findViewById(R.id.button_scan);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mNewDevicesArrayAdapter.clear();
                doDiscovery(whichDevice);
                v.setVisibility(View.GONE);
            }
        });

        mActivityHandler = new ActivityHandler();

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.adapter_device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.adapter_device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }

        whichDevice = getIntent().getIntExtra(SCANING_DEVICE_TYPE, SCAN_BLUETOOTH);
        if(whichDevice == SCAN_BLUETOOTH){
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            this.registerReceiver(mReceiver, filter);
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            this.registerReceiver(mReceiver, filter);
        } else {
            mBleManager = BleManager.getInstance(getApplicationContext(), null);
            mBleManager.setScanCallback(mLeScanCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        if(whichDevice == SCAN_BLUETOOTH){
            // Unregister broadcast listeners
            this.unregisterReceiver(mReceiver);
        }
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery(int whichDevice) {
        Log.d(TAG, "doDiscovery() : " + whichDevice);
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        mDevices.clear();
        if (mBleManager.getState() == BleManager.STATE_SCANNING) {
            mBleManager.scanLeDevice(false);
        }

        // Request discover from BluetoothAdapter
        if(whichDevice == SCAN_BLUETOOTH) {
            mBtAdapter.startDiscovery();
        } else if(whichDevice == SCAN_BLE) {
            mBleManager.scanLeDevice(true);
        }

        // Stops scanning after a pre-defined scan period.
        mActivityHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopDiscovery();
            }
        }, SCAN_PERIOD);

    }

    /**
     * Stop device discover
     */
    private void stopDiscovery() {
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(false);
        setTitle(R.string.bt_title);
        // Show scan button
        mScanButton.setVisibility(View.VISIBLE);
        mBleManager.scanLeDevice(false);
    }

    // The on-click listener for all devices in the ListViews
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            if(info != null && info.length() > 16) {
                String address = info.substring(info.length() - 17);
                Log.d(TAG, "User selected device : " + address);

                // Create the result Intent and include the MAC address
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
                mScanButton.setVisibility(View.VISIBLE);
            }
        }
    };

    /**
     * Check if it's already cached
     */
    private boolean checkDuplicated(BluetoothDevice device) {
        for(BluetoothDevice dvc : mDevices) {
            if(device.getAddress().equalsIgnoreCase(dvc.getAddress())) {
                return true;
            }
        }
        return false;
    }
    /**
     * BLE scan callback
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, "# Scan device rssi is " + rssi);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        if(!checkDuplicated(device)) {
                            mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                            mNewDevicesArrayAdapter.notifyDataSetChanged();
                            mDevices.add(device);
                        }
                    }
                }
            });
        }
    };

    public class ActivityHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {}
            super.handleMessage(msg);
        }
    }
}
