package com.kka.mp.dialog;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kka.mp.R;
import com.kka.mp.adapter.SongChoiceCursorAdapter;
import com.kka.mp.db.DBHelper;
import com.kka.mp.db.Facade;
import com.kka.mp.db.MusicContract;

import java.util.ArrayList;
import java.util.List;

public class SongChoiceDialog extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private TextView mChoiceCount;
    private ListView mSongList;
    private String mPlaylistId;
    private SongChoiceCursorAdapter mAdapter;
    private DBHelper mHelper;
    private ContentValues values;
    private CheckBox checked;
    private List<String> mCheckedListIndexList = new ArrayList();
    private int mCount = 0;
    private Facade mFacade;
    private Cursor mMusicData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_choicedialog);

        getWindow().setWindowAnimations(android.R.style.Animation_Toast);

        Intent intent = getIntent();
        mPlaylistId = String.valueOf(intent.getLongExtra("playlistId", -1));

        mChoiceCount = (TextView) findViewById(R.id.playlist_name_text);
        mSongList = (ListView) findViewById(R.id.song_list);

        mFacade = new Facade(this);

        findViewById(R.id.negative_text).setOnClickListener(this);
        findViewById(R.id.positive_text).setOnClickListener(this);

        // unknown 값 빼고 나오게

        mMusicData = mFacade.queryAllMusic(this);

        if(mMusicData!=null) {
            mAdapter = new SongChoiceCursorAdapter(this, mMusicData);
            mSongList.setAdapter(mAdapter);
        }
        mSongList.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.negative_text:

                break;
            case R.id.positive_text:

                boolean[] checkedList = mAdapter.getCheckedList();
                for (int i = 0; i < checkedList.length; i++) {

                    if (checkedList[i] == true) {
                        long musicId = mAdapter.getItemId(i);
                        mCheckedListIndexList.add(String.valueOf(musicId));
                        //인덱스리스트에 뮤직아이디저장
                    }
                }

                mHelper = new DBHelper(this);
                SQLiteDatabase db = mHelper.getWritableDatabase();
                db.beginTransaction();

                for (int i = 0; i < mCheckedListIndexList.size(); i++) {
                    values = new ContentValues();
                    values.put(MusicContract.SongEntry.COLUMN_NAME_PLAYLIST_ID, mPlaylistId);
                    values.put(MusicContract.SongEntry.COLUMN_NAME_MUSIC_ID, mCheckedListIndexList.get(i));

                    db.insert(MusicContract.SongEntry.SONG_TABLE_NAME, null, values);
                }

                db.setTransactionSuccessful();
                db.endTransaction();

                Toast.makeText(this, mCount + "개의 노래가 저장 되었습니다", Toast.LENGTH_SHORT).show();
                //플레이리스트아이디랑 musicID 인서트트

                break;//                아이디값 저장
        }

        finish();
    }

    //클릭된 뷰값
    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

        checked = (CheckBox) view.findViewById(R.id.song_check);

        if (checked.isChecked() == false) {
            checked.setChecked(true);
            mCount++;
        } else {
            checked.setChecked(false);
            mCount--;
        }
        mChoiceCount.setText(mCount + "개 선택");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMusicData.close();
    }
}
