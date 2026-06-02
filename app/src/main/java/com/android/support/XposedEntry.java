package com.android.support;

import android.app.Activity;
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
        if (!lpparam.packageName.equals("com.bilibili.fatego") && 
            !lpparam.packageName.equals("com.aniplex.fategrandorder") && 
            !lpparam.packageName.equals("com.xiaomeng.fategrandorder")) return;
        
        XposedBridge.log("FGO Mod Menu: Hooked " + lpparam.packageName);

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!isHooked) {
                    isHooked = true;
                    Activity activity = (Activity) param.thisObject;
                    activity.runOnUiThread(() -> {
                        try {
                            XposedBridge.log("FGO Mod Menu: Starting Menu UI");
                            new Menu(activity);
                        } catch (Exception e) {
                            XposedBridge.log(e);
                        }
                    });
                }
            }
        });
    }
}