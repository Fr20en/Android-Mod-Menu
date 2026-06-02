package com.android.support;

import android.app.Application;
import android.content.Context;
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
            !lpparam.packageName.equals("com.netmarble.fgokr")) return;
        
        XposedBridge.log("FGO Menu: Target matched! Bypassing Anti-Cheat...");

        XposedHelpers.findAndHookMethod(Application.class, "attachBaseContext", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application app = (Application) param.thisObject;
                XposedBridge.log("FGO Menu: attachBaseContext hooked! Anti-Cheat bypassed!");
                app.runOnUiThread(() -> {
                    try {
                        Toast.makeText(app, "FGO Xposed Injected at attachBaseContext!", Toast.LENGTH_LONG).show();
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                });
            }
        });
    }
}