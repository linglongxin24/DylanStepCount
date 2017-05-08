package cn.bluemobi.dylan.step.step.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.bluemobi.dylan.step.R;
import cn.bluemobi.dylan.step.activity.MainActivity;
import cn.bluemobi.dylan.step.step.config.Constant;
import cn.bluemobi.dylan.step.step.bean.StepData;
import cn.bluemobi.dylan.step.step.utils.DbUtils;

public class StepService extends Service implements SensorEventListener {
    private String TAG = "StepService";
    //默认为30秒进行一次存储
    private static int duration = 30000;
    private static String CURRENTDATE = "";
    private SensorManager sensorManager;
    //    private StepDcretor stepDetector;
    private NotificationManager nm;
    private NotificationCompat.Builder builder;
    private Messenger messenger = new Messenger(new MessenerHandler());
    private BroadcastReceiver mBatInfoReceiver;
    private WakeLock mWakeLock;
    private TimeCount time;
    private int CURRENT_SETP;
    //计步传感器类型 0-counter 1-detector
    private static int stepSensor = -1;
    private boolean hasRecord = false;
    private int hasStepCount = 0;
    private int prviousStepCount = 0;

    private class MessenerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constant.MSG_FROM_CLIENT:
                    try {
                        Messenger messenger = msg.replyTo;
                        Message replyMsg = Message.obtain(null, Constant.MSG_FROM_SERVER);
                        Bundle bundle = new Bundle();
                        bundle.putInt("step", CURRENT_SETP);
                        replyMsg.setData(bundle);
                        messenger.send(replyMsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initBroadcastReceiver();
        new Thread(new Runnable() {
            public void run() {
                startStepDetector();
            }
        }).start();

        startTimeCount();
        initTodayData();

    }

    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    /**
     * 初始化当天的步数
     */
    private void initTodayData() {
        CURRENTDATE = getTodayDate();
        DbUtils.createDb(this, "jingzhi");
        DbUtils.getLiteOrm().setDebugged(false);
        //获取当天的数据，用于展示
        List<StepData> list = DbUtils.getQueryByWhere(StepData.class, "today", new String[]{CURRENTDATE});
        if (list.size() == 0 || list.isEmpty()) {
            CURRENT_SETP = 0;
        } else if (list.size() == 1) {
            Log.v(TAG, "StepData=" + list.get(0).toString());
            CURRENT_SETP = Integer.parseInt(list.get(0).getStep());
        } else {
            Log.v(TAG, "出错了！");
        }
//        updateNotification("今日步数：" + StepDcretor.CURRENT_SETP + " 步");
    }

    /**
     * 注册广播
     */
    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        // 屏幕灭屏广播
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //关机广播
        filter.addAction(Intent.ACTION_SHUTDOWN);
        // 屏幕亮屏广播
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // 屏幕解锁广播
//        filter.addAction(Intent.ACTION_USER_PRESENT);
        // 当长按电源键弹出“关机”对话或者锁屏时系统会发出这个广播
        // example：有时候会用到系统对话框，权限可能很高，会覆盖在锁屏界面或者“关机”对话框之上，
        // 所以监听这个广播，当收到时就隐藏自己的对话，如点击pad右下角部分弹出的对话框
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //监听日期变化
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);

        mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    Log.d(TAG, "screen on");
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    Log.d(TAG, "screen off");
                    //改为60秒一存储
                    duration = 60000;
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    Log.d(TAG, "screen unlock");
//                    save();
                    //改为30秒一存储
                    duration = 30000;
                } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                    Log.i(TAG, " receive Intent.ACTION_CLOSE_SYSTEM_DIALOGS");
                    //保存一次
                    save();
                } else if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
                    Log.i(TAG, " receive ACTION_SHUTDOWN");
                    save();
                } else if (Intent.ACTION_DATE_CHANGED.equals(action)) {//日期变化步数重置为0
//                    Logger.d("重置步数" + StepDcretor.CURRENT_SETP);
                    save();
                    isNewDay();
                } else if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                    //时间变化步数重置为0
                    isCall();
                    save();
                    isNewDay();
                } else if (Intent.ACTION_TIME_TICK.equals(action)) {//日期变化步数重置为0
                    isCall();
//                    Logger.d("重置步数" + StepDcretor.CURRENT_SETP);
                    save();
                    isNewDay();
                }
            }
        };
        registerReceiver(mBatInfoReceiver, filter);
    }


    /**
     * 监听晚上0点变化初始化数据
     */
    private void isNewDay() {
        String time = "00:00";
        if (time.equals(new SimpleDateFormat("HH:mm").format(new Date())) || !CURRENTDATE.equals(getTodayDate())) {
            initTodayData();
        }
    }


    /**
     * 监听时间变化提醒用户锻炼
     */
    private void isCall() {
        String time = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("achieveTime", "21:00");
        String plan = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("planWalk_QTY", "7000");
        String remind = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("remind", "1");
        Logger.d("time=" + time + "\n" +
                "new SimpleDateFormat(\"HH: mm\").format(new Date()))=" + new SimpleDateFormat("HH:mm").format(new Date()));
        if (("1".equals(remind)) &&
                (CURRENT_SETP < Integer.parseInt(plan)) &&
                (time.equals(new SimpleDateFormat("HH:mm").format(new Date())))
                ) {
//            workPlanNotification();
            initNotify();
        }

    }

    private void startTimeCount() {
        time = new TimeCount(duration, 1000);
        time.start();
    }

    /**
     * 更新通知
     */
    private void updateNotification(String content) {
        builder = new NotificationCompat.Builder(this);
        builder.setPriority(Notification.PRIORITY_MIN);

        //Notification.Builder builder = new Notification.Builder(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setTicker("Dylan计步");
        builder.setContentTitle("Dylan计步");
        //设置不可清除
        builder.setOngoing(true);
        builder.setContentText(content);
        Notification notification = builder.build();

        startForeground(0, notification);

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(R.string.app_name, notification);
    }

    /**
     * 通知锻炼
     */
    private void workPlanNotification() {
        builder = new NotificationCompat.Builder(this);
        builder.setPriority(Notification.PRIORITY_MIN);

        //Notification.Builder builder = new Notification.Builder(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setTicker("Dylan计步提醒您开始锻炼了");
        builder.setContentTitle("今日步数" + CURRENT_SETP + " 步");
        //设置不可清除
        builder.setOngoing(false);
//        builder.setContentText("距离目标还差" + (Integer.valueOf(LoginUser.getLoginUser().getPlanWalk_QTY()) - CURRENT_SETP) + "步，加油！");
        builder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND);
        builder.setAutoCancel(true);
        Notification notification = builder.build();

//        startForeground(0, notification);
        /**
         * 震动
         */

//        notification.defaults |= Notification.DEFAULT_VIBRATE;
//        long[] vibrate = {0, 100, 200, 300};
//        notification.vibrate = vibrate;

        /**
         * 铃声
         */
//        notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // 系统默认铃声
//        notification.defaults=Notification.DEFAULT_SOUND;
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(100, notification);
    }

    /**
     * Notification构造器
     */
    android.support.v4.app.NotificationCompat.Builder mBuilder;
    /**
     * Notification的ID
     */
    int notifyId = 100;

    /**
     * 初始化通知栏
     */
    private void initNotify() {

        String plan = this.getSharedPreferences("share_date", Context.MODE_MULTI_PROCESS).getString("planWalk_QTY", "7000");
        mBuilder = new android.support.v4.app.NotificationCompat.Builder(this);
        mBuilder.setContentTitle("今日步数" + CURRENT_SETP + " 步")
                .setContentText("距离目标还差" + (Integer.valueOf(plan) - CURRENT_SETP) + "步，加油！")
                .setContentIntent(getDefalutIntent(Notification.FLAG_AUTO_CANCEL))
                .setTicker("Dylan计步提醒您开始锻炼了")//通知首次出现在通知栏，带上升动画效果的
                .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示
                .setPriority(Notification.PRIORITY_DEFAULT)//设置该通知优先级
                .setAutoCancel(true)//设置这个标志当用户单击面板就可以让通知将自动取消
                .setOngoing(false)//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)//向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合：
                //Notification.DEFAULT_ALL  Notification.DEFAULT_SOUND 添加声音 // requires VIBRATE permission
                .setSmallIcon(R.mipmap.logo);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifyId, mBuilder.build());
    }

    /**
     * @获取默认的pendingIntent,为了防止2.3及以下版本报错
     * @flags属性: 在顶部常驻:Notification.FLAG_ONGOING_EVENT
     * 点击去除： Notification.FLAG_AUTO_CANCEL
     */
    public PendingIntent getDefalutIntent(int flags) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, new Intent(), flags);
        return pendingIntent;
    }

    @Override
    public IBinder onBind(Intent intent) {

        return messenger.getBinder();
    }


    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * 获取传感器实例
     */
    private void startStepDetector() {
        if (sensorManager != null) {
            sensorManager = null;
        }
//        getLock(this);
        // 获取传感器管理器的实例
        sensorManager = (SensorManager) this
                .getSystemService(SENSOR_SERVICE);
        //android4.4以后可以使用计步传感器
        int VERSION_CODES = Build.VERSION.SDK_INT;
        if (VERSION_CODES >= 19) {
            addCountStepListener();
        } else {
//            addBasePedoListener();
        }
    }

    /**
     * 添加传感器监听
     * 1. TYPE_STEP_COUNTER API的解释说返回从开机被激活后统计的步数，当重启手机后该数据归零，
     * 该传感器是一个硬件传感器所以它是低功耗的。
     * 为了能持续的计步，请不要反注册事件，就算手机处于休眠状态它依然会计步。
     * 当激活的时候依然会上报步数。该sensor适合在长时间的计步需求。
     * <p>
     * 2.TYPE_STEP_DETECTOR翻译过来就是走路检测，
     * API文档也确实是这样说的，该sensor只用来监监测走步，每次返回数字1.0。
     * 如果需要长事件的计步请使用TYPE_STEP_COUNTER。
     */
    private void addCountStepListener() {
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (countSensor != null) {
            stepSensor = 0;
            Log.v(TAG, "countSensor");
            sensorManager.registerListener(StepService.this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else if (detectorSensor != null) {
            stepSensor = 1;
            Log.v(TAG, "detectorSensor");
            sensorManager.registerListener(StepService.this, detectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.v(TAG, "Count sensor not available!");
//            addBasePedoListener();
        }
    }

//    private void addBasePedoListener() {
//        stepDetector = new StepDcretor(this);
//        // 获得传感器的类型，这里获得的类型是加速度传感器
//        // 此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
//        Sensor sensor = sensorManager
//                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        // sensorManager.unregisterListener(stepDetector);
//        sensorManager.registerListener(stepDetector, sensor,
//                SensorManager.SENSOR_DELAY_UI);
//        stepDetector
//                .setOnSensorChangeListener(new StepDcretor.OnSensorChangeListener() {
//
//                    @Override
//                    public void onChange() {
////                        updateNotification("今日步数：" + StepDcretor.CURRENT_SETP + " 步");
//                    }
//                });
//    }

    /**
     * 传感器监听回调
     *
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        //   i++;
        if (stepSensor == 0) {
            int tempStep = (int) event.values[0];
            if (!hasRecord) {
                hasRecord = true;
                hasStepCount = tempStep;
            } else {
                int thisStepCount = tempStep - hasStepCount;
                CURRENT_SETP += (thisStepCount - prviousStepCount);
                prviousStepCount = thisStepCount;
//                StepDcretor.CURRENT_SETP++;

            }
            Logger.d("tempStep" + tempStep);
        } else if (stepSensor == 1) {
            if (event.values[0] == 1.0) {
                CURRENT_SETP++;
            }

        }
//        updateNotification("今日步数：" + StepDcretor.CURRENT_SETP + " 步");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // 如果计时器正常结束，则开始计步
            time.cancel();
            save();
            startTimeCount();
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

    }

    private void save() {
        int tempStep = CURRENT_SETP;

        List<StepData> list = DbUtils.getQueryByWhere(StepData.class, "today", new String[]{CURRENTDATE});
        if (list.size() == 0 || list.isEmpty()) {
            StepData data = new StepData();
            data.setToday(CURRENTDATE);
            data.setStep(tempStep + "");
            DbUtils.insert(data);
        } else if (list.size() == 1) {
            StepData data = list.get(0);
            data.setStep(tempStep + "");
            DbUtils.update(data);
        } else {
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //取消前台进程
        stopForeground(true);
        DbUtils.closeDb();
        unregisterReceiver(mBatInfoReceiver);
//        Intent intent = new Intent(this, StepService.class);
//        startService(intent);
        Logger.d("stepService关闭");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

//    private  void unlock(){
//        setLockPatternEnabled(android.provider.Settings.Secure.LOCK_PATTERN_ENABLED,false);
//    }
//
//    private void setLockPatternEnabled(String systemSettingKey, boolean enabled) {
//        //推荐使用
//        android.provider.Settings.Secure.putInt(getContentResolver(), systemSettingKey,enabled ? 1 : 0);
//    }

//    synchronized private WakeLock getLock(Context context) {
//        if (mWakeLock != null) {
//            if (mWakeLock.isHeld())
//                mWakeLock.release();
//            mWakeLock = null;
//        }
//
//        if (mWakeLock == null) {
//            PowerManager mgr = (PowerManager) context
//                    .getSystemService(Context.POWER_SERVICE);
//            mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
//                    StepService.class.getName());
//            mWakeLock.setReferenceCounted(true);
//            Calendar c = Calendar.getInstance();
//            c.setTimeInMillis(System.currentTimeMillis());
//            int hour = c.get(Calendar.HOUR_OF_DAY);
//            if (hour >= 23 || hour <= 6) {
//                mWakeLock.acquire(5000);
//            } else {
//                mWakeLock.acquire(300000);
//            }
//        }
//
//        return (mWakeLock);
//    }
}
