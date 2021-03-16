package com.example.arduino_car;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.example.arduino_car.CreateConnectThread.CONNECTING_STATUS;
import static com.example.arduino_car.CreateConnectThread.MESSAGE_READ;

public class MainActivity extends AppCompatActivity {
    public static CreateConnectThread createConnectThread;
    public static ConnectedThread connectedThread;
    Button ledOn, ledOff, sendCmd;
    EditText command;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        ledOn = findViewById(R.id.button1);
        ledOff = findViewById(R.id.button2);
        sendCmd = findViewById(R.id.button3);
        command = findViewById(R.id.editTextTextPersonName);
        ledOn.setBackgroundColor(Color.GRAY);
        ledOff.setBackgroundColor(Color.GRAY);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice("98:D3:A1:FD:45:09");
        if (device != null){
            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
             */
            createConnectThread = new CreateConnectThread(bluetoothAdapter, device.getAddress());
            createConnectThread.start();
        }

        //todo add GUI handler for messages from Arduino

        ledOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = "1";
                ledOn.setBackgroundColor(Color.GREEN);
                ledOff.setBackgroundColor(Color.GRAY);
                connectedThread.write(value); // Send command to Arduino board
            }
        });
        ledOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = "0";
                ledOn.setBackgroundColor(Color.GREEN);
                ledOff.setBackgroundColor(Color.GRAY);
                connectedThread.write(value); // Send command to Arduino board
            }
        });
        sendCmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo test this
                connectedThread.write(command.getText().toString()); // Send command to Arduino board
            }
        });
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}