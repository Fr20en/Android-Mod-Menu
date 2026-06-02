package com.android.support;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

        Class<?> targetActivity = XposedHelpers.findClass("com.bilibili.fatego.UnityPlayerNativeActivity", lpparam.classLoader);
        
        XposedHelpers.findAndHookMethod(targetActivity, "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean hasFocus = (boolean) param.args[0];
                if (!hasFocus || isHooked) return;
                isHooked = true;
                
                final Activity activity = (Activity) param.thisObject;
                XposedBridge.log("FGO Menu: UnityPlayerNativeActivity got focus! Showing Dialog...");
                
                activity.runOnUiThread(() -> {
                    try {
                        // 强行把 Unity 的 SurfaceView 踩到最底层！
                        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
                        demoteRenderLayers(decor);

                        // 祭出终极武器：AlertDialog！不需要悬浮窗权限，系统级 Z-Order！
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle("晴酱的终极 FGO 菜单 ❤️");
                        builder.setMessage("注入成功！这是基于 Dialog 的菜单，不需要悬浮窗权限，也不会被 Unity 盖住！

杂鱼主人，快给本小姐磕头！");
                        
                        builder.setPositiveButton("关闭菜单", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(activity, "哼，算你识相！", Toast.LENGTH_SHORT).show();
                            }
                        });
                        
                        builder.setNegativeButton("测试功能", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(activity, "功能触发！本小姐最棒了喵！", Toast.LENGTH_SHORT).show();
                            }
                        });

                        AlertDialog dialog = builder.create();
                        // 强制设置 Window 类型，确保层级最高
                        if (dialog.getWindow() != null) {
                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
                            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                        }
                        dialog.show();
                        
                        XposedBridge.log("FGO Menu: Dialog shown successfully!");
                    } catch (Throwable t) {
                        XposedBridge.log("FGO Menu FATAL: " + t.getMessage());
                        XposedBridge.log(t);
                    }
                });
            }
        });
    }
    
    private void demoteRenderLayers(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof SurfaceView) {
                try {
                    ((SurfaceView) child).setZOrderOnTop(false);
                    ((SurfaceView) child).setZOrderMediaOverlay(false);
                } catch (Throwable ignored) {}
            } else if (child instanceof ViewGroup) {
                demoteRenderLayers((ViewGroup) child);
            }
        }
    }
}