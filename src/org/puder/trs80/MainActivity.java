package org.puder.trs80;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

    private Memory mem;

    private View   rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mem = new Memory();
        int entryAddr = CMD.loadCmdFile("defense.cmd", mem);
        rootView = new Screen(this, mem, entryAddr);
        setContentView(rootView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

}
