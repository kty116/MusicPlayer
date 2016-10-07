package com.kka.mp.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.kka.mp.R;

public class SongChoiceCursorAdapter extends CursorAdapter {

    private boolean[] checkedList;

    public SongChoiceCursorAdapter(Context context, Cursor c) {
        super(context, c, false);

        checkedList = new boolean[c.getCount()];
    }

    public boolean[] getCheckedList() {
        return checkedList;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View convertView = LayoutInflater.from(context).inflate(R.layout.item_playlist2, parent, false);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.title = (TextView) convertView.findViewById(R.id.text_title1);
        viewHolder.artist = (TextView) convertView.findViewById(R.id.text_artist1);
        viewHolder.image = (ImageView) convertView.findViewById(R.id.image1);

        convertView.setTag(viewHolder);
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)));

        String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
        String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, uri);
        byte albumImage[] = retriever.getEmbeddedPicture();
//        BitmapFactory.Options options = new BitmapFactory.Options();

//        options.inSampleSize = 6;
        if (albumImage != null) {
//            Bitmap bitmap = BitmapFactory.decodeByteArray(albumImage, 0, albumImage.length, options);
//            viewHolder.image.setImageBitmap(bitmap);
            Glide.with(context).load(albumImage).into(viewHolder.image);
        }else {
            viewHolder.image.setImageResource(R.drawable.ic_music_image_24dp);
        }
        viewHolder.title.setText(title);
        viewHolder.artist.setText(artist);

        viewHolder.checkBox = (CheckBox) view.findViewById(R.id.song_check);
        viewHolder.checkBox.setClickable(false);
        viewHolder.checkBox.setFocusable(false);

        final int position = cursor.getPosition();

        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkedList[position]=isChecked;
            }
        });

        viewHolder.checkBox.setChecked(checkedList[position]);

    }

    private static class ViewHolder {
        CheckBox checkBox;
        TextView title;
        TextView artist;
        ImageView image;
    }
}
