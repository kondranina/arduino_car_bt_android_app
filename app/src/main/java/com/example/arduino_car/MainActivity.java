package com.example.arduino_car;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static com.example.arduino_car.CommandTypeEnum.LED_OFF;
import static com.example.arduino_car.CommandTypeEnum.LED_ON;


public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.class.getSimpleName();

    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names по докам любой инт больше 0
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    //GUI elements
    Button ledOn, ledOff, sendCmd, bt_on, bt_off, connect, disconnect;
    EditText command;
    TextView btStatus, mReadBuffer;

    private BluetoothAdapter bluetoothAdapter;

    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private Thread connectStartThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initGUI();

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    mReadBuffer.setText(""); //почистим от предыдущих сообщений
                    mReadBuffer.setText(readMessage);
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        btStatus.setText("Connected to Device: " + msg.obj);
                    else
                        btStatus.setText("Connection Failed");
                }
            }
        };


        ledOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write(LED_ON.getValue());
            }
        });
        ledOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write(LED_OFF.getValue());
            }
        });
        sendCmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write(command.getText().toString());
            }
        });
        bt_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOn();
            }
        });
        bt_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOff();
            }
        });
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                    return;
                }
                btStatus.setText("Connecting...");
                connectToArduino();
            }
        });
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectArduino();
            }
        });

    }

    private void disconnectArduino() {
        try {
            if(mBTSocket != null) {
                mBTSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(getBaseContext(), "Disconnected", Toast.LENGTH_SHORT).show();
        updateBtStatusField();
    }

    private void connectToArduino() {
        connectStartThread = new Thread() {
            @Override
            public void run() {
                boolean fail = false;

                //todo пока втупую вот так, потом добавить выбор устройств
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice("98:D3:A1:FD:45:09");

                try {
                    Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
                    mBTSocket = (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    fail = true;
                    Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                }
                // Establish the Bluetooth socket connection.
                try {
                    mBTSocket.connect();
                } catch (IOException e) {
                    try {
                        fail = true;
                        mBTSocket.close();
                        mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget();
                    } catch (IOException e2) {
                        //insert code to deal with this
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                }
                if (!fail) {
                    mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
                    mConnectedThread.start();

                    mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, device.getName())
                            .sendToTarget();
                }
            }
        };
        connectStartThread.start();
    }

    private void initGUI() {
        ledOn = findViewById(R.id.button1);
        ledOff = findViewById(R.id.button2);
        sendCmd = findViewById(R.id.button3);
        command = findViewById(R.id.editTextTextPersonName);
        btStatus = findViewById(R.id.btStatus);
        bt_on = findViewById(R.id.btn_BT_on);
        bt_off = findViewById(R.id.btn_BT_off);
        connect = findViewById(R.id.btn_connect_on);
        disconnect = findViewById(R.id.btn_connect_off);
        mReadBuffer = findViewById(R.id.receivedTxt);
        updateBtStatusField();
    }

    private void bluetoothOff() {
        disconnectArduino();
        bluetoothAdapter.disable();
        btStatus.setText("Bluetooth disabled");
    }

    private void bluetoothOn() {
        //если блютус выключен - выведем диалог включения (обработчик связан по REQUEST_ENABLE_BT)
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
        btStatus.setText("Bluetooth enabled");
    }

    private void updateBtStatusField() {
        if(bluetoothAdapter.isEnabled()) {
            btStatus.setText("Bluetooth enabled");
        } else {
            btStatus.setText("Bluetooth disabled");
        }
    }

    /**
     * вызовется автоматом, когда пользователь нажмёт да или нет
     * на окошке включения bluetooth
     * @param requestCode по этому параметру поймём от какого события прилетело
     * @param resultCode результат действий пользователя
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                btStatus.setText("Bluetooth enabled");
            }
            if (resultCode == RESULT_CANCELED) {
                btStatus.setText("Bluetooth disabled");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }
}