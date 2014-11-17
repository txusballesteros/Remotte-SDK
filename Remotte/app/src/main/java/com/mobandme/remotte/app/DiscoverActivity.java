package com.mobandme.remotte.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.mobandme.remotte.app.adapter.DeviceAdapter;
import com.mobandme.remotte.app.bus.DeviceBus;

public class DiscoverActivity extends Activity implements BluetoothAdapter.LeScanCallback, AdapterView.OnItemClickListener {

    private static final int   SCANING_PERIOD = (30 * 1000);

    private View             mLoadingView;
    private Handler          mBluetoothScanHandler = new Handler();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private ListView         mList;
    private DeviceAdapter    mAdapter;

    private Runnable mStartScanRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };

    private Runnable mStopScanRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        initializeActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!supportBluetoothLE()) {
            Toast.makeText(this, "Sorry, but Your device does not support Bluetooth Low Energy capabilities.", Toast.LENGTH_LONG).show();
            return;
        }

        mAdapter.clear();
        mAdapter.notifyDataSetChanged();

        mBluetoothScanHandler.post(mStartScanRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopScan();
    }

    @Override
    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.add(device);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void initializeActivity() {
        mLoadingView = LayoutInflater.from(this).inflate(R.layout.scaning_layout, null);

        mAdapter = new DeviceAdapter(this);
        mList = (ListView)findViewById(R.id.list);
        mList.setOnItemClickListener(this);
        mList.addFooterView(mLoadingView);
        mList.setFooterDividersEnabled(false);
        mList.setAdapter(mAdapter);

        mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter != null && hasBluetoothLE()) {
            if (!mBluetoothAdapter.isEnabled())
                mBluetoothAdapter.enable();
        }
    }
    private boolean hasBluetoothLE() { return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE); }

    private void startScan() {
        if (mBluetoothAdapter != null) {
            mList.removeFooterView(mLoadingView);
            mList.addFooterView(mLoadingView);
            mBluetoothAdapter.startLeScan(this);
        }

        mBluetoothScanHandler.postDelayed(mStopScanRunnable, SCANING_PERIOD);
    }

    private void stopScan() {
        if (mBluetoothAdapter != null) {
            mList.removeFooterView(mLoadingView);
            mBluetoothAdapter.stopLeScan(this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        stopScan();
        DeviceBus.deviceAddress = ((BluetoothDevice)view.getTag()).getAddress();
        startActivity(new Intent(this, MenuActivity.class));
    }

    private boolean supportBluetoothLE() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false;
        }
        return true;
    }
}
