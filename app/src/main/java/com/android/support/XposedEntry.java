package com.android.support;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
        if (!lpparam.packageName.equals("com.bilibili.fatego") && 
            !lpparam.packageName.equals("com.aniplex.fategrandorder") &&
            !lpparam.packageName.equals("com.xiaomeng.fategrandorder")) return;
        
        XposedBridge.log("FGO Menu: Target matched! " + lpparam.packageName);

        // 终极杀招 1：Hook PackageManager，伪造权限声明！
        try {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "checkPermission", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String perm = (String) param.args[0];
                    if ("android.permission.SYSTEM_ALERT_WINDOW".equals(perm)) {
                        param.setResult(0); // 0 = PackageManager.PERMISSION_GRANTED
                        XposedBridge.log("FGO Menu: Faked PackageManager PERMISSION_GRANTED!");
                    }
                }
            });
        } catch (Throwable t) { XposedBridge.log("FGO Menu: Hook PackageManager failed: " + t.getMessage()); }

        // 终极杀招 2：Hook AppOpsManager 的所有检查方法，伪造操作权限！
        String[] opsMethods = {"checkOpNoThrow", "noteOpNoThrow", "checkOp", "noteOp", "startOpNoThrow", "startOp"};
        for (String method : opsMethods) {
            try {
                XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, method, int.class, int.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int op = (int) param.args[0];
                        if (op == 24) { // 24 = AppOpsManager.OP_SYSTEM_ALERT_WINDOW
                            param.setResult(0); // 0 = AppOpsManager.MODE_ALLOWED
                        }
                    }
                });
            } catch (Throwable ignored) {}
        }
        
        // 终极杀招 3：Hook Settings.canDrawOverlays (Android 6.0+)
        try {
            XposedHelpers.findAndHookMethod("android.provider.Settings", lpparam.classLoader, "canDrawOverlays", android.content.Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Throwable ignored) {}

        // 启动原作者的 Launcher
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isHooked) return;
                isHooked = true;
                Activity activity = (Activity) param.thisObject;
                activity.runOnUiThread(() -> {
                    try {
                        Toast.makeText(activity, "晴酱的菜单注入成功喵！权限已伪造！", Toast.LENGTH_LONG).show();
                        activity.startService(new Intent(activity, Launcher.class));
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                });
            }
        });
    }
}