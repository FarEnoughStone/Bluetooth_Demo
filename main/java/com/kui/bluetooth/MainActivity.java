package com.kui.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button turn,found,send;
    private EditText data,btAddress;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private TextView tv;
    private final UUID MY_UUID = UUID.
            fromString("00001101-0000-1000-8000-00805f9b34fb");//"00001101-0000-1000-8000-00805F9B34FB");// UUID，蓝牙建立链接需要的
    private final String NAME = "Bluetooth_Socket";// 为其链接创建一个名称
    private BluetoothDevice selectDevice;// 选中发送数据的蓝牙设备
    private BluetoothSocket clientSocket;// 获取到选中设备的客户端串口
    private OutputStream os;// 获取到向设备写的输出流
    //服务端利用线程不断接受客户端信息
    private AcceptThread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.tv);
        turn = (Button) findViewById(R.id.turnOnOff);
        found = (Button) findViewById(R.id.found);
        send = (Button) findViewById(R.id.send);
        data = (EditText) findViewById(R.id.data);
        btAddress = (EditText) findViewById(R.id.btAddress);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listener();//点击事件监听
        if (bluetoothAdapter == null) {
            toast("对不起 ，您的机器不具备蓝牙功能");
            return;
        }


        //蓝牙开关相关设备
        IntentFilter bluetoothFilter = new IntentFilter();
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        this.registerReceiver(BluetoothReciever, bluetoothFilter);

        //蓝牙扫描相关设备
        IntentFilter btDiscoveryFilter = new IntentFilter();
        btDiscoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        btDiscoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btDiscoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);
        btDiscoveryFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.registerReceiver(BTDiscoveryReceiver, btDiscoveryFilter);

//        tv.setText(" Name : " + bluetoothAdapter.getName() + " Address : "
//                + bluetoothAdapter.getAddress() + " Scan Mode --" + bluetoothAdapter.getScanMode());

        //打印处当前已经绑定成功的蓝牙设备
        pairedDevices = bluetoothAdapter.getBondedDevices();
        Iterator<BluetoothDevice> iterator  = pairedDevices.iterator();
        tv.setText("已配对蓝牙设备：");
        while(iterator.hasNext())
        {
            BluetoothDevice bd = iterator.next() ;
            tv.setText(tv.getText() + "\n" + " Name : " + bd.getName() + " Address : " + bd.getAddress());
        }
        //判断蓝牙是否开启
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        thread = new AcceptThread();// 实例接收客户端传过来的数据线程
        thread.start();// 线程开始
        //(new AcceptThread()).start();
    }

    //蓝牙开关状态以及扫描状态的广播接收器
    private BroadcastReceiver BluetoothReciever = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // TODO Auto-generated method stub
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {

            } else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction())) {
                int cur_mode_state = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
                int previous_mode_state = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
            }
        }
    };
    //蓝牙扫描时的广播接收器
    private BroadcastReceiver BTDiscoveryReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // TODO Auto-generated method stub
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction()))
            {
                tv.setText(tv.getText() + "\n" + "扫描开始...");
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction()))
            {
                tv.setText(tv.getText() + "\n" + "扫描完成");
            }
            else if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction()))
            {
                tv.setText(tv.getText() + "\n" + "找到设备：");
                BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(btDevice != null)
                    tv.setText(tv.getText() + "Name : " + btDevice.getName() + " Address: " + btDevice.getAddress());
            }
            else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction()))
            {
                toast("蓝牙状态改变");
                //int cur_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                //int previous_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
                //tv.setText(tv.getText() + "\n"+"### cur_bond_state ##" + cur_bond_state + " ~~ previous_bond_state" + previous_bond_state);
            }
        }

    };
    private void listener() {
        turn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isEnabled()) {
                    if (bluetoothAdapter.disable()) {
                        toast("蓝牙已关闭");
                    } else {
                        toast("蓝牙关闭失败");
                    }
                } else {
                    if (bluetoothAdapter.enable()) {
                        toast("蓝牙已开启");
                    } else {
                        toast("蓝牙开启失败");
                    }
                }
            }
        });
        found.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isEnabled()) {
                    if (!bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.startDiscovery();
                        tv.setText(tv.getText() + "\n" + "正在准备扫描...");
                    } else {
                        toast("正在扫描");
                    }
                } else {
                    toast("请打开蓝牙设备");
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!thread.isAlive()) {
                    thread = new AcceptThread();
                    thread.start();
                }
                // 判断当前是否还是正在搜索周边设备，如果是则暂停搜索
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                // 如果选择设备为空则代表还没有选择设备
                if (selectDevice == null) {
                    //通过地址获取到该设备
                    selectDevice = bluetoothAdapter.getRemoteDevice(btAddress.getText().toString().trim());
                }
                try {
                    // 获取到客户端接口
                    clientSocket = selectDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                    /**当Android版本低于2.3时使用下方代码**/
                    // clientSocket = selectDevice.createRfcommSocketToServiceRecord(MY_UUID);

                    clientSocket.connect();// 向服务端发送连接
                    os = clientSocket.getOutputStream();// 获取到输出流，向外写数据
                    if (os != null) {// 判断是否拿到输出流
                        // 需要发送的信息
                        String text = data.getText().toString();
                        // 以utf-8的格式发送出去
                        os.write(text.getBytes("utf-8"));
                        toast("发送信息成功，请查收");
                        os.close();
                    } else {
                        toast("输出流为空");
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    // 如果发生异常则告诉用户发送失败
                    toast("io异常");
                }
            }
        });
    }
    private void toast(String str) {
        (Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT)).show();
    }


    // 创建handler，因为我们接收是采用线程来接收的，在线程中无法操作UI，所以需要handler
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            tv.setText(tv.getText() + "\n"+"收到消息：" + msg.obj.toString());
            super.handleMessage(msg);
        }
    };
    // 服务端接收信息线程
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;// 服务端接口
        private BluetoothSocket socket;// 获取到客户端的接口
        private InputStream is;// 获取到输入流
        private OutputStream os;// 获取到输出流

        AcceptThread() {
            try {
                // 通过UUID监听请求，然后获取到对应的服务端接口
                serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
                /**当Android版本低于2.3时使用下方代码**/
                //serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                while (true) {
                    socket = serverSocket.accept();
                    is = socket.getInputStream();
                    os = socket.getOutputStream();//必须要有这个

                    byte[] buffer = new byte[102400];//笨方法
                    int count = is.read(buffer);
                    //while (-1 != (count = is.read(buffer))) {
                        Message msg = new Message();//一个message只能发送一个消息
                        msg.obj = new String(buffer, 0, count, "utf-8");
                        handler.sendMessage(msg);
                   // }
                    os.close();//这一句代码是个坑，在上面耗费时间2天
                }
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
                Message msg = new Message();
                msg.obj = "服务端异常";
                handler.sendMessage(msg);
            }
        }
    }
}
