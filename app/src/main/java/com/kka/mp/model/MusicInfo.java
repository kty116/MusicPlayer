package com.kka.mp.model;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;

public class MusicInfo {

    private long musicId;
    private Bitmap albumArt;
    private String title;
    private String artist;
    private Uri musicUri;

    public MusicInfo(Context context, long musicId) {

        this.musicId = musicId;

        this.musicUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, musicUri);

        this.artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        this.title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

        byte[] albumArt = retriever.getEmbeddedPicture();
        if (albumArt != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            if (albumArt.length > 400000) {
                options.inSampleSize = 4;
                this.albumArt = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length, options);
            }else if(albumArt.length > 200000){
                options.inSampleSize = 2;
                this.albumArt = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length, options);
        }else {
                this.albumArt = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
            }
        }
    }

    public long getMusicId() {
        return musicId;
    }

    public void setMusicId(long musicId) {
        this.musicId = musicId;
    }

    public Bitmap getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(Bitmap albumArt) {
        this.albumArt = albumArt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
