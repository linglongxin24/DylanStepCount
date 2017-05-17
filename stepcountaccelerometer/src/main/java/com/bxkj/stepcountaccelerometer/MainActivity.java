package com.bxkj.stepcountaccelerometer;

import android.app.Activity;
        import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.telephony.SmsManager;
import android.widget.TextView;

/*
import android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
*/

public class MainActivity extends Activity implements SensorEventListener {

    private float lastX, lastY, lastZ;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float deltaXMax = 0;
    private float deltaYMax = 0;
    private float deltaZMax = 0;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;
    private int countSteps= 0;
    private int stepCountTemp=0;

    private float vibrateThreshold = 0;
    private int threshold = 0;
    private TextView currentX, currentY, currentZ, maxX, maxY, maxZ,numSteps;
    //private Button calculate;
    public Vibrator v;
    boolean showMsg=true;
    //private GoogleApiClient client;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            vibrateThreshold = accelerometer.getMaximumRange() / 2;
        } else {
            // fai! we dont have an accelerometer!
        }

        //initialize vibration
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    public void initializeViews() {
        currentZ = (TextView) findViewById(R.id.currentZ);
        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);

        maxX = (TextView) findViewById(R.id.maxX);
        maxY = (TextView) findViewById(R.id.maxY);
        maxZ = (TextView) findViewById(R.id.maxZ);
        threshold = 2;
        numSteps = (TextView) findViewById(R.id.numSteps);
       // calculate = (Button) findViewById(R.id.calculate);
    }


    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // clean current values
        displayCleanValues();
        // display the current x,y,z accelerometer values
        displayCurrentValues();
        // display the max x,y,z accelerometer values
        displayMaxValues();
        //calculate();

        // get the change of the x,y,z values of the accelerometer
        deltaX = Math.abs(lastX - event.values[0]);
        deltaY = Math.abs(lastY - event.values[1]);
        deltaZ = Math.abs(lastZ - event.values[2]);

        // if the change is below 2, it is just plain noise
        if (deltaX < 2)
            deltaX = 0;
        if (deltaY < 2)
            deltaY = 0;
        if (deltaZ < 2)
            deltaZ = 0;
        if ((deltaX > vibrateThreshold) || (deltaY > vibrateThreshold) || (deltaZ > vibrateThreshold)) {
            v.vibrate(50);
        }
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float g = Math.abs((x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH));

        //deltaY = y;
        //if(Math.abs((x*x + y*y + z*z)) > threshold ){
        if(g > threshold){
           countSteps++;
            //numSteps = countSteps;
        }
        Thread thread=  new Thread(){
            @Override
            public void run(){
                try {
                    synchronized(this){
                        wait(35000);


                    }
                }
                catch(InterruptedException ex){
                }


                // TODO

                if (showMsg) {
                    String messageToSend = "alert! your  step count is " + countSteps;
                    String number = "+12564790693";
                    SmsManager manager = SmsManager.getDefault();
                    manager.sendTextMessage(number, null, messageToSend, null, null);
                    showMsg=false;
                }
            }
        };

        thread.start();

        /*String messageToSend = "alert!!! your phone is being used by someone";
        String number = "+12564790693";
        SmsManager manager = SmsManager.getDefault();
        manager.sendTextMessage(number,null,messageToSend,null,null);*/
    }
    //public void calculate(){

    //}

    public void displayCleanValues() {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");
        numSteps.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
        currentX.setText(Float.toString(deltaX));
        currentY.setText(Float.toString(deltaY));
        currentZ.setText(Float.toString(deltaZ));
        numSteps.setText(Float.toString(countSteps));
    }

    // display the max x,y,z accelerometer values
    public void displayMaxValues() {
        if (deltaX > deltaXMax) {
            deltaXMax = deltaX;
            maxX.setText(Float.toString(deltaXMax));
        }
        if (deltaY > deltaYMax) {
            deltaYMax = deltaY;
            maxY.setText(Float.toString(deltaYMax));
        }
        if (deltaZ > deltaZMax) {
            deltaZMax = deltaZ;
            maxZ.setText(Float.toString(deltaZMax));
        }


    }
    /*if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
        return;
    }

    final float x = deltaXMax;
    final float y = deltaYMax;
    final float z = deltaZMax;
    final float g = Math.abs((x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH));
    if(g > 2){
            /*check step times and other checkings*/
        /*stepCounter++;
    }
    public void sendSMS(String phoneNo, String msg){
 try {
    SmsManager smsManager = SmsManager.getDefault();
    smsManager.sendTextMessage(phoneNo, null, msg, null, null);
    Toast.makeText(getApplicationContext(), "Message Sent",
       Toast.LENGTH_LONG).show();
    } catch (Exception ex) {
         Toast.makeText(getApplicationContext(),ex.getMessage().toString(),
             Toast.LENGTH_LONG).show();
          ex.printStackTrace();
 }
}
    */
}
