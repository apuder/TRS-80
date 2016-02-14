package org.puder.trs80.market;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.puder.trs80.R;

public class MarketDataHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "trs80market.db";
    private static final int DATABASE_VERSION = 1;
    private static final String LOG_TAG = MarketDataHelper.class.getSimpleName();

    private Dao<MarketApp, Integer> trs80MarketDao;

    public MarketDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.xml.ormlite_config);
    }

    @Override
    public void onCreate(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource) {
        try {
            // Create tables. This onCreate() method will be invoked only once of the application life time i.e. the first time when the application starts.
            TableUtils.createTable(connectionSource, MarketApp.class);
        } catch (java.sql.SQLException e) {
            Log.e(LOG_TAG, "Unable to create datbases", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int i, int i1) {

    }

    public static void loadMarket() {

    }

/*
    public static void loadMarket() {

        Delete.table(MarketApp.class);

        Insert<MarketApp> insert = null;

        insert = Insert.into(MarketApp.class).orFail()
                .columns(MarketApp$Table.NAME, MarketApp$Table.PRICE)//, MarketApp$Table.ATYPE)
                .values("Dancing Demon", .99); //, MarketApp.TYPE_OTHER);
        FlowManager.getDatabase(MarketDatabase.NAME).getWritableDatabase().execSQL(insert.getQuery());

        insert = Insert.into(MarketApp.class).orFail()
                .columns(MarketApp$Table.NAME, MarketApp$Table.PRICE)//, MarketApp$Table.ATYPE)
                .values("Missle Defense", 1.99);//, MarketApp.TYPE_GAME);
        FlowManager.getDatabase(MarketDatabase.NAME).getWritableDatabase().execSQL(insert.getQuery());

        insert = Insert.into(MarketApp.class).orFail()
                .columns(MarketApp$Table.NAME, MarketApp$Table.PRICE)//, MarketApp$Table.ATYPE)
                .values("Cosmic Fighter", .99);//, MarketApp.TYPE_GAME);
        FlowManager.getDatabase(MarketDatabase.NAME).getWritableDatabase().execSQL(insert.getQuery());

        insert = Insert.into(MarketApp.class).orFail()
                .columns(MarketApp$Table.NAME, MarketApp$Table.PRICE)//, MarketApp$Table.ATYPE)
                .values("B-1 Nuclear Bomber", .99);//, MarketApp.TYPE_GAME);
        FlowManager.getDatabase(MarketDatabase.NAME).getWritableDatabase().execSQL(insert.getQuery());

        insert = Insert.into(MarketApp.class).orFail()
                .columns(MarketApp$Table.NAME, MarketApp$Table.PRICE)//, MarketApp$Table.ATYPE)
                .values("Super Nova", 0.0);//, MarketApp.TYPE_GAME);
        FlowManager.getDatabase(MarketDatabase.NAME).getWritableDatabase().execSQL(insert.getQuery());

        insert = Insert.into(MarketApp.class).orFail()
                .columns(MarketApp$Table.NAME, MarketApp$Table.PRICE)//, MarketApp$Table.ATYPE)
                .values("Galaxy Invasion", 0.0);//, MarketApp.TYPE_GAME);
        FlowManager.getDatabase(MarketDatabase.NAME).getWritableDatabase().execSQL(insert.getQuery());

        insert = Insert.into(MarketApp.class).orFail()
                .columns(MarketApp$Table.NAME, MarketApp$Table.PRICE)//, MarketApp$Table.ATYPE)
                .values("Robot Attack", 0.0);//, MarketApp.TYPE_GAME);
        FlowManager.getDatabase(MarketDatabase.NAME).getWritableDatabase().execSQL(insert.getQuery());

        insert = Insert.into(MarketApp.class).orFail()
                .columns(MarketApp$Table.NAME, MarketApp$Table.PRICE)//, MarketApp$Table.ATYPE)
                .values("Attack Force", 0.0);//, MarketApp.TYPE_GAME);
        FlowManager.getDatabase(MarketDatabase.NAME).getWritableDatabase().execSQL(insert.getQuery());

    }
*/
}
