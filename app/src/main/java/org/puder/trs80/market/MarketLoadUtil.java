package org.puder.trs80.market;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Insert;

public class MarketLoadUtil {

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
}
