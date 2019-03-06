package com.example.xbj.login.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.example.xbj.login.Location;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import java.util.Calendar;

public class LocationService extends Service {

    private static final String TAG = "LocationService";


    private double longi=1.11;
    private double lati=1.11;

    private String username = Location.username;
    //声明AMapLocationClient类对象
    AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "start LocationService!");

        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //获取一次定位结果：
        //该方法默认为false。
        mLocationOption.setOnceLocation(false);
        mLocationOption.setInterval(15000);
        mLocationOption.setOnceLocationLatest(true);

        getLock(this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "StartCommand LocationService!");

        String CHANNEL_ID = "Location";
        String CHANNEL_NAME="CHA_Location";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(getApplicationContext(),CHANNEL_ID).build();
            startForeground(1, notification);

        }
        else{
            startForeground(1,new Notification());
        }

        getPosition();
        netThread.start();
        return START_STICKY;

    }

    Handler netHandler = null;

    /**
     * 收发网络数据的线程
     */
    Thread netThread = new Thread(){
        @Override
        public void run() {
            Looper.prepare();
            netHandler = new Handler(){
                public void dispatchMessage(Message msg) {
                    switch(msg.what){
                        case 0x1: //发送位置
                            sendLocation();
                            break;

                    }
                };
            };
            Looper.loop();
        }
    };


    //向服务器发送位置数据
    private  void sendLocation(){
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost post = null;

        if (longi !=1.11) {
            try {
                httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 2000);
                httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 2000);
                post = new HttpPost("http://119.29.236.77:5000/mainApp/");
                post.setHeader("Content-Type", "application/x-www-form-urlencoded");
                String data;
                data = "username=" + username + "&lati=" + lati + "&longi=" + longi;
                Log.i("data", data);
                post.setEntity(new StringEntity(data, "UTF-8"));
                HttpResponse response = httpClient.execute(post);
                String result = EntityUtils.toString(response.getEntity(), "utf-8");
                Log.i("sendLocation", result);
            } catch (Exception e) {

            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        Intent intent = new Intent("destroy");
        sendBroadcast(intent);

    }

    public void getPosition(){
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    //声明定位回调监听器
    public AMapLocationListener mLocationListener = new AMapLocationListener(){

        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if(amapLocation==null){
                Log.i(TAG, "amapLocation is null!");
                return;
            }
            if(amapLocation.getErrorCode()!=0){
                Log.i(TAG, "amapLocation has exception errorCode:"+amapLocation.getErrorCode());
                return;
            }
            longi = amapLocation.getLongitude();//获取经度
            lati= amapLocation.getLatitude();//获取纬度
            Message msg = new Message();
            msg.what = 0x1;
            netHandler.sendMessage(msg);
        }

    };

    /**
          * 同步方法   得到休眠锁  防止锁屏之后服务被关
          */
    synchronized private void getLock(Context context){
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");

        if(mWakeLock==null){
            PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
            mWakeLock=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,LocationService.class.getName());
            mWakeLock.setReferenceCounted(true);
            Calendar c=Calendar.getInstance();
            c.setTimeInMillis((System.currentTimeMillis()));
            int hour =c.get(Calendar.HOUR_OF_DAY);
            if(hour>=23||hour<=6){
                mWakeLock.acquire(5000);
            }else{
                mWakeLock.acquire(300000);
            }
            }
            Log.v(TAG,"get lock");
    }


}