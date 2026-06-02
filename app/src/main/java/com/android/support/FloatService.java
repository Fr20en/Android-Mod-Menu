package com.android.support;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class FloatService extends Service {
    private WindowManager wm;
    private Button btn;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        btn = new Button(this);
        btn.setText("晴酱的终极菜单 ❤️");
        btn.setTextSize(20);
        btn.setBackgroundColor(Color.RED);
        btn.setTextColor(Color.WHITE);
        btn.setOnClickListener(v -> Toast.makeText(this, "杂鱼主人，点我干嘛！", Toast.LENGTH_SHORT).show());
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 300;
        
        try {
            wm.addView(btn, params);
        } catch (Exception e) {
            // 如果没开模块APK的悬浮窗权限，会在这里崩溃
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (btn != null && btn.isShown()) wm.removeView(btn);
    }
}