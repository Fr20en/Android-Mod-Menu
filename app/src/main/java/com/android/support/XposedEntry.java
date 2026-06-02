package com.android.support;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
        XposedBridge.log("FGO Menu: Bili FGO detected!");

        Class<?> targetActivity = XposedHelpers.findClass("com.bilibili.fatego.UnityPlayerNativeActivity", lpparam.classLoader);
        
        XposedHelpers.findAndHookMethod(targetActivity, "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean hasFocus = (boolean) param.args[0];
                if (!hasFocus || isHooked) return;
                isHooked = true;
                
                final Activity activity = (Activity) param.thisObject;
                XposedBridge.log("FGO Menu: UnityPlayerNativeActivity got focus! Adding RED SCREEN...");
                
                activity.runOnUiThread(() -> {
                    try {
                        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
                        
                        // 最暴力的全屏红色遮罩，不需要任何权限和资源！
                        View redScreen = new View(activity);
                        redScreen.setBackgroundColor(Color.parseColor("#CCFF0000")); // 半透明红色
                        
                        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        );
                        
                        decor.addView(redScreen, flp);
                        redScreen.bringToFront();
                        
                        XposedBridge.log("FGO Menu: RED SCREEN ADDED SUCCESSFULLY!");
                    } catch (Throwable t) {
                        XposedBridge.log("FGO Menu FATAL: " + t.getMessage());
                        XposedBridge.log(t);
                    }
                });
            }
        });
    }
}