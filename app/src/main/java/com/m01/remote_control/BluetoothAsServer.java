package com.m01.remote_control;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;

/**
 * Created by yacin socketSearch 5/14/2020.
 */

public class BluetoothAsServer extends Thread {
    private MainActivity main;
    public SendReceive sendReceive;
    private BluetoothServerSocket bluetoothServerSocket;
    public boolean stop = false;



    public BluetoothAsServer(MainActivity main){
        this.main = main;

        try {
            bluetoothServerSocket = main.myBluetoothAdapter.listenUsingRfcommWithServiceRecord("app",main.uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        if(stop)return;
        BluetoothSocket socket = null;
        while(socket == null){
            main.isConnected = false;
            try {
                main.MSG = "WAITING...";
                socket = bluetoothServerSocket.accept();
                main.MSG = "CONNECTING...";


            } catch (IOException e) {
                main.MSG = "NO DEVICE";
                e.printStackTrace();
                main.MSG = "DESCONNECTED";
                main.restart = true;
            }

            if(socket != null){
                main.MSG = "CONNECTED";
                main.isConnected=true;
                sendReceive = new SendReceive(main,socket);
                sendReceive.start();
                break;
            }
        }
    }

    public void cancel(){
        try {
            if(bluetoothServerSocket!=null)
                sendReceive.write("CLOSE".getBytes());
            main.isConnected=false;
            main.restart = true;
            main.MSG = "DECONNECTED";
            if (bluetoothServerSocket != null) {
                bluetoothServerSocket.close();
            }
            stop = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
