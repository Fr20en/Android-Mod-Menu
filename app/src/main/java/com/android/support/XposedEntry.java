package com.android.support;

import android.app.Activity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
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
        if (!lpparam.packageName.equals("com.bilibili.fatego")) return;
        XposedBridge.log("FGO Menu: Bili FGO detected!");

        Class<?> activityClass = XposedHelpers.findClass("com.bilibili.fatego.UnityPlayerNativeActivity", lpparam.classLoader);
        
        XposedHelpers.findAndHookMethod(activityClass, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isHooked) return;
                isHooked = true;
                Activity activity = (Activity) param.thisObject;
                XposedBridge.log("FGO Menu: UnityPlayerNativeActivity onResume!");
                
                activity.runOnUiThread(() -> {
                    try {
                        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
                        findAndDemoteSurfaceView(decor);
                        
                        Toast.makeText(activity, "FGO Menu Injected!", Toast.LENGTH_SHORT).show();
                        Menu menu = new Menu(activity);
                        menu.SetWindowManagerWindowService();
                        menu.ShowMenu();
                        XposedBridge.log("FGO Menu: Success!");
                    } catch (Throwable t) {
                        XposedBridge.log("FGO Menu Error: " + t.getMessage());
                        XposedBridge.log(t);
                    }
                });
            }
        });
    }
    
    private void findAndDemoteSurfaceView(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof SurfaceView) {
                ((SurfaceView) child).setZOrderOnTop(false);
                ((SurfaceView) child).setZOrderMediaOverlay(false);
                XposedBridge.log("FGO Menu: Demoted SurfaceView Z-Order!");
            } else if (child instanceof ViewGroup) {
                findAndDemoteSurfaceView((ViewGroup) child);
            }
        }
    }
}