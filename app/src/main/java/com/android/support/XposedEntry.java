package com.android.support;

import android.app.Activity;
import android.content.Context;
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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

        // 1. 抓取当前 Activity
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                currentActivity = (Activity) param.thisObject;
            }
        });

        // 2. 终极黑魔法：Hook ContextImpl.getSystemService，劫持 WindowManager！
        Class<?> contextImplClass = XposedHelpers.findClass("android.app.ContextImpl", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(contextImplClass, "getSystemService", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String name = (String) param.args[0];
                if (Context.WINDOW_SERVICE.equals(name)) {
                    final Object originalWM = param.getResult();
                    if (originalWM != null && !Proxy.isProxyClass(originalWM.getClass())) {
                        Object proxyWM = Proxy.newProxyInstance(
                            originalWM.getClass().getClassLoader(),
                            new Class[]{WindowManager.class},
                            new InvocationHandler() {
                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                    if ("addView".equals(method.getName()) && args.length == 2) {
                                        View v = (View) args[0];
                                        ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) args[1];
                                        if (currentActivity != null) {
                                            FrameLayout decor = (FrameLayout) currentActivity.getWindow().getDecorView();
                                            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(lp.width, lp.height);
                                            if (lp instanceof WindowManager.LayoutParams) {
                                                WindowManager.LayoutParams wlp = (WindowManager.LayoutParams) lp;
                                                flp.leftMargin = wlp.x;
                                                flp.topMargin = wlp.y;
                                                flp.gravity = wlp.gravity;
                                            }
                                            // 强制置顶，穿透 Unity 的 SurfaceView！
                                            try { v.bringToFront(); } catch (Throwable ignored) {}
                                            decor.addView(v, flp);
                                            decor.bringChildToFront(v);
                                            XposedBridge.log("FGO Menu: Proxy addView to DecorView! Z-Order forced!");
                                            return null;
                                        }
                                    } else if ("updateViewLayout".equals(method.getName()) && args.length == 2) {
                                        View v = (View) args[0];
                                        ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) args[1];
                                        if (v.getParent() instanceof FrameLayout) {
                                            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(lp.width, lp.height);
                                            if (lp instanceof WindowManager.LayoutParams) {
                                                WindowManager.LayoutParams wlp = (WindowManager.LayoutParams) lp;
                                                flp.leftMargin = wlp.x;
                                                flp.topMargin = wlp.y;
                                                flp.gravity = wlp.gravity;
                                            }
                                            v.setLayoutParams(flp);
                                            v.bringToFront();
                                            return null;
                                        }
                                    } else if ("removeView".equals(method.getName()) && args.length == 1) {
                                        View v = (View) args[0];
                                        if (v.getParent() instanceof ViewGroup) {
                                            ((ViewGroup) v.getParent()).removeView(v);
                                            return null;
                                        }
                                    }
                                    return method.invoke(originalWM, args);
                                }
                            }
                        );
                        param.setResult(proxyWM);
                    }
                }
            }
        });

        // 3. 启动 Launcher
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isHooked) return;
                Activity activity = (Activity) param.thisObject;
                isHooked = true;
                activity.runOnUiThread(() -> {
                    try {
                        XposedBridge.log("FGO Menu: Starting Launcher...");
                        Toast.makeText(activity, "FGO Menu Injected (Proxy Mode)!", Toast.LENGTH_LONG).show();
                        activity.startService(new Intent(activity, Launcher.class));
                    } catch (Throwable t) {
                        XposedBridge.log("FGO Menu Error: " + t.getMessage());
                    }
                });
            }
        });
    }
}