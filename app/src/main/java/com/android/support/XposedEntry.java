package com.android.support;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
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
    private static Activity currentActivity = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.bilibili.fatego") && 
            !lpparam.packageName.equals("com.aniplex.fategrandorder") &&
            !lpparam.packageName.equals("com.xiaomeng.fategrandorder")) return;
        
        XposedBridge.log("FGO Menu: Target matched! " + lpparam.packageName);

        // 1. 废掉 SurfaceView 的置顶能力
        XposedHelpers.findAndHookMethod(SurfaceView.class, "setZOrderOnTop", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = false;
            }
        });
        XposedHelpers.findAndHookMethod(SurfaceView.class, "setZOrderMediaOverlay", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = false;
            }
        });

        // 2. 代理 WindowManager，把悬浮窗强行塞进 DecorView
        Class<?> wmImplClass = XposedHelpers.findClass("android.view.WindowManagerImpl", lpparam.classLoader);
        
        XposedHelpers.findAndHookMethod(wmImplClass, "addView", View.class, ViewGroup.LayoutParams.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (currentActivity == null) return;
                View v = (View) param.args[0];
                ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) param.args[1];
                
                FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(lp.width, lp.height);
                if (lp instanceof WindowManager.LayoutParams) {
                    WindowManager.LayoutParams wlp = (WindowManager.LayoutParams) lp;
                    flp.leftMargin = wlp.x;
                    flp.topMargin = wlp.y;
                    flp.gravity = wlp.gravity;
                }
                
                param.setResult(null);
                currentActivity.addContentView(v, flp);
                v.bringToFront();
                XposedBridge.log("FGO Menu: Bypassed WindowManager! Used addContentView!");
            }
        });

        XposedHelpers.findAndHookMethod(wmImplClass, "updateViewLayout", View.class, ViewGroup.LayoutParams.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (currentActivity == null) return;
                View v = (View) param.args[0];
                ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) param.args[1];
                if (v.getParent() != null) {
                    FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(lp.width, lp.height);
                    if (lp instanceof WindowManager.LayoutParams) {
                        WindowManager.LayoutParams wlp = (WindowManager.LayoutParams) lp;
                        flp.leftMargin = wlp.x;
                        flp.topMargin = wlp.y;
                        flp.gravity = wlp.gravity;
                    }
                    param.setResult(null);
                    v.setLayoutParams(flp);
                    v.bringToFront();
                }
            }
        });

        XposedHelpers.findAndHookMethod(wmImplClass, "removeView", View.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                View v = (View) param.args[0];
                if (v.getParent() instanceof ViewGroup) {
                    param.setResult(null);
                    ((ViewGroup) v.getParent()).removeView(v);
                }
            }
        });

        // 3. 精准 Hook B服主 Activity
        try {
            Class<?> activityClass = XposedHelpers.findClass("com.bilibili.fatego.UnityPlayerNativeActivity", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(activityClass, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    currentActivity = (Activity) param.thisObject;
                    if (isHooked) return;
                    isHooked = true;
                    XposedBridge.log("FGO Menu: UnityPlayerNativeActivity onResume!");
                    
                    currentActivity.runOnUiThread(() -> {
                        try {
                            ViewGroup decor = (ViewGroup) currentActivity.getWindow().getDecorView();
                            demoteSurfaceViews(decor);
                            
                            Toast.makeText(currentActivity, "FGO Menu Injected!", Toast.LENGTH_SHORT).show();
                            Menu menu = new Menu(currentActivity);
                            menu.SetWindowManagerWindowService();
                            menu.ShowMenu();
                            XposedBridge.log("FGO Menu: Success!");
                        } catch (Throwable t) {
                            XposedBridge.log("FGO Menu Error: " + t.getMessage());
                            XposedBridge.log(t);
                        }
                    });
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("FGO Menu: Failed to hook UnityPlayerNativeActivity: " + t.getMessage());
        }
    }
    
    private void demoteSurfaceViews(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof SurfaceView) {
                ((SurfaceView) child).setZOrderOnTop(false);
                ((SurfaceView) child).setZOrderMediaOverlay(false);
            } else if (child instanceof ViewGroup) {
                demoteSurfaceViews((ViewGroup) child);
            }
        }
    }
}