package com.kka.mp.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.kka.mp.R;
import com.kka.mp.db.DBHelper;
import com.kka.mp.db.Facade;
import com.kka.mp.db.MusicContract;
import com.kka.mp.service.MusicService;

public class PlayListSongCursorAdapter extends CursorAdapter {

    private int nowMusicPosition = -1;
    private boolean visible = true;

    public PlayListSongCursorAdapter(Context context, Cursor c) {
        super(context, c, false);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View convertView = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.title = (TextView) convertView.findViewById(R.id.text_title1);
        viewHolder.artist = (TextView) convertView.findViewById(R.id.text_artist1);
        viewHolder.image = (ImageView) convertView.findViewById(R.id.image1);


        convertView.setTag(viewHolder);

        return convertView;
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        final int position = cursor.getPosition();

        viewHolder.nowMusic = (ImageView) view.findViewById(R.id.now_music_sign);

        viewHolder.menuButton = (ImageView) view.findViewById(R.id.menu_button);
        String musicId = cursor.getString(cursor.getColumnIndexOrThrow(MusicContract.SongEntry.COLUMN_NAME_MUSIC_ID));
        final String playlistId = cursor.getString(cursor.getColumnIndexOrThrow(MusicContract.SongEntry.COLUMN_NAME_PLAYLIST_ID));
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(musicId));
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, uri);

//            cursor.moveToFirst();
//            String artist = cursor.getString(cursor.getColumnIndexOrThrow(MusicContract.SongEntry.));
//            String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));

        String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

        byte albumImage[] = retriever.getEmbeddedPicture();
        if (albumImage != null) {
            Glide.with(context).load(albumImage).into(viewHolder.image);
        } else {
            viewHolder.image.setImageResource(R.drawable.ic_music_image_24dp);
        }
        viewHolder.title.setText(title);
        viewHolder.artist.setText(artist);

        if (cursor.getPosition() == nowMusicPosition) {
            viewHolder.nowMusic.setVisibility(View.VISIBLE);
        } else {
            viewHolder.nowMusic.setVisibility(View.GONE);
        }

        if (!visible) {
            viewHolder.menuButton.setVisibility(View.GONE);
        }

        viewHolder.menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(context, v);
                popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());
                popup.show();

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()) {
                            case R.id.delete:
                                DBHelper helper = new DBHelper(context);
                                SQLiteDatabase db = helper.getWritableDatabase();
                                Toast.makeText(context, "" + getItemId(position), Toast.LENGTH_SHORT).show();
                                db.delete(MusicContract.SongEntry.SONG_TABLE_NAME, MusicContract.SongEntry._ID + "=?", new String[]{String.valueOf(getItemId(position))});

                                swapCursor(new Facade(context).querySong(playlistId));
                                notifyDataSetChanged();

                                Intent musicDataIntent = new Intent(context, MusicService.class);
                                musicDataIntent.setAction(MusicService.ACTION_PLAYLIST_CHANGE);
                                musicDataIntent.putExtra("playlistId", playlistId);
                                context.startService(musicDataIntent);

                                break;
                        }
                        return true;
                    }
                });
            }
        });
    }

    public void setNowMusicPosition(int position) {
        nowMusicPosition = position;
    }

    public void setmenuButton(boolean visible) {
        this.visible = visible;
    }

    private static class ViewHolder {
        TextView title;
        TextView artist;
        ImageView image;
        ImageView nowMusic;
        ImageView menuButton;
    }
}
