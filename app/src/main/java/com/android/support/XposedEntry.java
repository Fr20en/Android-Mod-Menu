package com.android.support;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {
    private static boolean isHooked = false;
    public static Activity currentActivity = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String pkg = lpparam.packageName;
        if (!pkg.equals("com.bilibili.fatego") && 
            !pkg.equals("com.aniplex.fategrandorder") &&
            !pkg.equals("com.xiaomeng.fategrandorder") &&
            !pkg.equals("com.netease.fgo")) return;
        
        XposedBridge.log("FGO Menu: Target matched! " + pkg);

        // 1. 抓取当前最顶层的 Activity
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                currentActivity = (Activity) param.thisObject;
            }
        });

        // 2. 偷天换日：Hook WindowManagerImpl，拦截悬浮窗，强行塞进 DecorView！
        Class<?> wmImplClass = XposedHelpers.findClass("android.view.WindowManagerImpl", lpparam.classLoader);
        
        XposedHelpers.findAndHookMethod(wmImplClass, "addView", View.class, ViewGroup.LayoutParams.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) param.args[1];
                if (lp instanceof WindowManager.LayoutParams) {
                    WindowManager.LayoutParams wlp = (WindowManager.LayoutParams) lp;
                    if (wlp.type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY || 
                        wlp.type == WindowManager.LayoutParams.TYPE_PHONE || 
                        wlp.type == 2038 || wlp.type == 2002) {
                        
                        param.setResult(null); // 阻断原方法
                        if (currentActivity != null) {
                            View v = (View) param.args[0];
                            FrameLayout decor = (FrameLayout) currentActivity.getWindow().getDecorView();
                            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(wlp.width, wlp.height);
                            flp.leftMargin = wlp.x;
                            flp.topMargin = wlp.y;
                            v.setTag(wlp); // 保存原始参数
                            decor.addView(v, flp);
                            v.bringToFront();
                            XposedBridge.log("FGO Menu: Intercepted addView! Injected into DecorView. No permission needed!");
                        }
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod(wmImplClass, "updateViewLayout", View.class, ViewGroup.LayoutParams.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) param.args[1];
                if (lp instanceof WindowManager.LayoutParams) {
                    WindowManager.LayoutParams wlp = (WindowManager.LayoutParams) lp;
                    if (wlp.type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY || wlp.type == 2038 || wlp.type == 2002) {
                        param.setResult(null);
                        View v = (View) param.args[0];
                        if (v.getParent() != null) {
                            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(wlp.width, wlp.height);
                            flp.leftMargin = wlp.x;
                            flp.topMargin = wlp.y;
                            v.setLayoutParams(flp);
                            v.bringToFront();
                        }
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod(wmImplClass, "removeView", View.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                View v = (View) param.args[0];
                if (v.getTag() instanceof WindowManager.LayoutParams && v.getParent() instanceof FrameLayout) {
                    param.setResult(null);
                    ((ViewGroup) v.getParent()).removeView(v);
                }
            }
        });

        // 3. 启动原作者的 Launcher
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isHooked) return;
                Activity activity = (Activity) param.thisObject;
                String clsName = activity.getClass().getName();
                
                if (clsName.contains("UnityPlayer") || clsName.contains("MainActivity") || clsName.contains("Splash") || clsName.contains("BiliGame")) {
                    isHooked = true;
                    activity.runOnUiThread(() -> {
                        try {
                            XposedBridge.log("FGO Menu: Starting Launcher...");
                            Toast.makeText(activity, "FGO Menu Injected (DecorView Mode)!", Toast.LENGTH_LONG).show();
                            activity.startService(new Intent(activity, Launcher.class));
                        } catch (Throwable t) {
                            XposedBridge.log("FGO Menu Error: " + t.getMessage());
                        }
                    });
                }
            }
        });
    }
}