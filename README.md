# Android精准计步开发
 亲测在小米.魅族.华为上可用
 <div  align="center">    
<img src="screenshots/主页.jpg" width="30%" height="30%"/>
<img src="screenshots/锻炼计划.jpg" width="30%" height="30%"/>
<img src="screenshots/历史记录.jpg" width="30%" height="30%"/>
</div>
#1.需要在AndroidManifest.xml中添加权限
```java
    <!--计歩需要的权限-->
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WRITE_SETTINGS" />
    <uses-feature android:name="android.hardware.sensor.accelerometer" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
    <uses-feature
        android:name="android.hardware.sensor.stepcounter"
        android:required="true" />
    <uses-feature
        android:name="android.hardware.sensor.stepdetector"
        android:required="true" />

```
#2.检测手机是否支持计歩
```java
 /**
     * 判断该设备是否支持计歩
     *
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isSupportStepCountSensor(Context context) {
        // 获取传感器管理器的实例
        SensorManager sensorManager = (SensorManager) context
                .getSystemService(context.SENSOR_SERVICE);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        return countSensor != null || detectorSensor != null;
    }
```
#3.功能使用
```java
/**
     * 开启计步服务
     */
    private void setupService() {
        Intent intent = new Intent(this, StepService.class);
        isBind = bindService(intent, conn, Context.BIND_AUTO_CREATE);
        startService(intent);


    }
```
