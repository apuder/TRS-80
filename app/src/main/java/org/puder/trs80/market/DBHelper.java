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

public class DBHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "trs80market.db";
    private static final int DATABASE_VERSION = 1;
    private static final String LOG_TAG = DBHelper.class.getSimpleName();

    private Dao<MarketApp, String> mMarketAppDao;
    private ConnectionSource mConnectionSource;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
        //super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource) {
        try {
            // Create tables and will be invoked only once of the application life time
            // i.e. the first time when the application starts.
            TableUtils.createTable(connectionSource, MarketApp.class);
            mMarketAppDao = getDao(MarketApp.class);
        } catch (java.sql.SQLException e) {
            Log.e(LOG_TAG, "Unable to create datbases", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int i, int i1) {

    }

    public List<MarketApp> getFreeApps() {
        try {
            QueryBuilder<MarketApp, String> qb = mMarketAppDao.queryBuilder();
            qb.where().eq(MarketApp.PRICE_FIELD, 0.0);
            return mMarketAppDao.query(qb.prepare());
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<MarketApp> getPaidApps() {
        try {
            QueryBuilder<MarketApp, String> qb = mMarketAppDao.queryBuilder();
            qb.where().gt(MarketApp.PRICE_FIELD, 0.0);
            return mMarketAppDao.query(qb.prepare());
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<MarketApp> getGames() {
        try {
            QueryBuilder<MarketApp, String> qb = mMarketAppDao.queryBuilder();
            qb.where().eq(MarketApp.CATEGORY_FIELD, 0);
            return mMarketAppDao.query(qb.prepare());
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void loadMarket() {
        try {
            TableUtils.dropTable(connectionSource, MarketApp.class, true);
            TableUtils.createTable(connectionSource, MarketApp.class);

            MarketApp app = null;

            if (mMarketAppDao == null) {
                mMarketAppDao = getDao(MarketApp.class);
            }

            app = new MarketApp();
            app.setName("Dancing Demon");
            app.setPrice(.99);
            app.setCategory(0);
            app.setThumbnail_url("");
            mMarketAppDao.create(app);

            app = new MarketApp();
            app.setName("Missle Defense");
            app.setPrice(1.99);
            app.setCategory(0);
            mMarketAppDao.create(app);

            app = new MarketApp();
            app.setName("Cosmic Fighter");
            app.setPrice(.99);
            app.setCategory(0);
            mMarketAppDao.create(app);

            app = new MarketApp();
            app.setName("B-1 Nuclear Bomber");
            app.setPrice(.99);
            app.setCategory(0);
            mMarketAppDao.create(app);

            app = new MarketApp();
            app.setName("Super Nova");
            app.setPrice(.00);
            app.setCategory(0);
            mMarketAppDao.create(app);

            app = new MarketApp();
            app.setName("Galaxy Invasion");
            app.setPrice(.00);
            app.setCategory(0);
            mMarketAppDao.create(app);

            app = new MarketApp();
            app.setName("Robot Attack");
            app.setPrice(.00);
            app.setCategory(0);
            mMarketAppDao.create(app);

            app = new MarketApp();
            app.setName("Attack Force");
            app.setPrice(.00);
            app.setCategory(0);
            mMarketAppDao.create(app);

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }
}
