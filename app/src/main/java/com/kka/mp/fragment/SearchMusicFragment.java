package com.kka.mp.fragment;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.kka.mp.R;
import com.kka.mp.adapter.AllListCursorAdapter;
import com.kka.mp.db.Facade;
import com.kka.mp.event.Event;
import com.kka.mp.event.SearchMusicEvent;
import com.kka.mp.service.MusicService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;


public class SearchMusicFragment extends Fragment implements AdapterView.OnItemClickListener {

    public String[] projection = {MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM};

    private ListView mListview;
    private ArrayList playlist;
    private AllListCursorAdapter mAdapter;
    private String mWord;
    private Cursor mSearchMusicData;
    private Facade mFacade;

    public static SearchMusicFragment newInstance(String word) {
        SearchMusicFragment fragment = new SearchMusicFragment();
        Bundle args = new Bundle();
        args.putString("word", word);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
        mSearchMusicData.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mWord = getArguments().getString("word");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mFacade = new Facade(getActivity());
        mListview = (ListView) view.findViewById(R.id.all_list);
        playlist = new ArrayList();

        mSearchMusicData = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,

                MediaStore.Audio.Media.TITLE + " LIKE '%" + mWord + "%' " + "OR " +
                        MediaStore.Audio.Media.ARTIST + " LIKE '%" + mWord + "%'",
                null,
                MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC");

        mAdapter = new AllListCursorAdapter(getActivity(), mSearchMusicData);

        mListview.setAdapter(mAdapter);

        mListview.setOnItemClickListener(this);

        Cursor mAllMusicData = mFacade.queryAllMusic(getActivity());

        if (mAllMusicData != null) {
            mAllMusicData.moveToFirst();

            for (int i = 0; i < mAllMusicData.getCount(); i++) {
                playlist.add(i, mAllMusicData.getString(mAllMusicData.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                mAllMusicData.moveToNext();
            }
            mAllMusicData.close();
        }
    }

    @Subscribe
    public void onEvent(Event event) {
        if (event instanceof SearchMusicEvent) {
            SearchMusicEvent searchMusicEvent = (SearchMusicEvent) event;
            mWord = searchMusicEvent.getWord().trim();

            Cursor mSearchMusicData = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Audio.Media.TITLE + " LIKE '%" + mWord + "%' " + "OR " +
                            MediaStore.Audio.Media.ARTIST + " LIKE '%" + mWord + "%'",
                    null,
                    MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC");

            mAdapter.swapCursor(mSearchMusicData);
            mAdapter.notifyDataSetChanged();

        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Intent clickMusicIntent = new Intent(getActivity(), MusicService.class);
        clickMusicIntent.putExtra("searchMusicId", id);
        clickMusicIntent.putExtra("playlist", playlist);
        clickMusicIntent.setAction(MusicService.ACTION_CLICK_MUSIC);
        getActivity().startService(clickMusicIntent);
    }
}
