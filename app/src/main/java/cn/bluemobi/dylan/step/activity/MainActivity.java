package cn.bluemobi.dylan.step.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import cn.bluemobi.dylan.step.R;
import cn.bluemobi.dylan.step.step.config.Constant;
import cn.bluemobi.dylan.step.step.service.StepService;
import cn.bluemobi.dylan.step.step.utils.SharedPreferencesUtils;
import cn.bluemobi.dylan.step.step.utils.StepCountModeDispatcher;
import cn.bluemobi.dylan.step.view.CustomCircleView;

public class MainActivity extends AppCompatActivity implements Handler.Callback, View.OnClickListener {

    //循环取当前时刻的步数中间的间隔时间
    private long TIME_INTERVAL = 500;

    private boolean isBind = false;

    private Messenger mGetReplyMessenger = new Messenger(new Handler(this));

    private Messenger messenger;

    private TextView tv_data;
    private CustomCircleView cc;
    private TextView tv_set;
    private TextView tv_isSupport;
    private Handler delayHandler;

    private void assignViews() {
        tv_data = (TextView) findViewById(R.id.tv_data);
        cc = (CustomCircleView) findViewById(R.id.cc);
        tv_set = (TextView) findViewById(R.id.tv_set);
        tv_isSupport = (TextView) findViewById(R.id.tv_isSupport);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assignViews();
        initData();
        addListener();
    }

    private void initData() {
        cc.setTextSize(50);
        SharedPreferencesUtils   sp = new SharedPreferencesUtils(this);
        String planWalk_QTY = (String) sp.getParam("planWalk_QTY", "7000");
        cc.setCurrentCount(Integer.parseInt(planWalk_QTY), 0);
        if (StepCountModeDispatcher.isSupportStepCountSensor(this)) {
            tv_isSupport.setText("计步中...");
            delayHandler = new Handler(this);
            setupService();
        } else {
            tv_isSupport.setText("该设备不支持计步");
        }
    }


    private void addListener() {
        tv_set.setOnClickListener(this);
        tv_data.setOnClickListener(this);
    }
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Constant.MSG_FROM_SERVER:
                cc.setCurrentCount(10000, msg.getData().getInt("step"));
                delayHandler.sendEmptyMessageDelayed(Constant.REQUEST_SERVER, TIME_INTERVAL);
                break;
            case Constant.REQUEST_SERVER:
                try {
                    Message msg1 = Message.obtain(null, Constant.MSG_FROM_CLIENT);
                    msg1.replyTo = mGetReplyMessenger;
                    messenger.send(msg1);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                break;
        }
        return false;
    }

    /**
     * 开启计步服务
     */
    private void setupService() {
        Intent intent = new Intent(this, StepService.class);
        isBind = bindService(intent, conn, Context.BIND_AUTO_CREATE);
        startService(intent);


    }

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                messenger = new Messenger(service);
                Message msg = Message.obtain(null, Constant.MSG_FROM_CLIENT);
                msg.replyTo = mGetReplyMessenger;
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isBind) {
            this.unbindService(conn);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_set:
                startActivity(new Intent(this,SetPlanActivity.class));
                break;
            case R.id.tv_data:
                startActivity(new Intent(this,HistoryActivity.class));
                break;
        }
    }
}
