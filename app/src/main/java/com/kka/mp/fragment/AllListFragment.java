package com.kka.mp.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.kka.mp.R;
import com.kka.mp.adapter.AllListCursorAdapter;
import com.kka.mp.db.Facade;
import com.kka.mp.event.Event;
import com.kka.mp.event.PlayingMusicSignEvent;
import com.kka.mp.service.MusicService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;


public class AllListFragment extends Fragment implements AdapterView.OnItemClickListener {
    public static final String TAG = AllListFragment.class.getSimpleName();
    private ListView mListview;
    private AllListCursorAdapter mAdapter;
    private AllListFragmentDataListener mListener;
    private Facade mFacade;
    private Cursor mAllMusicData;
    private ArrayList<String> mPlaylist;

    public interface AllListFragmentDataListener {
        void AllListMusicData(ArrayList playlist, int position, long id);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all, container, false);
    }

    @Override
    public void onAttach(Context context) {

        EventBus.getDefault().register(this);

        Intent intent = new Intent(getActivity(), MusicService.class);
        intent.setAction(MusicService.ACTION_MUSIC_DATA);
        getActivity().startService(intent);

        super.onAttach(context);

        try {
            mListener = (AllListFragmentDataListener) context;
        } catch (ClassCastException err) {
            throw new ClassCastException(context.toString() + "리스너 구현 x");
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListview = (ListView) view.findViewById(R.id.all_list);

        mFacade = new Facade(getActivity());
        mPlaylist = new ArrayList<>();

        mListview.setOnItemClickListener(this);

        mAllMusicData = mFacade.queryAllMusic(getActivity());
        if (mAllMusicData != null) {
            mAdapter = new AllListCursorAdapter(getActivity(), mAllMusicData);
            mListview.setAdapter(mAdapter);
            mAllMusicData.moveToFirst();
            for (int i = 0; i < mAllMusicData.getCount(); i++) {
                mPlaylist.add(i, mAllMusicData.getString(mAllMusicData.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                mAllMusicData.moveToNext();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mListener.AllListMusicData(mPlaylist, position, id);
    }

    @Subscribe
    public void onEvent(Event event) {
        if (event instanceof PlayingMusicSignEvent) {
            PlayingMusicSignEvent playingMusicSignEvent = (PlayingMusicSignEvent) event;
            int currentMusicPosition = playingMusicSignEvent.getPosition();
            String currentPlaylistId = playingMusicSignEvent.getPlayListId();
            MediaPlayer mediaPlayer = playingMusicSignEvent.getMediaPlayer();

            if (mediaPlayer.isPlaying()) {
                if (currentPlaylistId == null) {
                    mAdapter.setNowMusicPosition(currentMusicPosition);
                } else {
                    mAdapter.setNowMusicPosition(-1);
                }
                mAdapter.notifyDataSetChanged();
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
        mAllMusicData.close();

    }
}













