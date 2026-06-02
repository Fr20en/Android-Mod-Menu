package com.android.support;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isHooked) return;
                isHooked = true;
                Activity activity = (Activity) param.thisObject;
                
                activity.runOnUiThread(() -> {
                    try {
                        XposedBridge.log("FGO Menu: Starting external MenuActivity!");
                        Intent intent = new Intent();
                        intent.setClassName("com.android.support", "com.android.support.MenuActivity");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    } catch (Throwable t) {
                        XposedBridge.log("FGO Menu FATAL: " + t.getMessage());
                    }
                });
            }
        });
    }
}