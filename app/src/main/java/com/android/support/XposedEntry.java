package com.android.support;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
            !lpparam.packageName.equals("com.xiaomeng.fategrandorder") &&
            !lpparam.packageName.equals("com.netmarble.fgokr")) return;
        
        XposedBridge.log("FGO Menu: Target matched! " + lpparam.packageName);

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isHooked) return;
                Activity activity = (Activity) param.thisObject;
                String clsName = activity.getClass().getName();
                
                if (clsName.contains("UnityPlayer") || clsName.contains("MainActivity") || clsName.contains("Splash") || clsName.contains("BiliGame") || clsName.contains("Act")) {
                    isHooked = true;
                    activity.runOnUiThread(() -> {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
                                Toast.makeText(activity, "FGO Menu: 请授予悬浮窗权限！", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                activity.startActivity(intent);
                                return;
                            }
                            Toast.makeText(activity, "FGO Menu Injected!", Toast.LENGTH_SHORT).show();
                            activity.startService(new Intent(activity, Launcher.class));
                        } catch (Throwable t) {
                            XposedBridge.log("FGO Menu Error: " + t.getMessage());
                        }
                    });
                }
            }
        });
    }
}