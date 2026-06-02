package com.android.support;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {
    private static boolean isInjected = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String pkg = lpparam.packageName;
        if (!pkg.equals("com.bilibili.fatego") && 
            !pkg.equals("com.aniplex.fategrandorder") &&
            !pkg.equals("com.xiaomeng.fategrandorder") &&
            !pkg.equals("com.netease.fgo")) return;
        
        XposedBridge.log("FGO Menu: Target matched! " + pkg);

        // Hook Activity.onResume 确保 UI 已经初始化
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isInjected) return;
                
                Activity activity = (Activity) param.thisObject;
                String clsName = activity.getClass().getName();
                XposedBridge.log("FGO Menu: onResume triggered in " + clsName);
                
                // 只在主 Activity 注入
                if (clsName.contains("UnityPlayer") || clsName.contains("MainActivity") || clsName.contains("Splash") || clsName.contains("BiliGame")) {
                    isInjected = true;
                    activity.runOnUiThread(() -> {
                        try {
                            XposedBridge.log("FGO Menu: Initializing Menu directly in Activity...");
                            Toast.makeText(activity, "FGO Menu Injected Successfully!", Toast.LENGTH_LONG).show();
                            
                            // 直接实例化并显示，抛弃 Launcher Service
                            Menu menu = new Menu(activity);
                            menu.SetWindowManagerWindowService();
                            menu.ShowMenu();
                            
                            XposedBridge.log("FGO Menu: Menu UI displayed!");
                        } catch (Throwable t) {
                            XposedBridge.log("FGO Menu FATAL Error: " + t.getMessage());
                            XposedBridge.log(t);
                        }
                    });
                }
            }
        });
    }
}