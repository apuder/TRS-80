package org.puder.trs80.model;

import com.raizlabs.android.dbflow.annotation.Database;

@Database(name = AppDatabase.NAME, version = AppDatabase.VERSION)

public class AppDatabase {
    public static final String NAME = "TRS80AppDB";
    public static final int VERSION = 1;
}