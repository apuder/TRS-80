package org.puder.trs80.browser;

import java.io.File;
import java.util.List;

import org.puder.trs80.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class BrowserListViewAdapter extends ArrayAdapter<String> {

    Context context;

    public BrowserListViewAdapter(Context context, List<String> items) {
        super(context, 0, items);
        this.context = context;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TextView holder = null;
        String path = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.file_browser_item, null);
            holder = (TextView) convertView.findViewById(R.id.path);
            convertView.setTag(holder);
        } else {
            holder = (TextView) convertView.getTag();
        }

        ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
        if (new File(path).isDirectory()) {
            icon.setImageResource(R.drawable.folder_icon);
        } else {
            icon.setImageResource(R.drawable.file_icon);
        }

        int idx = path.lastIndexOf(File.separator);
        if (idx != -1) {
            path = path.substring(idx + 1);
        }
        holder.setText(path);

        return convertView;
    }
}