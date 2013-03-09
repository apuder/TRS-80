package org.puder.trs80;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Memory mem = new Memory(0x20000, 0x3c00, 0x3fff);
        TRS80Application.setMemory(mem);
        int entryAddr = CMD.loadCmdFile("defense.cmd", mem);
        setContentView(R.layout.activity_main);
        Screen screen = (Screen) findViewById(R.id.screen);
        screen.setContext(mem, entryAddr);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

}
