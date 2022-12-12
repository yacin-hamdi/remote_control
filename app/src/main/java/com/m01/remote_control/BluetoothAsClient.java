package com.m01.remote_control;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;

/**
 * Created by yacin socketSearch 5/14/2020.
 */

public class BluetoothAsClient extends Thread {
    private MainActivity main;
    private BluetoothSocket socket=null;
    public SendReceive sendReceive;
    public boolean stop = false;


    public BluetoothAsClient(MainActivity main){
        this.main = main;
        BluetoothSocket tmp = null;
        try {
            tmp = main.device.createRfcommSocketToServiceRecord(main.uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }

        socket = tmp;


    }

    @Override
    public void run() {
        if(stop)return;
        main.myBluetoothAdapter.cancelDiscovery();
        try {
            main.MSG = "LOOKING...";
            socket.connect();
            main.MSG = "CONNECTED";
            main.isConnected = true;
            sendReceive = new SendReceive(main,socket);
            sendReceive.start();
        } catch (IOException e) {

            try {
                socket.close();
                main.MSG = "DESCONNECTED";
                main.isConnected = false;
                main.restart = true;
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }




    }

    public void cancel(){
        try {
            if(main.isConnected)
                sendReceive.write("CLOSE".getBytes());
            main.isConnected = false;
            main.restart = true;
            main.MSG = "DECONNECTED";
            if (socket != null) {
                socket.close();
            }
            stop = true;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
