package com.example.arduino_car;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //Экземпляры классов наших кнопок
    Button ledOn;
    Button ledOff;
    Button sendCmd;
    EditText command;

    //Сокет, с помощью которого мы будем отправлять данные на Arduino
    BluetoothSocket clientSocket;

    //Эта функция запускается автоматически при запуске приложения
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //"Соединям" вид кнопки в окне приложения с реализацией
        ledOn = findViewById(R.id.button1);
        ledOff = findViewById(R.id.button2);
        sendCmd = findViewById(R.id.button3);
        command = findViewById(R.id.editTextTextPersonName);

        //Добавлем "слушатель нажатий" к кнопке
        ledOn.setOnClickListener(this);
        ledOff.setOnClickListener(this);
        sendCmd.setOnClickListener(this);

        ledOn.setBackgroundColor(Color.GRAY);
        ledOff.setBackgroundColor(Color.GRAY);

        //Включаем bluetooth. Если он уже включен, то ничего не произойдет
        String enableBT = BluetoothAdapter.ACTION_REQUEST_ENABLE;
        startActivityForResult(new Intent(enableBT), 0);

        //Мы хотим использовать тот bluetooth-адаптер, который задается по умолчанию
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

        //Пытаемся проделать эти действия
        try{
            //Устройство с данным адресом - наш Bluetooth Bee
            //Адрес опредеяется следующим образом: установите соединение
            //между ПК и модулем (пин: 1234), а затем посмотрите в настройках
            //соединения адрес модуля. Скорее всего он будет аналогичным.
            BluetoothDevice device = bluetooth.getRemoteDevice("98:D3:A1:FD:45:09"); //todo MAC

            //Инициируем соединение с устройством
            Method m = device.getClass().getMethod(
                    "createRfcommSocket", new Class[] {int.class});

            clientSocket = (BluetoothSocket) m.invoke(device, 1);
            clientSocket.connect();

            //В случае появления любых ошибок, выводим в лог сообщение
        } catch (IOException e) {
            Log.d("BLUETOOTH", e.getMessage(), e);
        } catch (SecurityException e) {
            Log.d("BLUETOOTH", e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            Log.d("BLUETOOTH", e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            Log.d("BLUETOOTH", e.getMessage(), e);
        } catch (IllegalAccessException e) {
            Log.d("BLUETOOTH", e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Log.d("BLUETOOTH", e.getMessage(), e);
        }

        //Выводим сообщение об успешном подключении
        Toast.makeText(getApplicationContext(), "CONNECTED", Toast.LENGTH_LONG).show();
    }

    @Override
    //Как раз эта функция и будет вызываться
    public void onClick(View v) {
        //Пытаемся послать данные
        try {
            //Получаем выходной поток для передачи данных
            OutputStream outStream = clientSocket.getOutputStream();
            outStream.flush();

            int value = 0;


            //В зависимости от того, какая кнопка была нажата,
            //изменяем данные для посылки
            if (v == ledOn) {
                value = 1;
                ledOn.setBackgroundColor(Color.GREEN);
                ledOff.setBackgroundColor(Color.GRAY);
            } else if (v == ledOff) {
                value = 0;
                ledOn.setBackgroundColor(Color.GRAY);
                ledOff.setBackgroundColor(Color.GREEN);
            }

            System.out.println("value before send = " + value);
            //Пишем данные в выходной поток
            outStream.write(value);

        } catch (IOException e) {
            //Если есть ошибки, выводим их в лог
            Log.d("BLUETOOTH", e.getMessage());
        }
    }
}