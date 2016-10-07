package com.kka.mp.fragment;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.kka.mp.R;
import com.kka.mp.adapter.PlayListCursorAdapter;
import com.kka.mp.db.DBHelper;
import com.kka.mp.db.Facade;
import com.kka.mp.db.MusicContract;

public class PlayListFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    public static final String TAG = PlayListFragment.class.getSimpleName();
    private ListView mListview;
    private FloatingActionButton mPlaylistAddButton;
    private EditText mPlaylistEdit;
    private PlayListCursorAdapter mAdapter;
    private DBHelper mHelper;

    private Facade mFacade;
    private PlayListFragmentDataListener mListener;
    private Cursor mPlaylistName;

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, final long id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("재생");
        builder.setMessage("해당 재생 목록을 삭제하시겠습니까?");
        builder.setPositiveButton("네", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mFacade.deletePlaylist(MusicContract.PlaylistEntry._ID + "=?", new String[]{String.valueOf(id)});
                mFacade.deleteSong(MusicContract.SongEntry.COLUMN_NAME_PLAYLIST_ID + "=?", new String[]{String.valueOf(id)});
                mAdapter.swapCursor(mFacade.queryPlaylist());
                mAdapter.notifyDataSetChanged();

            }
        });
        builder.setNegativeButton("아니오", null);
        builder.show();

        return true;
    }

    public interface PlayListFragmentDataListener {
        void PlayListMusicData(long playlistId);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (PlayListFragmentDataListener) context;
        } catch (ClassCastException err) {
            throw new ClassCastException(context.toString() + "리스너 구현 x");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListview = (ListView) view.findViewById(R.id.play_list);
        mPlaylistAddButton = (FloatingActionButton) view.findViewById(R.id.floating_button);
        mPlaylistAddButton.setOnClickListener(this);

        mFacade = new Facade(getActivity());
        mHelper = new DBHelper(getActivity());
        mPlaylistName = mFacade.queryPlaylist();

        if (mPlaylistName != null) {
            mAdapter = new PlayListCursorAdapter(getActivity(), mPlaylistName);
            mListview.setAdapter(mAdapter);
        }
        mListview.setOnItemClickListener(this);
        mListview.setOnItemLongClickListener(this);
    }

    @Override
    public void onClick(View v) {
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_image, null);
        mPlaylistEdit = (EditText) dialogView.findViewById(R.id.playlist_name_edit);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("재생 목록 생성");
        builder.setView(dialogView);
        builder.setNegativeButton("취소", null);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String playlistName = mPlaylistEdit.getText().toString();

                SQLiteDatabase db = mHelper.getReadableDatabase();
                Cursor samePlaylistQuery = db.query(MusicContract.PlaylistEntry.PLAYLIST_TABLE_NAME,
                        null,
                        MusicContract.PlaylistEntry.COLUMN_NAME_NAME + "=?",
                        new String[]{playlistName.trim()},
                        null,
                        null,
                        null);

                if (samePlaylistQuery != null) {

                    int nameCount = samePlaylistQuery.getCount();
                    if (nameCount == 0) {

                        if (!TextUtils.isEmpty(playlistName)) {
                            db = mHelper.getWritableDatabase();

                            ContentValues values = new ContentValues();
                            values.put(MusicContract.PlaylistEntry.COLUMN_NAME_NAME, playlistName);
                            db.insert(MusicContract.PlaylistEntry.PLAYLIST_TABLE_NAME,
                                    null,
                                    values);
                            mAdapter.swapCursor(mFacade.queryPlaylist());
                            mAdapter.notifyDataSetChanged();
                            mListview.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

                        } else {
                            Toast.makeText(getActivity(), "플레이리스트 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "같은 이름의 재생 목록이 존재합니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        builder.show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mAdapter.getCursor().close();
    }



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mListener.PlayListMusicData(id);
    }
}
