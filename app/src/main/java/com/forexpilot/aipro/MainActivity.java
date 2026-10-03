package com.forexpilot.aipro;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.rgb(10, 15, 25));

        TextView title = new TextView(this);
        title.setText("ForexPilot AI Pro");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        TextView status = new TextView(this);
        status.setText("LIVE MARKET ENGINE\nConnecting...");
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 30, 0, 0);

        layout.addView(title);
        layout.addView(status);

        setContentView(layout);
    }
}