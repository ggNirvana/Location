package com.example.xbj.login;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Map;
import java.io.IOException;
import java.net.URLEncoder;
import java.io.ByteArrayOutputStream;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.UrlQuerySanitizer;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.LoginFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.LitePal;
import org.litepal.crud.DataSupport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.READ_CONTACTS;


/**
 * 通过电子邮件/密码提供登录的登录屏幕。
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * ID用来识别READ_CONTACTS权限请求。
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * 包含已知用户名和密码的虚拟身份验证存储。
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * 保持跟踪登录任务，以确保，如果被请求的话，可以取消它
     */

    // UI references.
    private AutoCompleteTextView mAccountView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private String account = null;
    private String password = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if(savedInstanceState!=null)
            finish();

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        LitePal.getDatabase();



        setContentView(R.layout.activity_login);
        // 设置登录表单。



        mAccountView = (AutoCompleteTextView) findViewById(R.id.account);
        populateAutoComplete();
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {//判断软键盘所选择的类
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    Post();
                    return true;
                }
                return false;
            }
        });
        /**
         * 注册按钮
         */

        List<History> his = DataSupport.findAll(History.class);
        try {
            account = his.get(0).getUsername();
            password = his.get(0).getPassword();
            mAccountView.setText(account);
            mPasswordView.setText(password);
            if(account.length()!=0)

            Log.i("acc",account);

        }
        catch (Exception e){

        }


        Button mAccountRegisterButton = (Button) findViewById(R.id.account_register_button);
        mAccountRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });
        /**
         * 登录按钮
         */
        Button mAccountSignInButton = (Button) findViewById(R.id.account_sign_in_button);
        mAccountSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Post();
            }
        });




        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

    }

    /**
     * 构造自动补全的列表
     */
    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    /**
     * mayRequestContacts()
     * 判断是否继续执行，若通过判断则初始化Loaders，通过Loaders后台异步读取用户的账户信息。
     */
    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mAccountView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * 当权限请求完成时收到回调。
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    /**
     * 尝试登录或注册登录表单指定的帐户。
     * 如果存在表单错误(无效账号格式、缺失字段等)，则会出现错误，并且不会进行实际的登录尝试。
     */
    private void attemptLogin() {


    }

    /**
     *判断账号是否有效
     */
    private boolean isAccountValid(String account) {
        //TODO: 在这里判断账号格式逻辑，自定义
        return account.contains("@");
    }

    /**
     *判断密码是否有效
     */
    private boolean isPasswordValid(String password) {
        //TODO: 在这里判断密码格式逻辑，自定义
        return password.length() > 4;
    }

    /**
     * 显示进度UI并隐藏登录表单
     * showProgress函数主要是用户登录验证时界面的显示工作，
     * 界面显示一个等待对话框。在这个函数里主要做了应用程序的API与系统平台的API对比并处理
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        //在蜂窝MR2中，我们有ViewProperty动画API，它允许非常简单的动画。
        // 如果可用，使用这些API来淡出-在进度旋转器中。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Note
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> accounts = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            accounts.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addaccountsToAutoComplete(accounts);
    }

    private void addaccountsToAutoComplete(List<String> accounts) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    /*private void addAccountsToAutoComplete(List<String> accountAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, accountAddressCollection);

        mAccountView.setAdapter(adapter);
    }*/

    /**
     * 没搞明白干啥子的，好像是个API
     */
    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
    }



        public static StringBuffer getRequestData(Map<String, String> params, String encode) {
            StringBuffer stringBuffer = new StringBuffer();        //存储封装好的请求体信息
            try {
                for(Map.Entry<String, String> entry : params.entrySet()) {
                    stringBuffer.append(entry.getKey())
                            .append("=")
                            .append(URLEncoder.encode(entry.getValue(), encode))
                            .append("&");
                }
                stringBuffer.deleteCharAt(stringBuffer.length() - 1);    //删除最后的一个"&"
            } catch (Exception e) {
                e.printStackTrace();
            }
            return stringBuffer;
        }

    protected String Post() {
        Map<String,String> params = new HashMap<String, String>();
        account = mAccountView.getText().toString();
        password = mPasswordView.getText().toString();
        params.put("username", account);
        params.put("password",password);
        byte[] data = getRequestData(params, "UTF-8").toString().getBytes();//获得请求体
        try {

            URL url = new URL("http://119.29.236.77:5000/login/");
            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.setConnectTimeout(3000);     //设置连接超时时间
            httpURLConnection.setDoInput(true);                  //打开输入流，以便从服务器获取数据
            httpURLConnection.setDoOutput(true);                 //打开输出流，以便向服务器提交数据
            httpURLConnection.setRequestMethod("POST");     //设置以Post方式提交数据
            httpURLConnection.setUseCaches(false);               //使用Post方式不能使用缓存

            //设置请求体的类型是文本类型
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            //获得输出流，向服务器写入数据
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(data);

            int response = httpURLConnection.getResponseCode();            //获得服务器的响应码
            if(response == HttpURLConnection.HTTP_OK) {
                InputStream inptStream = httpURLConnection.getInputStream();
                return dealResponseResult(inptStream);                     //处理服务器的响应结果
            }
        } catch (IOException e) {
            //e.printStackTrace();
            Log.i("err:",e.getMessage().toString());
            return "err: " + e.getMessage().toString();
        }
        return "-1";
    }

    public  String dealResponseResult(InputStream inputStream) {
        String resultData = null;      //存储处理结果
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len = 0;
        try {
            while((len = inputStream.read(data)) != -1) {
                byteArrayOutputStream.write(data, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        resultData = new String(byteArrayOutputStream.toByteArray());
        Log.i("result",resultData);
        if(resultData.charAt(0)=='L'){

            List<History> list = DataSupport.findAll(History.class);
            try{
                if(list.get(0).getUsername()!=account)
                {
                    History his = new History();
                    his.setPassword(password);
                    his.setUsername(account);
                    his.save();

                }
            }
            catch (Exception e)
            {
                History his = new History();
                his.setPassword(password);
                his.setUsername(account);
                his.save();
            }
            Intent intent = new Intent(LoginActivity.this,Location.class);
            intent.putExtra("username",account);
            startActivity(intent);

        }
        else if(resultData.charAt(0)=='N'){
            Toast.makeText(LoginActivity.this,"密码错误",Toast.LENGTH_SHORT).show();
        }
        else if (resultData.charAt(0)=='U'){
            Toast.makeText(LoginActivity.this,"账号不存在",Toast.LENGTH_SHORT).show();
        }
        return resultData;
    }
}

