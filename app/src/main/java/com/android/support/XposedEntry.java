package com.android.support;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("FGO Menu: handleLoadPackage called for " + lpparam.packageName);
        
        if (!lpparam.packageName.equals("com.bilibili.fatego") && 
            !lpparam.packageName.equals("com.aniplex.fategrandorder") &&
            !lpparam.packageName.equals("com.xiaomeng.fategrandorder") &&
            !lpparam.packageName.equals("com.netease.fgo")) return;
        
        XposedBridge.log("FGO Menu: Target matched! Injecting...");

        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("FGO Menu: Application.onCreate hooked!");
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                XposedBridge.log("FGO Menu: Activity.onCreate hooked: " + activity.getClass().getName());
                activity.runOnUiThread(() -> {
                    try {
                        Toast.makeText(activity, "FGO Xposed Injected Successfully!", Toast.LENGTH_LONG).show();
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                });
            }
        });
    }
}