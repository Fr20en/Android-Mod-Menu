package com.android.support;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MenuActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#CC000000"));

        Button btn = new Button(this);
        btn.setText("FGO MENU INJECTED!");
        btn.setTextSize(30);
        btn.setTextColor(Color.RED);
        btn.setBackgroundColor(Color.YELLOW);
        btn.setOnClickListener(v -> Toast.makeText(this, "Menu Clicked!", Toast.LENGTH_SHORT).show());

        layout.addView(btn);
        setContentView(layout);
        
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    }
}