package com.example.xbj.login;


import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Text;
import com.amap.api.maps.model.TextOptions;
import com.example.xbj.login.util.*;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.params.CoreConnectionPNames;

import com.dou361.dialogui.DialogUIUtils;
import com.dou361.dialogui.adapter.TieAdapter;
import com.dou361.dialogui.bean.BuildBean;
import com.dou361.dialogui.bean.PopuBean;
import com.dou361.dialogui.bean.TieBean;
import com.dou361.dialogui.listener.DialogUIDateTimeSaveListener;
import com.dou361.dialogui.listener.DialogUIItemListener;
import com.dou361.dialogui.listener.DialogUIListener;
import com.dou361.dialogui.listener.TdataListener;
import com.dou361.dialogui.widget.DateSelectorWheelView;

public class Location extends AppCompatActivity  implements AMap.OnMarkerClickListener {
    public static String username ;
    private ArrayList<User> userList = new ArrayList<User>();
    private ArrayList<MarkerOptions> markerOptionlst = new ArrayList<MarkerOptions>();

    private MarkerOptions markerOption;

    private Handler handler = new Handler();
    MapView mMapView = null;
    private AMap aMap;
    private Runnable updateData = new Runnable() {
        @Override
        public void run() {
            getLocation();
            addMarkersToMap();
            handler.postDelayed(this,60000);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty);


        Intent intent = getIntent();
        username = intent.getStringExtra("username");


        setContentView(R.layout.activity_empty);


        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);

        //初始化地图控制器对象
        if (aMap == null) {
            aMap = mMapView.getMap();
        }



        aMap.setOnMarkerClickListener(this);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();

        Log.i("11",username);

        handler.getLooper();
        handler.post(updateData);

        Log.i("size",userList.size()+" ");


        //动态申请权限
        if(ContextCompat.checkSelfPermission(Location.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){//未开启定位权限
            //开启定位权限,200是标识码
            ActivityCompat.requestPermissions(Location.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},200);
        }else{
            Toast.makeText(Location.this,"已开启定位权限",Toast.LENGTH_LONG).show();
        }



        //启动定位服务


        Intent intentLocation = new Intent(Location.this, LocationService.class);

        bindService(intentLocation, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            }
            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        }, Context.BIND_AUTO_CREATE);
        startService(intentLocation);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateData);
        mMapView.onDestroy();
    }
    //点击返回键返回桌面而不是退出程序
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //从服务器获取当前所有用户的位置信息


    private void getLocation(){

        HttpClient client = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://119.29.236.77:5000/getlocation/");
        try {
            HttpResponse httpresponse = client.execute(httpget);
            HttpEntity entity = httpresponse.getEntity();
            String response = EntityUtils.toString(entity, "utf-8");
            praseJson(response);
            Log.i("getLocation",response);

        } catch (Exception e) { }

    }

    //Json数据解析

    private void praseJson(String jsonData){
        try{
            JSONArray arr = new JSONArray(jsonData);

            userList.clear();
            for(int i = 0; i < arr.length() ; i++){
                JSONObject tmp = (JSONObject)arr.get(i);
                String name = tmp.getString("name");
                String username = tmp.getString("username");
                String longi = tmp.getString("longi");
                String lati = tmp.getString("lati");

                if(!username.equals(this.username))
                {
                    try{
                        double lo = Double.parseDouble(longi);
                        double la = Double.parseDouble(lati);

                        User user = new User();
                        user.setLati(la);
                        user.setLongi(lo);
                        user.setName(name);
                        user.setUsername(username);
                        userList.add(user);

                        Log.i("1111111",lo+" "+la+" "+name);
                    }
                    catch (Exception e ){Log.i("error",e.toString());}
                }

            }


        }
        catch (Exception e){}
    }


    @Override
    protected void onResume() {
        super.onResume();
        getLocation();
        addMarkersToMap();
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    //在地图上标明所有用户位置点
    private void addMarkersToMap() {

        aMap.clear();

        markerOptionlst.clear();

        for (User user : userList){

            MarkerOptions marker = new MarkerOptions();
            marker.position(new LatLng(user.getLati(), user.getLongi()));
            marker.title(user.getName()).snippet(user.getUsername());

            marker.draggable(false);
            marker.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                    .decodeResource(getResources(), R.mipmap.mark)));
            marker.setFlat(true);
            markerOptionlst.add(marker);

        }

        aMap.addMarkers(markerOptionlst, true);

    }



    //监听位置标志点击事件

    public boolean onMarkerClick(final Marker marker) {
        if (aMap != null) {

            final String phNum = marker.getSnippet();
            DialogUIUtils.showMdAlert(Location.this, "联系成员", "是否拨打电话给"+marker.getTitle()+"\n"+phNum, new DialogUIListener() {
                @Override
                public void onPositive() {
                    Intent dialIntent =  new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phNum));//跳转到拨号界面，同时传递电话号码
                    startActivity(dialIntent);
                }

                @Override
                public void onNegative() {

            }

            }).show();


        }

        return false;
    }


}
