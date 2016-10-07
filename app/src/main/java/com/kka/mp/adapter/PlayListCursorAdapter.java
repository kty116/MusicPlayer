package com.kka.mp.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kka.mp.R;
import com.kka.mp.db.MusicContract;

public class PlayListCursorAdapter extends CursorAdapter {

    public PlayListCursorAdapter(Context context, Cursor c) {
        super(context, c, false);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View convertView = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent,false);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.playlistText = (TextView) convertView.findViewById(R.id.playlist_name_text);
        convertView.setTag(viewHolder);

        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        String playlistname = cursor.getString(cursor.getColumnIndexOrThrow(MusicContract.PlaylistEntry.COLUMN_NAME_NAME));
        viewHolder.playlistText.setText(playlistname);

    }

    private static class ViewHolder {
        TextView playlistText;
    }
}
