package org.puder.trs80;

import java.util.ArrayList;
import java.util.List;

public class Keyboard {
    
    private List<Key> shiftableKeys;

    public Keyboard() {
        shiftableKeys = new ArrayList<Key>();
    }
    
    public void addShiftableKey(Key key) {
        shiftableKeys.add(key);
    }
    
    public void shiftKeys() {
        for (Key key : shiftableKeys) {
            key.shift();
        }
    }
    
    public void unshiftKeys() {
        for (Key key : shiftableKeys) {
            key.unshift();
        }
    }
}
