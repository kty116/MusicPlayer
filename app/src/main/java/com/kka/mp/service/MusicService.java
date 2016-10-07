package com.kka.mp.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.kka.mp.R;
import com.kka.mp.activity.MainActivity;
import com.kka.mp.db.Facade;
import com.kka.mp.db.MusicContract;
import com.kka.mp.event.PlayingMusicSignEvent;
import com.kka.mp.model.MusicInfo;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {

    public static final String ACTION_CLICK_MUSIC = "action_click_music";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_CLICK_NOTIBAR = "action_click_notibar";
    public static final String ACTION_CLOSE_NOTIBAR = "action_close_notibar";
    public static final String ACTION_MUSIC_DATA = "action_music_data";
    public static final String ACTION_PLAYLIST_CHANGE = "action_playlist_change";
    public static final String TAG = MusicService.class.getSimpleName();
    private MusicInfo mMusicInfo;
    private MediaPlayer mMediaPlayer;
    private long mMusicId;
    private RemoteViews mViews;
    private NotificationCompat.Builder mMusicNoti;
    private Uri mMusicUri;
    private SharedPreferences mSharedPreferences;
    private boolean mRepeatMode;
    private int mPlaylistIndex;
    private ArrayList<String> mPlaylist;
    private String mDBPlaylistId;
    private Facade mFacade;
    private Cursor mPlaylistData;
    private Cursor mAllMusicData;

    @Override
    public void onCreate() {
        super.onCreate();

        mMediaPlayer = new MediaPlayer();
        mFacade = new Facade(this);
        mPlaylist = new ArrayList<>();
        mMediaPlayer.setOnCompletionListener(this);

        if (getSharedPreferences("sharedFile", MODE_PRIVATE) != null) {
            mSharedPreferences = getSharedPreferences("sharedFile", Context.MODE_PRIVATE);
            mPlaylistIndex = mSharedPreferences.getInt("position", -1);
            mDBPlaylistId = mSharedPreferences.getString("playlistId", null);

            if (mDBPlaylistId != null) {
                mPlaylistData = mFacade.querySong(mDBPlaylistId);

                if (mPlaylistData != null) {
                    mPlaylistData.moveToFirst();

                    for (int i = 0; i < mPlaylistData.getCount(); i++) {
                        mPlaylist.add(i, mPlaylistData.getString(mPlaylistData.getColumnIndexOrThrow(MusicContract.SongEntry.COLUMN_NAME_MUSIC_ID)));
                        mPlaylistData.moveToNext();
                    }
                    mPlaylistData.close();
                }
            } else {
                mAllMusicData = mFacade.queryAllMusic(this);

                if (mAllMusicData != null) {
                    mAllMusicData.moveToFirst();

                    for (int i = 0; i < mAllMusicData.getCount(); i++) {
                        mPlaylist.add(i, mAllMusicData.getString(mAllMusicData.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                        mAllMusicData.moveToNext();
                    }
                    mAllMusicData.close();
                }
            }

            mMusicId = Long.parseLong(mPlaylist.get(mPlaylistIndex));
            mMusicUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mMusicId);

            try {
                mMediaPlayer.setDataSource(getApplicationContext(), mMusicUri);
                mMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null) {

            mMusicNoti = new NotificationCompat.Builder(this);
            mMusicNoti.setSmallIcon(R.drawable.ic_play_button);

            mViews = new RemoteViews(getPackageName(), R.layout.noti);
            mViews.setOnClickPendingIntent(R.id.button_play, playActionPendingIntent());
            mViews.setOnClickPendingIntent(R.id.button_exit, exitActionPendingIntent());
            mViews.setOnClickPendingIntent(R.id.button_next, nextActionPendingIntent());
            mViews.setOnClickPendingIntent(R.id.button_previous, previousActionPendingIntent());
            mViews.setOnClickPendingIntent(R.id.album_image, clickNotiPendingIntent());

            switch (intent.getAction()) {
                case ACTION_CLOSE_NOTIBAR:
                    mMediaPlayer.stop();
                    EventBus.getDefault().post(new PlayingMusicSignEvent(mDBPlaylistId, mPlaylistIndex, mMusicId, mMediaPlayer));
                    stopForeground(true);
                    mMusicInfo.getAlbumArt().recycle();

                    mSharedPreferences = getSharedPreferences("sharedFile", Context.MODE_PRIVATE);

                    SharedPreferences.Editor sharedPreferencesEdit = mSharedPreferences.edit();
                    sharedPreferencesEdit.putLong("musicId", mMusicId).commit();
                    if (mSharedPreferences.getString("playlistId", null) != null) {
                        sharedPreferencesEdit.remove("playlistId").commit();
                    }
                    sharedPreferencesEdit.putString("playlistId", mDBPlaylistId).commit();
                    sharedPreferencesEdit.putInt("position", mPlaylistIndex).commit();

                    return START_NOT_STICKY;

                case ACTION_MUSIC_DATA:
                    EventBus.getDefault().post(new PlayingMusicSignEvent(mDBPlaylistId, mPlaylistIndex, mMusicId, mMediaPlayer));
                    return START_NOT_STICKY;

                case ACTION_CLICK_MUSIC:

                    mMediaPlayer.stop();
                    mMediaPlayer.reset();

                    //DB Id값
                    mDBPlaylistId = intent.getStringExtra("playlistId");
                    //playlist
                    mPlaylist = (ArrayList<String>) intent.getSerializableExtra("playlist");

                    if (mDBPlaylistId != null) {
                        mPlaylistIndex = intent.getIntExtra("position", -1);
                        mMusicId = Long.parseLong(mPlaylist.get(mPlaylistIndex));
                        Log.d(TAG, "onStartCommand: "+ mMusicId);
                    } else {
                        //searchMusic 값
                        long searchMusicId = intent.getLongExtra("searchMusicId", -1);
                        //music 값
                        mMusicId = intent.getLongExtra("musicId", -1);
                        if (searchMusicId != -1) {
                            mMusicId = searchMusicId;
                        }
                        mPlaylistIndex = mPlaylist.indexOf(String.valueOf(mMusicId));
                    }
                    mMusicUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mMusicId);
                    try {
                        mMediaPlayer.setDataSource(getApplicationContext(), mMusicUri);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;

                case ACTION_PLAY:

                    if (mMediaPlayer.isPlaying()) {
                        //멈춤
                        mMediaPlayer.pause();
                        mViews.setImageViewResource(R.id.button_play, R.drawable.ic_pause_button);
                    } else {
                        //재생
                        mMediaPlayer.start();
                        mViews.setImageViewResource(R.id.button_play, R.drawable.ic_play_button);
                    }
                    break;

                case ACTION_NEXT:

                    mMediaPlayer.stop();
                    mMediaPlayer.reset();
                    startNextMusic();
                    try {
                        mMediaPlayer.setDataSource(getApplicationContext(), mMusicUri);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case ACTION_PREVIOUS:

                    mMediaPlayer.stop();
                    mMediaPlayer.reset();
                    startPreviousMusic();
                    try {
                        mMediaPlayer.setDataSource(getApplicationContext(), mMusicUri);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;

                case ACTION_PLAYLIST_CHANGE:

                    mPlaylist = new ArrayList<>();

                    mDBPlaylistId = intent.getStringExtra("playlistId");
                    if (mDBPlaylistId != null) {
                        mPlaylistData = mFacade.querySong(mDBPlaylistId);

                        if (mPlaylistData != null) {
                            mPlaylistData.moveToFirst();

                            for (int i = 0; i < mPlaylistData.getCount(); i++) {
                                mPlaylist.add(i, mPlaylistData.getString(mPlaylistData.getColumnIndexOrThrow(MusicContract.SongEntry.COLUMN_NAME_MUSIC_ID)));
                                mPlaylistData.moveToNext();
                            }
                            mPlaylistData.close();
                        }
                    } else {
                        mAllMusicData = mFacade.queryAllMusic(this);

                        if (mAllMusicData != null) {
                            mAllMusicData.moveToFirst();

                            for (int i = 0; i < mAllMusicData.getCount(); i++) {
                                mPlaylist.add(i, mAllMusicData.getString(mAllMusicData.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                                mAllMusicData.moveToNext();
                            }
                            mAllMusicData.close();
                        }
                    }
                    if (!mPlaylist.contains(String.valueOf(mMusicId))) {
                        //재생중인 뮤직아이디가 플레이리스트에 없으면
                        mMediaPlayer.reset();
                        mMusicId = Long.parseLong(mPlaylist.get(mPlaylistIndex));

                        mMusicUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mMusicId);

                        try {
                            mMediaPlayer.setDataSource(getApplicationContext(), mMusicUri);
                            mMediaPlayer.prepare();
                            mMediaPlayer.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
            }
        }

        mMusicInfo = new MusicInfo(getApplicationContext(), mMusicId);

        if (mMusicInfo.getArtist() != null) {
            mViews.setImageViewBitmap(R.id.album_image, mMusicInfo.getAlbumArt());
        } else {
            mViews.setImageViewResource(R.id.album_image, R.drawable.ic_music_image_24dp);
        }

        if (mMediaPlayer.isPlaying()) {
            mViews.setImageViewResource(R.id.button_play, R.drawable.ic_pause_button);
        } else {
            mViews.setImageViewResource(R.id.button_play, R.drawable.ic_play_button);
        }

        mViews.setTextViewText(R.id.title, mMusicInfo.getTitle());
        mViews.setTextViewText(R.id.artist, mMusicInfo.getArtist());
        EventBus.getDefault().post(new PlayingMusicSignEvent(mDBPlaylistId, mPlaylistIndex, mMusicId, mMediaPlayer));
        mMusicNoti.setContent(mViews);

        startForeground(1, mMusicNoti.build());

        return START_NOT_STICKY;
    }

    private PendingIntent clickNotiPendingIntent() {
        Intent clickNotiIntent = new Intent(this, MainActivity.class);
        clickNotiIntent.setAction(ACTION_CLICK_NOTIBAR);
        PendingIntent clickNotiPending = PendingIntent.getActivity(this, 4, clickNotiIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return clickNotiPending;
    }

    private PendingIntent playActionPendingIntent() {
        Intent playIntent = new Intent(this, MusicService.class);
        playIntent.setAction(ACTION_PLAY);
        PendingIntent playPending = PendingIntent.getService(getApplicationContext(), 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return playPending;
    }

    private PendingIntent exitActionPendingIntent() {
        Intent exitIntent = new Intent(this, MusicService.class);
        exitIntent.setAction(ACTION_CLOSE_NOTIBAR);
        PendingIntent exitPending = PendingIntent.getService(getApplicationContext(), 1, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return exitPending;
    }

    private PendingIntent nextActionPendingIntent() {
        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getService(getApplicationContext(), 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return nextPending;
    }

    private PendingIntent previousActionPendingIntent() {
        Intent previousIntent = new Intent(this, MusicService.class);
        previousIntent.setAction(ACTION_PREVIOUS);
        PendingIntent previousPending = PendingIntent.getService(getApplicationContext(), 3, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return previousPending;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

        //if 리핏 모드 켜있으면 그 노래 반복으로 설정 on off

        mSharedPreferences = getSharedPreferences("sharedFile", Context.MODE_PRIVATE);
        mRepeatMode = mSharedPreferences.getBoolean("repeat", false);

        //searchMusic id값

        if (mRepeatMode == true) {
            mMediaPlayer.start();
        } else {
            mMediaPlayer.reset();
            startNextMusic();
            try {
                mMediaPlayer.setDataSource(getApplicationContext(), mMusicUri);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mMusicInfo = new MusicInfo(getApplicationContext(), mMusicId);

        if (mMusicInfo.getAlbumArt() != null) {
            mViews.setImageViewBitmap(R.id.album_image, mMusicInfo.getAlbumArt());
        } else {
            mViews.setImageViewResource(R.id.album_image, R.drawable.ic_music_image_24dp);
        }
        mViews.setTextViewText(R.id.title, mMusicInfo.getTitle());
        mViews.setTextViewText(R.id.artist, mMusicInfo.getArtist());

        EventBus.getDefault().post(new PlayingMusicSignEvent(mDBPlaylistId, mPlaylistIndex, mMusicId, mMediaPlayer));

        mMusicNoti.setContent(mViews);

        startForeground(1, mMusicNoti.build());

    }

    private void startNextMusic() {

        if (mPlaylist != null) {
            if (mPlaylistIndex < mPlaylist.size() - 1) {
                mPlaylistIndex++;
            } else {
                //인덱스가 mPlaylistdata.getCount() -1이거나 mPlaylistdata.getCount() -1보다 클 때
                mPlaylistIndex = 0;
            }
            mMusicId = Long.parseLong(mPlaylist.get(mPlaylistIndex));

            mMusicUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mMusicId);
        }
    }

    private void startPreviousMusic() {
        if (mPlaylist != null) {
            if (mPlaylistIndex > 0) {
                mPlaylistIndex--;
            } else {
                //인덱스가 0이거나 0보다 작을 때
                mPlaylistIndex = mPlaylist.size() - 1;
            }
            mMusicId = Long.parseLong(mPlaylist.get(mPlaylistIndex));

            mMusicUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mMusicId);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mMediaPlayer.release();
        mMediaPlayer = null;
    }

}
