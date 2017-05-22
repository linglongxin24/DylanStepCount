package com.personal_dev.finnfu.stepcount_demo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity {
    private TextView tv_step;
    private Button btn_reset;
    private StepService mService;
    private boolean mIsRunning;
    private SharedPreferences mySharedPreferences;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1){
                tv_step.setText(mySharedPreferences.getString("steps","0"));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_step = (TextView) findViewById(R.id.step_tv);
        btn_reset = (Button) findViewById(R.id.reset_btn);
        mySharedPreferences = getSharedPreferences("relevant_data",Activity.MODE_PRIVATE);
        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.resetValues();
                tv_step.setText(mySharedPreferences.getString("steps", "0"));
            }
        });
        startStepService();
    }

    protected void onDestroy() {
        super.onDestroy();
//        stopStepService();
    }

    protected void onPause() {
        unbindStepService();
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        tv_step.setText(mySharedPreferences.getString("steps", "0"));
        if (this.mIsRunning){
            bindStepService();
        }
    }



    private UpdateUiCallBack mUiCallback = new UpdateUiCallBack() {
        @Override
        public void updateUi() {
            Message message = mHandler.obtainMessage();
            message.what = 1;
            mHandler.sendMessage(message);
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepService.StepBinder binder = (StepService.StepBinder) service;
            mService = binder.getService();
            mService.registerCallback(mUiCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void bindStepService() {
        bindService(new Intent(this, StepService.class), this.mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindStepService() {
        unbindService(this.mConnection);
    }

    private void startStepService() {
        this.mIsRunning = true;
        startService(new Intent(this, StepService.class));
    }

    private void stopStepService() {
        this.mIsRunning = false;
        if (this.mService != null)
            stopService(new Intent(this, StepService.class));
    }

}
