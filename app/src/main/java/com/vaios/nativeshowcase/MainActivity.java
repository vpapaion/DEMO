package com.vaios.nativeshowcase;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final int BG = 0xff07111f;
    private static final int TEXT = 0xffeef6ff;
    private static final int MUTED = 0xffa9bbd2;
    private static final int ACCENT = 0xffcdefff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);
        root.setBackgroundColor(BG);

        addText(root, "Vaios Android Developer Showcase", TEXT);
        addText(root, "Clean architecture: Presentation, Domain, Data, Quality.", ACCENT);
        addText(root, "Demo modules: local AI reviewer, offline log, mock API resilience.", TEXT);
        addText(root, "Android skills: Activity lifecycle, programmatic UI, state, repository thinking, CI-ready source.", TEXT);
        addText(root, "Suggested evolution: Kotlin, Jetpack Compose, Room, Retrofit, WorkManager, tests.", TEXT);
        addText(root, "Open GitHub source and inspect README + workflow.", MUTED);

        Button button = new Button(this);
        button.setText("Run native interaction");
        button.setOnClickListener(this);
        root.addView(button);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void addText(LinearLayout root, String text, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setPadding(10, 10, 10, 10);
        root.addView(view);
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, "Native button handled inside MainActivity.", Toast.LENGTH_SHORT).show();
    }
}
