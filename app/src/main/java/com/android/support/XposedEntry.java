package com.android.support;

import android.app.Application;
import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("FGO Menu: handleLoadPackage called for " + lpparam.packageName);
        
        if (!lpparam.packageName.equals("com.bilibili.fatego")) return;
        
        XposedBridge.log("FGO Menu: Bili FGO matched! Injecting...");

        XposedHelpers.findAndHookMethod(Application.class, "attachBaseContext", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("FGO Menu: attachBaseContext SUCCESS! Code is running!");
            }
        });
    }
}