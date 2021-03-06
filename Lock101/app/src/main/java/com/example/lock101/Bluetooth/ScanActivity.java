package com.example.lock101.Bluetooth;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.lock101.R;

import java.util.ArrayList;

public class ScanActivity extends AppCompatActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning = true;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000; // 스캔 시간 10초
//    private final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;

    private static int PERMISSION_REQUEST_CODE = 3;
    private final static String TAG = ScanActivity.class.getSimpleName();



    ListView list1;
    TextView text1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Request Location Permissions:");
        // 버전 체크후 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        }



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        mHandler = new Handler();
        list1 = findViewById(R.id.list1);
        text1 = findViewById(R.id.textView);

        // ble 지원하는 기기인지 확인
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // 블루투스 어댑터 가져오기
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 블루투스를 지원하는 기기인지 확인
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 블루투스가 사용중인지 확인하고 아니면 확인창 띄우기
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // 어댑터 세팅
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        list1.setAdapter(mLeDeviceListAdapter);

        scanLeDevice(true);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for(int i=0; i<grantResults.length; i++){
            if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                text1.append("\n" + permissions[i] + " : 허용\n");
            } else {
                text1.append(permissions[i] + " : 거부\n");
            }
        }


//        if (requestCode == PERMISSION_REQUEST_CODE) {
//            //Do something based on grantResults
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                text1.setText("permission granted");
//                Log.d(TAG, "coarse location permission granted");
//            } else {
//                Log.d(TAG, "coarse location permission denied");
//            }
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // 사용자가 블루투스 사용 중지시 종료
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    // 옵션 메뉴 구성
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);

        return true;
    }

    // 옵션 메뉴 선택시
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

//    private ScanCallback mScanCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            super.onScanResult(callbackType, result);
//            Log.d("ddd", "onScanResult : " + result.getDevice().getName());
//        }
//
//        @Override
//        public void onBatchScanResults(List<ScanResult> results) {
//            for(ScanResult result : results){
////                processResult(result);
//                Log.d("ddd", "onBatchScanResults : " + result.getDevice().getName());
//
//                mLeDeviceListAdapter.addDevice(result.getDevice());
//                mLeDeviceListAdapter.notifyDataSetChanged();
//            }
//        }

//    private void processResult(final ScanResult result) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("ddd", "ddddddddd");
//                mLeDeviceListAdapter.addDevice(result.getDevice());
//                mLeDeviceListAdapter.notifyDataSetChanged();
//            }
//        });
//    }






    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("ddd", "device name : " + device.getName());
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevcies;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter(){
            super();
            mLeDevcies = new ArrayList<BluetoothDevice>();
            mInflator = ScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device){
            if(!mLeDevcies.contains(device)){
                mLeDevcies.add(device);
            }
        }

        public BluetoothDevice getDevcie(int position){ return mLeDevcies.get(position); }

//        mLeDevcies.clear();
        public void clear(){
//            if(mLeDevcies != null){
                mLeDevcies.clear();
//            }

        }

        @Override
        public int getCount() {
            return mLeDevcies.size();
        }

        @Override
        public Object getItem(int position) {
            return mLeDevcies.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            // 재사용 가능한 뷰가 없다면 뷰를 생성
            if(convertView == null){
                convertView = mInflator.inflate(R.layout.listitem_device, null);
            }
            // 뷰를 구성
            TextView sub_text = (TextView)convertView.findViewById(R.id.device_name);
            TextView sub_text2 = (TextView)convertView.findViewById(R.id.device_address);

            BluetoothDevice device = mLeDevcies.get(position);
            final String devcieName = device.getName();

            Log.d(TAG, "device :" + device.getName());
            if(devcieName != null && devcieName.length() > 0)
                sub_text.setText(devcieName);
            else
                sub_text.setText("Unknow device");
            sub_text2.setText(device.getAddress());

            return convertView;
        }
    }

}
