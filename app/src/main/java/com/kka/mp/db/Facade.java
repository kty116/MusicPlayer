package com.kka.mp.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.MediaStore;

public class Facade {

    private DBHelper mHelper;
    private String mSelections = MediaStore.Audio.Media.ARTIST + "!= ? AND "
            + MediaStore.Audio.Media.IS_RINGTONE + "= ? AND "
            + MediaStore.Audio.Media.IS_NOTIFICATION + "= ?";

    private String[] mProjection = {MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM};

    public Facade(Context context) {
        this.mHelper = new DBHelper(context);
    }

    /**
     * playlistId 값에 맞는 song data 가져오기
     */
    public Cursor querySong(String playlistId) {
        SQLiteDatabase db = mHelper.getReadableDatabase();

        Cursor cursor = db.query(MusicContract.SongEntry.SONG_TABLE_NAME,
                null, MusicContract.SongEntry.COLUMN_NAME_PLAYLIST_ID + "=?", new String[]{playlistId}, null, null, null);
        return cursor;

    }

    /**
     * playlist에 저장된 song 삭제
     */

    public int deleteSong(String whereClause, String[] whereArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        return db.delete(MusicContract.SongEntry.SONG_TABLE_NAME,
                whereClause,
                whereArgs);

    }

    /**
     * playlist 가져오기
     */
    public Cursor queryPlaylist() {
        SQLiteDatabase db = mHelper.getReadableDatabase();

        Cursor cursor = db.query(MusicContract.PlaylistEntry.PLAYLIST_TABLE_NAME,
                null, null, null, null, null, null);
        return cursor;
    }

    /**
     * playlist 삭제
     */

    public int deletePlaylist(String whereClause, String[] whereArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();

        return db.delete(MusicContract.PlaylistEntry.PLAYLIST_TABLE_NAME,
                whereClause,
                whereArgs);
    }

    /**
     *
     * @param context
     * @return cursor
     */
    public Cursor queryAllMusic(Context context) {
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                mProjection,
                mSelections,
                new String[]{MediaStore.UNKNOWN_STRING, "0", "0"},
                MediaStore.Audio.Media.TITLE + " ASC");
        return cursor;
    }
}

