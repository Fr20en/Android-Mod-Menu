package com.android.support;

import android.app.Activity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
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
        XposedBridge.log("FGO Menu: Bili FGO detected!");

        XposedHelpers.findAndHookMethod(SurfaceView.class, "setZOrderOnTop", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = false;
            }
        });

        Class<?> targetActivity = XposedHelpers.findClass("com.bilibili.fatego.UnityPlayerNativeActivity", lpparam.classLoader);
        
        XposedHelpers.findAndHookMethod(targetActivity, "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean hasFocus = (boolean) param.args[0];
                if (!hasFocus || isHooked) return;
                isHooked = true;
                
                Activity activity = (Activity) param.thisObject;
                XposedBridge.log("FGO Menu: UnityPlayerNativeActivity got focus!");
                
                activity.runOnUiThread(() -> {
                    try {
                        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
                        demoteSurfaceViews(decor);
                        
                        Toast.makeText(activity, "FGO Menu Injected (Activity Mode)!", Toast.LENGTH_LONG).show();
                        
                        // 核心：直接用 Activity Context 实例化 Menu，绝对不用 Service！
                        Menu menu = new Menu(activity);
                        menu.SetWindowManagerWindowService();
                        menu.ShowMenu();
                        
                        XposedBridge.log("FGO Menu: Menu shown successfully!");
                    } catch (Throwable t) {
                        XposedBridge.log("FGO Menu FATAL: " + t.getMessage());
                        XposedBridge.log(t);
                    }
                });
            }
        });
    }
    
    private void demoteSurfaceViews(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof SurfaceView) {
                try {
                    ((SurfaceView) child).setZOrderOnTop(false);
                    ((SurfaceView) child).setZOrderMediaOverlay(false);
                } catch (Throwable ignored) {}
            } else if (child instanceof ViewGroup) {
                demoteSurfaceViews((ViewGroup) child);
            }
        }
    }
}