package com.kka.mp.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.kka.mp.R;
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

public class PlayListSongFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private TextView mPlaylistName;
    private ListView mSongList;
    private String mPlaylistId;
    private DBHelper mHelper;
    private PlayListSongFragmentDataListener mListener;
    private PlayListSongCursorAdapter mAdapter;
    private SQLiteDatabase mDB;
    private Facade mFacade;
    private Cursor mMusicData;
    private ArrayList<String> mPlaylist;

    public interface PlayListSongFragmentDataListener {
        void playlistSongData(long musicId);
    }

    public static PlayListSongFragment newInstance(long playlistId) {

        PlayListSongFragment fragment = new PlayListSongFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("playlistId", playlistId);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPlaylistId = String.valueOf(getArguments().getLong("playlistId"));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_song, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPlaylist = new ArrayList<>();
        mFacade = new Facade(getActivity());
        mPlaylistName = (TextView) view.findViewById(R.id.playlist_name_text);
        mSongList = (ListView) view.findViewById(R.id.song_list);
        view.findViewById(R.id.add_song_button).setOnClickListener(this);
        mSongList.setOnItemClickListener(this);

        mHelper = new DBHelper(getActivity());
        mDB = mHelper.getReadableDatabase();

        //아이디에 네임
        Cursor playlistName = mDB.query(MusicContract.PlaylistEntry.PLAYLIST_TABLE_NAME,
                null, MusicContract.PlaylistEntry._ID + "=?", new String[]{mPlaylistId}, null, null, null);

        if (playlistName != null) {
            playlistName.moveToFirst();
            mPlaylistName.setText(playlistName.getString(playlistName.getColumnIndexOrThrow(MusicContract.PlaylistEntry.COLUMN_NAME_NAME)));
        }
        playlistName.close();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (PlayListSongFragmentDataListener) context;
        } catch (ClassCastException err) {
            throw new ClassCastException(context.toString() + "리스너 구현 x");
        }
        EventBus.getDefault().register(this);

//        Intent intent = new Intent(getActivity(), MusicService.class);
//        intent.setAction("mediaPlayerData");
//        getActivity().startService(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        mMusicData = mFacade.querySong(mPlaylistId);

        if (mMusicData != null) {
            mAdapter = new PlayListSongCursorAdapter(getActivity(), mMusicData);
            mSongList.setAdapter(mAdapter);

            mMusicData.moveToFirst();
            for (int i = 0; i < mMusicData.getCount(); i++) {
                mPlaylist.add(i, mMusicData.getString(mMusicData.getColumnIndexOrThrow(MusicContract.SongEntry.COLUMN_NAME_MUSIC_ID)));
                mMusicData.moveToNext();
            }
        }
    }

    @Subscribe
    public void onEvent(Event event) {
        if (event instanceof PlayingMusicSignEvent) {
            PlayingMusicSignEvent playingMusicSignEvent = (PlayingMusicSignEvent) event;
            int currentMusicPosition = playingMusicSignEvent.getPosition();
            String currentPlaylistId = playingMusicSignEvent.getPlayListId();
            MediaPlayer mediaPlayer = playingMusicSignEvent.getMediaPlayer();

            if (mediaPlayer.isPlaying()) {
                if (currentPlaylistId.equals(mPlaylistId)) {
                    mAdapter.setNowMusicPosition(currentMusicPosition);
                    mAdapter.notifyDataSetChanged();
                }
            } else {
                mAdapter.setNowMusicPosition(-1);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
        mListener = null;
        mAdapter.setNowMusicPosition(-1);
        mAdapter.notifyDataSetChanged();
        mMusicData.close();
    }

    @Override
    public void onClick(View v) {
        mListener.playlistSongData(Long.parseLong(mPlaylistId));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Intent clickMusicIntent = new Intent(getActivity(), MusicService.class);
        clickMusicIntent.putExtra("playlistId", mPlaylistId);
        clickMusicIntent.putExtra("position", position);
        clickMusicIntent.putExtra("playlist", mPlaylist);
        clickMusicIntent.setAction(MusicService.ACTION_CLICK_MUSIC);
        getActivity().startService(clickMusicIntent);

    }
}

