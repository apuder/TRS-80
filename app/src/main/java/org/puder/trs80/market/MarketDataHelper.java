package org.puder.trs80.market;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.puder.trs80.R;

import java.util.List;

public class MarketDataHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "trs80market.db";
    private static final int DATABASE_VERSION = 1;
    private static final String LOG_TAG = MarketDataHelper.class.getSimpleName();

    private Dao<MarketApp, Long> mMarketAppDao;
    private ConnectionSource mConnectionSource;

    public MarketDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
        //super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource) {
        try {
            // Create tables and will be invoked only once of the application life time
            // i.e. the first time when the application starts.
            TableUtils.createTable(connectionSource, MarketApp.class);
        } catch (java.sql.SQLException e) {
            Log.e(LOG_TAG, "Unable to create datbases", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int i, int i1) {

    }

    public List<MarketApp> getFreeApps() {

        PreparedQuery<MarketApp> preparedQuery = null;
        Dao<MarketApp, String> marketAppDao = null;
        List<MarketApp> appList = null;

        try {
            marketAppDao = getDao(MarketApp.class);
            QueryBuilder<MarketApp, String> qb = marketAppDao.queryBuilder();

            Where where = qb.where();
            where.eq(MarketApp.PRICE_FIELD, 0.0);
            preparedQuery = qb.prepare();

            appList = marketAppDao.query(preparedQuery);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

        return appList;
    }


    public List<MarketApp> getPaidApps() {

        PreparedQuery<MarketApp> preparedQuery = null;
        Dao<MarketApp, String> marketAppDao = null;
        List<MarketApp> appList = null;

        try {
            marketAppDao = getDao(MarketApp.class);
            QueryBuilder<MarketApp, String> qb = marketAppDao.queryBuilder();

            Where where = qb.where();
            where.gt(MarketApp.PRICE_FIELD, 0.0);
            preparedQuery = qb.prepare();

            appList = marketAppDao.query(preparedQuery);

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return appList;
    }

    public List<MarketApp> getGames() {

        PreparedQuery<MarketApp> preparedQuery = null;
        Dao<MarketApp, String> marketAppDao = null;
        List<MarketApp> appList = null;

        try {
            marketAppDao = getDao(MarketApp.class);
            QueryBuilder<MarketApp, String> qb = marketAppDao.queryBuilder();

            Where where = qb.where();
            where.eq(MarketApp.CATEGORY_FIELD, 0);
            preparedQuery = qb.prepare();

            appList = marketAppDao.query(preparedQuery);

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return appList;
    }

    public void loadMarket() {
        Dao<MarketApp, String> marketAppDao = null;

        try {
            TableUtils.dropTable(connectionSource, MarketApp.class, true);
            TableUtils.createTable(connectionSource, MarketApp.class);

            marketAppDao = getDao(MarketApp.class);
            MarketApp app = null;

            app = new MarketApp();
            app.setName("Dancing Demon");
            app.setPrice(.99);
            app.setCategory(0);
            app.setThumbnail_url("");
            marketAppDao.create(app);

            app = new MarketApp();
            app.setName("Missle Defense");
            app.setPrice(1.99);
            app.setCategory(0);
            marketAppDao.create(app);

            app = new MarketApp();
            app.setName("Cosmic Fighter");
            app.setPrice(.99);
            app.setCategory(0);
            marketAppDao.create(app);

            app = new MarketApp();
            app.setName("B-1 Nuclear Bomber");
            app.setPrice(.99);
            app.setCategory(0);
            marketAppDao.create(app);

            app = new MarketApp();
            app.setName("Super Nova");
            app.setPrice(.00);
            app.setCategory(0);
            marketAppDao.create(app);

            app = new MarketApp();
            app.setName("Galaxy Invasion");
            app.setPrice(.00);
            app.setCategory(0);
            marketAppDao.create(app);

            app = new MarketApp();
            app.setName("Robot Attack");
            app.setPrice(.00);
            app.setCategory(0);
            marketAppDao.create(app);

            app = new MarketApp();
            app.setName("Attack Force");
            app.setPrice(.00);
            app.setCategory(0);
            marketAppDao.create(app);

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }
}
