package com.kka.mp.dialog;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.kka.mp.R;
import com.kka.mp.adapter.AllListCursorAdapter;
import com.kka.mp.adapter.PlayListSongCursorAdapter;
import com.kka.mp.db.DBHelper;
import com.kka.mp.db.Facade;
import com.kka.mp.db.MusicContract;
import com.kka.mp.event.Event;
import com.kka.mp.event.PlayingMusicSignEvent;
import com.kka.mp.service.MusicService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

public class PlaylistDialog extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private TextView mPlaylistNameText;
    private ListView mSongList;
    private String mPlaylistId;
    private int mMusicPosition;
    private PlayListSongCursorAdapter mDBAdapter;
    private AllListCursorAdapter mAdapter;
    private Facade mFacade;
    private Cursor mMusicData;
    private ArrayList<String> mPlaylist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list_dialog);
        EventBus.getDefault().register(this);

        getWindow().setWindowAnimations(android.R.style.Animation_Toast);

        //playing fragment에 있는 디비플레이리스트아이디 넘겨받고

        Intent intent = getIntent();
        mPlaylistId = intent.getStringExtra("playlistId");
        mMusicPosition = intent.getIntExtra("position", -1);

        mPlaylistNameText = (TextView) findViewById(R.id.playlist_name_text);
        mSongList = (ListView) findViewById(R.id.song_list);

        mFacade = new Facade(this);
        mPlaylist = new ArrayList<>();
        // unknown 값 빼고 나오게
        if (mPlaylistId != null) {
            //값 있음
            //아이디에 네임
            DBHelper mHelper = new DBHelper(this);
            SQLiteDatabase db = mHelper.getReadableDatabase();
            Cursor playlistName = db.query(MusicContract.PlaylistEntry.PLAYLIST_TABLE_NAME,
                    null, MusicContract.PlaylistEntry._ID + "=?", new String[]{mPlaylistId}, null, null, null);

            if (playlistName != null) {
                mPlaylistNameText.setText(playlistName.getString(playlistName.getColumnIndexOrThrow(MusicContract.PlaylistEntry.COLUMN_NAME_NAME)));
            }
            playlistName.close();

            //같은 값의 id
            mMusicData = mFacade.querySong(mPlaylistId);
            if (mMusicData != null) {
                mDBAdapter = new PlayListSongCursorAdapter(this, mMusicData);
                mDBAdapter.setmenuButton(false);
                mSongList.setAdapter(mDBAdapter);
                mDBAdapter.setNowMusicPosition(mMusicPosition);

                mMusicData.moveToFirst();
                for (int i = 0; i < mMusicData.getCount(); i++) {
                    mPlaylist.add(i, mMusicData.getString(mMusicData.getColumnIndexOrThrow(MusicContract.SongEntry.COLUMN_NAME_MUSIC_ID)));
                    mMusicData.moveToNext();
                }
            }
        } else {
            mMusicData = mFacade.queryAllMusic(this);

            if (mMusicData != null) {
                mAdapter = new AllListCursorAdapter(this, mMusicData);
                mAdapter.setmenuButton(false);
                mSongList.setAdapter(mAdapter);
                mAdapter.setNowMusicPosition(mMusicPosition);

                mMusicData.moveToFirst();
                for (int i = 0; i < mMusicData.getCount(); i++) {
                    mPlaylist.add(i, mMusicData.getString(mMusicData.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                    mMusicData.moveToNext();
                }
            }
        }

        mSongList.setOnItemClickListener(this);
    }

    //클릭된 뷰값
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_CLICK_MUSIC);
        serviceIntent.putExtra("playlist", mPlaylist);
//        serviceIntent.putExtra("position", position);
        serviceIntent.putExtra("musicId", id);
        serviceIntent.putExtra("playlistId", mPlaylistId);
        startService(serviceIntent);

        if (mPlaylistId != null) {
            mDBAdapter.setNowMusicPosition(position);
        } else {
            mAdapter.setNowMusicPosition(position);
        }
        mAdapter.notifyDataSetChanged();

    }

    @Subscribe
    public void onEvent(Event event) {
        if (event instanceof PlayingMusicSignEvent) {
            PlayingMusicSignEvent playingMusicSignEvent = (PlayingMusicSignEvent) event;
            int currentMusicPosition = playingMusicSignEvent.getPosition();
            String currentPlaylistId = playingMusicSignEvent.getPlayListId();
            if (currentPlaylistId == null) {
                mAdapter.setNowMusicPosition(currentMusicPosition);
            } else {
                mAdapter.setNowMusicPosition(-1);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
        mMusicData.close();
    }
}
