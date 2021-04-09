package com.example.test;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static final int MESSAGE_STATE_CHANGE = 1;//状态改变
    public static final int MESSAGE_READ = 2;   //读消息
    public static final int MESSAGE_WRITE = 3;  //写消息
    public static final int MESSAGE_DEVICE_NAME = 4; //
    public static final int MESSAGE_TOAST = 5;//弹消息
    public static final String  DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final int REQUEST_CONNECT_DEVICE = 1; //请求连接
    public static final int REQUEST_ENABLE_BT = 2;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter;
    private ChatService mChatService = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 得到本地蓝牙适配器
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        // 若当前设备不支持蓝牙功能
        if(mBluetoothAdapter == null){
            Toast.makeText(this,"蓝牙不可用",Toast.LENGTH_LONG).show();
            finish();//结束进程
            return;
        }
    }
    @Override
    public void onStart(){
        super.onStart();
        if(!mBluetoothAdapter.isEnabled()){
            // 若当前设备蓝牙功能未开启，则开启蓝牙、弹窗
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //启动活动、startActivityForResult的主要作用就是它可以回传数据、调用本地的蓝牙弹窗
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        } else{
            if(mChatService==null)
                setupChat();
        }
    }

    //synchronized 放在public后面是成员锁
    @Override
    public synchronized void onResume(){
        super.onResume();
        if(mChatService != null)
            if(mChatService.getState() == ChatService.STATE_NONE)
                mChatService.start();
    }
    @Override
    public synchronized void onPause(){
        super.onPause();
    }
    @Override
    public synchronized void onStop(){
        super.onStop();
    }
    @Override
    public synchronized void onDestroy(){
        super.onDestroy();
        if(mChatService != null)
            mChatService.stop();
    }
    //增加侧边菜单、按钮弹窗
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //增加下拉菜单的功能
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()){
            case R.id.scan:
                Intent serverIntent=new Intent(this,DeviceList.class);
                startActivityForResult(serverIntent,REQUEST_CONNECT_DEVICE);//跳转到DeviceList、并实现返回结果
                return true;
            case R.id.discoverable:
                ensureDiscoverable();//使蓝牙能够被其他设备可见
                return true;
            case R.id.BtOpen://打开手机蓝牙、采用手机默认的弹窗打开
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnOn, REQUEST_ENABLE_BT);
                }
                return true;
            case R.id.BtOff:
                mBluetoothAdapter.disable();//关闭蓝牙、调用手机默认的弹窗关闭
                return true;
        }
        return false;
    }

    //使蓝牙能够被其他设备可见函数
    private void ensureDiscoverable(){
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(discoverableIntent);
        }
    }
    //发信息函数
    private void sendMessage(String message){
        //
        if(mChatService.getState() != ChatService.STATE_CONNECTED){
            Toast.makeText(this,R.string.not_connected,Toast.LENGTH_SHORT).show();
            return;
        }

        if(message.length() > 0){
            byte[] send=message.getBytes();
            mChatService.write(send);

            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    //绑定插件和适配器
    private void setupChat(){
        mConversationArrayAdapter=new ArrayAdapter<String>(this, R.layout.list_item);
        mConversationView=(ListView)findViewById(R.id.list_conversation);
        mConversationView.setAdapter(mConversationArrayAdapter);

        mOutEditText=(EditText)findViewById(R.id.edit_text_out);
        mSendButton = (Button)findViewById(R.id.button_send);
        mChatService = new ChatService(this,mHandler);
        mOutStringBuffer=new StringBuffer("");
    }

    private final Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case MESSAGE_STATE_CHANGE://状态的改变
                    switch (msg.arg1){
                        case ChatService.STATE_CONNECTED://已经连接
                            mConversationArrayAdapter.clear();
                            break;
                        case ChatService.STATE_CONNECTING://正在连接
                            break;
                        case ChatService.STATE_LISTEN://监听
                        case ChatService.STATE_NONE://没有连接
                            break;
                    }break;
                case MESSAGE_WRITE://消息的写入
                    byte[]writeBuf =(byte[])msg.obj;
                    String writeMessage=new String(writeBuf);
                    mConversationArrayAdapter.add("我： " + writeMessage);
                    break;
                case MESSAGE_READ://消息的读
                    byte[]readBuf =(byte[])msg.obj;
                    String readMessage=new String(readBuf,0,msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName+": "+readMessage);
                    break;
                case MESSAGE_DEVICE_NAME://连接的名字
                    mConnectedDeviceName=msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "链接到"+mConnectedDeviceName,Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST://弹出消息
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    //requesstCode请求码、resultCode结果码:实现从一个界面到另一个界面的的跳转结果返回
    public void onActivityResult(int requesstCode, int resultCode, Intent data) {
        super.onActivityResult(requesstCode, resultCode, data);
        switch (requesstCode) {
            case REQUEST_CONNECT_DEVICE: //DeviceList的返回结果
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(DeviceList.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT://打开蓝牙返回的结果
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Toast.makeText(this, "bt_not_enable_leaving", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }
    //点击发送按钮发送内容
    public void send(View view) {
        String message = mOutEditText.getText().toString();
        sendMessage(message);
    }
}