package com.m01.remote_control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public boolean restart =true;
    private boolean micBool=false;
    public boolean isConnected = false;
    public boolean asServer = false;
    public boolean incBool = false;
    public boolean decBool = false;

    public final String RIGHT = "droite";
    public final String LEFT = "gauche";
    public final String FORWARD = "avant";
    public final String BACKWARD = "arrière";
    public final String STOP = "stop";
    public final String INCREASE = "rapide";
    public final String DECREASE = "lentement";

    public String command ="";
    public String MSG = "DESCONNECTED";


    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;


    Animation fadeIn;
    Animation fadeOut;

    ImageView forwardImg;
    ImageView backwardImg;
    ImageView leftImg;
    ImageView rightImg;
    ImageView stopImg;
    ImageView onOffImg;
    ImageView buzzImg;
    ImageView incSpeedImg;
    ImageView decSpeedImg;
    ImageView micImg;



    TextView text;
    TextView receive;



    Status status = Status.STOP;

    int speed = 1;
    int speechRepeatCount=0;




    BluetoothAdapter myBluetoothAdapter = null;
    BluetoothSocket socket = null;
    BluetoothDevice device =null;
    BluetoothAsClient bluetoothAsClient;
    BluetoothAsServer bluetoothAsServer;
    public final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Runnable visualThread;
    private Runnable speechThread;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewByids();
        text.setText(MSG);

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBluetooth();


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Need to speak");
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());

        handler = new Handler();
        visualThread = new Runnable() {
            @Override
            public void run() {
                if(status == Status.STOP&&fadeOut!=null){
                    if(fadeOut.hasEnded()){
                        stopImg.setImageDrawable(null);
                    }
                }
                if(incBool&&fadeIn!=null){
                    if(fadeIn.hasEnded()){
                        incSpeedImg.setColorFilter(Color.parseColor("#a8b5af"));
                        incBool = false;
                    }
                }
                if(decBool&&fadeIn!=null){
                    if(fadeIn.hasEnded()){
                        decSpeedImg.setColorFilter(Color.parseColor("#a8b5af"));
                        decBool = false;
                    }
                }
                if (MSG.equals("CLOSE")) {
                    if(asServer)
                        bluetoothAsServer.cancel();
                    else
                        bluetoothAsClient.cancel();
                    MSG = "DECONNECTED";
                    status = Status.STOP;
                    buttonImg();
                    handler.removeCallbacks(speechThread);
                    micImg.setColorFilter(Color.parseColor("#a8b5af"));
                    speechRepeatCount = 0;
                    micBool = false;
                    speed = 1;

                }
                if(isConnected){
                    onOffImg.setColorFilter(Color.parseColor("#00adac"));
                }else{
                    onOffImg.setColorFilter(Color.parseColor("#a8b5af"));
                }
                text.setText(MSG);

                handler.postDelayed(this,100);
            }
        };
        handler.postDelayed(visualThread,100);

        speechThread = new Runnable() {
            @Override
            public void run() {
                speechRecognizer.startListening(speechRecognizerIntent);
                handler.postDelayed(this,500);
            }
        };





        //************************* TURN ON/OFF
        onOffImg.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(myBluetoothAdapter.isEnabled()){
                    if(!isConnected&&restart){
                        restart = false;
                        if(asServer)
                            connectAsServer();
                        else
                            connectAsClient();

                        fadeInView(onOffImg);
                    }else if(isConnected){
                        if(asServer){
                            bluetoothAsServer.cancel();
                        }else{
                            bluetoothAsClient.cancel();
                        }
                        MSG = "CLOSE";
                        fadeInView(onOffImg);

                    }
                }else{
                    Intent turnBlueOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnBlueOn,1);
                    fadeInView(onOffImg);
                }







            }
        });


        //************************* move FORWARD
        clickListener(Status.FORWARD,forwardImg,FORWARD);

        //************************* move BACKWARD
       clickListener(Status.BACKWARD,backwardImg,BACKWARD);

        //************************* move LEFT
        clickListener(Status.LEFT,leftImg,LEFT);

        //************************* move RIGHT
        clickListener(Status.RIGHT,rightImg,RIGHT);

        //************************* STOP
        clickListener(Status.STOP,stopImg,STOP);

        //************************* ACTIVATE MICROPHONE
        micImg.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(!micBool && isConnected){
                    micBool=true;
                    micImg.setColorFilter(Color.parseColor("#00adac"));
                    handler.postDelayed(speechThread, 2000);
                }else{
                    micImg.setColorFilter(Color.parseColor("#a8b5af"));
                    micBool=false;
                    handler.removeCallbacks(speechThread);
                }
                fadeInView(micImg);



            }
        });

        //************************* INCREASE SPEED
        incSpeedImg.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(isConnected){
                    increaseButton();
                    sendMsg(speed+"");
                }


            }
        });



        //************************* DECREASE SPEED

        decSpeedImg.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(isConnected){
                   decreaseButton();
                    sendMsg(speed+"");

                }



            }
        });


        //************************* buzz Button
        buzzImg.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(asServer){
                    asServer = false;
                    buzzImg.setColorFilter(Color.parseColor("#a8b5af"));
                }else{
                    asServer = true;
                    buzzImg.setColorFilter(Color.parseColor("#00adac"));
                }
            }
        });


        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                handler.removeCallbacks(speechThread);
            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                if(speechRepeatCount == 0){
                    speechRepeatCount =1;
                }else if(speechRepeatCount == 1){
                    speechRepeatCount = 2;
                }else if(speechRepeatCount == 2){
                    speechRepeatCount = 0;
                    micBool = false;
                }
                if(micBool){
                    handler.removeCallbacks(speechThread);
                    handler.postDelayed(speechThread,2000);
                }else{
                    handler.removeCallbacks(speechThread);
                    micImg.setColorFilter(Color.parseColor("#a8b5af"));
                }


            }

            @Override
            public void onResults(Bundle results) {
                speechRepeatCount=0;
                ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);



                if(micBool){
                    for (String word :result){
                        if(word.equals(STOP)){
                            command = STOP;
                            break;
                        }else if(word.equals(FORWARD)){
                            command =FORWARD;
                            break;
                        }else if(word.equals(BACKWARD)){
                            command = BACKWARD;
                            break;
                        }else if(word.equals(RIGHT)){
                            command = RIGHT;
                            break;
                        }else if(word.equals(LEFT)){
                            command = LEFT;
                            break;
                        }else if(word.equals(INCREASE)){
                            command = INCREASE;
                            break;
                        }else if(word.equals(DECREASE)){
                            command = DECREASE;
                            break;
                        }else{
                            command ="not found";
                        }
                    }
                    buttonUiUpdate();
                    sendMsg(command);
                    receive.setText(command);
                    handler.removeCallbacks(speechThread);
                    handler.postDelayed(speechThread,2000);
                }else{

                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });



    }

    public void sendMsg(String msg){
        if(asServer){
            bluetoothAsServer.sendReceive.write(msg.getBytes());
        }else{
            bluetoothAsClient.sendReceive.write(msg.getBytes());
        }
    }

    public void clickListener(final Status stat, ImageView view,final String msg){
        view.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(status!= stat && isConnected){
                    status = stat;
                    buttonImg();
                    sendMsg(msg);

                }
            }
        });

    }
    public void toast(String message,int time){
        Toast.makeText(getApplicationContext(),message,time).show();
    }

    public void checkBluetooth(){
        if(myBluetoothAdapter==null){
            toast("BlueTooth device not available",Toast.LENGTH_LONG);
            finish();
        }else{
            if(!myBluetoothAdapter.isEnabled()){
                Intent turnBlueOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBlueOn,1);
            }
        }
    }

    public void connectAsClient(){
        device=pairedDeviceMethod();
        bluetoothAsClient = new BluetoothAsClient(MainActivity.this);
        bluetoothAsClient.start();
    }

    public void connectAsServer(){
        bluetoothAsServer = new BluetoothAsServer(MainActivity.this);
        bluetoothAsServer.start();
    }

    public Handler handlers = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 1:
                    byte[] readBuff =(byte[]) msg.obj;
                    String message = new String(readBuff,0,msg.arg1);
                    if(message.equals("CLOSE")){
                        MSG = message;
                    }else{
                        command = message;
                    }
                    buttonUiUpdate();
                    text.setText(MSG);
                    receive.setText(command);
            }
        }
    };

    public void findViewByids(){
        text = (TextView)findViewById(R.id.text);
        receive = (TextView)findViewById(R.id.receive);
        forwardImg = (ImageView)findViewById(R.id.forward);
        backwardImg = (ImageView)findViewById(R.id.backward);
        leftImg = (ImageView)findViewById(R.id.left);
        rightImg = (ImageView)findViewById(R.id.right);
        stopImg = (ImageView)findViewById(R.id.stop);
        onOffImg = (ImageView)findViewById(R.id.on_off);
        buzzImg = (ImageView)findViewById(R.id.buzz);
        incSpeedImg = (ImageView)findViewById(R.id.inc);
        decSpeedImg = (ImageView)findViewById(R.id.dec);
        micImg = (ImageView)findViewById(R.id.mic_on_off);
    }

    public void buttonImg(){
        if(status== Status.FORWARD){
            forwardImg.setImageResource(R.drawable.arrow_glow);
            fadeInView(forwardImg);
            backwardImg.setImageResource(R.drawable.arrow);
            leftImg.setImageResource(R.drawable.arrow);
            rightImg.setImageResource(R.drawable.arrow);
            stopImg.setImageResource(R.drawable.circle);
            fadeInView(stopImg);
        }else if(status == Status.BACKWARD){
            forwardImg.setImageResource(R.drawable.arrow);
            backwardImg.setImageResource(R.drawable.arrow_glow);
            fadeInView(backwardImg);
            leftImg.setImageResource(R.drawable.arrow);
            rightImg.setImageResource(R.drawable.arrow);
            stopImg.setImageResource(R.drawable.circle);
            fadeInView(stopImg);
        }else if(status == Status.LEFT){
            forwardImg.setImageResource(R.drawable.arrow);
            backwardImg.setImageResource(R.drawable.arrow);
            leftImg.setImageResource(R.drawable.arrow_glow);
            fadeInView(leftImg);
            rightImg.setImageResource(R.drawable.arrow);
            stopImg.setImageResource(R.drawable.circle);
            fadeInView(stopImg);
        }else if(status == Status.RIGHT){
            forwardImg.setImageResource(R.drawable.arrow);
            backwardImg.setImageResource(R.drawable.arrow);
            leftImg.setImageResource(R.drawable.arrow);
            rightImg.setImageResource(R.drawable.arrow_glow);
            fadeInView(rightImg);
            stopImg.setImageResource(R.drawable.circle);
            fadeInView(stopImg);
        }else if(status == Status.STOP){
            forwardImg.setImageResource(R.drawable.arrow);
            backwardImg.setImageResource(R.drawable.arrow);
            leftImg.setImageResource(R.drawable.arrow);
            rightImg.setImageResource(R.drawable.arrow);
            fadeOutView(stopImg);
        }
    }

    public void decreaseButton(){
        if(speed == 2){
            speed = 1;
            fadeInView(decSpeedImg);
            decBool = true;
            decSpeedImg.setColorFilter(Color.parseColor("#00adac"));
        }else if(speed == 1){
            speed = 0;
            decBool = true;
            fadeInView(decSpeedImg);
            decSpeedImg.setColorFilter(Color.parseColor("#00adac"));
        }
    }

    public void increaseButton(){
        if(speed == 0){
            speed = 1;
            fadeInView(incSpeedImg);
            incBool = true;
            incSpeedImg.setColorFilter(Color.parseColor("#00adac"));
        }else if(speed == 1){
            speed = 2;
            fadeInView(incSpeedImg);
            incBool = true;
            incSpeedImg.setColorFilter(Color.parseColor("#00adac"));
        }
    }

    public void buttonUiUpdate(){
        if(command.equals(STOP)){
            status = Status.STOP;
            buttonImg();
        }else if(command.equals(LEFT)){
            status = Status.LEFT;
            buttonImg();
        }else if(command.equals(RIGHT)){
            status = Status.RIGHT;
            buttonImg();
        }else if(command.equals(FORWARD)){
            status = Status.FORWARD;
            buttonImg();
        }else if(command.equals(BACKWARD)){
            status = Status.BACKWARD;
            buttonImg();
        }else if(command.equals(INCREASE)){
            increaseButton();
            command = speed + "";
        }else if(command.equals(DECREASE)){
            decreaseButton();
            command = speed + "";
        }
    }

    public void fadeInView(ImageView view){
        fadeIn = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fadein);
        view.startAnimation(fadeIn);
    }

    public void fadeOutView(ImageView view){
        fadeOut = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fadeout);
        view.startAnimation(fadeOut);
    }

    public BluetoothDevice pairedDeviceMethod(){
        BluetoothDevice mdevice = null;
        Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size()>0){
            for(BluetoothDevice device:pairedDevices){
                mdevice=device;
            }
        }
        return mdevice;

    }


    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
    }


    public enum Status{
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        STOP
    }


}
