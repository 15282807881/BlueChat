package com.example.test;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

public class DeviceList extends AppCompatActivity {
    public static String EXTRA_DEVICE_ADDRESS="device_address";
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String>mNewDevicesArrayAdapter;
    private IntentFilter filter = new IntentFilter();//隐式跳转
    private ProgressBar progressBar;//进度条

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);
        setResult(Activity.RESULT_CANCELED);//设置返回结果：操作已经取消
        //绑定控件
        progressBar = (ProgressBar)findViewById(R.id.processbar);
        Button scanButton=(Button)findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doDiscovery();
                view.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        mPairedDevicesArrayAdapter=new ArrayAdapter<String>(this, R.layout.list_item);
        mNewDevicesArrayAdapter=new ArrayAdapter<String>(this, R.layout.list_item);

        ListView pairedListView=(ListView)findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListen);

        ListView newDeviceListView=(ListView)findViewById(R.id.new_devices);
        newDeviceListView.setAdapter(mNewDevicesArrayAdapter);
        newDeviceListView.setOnItemClickListener(mDeviceClickListen);

        filter.addAction(BluetoothDevice.ACTION_FOUND);//ACTION_FOUND蓝牙扫描时，扫描到任一远程蓝牙设备时，会发送此广播
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);// ACTION_DISCOVERY_FINISHED 蓝牙扫描过程结束
        this.registerReceiver(mReceiver,filter);
        //得到默认的适配器
        mBtAdapter=BluetoothAdapter.getDefaultAdapter();
        //将搜索到的蓝牙设备放在控件中
        Set<BluetoothDevice> pairedDevices=mBtAdapter.getBondedDevices();
        if(pairedDevices.size()>0){
            for(BluetoothDevice device : pairedDevices){
                mPairedDevicesArrayAdapter.add(device.getName()+"\n" +device.getAddress());
            }
        }else{
            String noDevices=getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }
    //销毁广播
    @Override protected void onDestroy(){
        super.onDestroy();
        if(mBtAdapter!=null){//判断蓝牙适配器是否为空，不为空则停止扫描
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);//销毁广播
    }
    //蓝牙扫描
    private void doDiscovery(){
        if(mBtAdapter.isDiscovering())//本地蓝牙设备是否正在扫描,正在扫描则停止扫描
            mBtAdapter.cancelDiscovery();
        mBtAdapter.startDiscovery();//蓝牙扫描
    }
    //当点击listview某一项时，这个回调方法就会被调用。蓝牙适配器里面点击会进行下一项
    private AdapterView.OnItemClickListener mDeviceClickListen=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mBtAdapter.cancelDiscovery();//取消蓝牙扫描

            String info=((TextView) view).getText().toString();
            String address=info.substring(info.length()-17);//最后17位的字符串
            Intent intent =new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS,address);//传递数据：传递mac地址

            setResult(Activity.RESULT_OK,intent);
            finish();
        }
    };
    //广播的得到蓝牙地址
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            // 获取事件类型
            String action=intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){//广播监听到蓝牙
                // 获取蓝牙设备
                BluetoothDevice  device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    // 添加到一开始定义的集合中
                    mNewDevicesArrayAdapter.add(device.getName()+"\n" + device.getAddress());
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))//蓝牙扫描过程结束
            {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DeviceList.this,"搜索完毕",Toast.LENGTH_SHORT).show();
                if(mNewDevicesArrayAdapter.getCount()==0)
                {
                    String noDevices=getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
}
