package org.puder.trs80;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.ViewGroup;

public class EmulatorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TRS80Application.getHardware().computeFontDimensions(getWindow());
        Keyboard keyboard = new Keyboard();
        TRS80Application.setKeyboard(keyboard);
        initView();
        Z80ExecutionThread threadZ80 = TRS80Application.getZ80Thread();
        if (threadZ80 == null) {
            threadZ80 = new Z80ExecutionThread();
            TRS80Application.setZ80Thread(threadZ80);
            threadZ80.setRunning(true);
            threadZ80.start();
        }
        Log.d("TRS80", "EmulatorActivity.onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("TRS80", "EmulatorActivity.onDestroy()");
    }

    private void initView0() {
        setContentView(R.layout.emulator);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup root = (ViewGroup) findViewById(R.id.keyboard_container);
        ViewGroup keyboard = (ViewGroup) inflater.inflate(R.layout.keyboard_default, null);

        Key key_0 = (Key) keyboard.findViewById(R.id.key_0);
        ((ViewGroup) key_0.getParent()).removeView(key_0);
        key_0.setPosition(100, 100);
        root.addView(key_0);

        Key key_1 = (Key) keyboard.findViewById(R.id.key_1);
        ((ViewGroup) key_1.getParent()).removeView(key_1);
        key_1.setPosition(200, 200);
        root.addView(key_1);
    }

    private void initView() {
        setContentView(R.layout.emulator);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup root = (ViewGroup) findViewById(R.id.keyboard_container);
        inflater.inflate(R.layout.keyboard_default, root);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

}
