package com.example.xbj.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
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


public class Loading extends Activity{

    private String result  = null;

    private Handler mhandler = new Handler(){
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    Toast.makeText(getApplicationContext(),"网络连接成功",Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(getApplicationContext(),"网络连接失败",Toast.LENGTH_SHORT).show();


            }
        }
    };

    private Thread t1 = new Thread(){
        @Override
        public void run(){
            HttpClient client = new DefaultHttpClient();
            HttpGet httpget = new HttpGet("http://119.29.236.77:5000/getlocation/");
            Message msg = new Message();
            try {
                HttpResponse httpresponse = client.execute(httpget);
                int code = httpresponse.getStatusLine().getStatusCode();
                Log.i("code",code+"");
                if(code==200) {
                    result = "success";
                    msg.what=1;
                    mhandler.sendMessage(msg);
                    Thread.sleep(1000);
                    Loading.this.finish();
                    Intent intent = new Intent(Loading.this, LoginActivity.class);
                    startActivity(intent);
            }
            }catch (Exception e){
                msg.what=2;
                mhandler.sendMessage(msg);
                try{
                    Thread.sleep(2000);
                }catch (Exception e1){};
                System.exit(0);
            }

        }
    };
    @Override

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isTaskRoot()) {
            finish();
            return;
        }
        setContentView(R.layout.loading);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        t1.start();

    }
}