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

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                String clsName = activity.getClass().getName();
                
                if (!isHooked && (clsName.contains("UnityPlayerActivity") || clsName.contains("MainActivity") || clsName.contains("Splash"))) {
                    isHooked = true;
                    activity.runOnUiThread(() -> {
                        try {
                            XposedBridge.log("FGO Menu: Injecting Menu into Activity DecorView...");
                            Toast.makeText(activity, "FGO Menu Injected!", Toast.LENGTH_SHORT).show();
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