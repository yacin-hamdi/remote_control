package com.m01.remote_control;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by yacin socketSearch 5/14/2020.
 */

public class SendReceive extends Thread {
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private MainActivity main;

    public SendReceive(MainActivity main, BluetoothSocket socket){
        this.socket = socket;
        this.main = main;

        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        inputStream = tmpIn;
        outputStream = tmpOut;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        while(true){
            try {
                bytes = inputStream.read(buffer);
                main.handlers.obtainMessage(1,bytes,-1,buffer).sendToTarget();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] bytes){
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel(){
        inputStream = null;
        outputStream = null;
    }
}
