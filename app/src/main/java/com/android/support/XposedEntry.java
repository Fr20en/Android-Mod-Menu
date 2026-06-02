package com.android.support;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
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
                Activity activity = (Activity) param.thisObject;
                String cls = activity.getClass().getName();
                XposedBridge.log("FGO Menu: Activity onResume: " + cls);
                
                if (isHooked) return;
                isHooked = true;
                
                activity.runOnUiThread(() -> {
                    try {
                        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
                        TextView tv = new TextView(activity);
                        tv.setText("晴酱的终极测试！如果你能看到这行字，说明注入成功！");
                        tv.setTextSize(30);
                        tv.setBackgroundColor(Color.RED);
                        tv.setTextColor(Color.WHITE);
                        tv.setGravity(Gravity.CENTER);
                        
                        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        );
                        decor.addView(tv, flp);
                        tv.bringToFront();
                        XposedBridge.log("FGO Menu: DecorView addView SUCCESS!");
                    } catch (Throwable t) {
                        XposedBridge.log("FGO Menu FATAL: " + t.getMessage());
                        XposedBridge.log(t);
                    }
                });
            }
        });
    }
}