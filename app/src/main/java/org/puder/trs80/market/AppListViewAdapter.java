package org.puder.trs80.market;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.puder.trs80.Fonts;
import org.puder.trs80.Hardware;
import org.puder.trs80.R;

import java.text.NumberFormat;
import java.util.List;

public class AppListViewAdapter extends RecyclerView.Adapter<AppListViewAdapter.Holder> {

    private final Context mContext;
    List<MarketApp> mMarketApps;

    public AppListViewAdapter(Context context,  List<MarketApp> apps) {
        this.mContext = context;
        mMarketApps = apps;
    }

    class Holder extends RecyclerView.ViewHolder {
        public int position;
        public TextView name;
        public TextView price;
        public TextView author;
        public TextView publisher;
        public ImageView thumbnail;

        public Holder(View itemView) {
            super(itemView);
            if (!(itemView instanceof CardView)) {
                return;
            }
            name = (TextView) itemView.findViewById(R.id.app_title);
            price = (TextView) itemView.findViewById(R.id.app_price);
            //author = (TextView) itemView.findViewById(R.id.app_author);
            publisher = (TextView) itemView.findViewById(R.id.app_publisher);
            thumbnail = (ImageView) itemView.findViewById(R.id.app_thumbnail);
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.market_app_view, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(final Holder holder, int position) {

        MarketApp app = mMarketApps.get(position);

        // Position
        holder.position = position;
        // Name
        holder.name.setText(app.getName());
        // Price
        if (app.getPrice() <= 0.0) {
            holder.price.setText(mContext.getString(R.string.free));
        } else {
            holder.price.setText(NumberFormat.getCurrencyInstance().format(app.getPrice()));
        }

        // Screenshot
        new AsyncTask<String, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(String... params) {
                //TODO fetch image with url
                // for now just create a default from drawable
                return BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.dancing_demon);
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                holder.thumbnail.setImageBitmap(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, app.getThumbnail_url());
    }

    @Override
    public int getItemCount() {
        return mMarketApps == null ? 0 : mMarketApps.size();
    }
}
