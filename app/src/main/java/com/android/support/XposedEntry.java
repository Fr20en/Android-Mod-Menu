package com.android.support;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {
    private static boolean isHooked = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.bilibili.fatego")) return;
        XposedBridge.log("FGO Menu: Target matched! " + lpparam.packageName);

        Class<?> targetActivity = XposedHelpers.findClass("com.bilibili.fatego.UnityPlayerNativeActivity", lpparam.classLoader);
        
        XposedHelpers.findAndHookMethod(targetActivity, "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean hasFocus = (boolean) param.args[0];
                if (!hasFocus || isHooked) return;
                isHooked = true;
                
                final Activity activity = (Activity) param.thisObject;
                XposedBridge.log("FGO Menu: UnityPlayerNativeActivity got focus!");
                
                activity.runOnUiThread(() -> {
                    try {
                        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
                        demoteRenderLayers(decor);

                        Button btn = new Button(activity);
                        btn.setText("晴酱的终极菜单");
                        btn.setTextSize(24);
                        btn.setBackgroundColor(Color.RED);
                        btn.setTextColor(Color.WHITE);
                        btn.setOnClickListener(v -> Toast.makeText(activity, "菜单点击成功！", Toast.LENGTH_SHORT).show());

                        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(600, 200);
                        flp.leftMargin = 100;
                        flp.topMargin = 300;
                        flp.gravity = Gravity.TOP | Gravity.LEFT;
                        
                        activity.addContentView(btn, flp);
                        btn.bringToFront();
                        
                        XposedBridge.log("FGO Menu: Button added to DecorView successfully!");
                    } catch (Throwable t) {
                        XposedBridge.log("FGO Menu FATAL: " + t.getMessage());
                        XposedBridge.log(t);
                    }
                });
            }
        });
    }
    
    private void demoteRenderLayers(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof SurfaceView) {
                try {
                    ((SurfaceView) child).setZOrderOnTop(false);
                    ((SurfaceView) child).setZOrderMediaOverlay(false);
                } catch (Throwable ignored) {}
            } else if (child instanceof TextureView) {
                try {
                    // TextureView doesn't have setZOrderOnTop, but bringToFront works
                } catch (Throwable ignored) {}
            } else if (child instanceof ViewGroup) {
                demoteRenderLayers((ViewGroup) child);
            }
        }
    }
}