package com.android.support;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import de.robv.android.xposed.XposedBridge;

public class SimpleMenu {
    public static void show(Activity activity) {
        try {
            ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            demoteSurfaceViews(decor);

            Button btn = new Button(activity);
            btn.setText("晴酱的终极菜单");
            btn.setTextSize(20);
            btn.setBackgroundColor(Color.RED);
            btn.setTextColor(Color.WHITE);

            WindowManager wm = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                2012, // TYPE_APPLICATION_ATTACHED_DIALOG (No permission needed)
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = 100;
            params.y = 200;

            try {
                wm.addView(btn, params);
                XposedBridge.log("FGO Menu: Added via WindowManager TYPE_APPLICATION_ATTACHED_DIALOG!");
            } catch (Exception e) {
                XposedBridge.log("FGO Menu: WindowManager failed, fallback to addContentView: " + e.getMessage());
                FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                );
                flp.leftMargin = 100;
                flp.topMargin = 200;
                activity.addContentView(btn, flp);
                btn.bringToFront();
            }
        } catch (Throwable t) {
            XposedBridge.log("FGO Menu SimpleMenu FATAL: " + t.getMessage());
            XposedBridge.log(t);
        }
    }

    private static void demoteSurfaceViews(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof SurfaceView) {
                try {
                    ((SurfaceView) child).setZOrderOnTop(false);
                    ((SurfaceView) child).setZOrderMediaOverlay(false);
                    XposedBridge.log("FGO Menu: Demoted SurfaceView!");
                } catch (Throwable ignored) {}
            } else if (child instanceof ViewGroup) {
                demoteSurfaceViews((ViewGroup) child);
            }
        }
    }
}