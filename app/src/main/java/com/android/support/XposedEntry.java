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
        String pkg = lpparam.packageName;
        if (!pkg.equals("com.bilibili.fatego") && 
            !pkg.equals("com.aniplex.fategrandorder") &&
            !pkg.equals("com.xiaomeng.fategrandorder")) return;
        
        XposedBridge.log("FGO Menu: Target matched! " + pkg);

        // 终极黑魔法：Hook AppOpsManager，在底层欺骗系统，强制赋予悬浮窗权限！
        try {
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "checkOpNoThrow", int.class, int.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int op = (int) param.args[0];
                    if (op == 24) { // 24 = OP_SYSTEM_ALERT_WINDOW
                        XposedBridge.log("FGO Menu: Bypassed AppOpsManager check for SYSTEM_ALERT_WINDOW!");
                        param.setResult(0); // 0 = MODE_ALLOWED
                    }
                }
            });
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteOpNoThrow", int.class, int.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int op = (int) param.args[0];
                    if (op == 24) {
                        param.setResult(0);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("FGO Menu: Failed to hook AppOpsManager: " + t.getMessage());
        }

        // 启动菜单
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isHooked) return;
                Activity activity = (Activity) param.thisObject;
                String clsName = activity.getClass().getName();
                
                if (clsName.contains("UnityPlayer") || clsName.contains("MainActivity") || clsName.contains("Splash")) {
                    isHooked = true;
                    activity.runOnUiThread(() -> {
                        try {
                            XposedBridge.log("FGO Menu: Starting Launcher Service...");
                            Toast.makeText(activity, "FGO Menu Injected (Permission Bypassed)!", Toast.LENGTH_LONG).show();
                            activity.startService(new Intent(activity, Launcher.class));
                        } catch (Throwable t) {
                            XposedBridge.log("FGO Menu Error: " + t.getMessage());
                            XposedBridge.log(t);
                        }
                    });
                }
            }
        });
    }
}