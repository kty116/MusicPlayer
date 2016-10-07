package com.kka.mp.db;

import android.provider.BaseColumns;

public class MusicContract {

    public static abstract class PlaylistEntry implements BaseColumns {
        public static final String PLAYLIST_TABLE_NAME = "playlist";
        public static final String COLUMN_NAME_NAME = "playlist_name";

        public static final String PLAYLIST_NAME_ENTRIES = "CREATE TABLE "+ PLAYLIST_TABLE_NAME+
        "("+_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
                COLUMN_NAME_NAME+" TEXT NOT NULL);";
    }

    public static abstract class SongEntry implements BaseColumns {
        public static final String SONG_TABLE_NAME = "song";
        public static final String COLUMN_NAME_PLAYLIST_ID = "playlist_id";
        public static final String COLUMN_NAME_MUSIC_ID = "music_id";

        public static final String SONG_NAME_ENTRIES = "CREATE TABLE " + SONG_TABLE_NAME+
                "("+_ID+ " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_NAME_PLAYLIST_ID+ " INTEGER," +
                COLUMN_NAME_MUSIC_ID+ " TEXT);";

    }
}
