package com.lizehao.watermelondiarynew.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lizehao.watermelondiarynew.R;
import com.lizehao.watermelondiarynew.db.DiaryDatabaseHelper;
import com.lizehao.watermelondiarynew.utils.AppManager;
import com.lizehao.watermelondiarynew.utils.SpHelper;
import com.lizehao.watermelondiarynew.utils.StatusBarCompat;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.lang.ref.WeakReference;

import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "tag";
    private FingerprintManagerCompat manager;
    private CancellationSignal cancel;
    private FingerprintManagerCompat.AuthenticationCallback callback;
    private MyHandler handler = new MyHandler(this);

    private TextView tv;
    private ImageView imageView,logo,common_iv_back;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppManager.getAppManager().addActivity(this);
        ButterKnife.bind(this);
        StatusBarCompat.compat(this, Color.parseColor("#161414"));
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_login);
        logo = (ImageView) findViewById(R.id.logo);
        common_iv_back = (ImageView) findViewById(R.id.common_iv_back1);
        common_iv_back.setVisibility(View.INVISIBLE);
        roundBitmap();
        tv = (TextView) findViewById(R.id.prompt);
        tv.setText("请将手指放在指纹传感器上");
        imageView = (ImageView) findViewById(R.id.fingerprint);
        imageView.setImageResource(R.drawable.images);

        cancel= new CancellationSignal();

        manager = FingerprintManagerCompat.from(this);
        if (!manager.isHardwareDetected()) {
            //是否支持指纹识别
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("没有传感器");
            builder.setCancelable(true);
            builder.create().show();
        } else if (!manager.hasEnrolledFingerprints()) {
            //是否已注册指纹
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("没有注册指纹");
            builder.setCancelable(true);
            builder.create().show();
        } else {
            try {
                callback = new FingerprintManagerCompat.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errMsgId, CharSequence errString) {
                        super.onAuthenticationError(errMsgId, errString);
                        //验证错误时，回调该方法。当连续验证5次错误时，将会走onAuthenticationFailed()方法
                        handler.obtainMessage(1,errMsgId,0).sendToTarget();
                    }

                    @Override
                    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        //验证成功时，回调该方法。fingerprint对象不能再验证
                        handler.obtainMessage(2).sendToTarget();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        //验证失败时，回调该方法。fingerprint对象不能再验证并且需要等待一段时间才能重新创建指纹管理对象进行验证
                        handler.obtainMessage(3).sendToTarget();
                    }
                };

                //这里去新建一个结果的回调，里面回调显示指纹验证的信息
                manager.authenticate(null, 0, cancel, callback, handler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }



    static class MyHandler extends Handler {
        WeakReference<LoginActivity> mActivity;
        MyHandler(LoginActivity activity){
            mActivity = new WeakReference<LoginActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            LoginActivity activity = mActivity.get();
            if(activity!=null){
                //todo 逻辑处理
                switch (msg.what) {
                    case 1:   //验证错误
                        //todo 界面处理
                        activity.handleErrorCode(msg.arg1);
                        break;
                    case 2:   //验证成功
                        //todo 界面处理
                        activity.handleCode(200);
                        break;
                    case 3:    //验证失败
                        //todo 界面处理
                        activity.handleCode(500);
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }

        };
    }

    //对应不同的错误，可以有不同的操作
    private void handleErrorCode(int code) {
        switch (code) {
            case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
                //todo 指纹传感器不可用，该操作被取消
                Log.i(TAG,"指纹传感器不可用，该操作被取消");
                tv.setText("指纹传感器不可用，该操作被取消");
                break;
            case FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE:
                //todo 当前设备不可用，请稍后再试
                Log.i(TAG,"当前设备不可用，请稍后再试");
                tv.setText("当前设备不可用，请稍后再试");
                break;
            case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
                //todo 由于太多次尝试失败导致被锁，该操作被取消
                Log.i(TAG,"由于太多次尝试失败导致被锁，该操作被取消");
                tv.setText("由于太多次尝试失败导致被锁，该操作被取消");
                break;
            case FingerprintManager.FINGERPRINT_ERROR_NO_SPACE:
                //todo 没有足够的存储空间保存这次操作，该操作不能完成
                Log.i(TAG,"没有足够的存储空间保存这次操作，该操作不能完成");
                tv.setText("没有足够的存储空间保存这次操作，该操作不能完成");
                break;
            case FingerprintManager.FINGERPRINT_ERROR_TIMEOUT:
                //todo 操作时间太长，一般为30秒
                Log.i(TAG,"指纹传感器超时");
                tv.setText("指纹传感器超时");
                break;
            case FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS:
                //todo 传感器不能处理当前指纹图片
                Log.i(TAG,"传感器不能处理当前指纹图片");
                tv.setText("传感器不能处理当前指纹图片");
                break;
        }
    }

    //对应不同的错误，可以有不同的操作
    private void handleCode(int code) {
        switch (code) {
            case 500:
                //todo 指纹传感器不可用，该操作被取消
                Log.i(TAG,"验证失败");
                tv.setText("验证失败");
                break;
            case 200:
                //todo 当前设备不可用，请稍后再试
                Log.i(TAG,"验证成功");
                tv.setText("验证成功");
                imageView.setImageResource(R.drawable.image);
                new Handler(new Handler.Callback() {
                    //处理接收到的消息的方法
                    @Override
                    public boolean handleMessage(Message arg0) {
                        //实现页面跳转
                        startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        return false;
                    }
                }).sendEmptyMessageDelayed(0, 1000); //表示延时三秒进行任务的执行
                break;
        }
    }

    private void roundBitmap(){
        //如果是圆的时候，我们应该把bitmap图片进行剪切成正方形， 然后再设置圆角半径为正方形边长的一半即可
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.me);
        Bitmap bitmap = null;
        //将长方形图片裁剪成正方形图片
        if (image.getWidth() == image.getHeight()) {
            bitmap = Bitmap.createBitmap(image, image.getWidth() / 2 - image.getHeight() / 2, 0, image.getHeight(), image.getHeight());
        } else {
            bitmap = Bitmap.createBitmap(image, 0, image.getHeight() / 2 - image.getWidth() / 2, image.getWidth(), image.getWidth());
        }
        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
        //圆角半径为正方形边长的一半
        roundedBitmapDrawable.setCornerRadius(bitmap.getWidth() / 2);
        //抗锯齿
        roundedBitmapDrawable.setAntiAlias(true);
        logo.setImageDrawable(roundedBitmapDrawable);
    }
}
