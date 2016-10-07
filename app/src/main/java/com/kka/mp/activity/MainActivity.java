package com.kka.mp.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.stetho.Stetho;
import com.kka.mp.R;
import com.kka.mp.db.Facade;
import com.kka.mp.dialog.PlaylistDialog;
import com.kka.mp.dialog.SongChoiceDialog;
import com.kka.mp.event.Event;
import com.kka.mp.event.PlayingMusicSignEvent;
import com.kka.mp.event.SearchMusicEvent;
import com.kka.mp.fragment.AllListFragment;
import com.kka.mp.fragment.PlayListFragment;
import com.kka.mp.fragment.PlayListSongFragment;
import com.kka.mp.fragment.PlayingFragment;
import com.kka.mp.fragment.SearchMusicFragment;
import com.kka.mp.model.MusicInfo;
import com.kka.mp.service.MusicService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener, PlayListFragment.PlayListFragmentDataListener, PlayListSongFragment.PlayListSongFragmentDataListener, View.OnClickListener, AllListFragment.AllListFragmentDataListener, SearchView.OnQueryTextListener, PlayingFragment.fragmentListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private TextView mTextSong;
    private TextView mTextSinger;
    private ImageView mImage;
    private ImageView mPlayButton;
    private MusicPagerAdapter mPagerAdapter;
    private ImageButton mPreviousButton;
    private ImageButton mNextButton;
    private long mMusicId;
    private MusicInfo mMusicInfo;
    private LinearLayout mMusicController;
    private SharedPreferences mSharedPreferences;
    private LinearLayout mFragmentParent;
    private PlayingFragment mPlayingFragment;
    private LinearLayout mActivityParent;
    private String mDBPlaylistId;
    private int mMusicPosition;
    private SearchView mSearchView;
    private MediaPlayer mMediaPlayer;
    private SearchMusicFragment mSearchMusicFragment;
    private MenuItem mMenuItem;
    private Facade mFacade;
    private PlayingMusicSignEvent playingMusicSignEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);
        Stetho.initializeWithDefaults(this);
        mFacade = new Facade(this);

        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mFragmentParent = (LinearLayout) findViewById(R.id.parent_fragment);
        mActivityParent = (LinearLayout) findViewById(R.id.parent_activity);

        mMusicController = (LinearLayout) findViewById(R.id.music_controller);
        mTextSong = (TextView) findViewById(R.id.song);
        mTextSinger = (TextView) findViewById(R.id.singer);

        mImage = (ImageView) findViewById(R.id.image);
        mPlayButton = (ImageButton) findViewById(R.id.button_play);
        mPreviousButton = (ImageButton) findViewById(R.id.button_previous);
        mNextButton = (ImageButton) findViewById(R.id.button_next);

        mImage.setOnClickListener(this);
        mMusicController.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mPreviousButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);

        mPlayingFragment = PlayingFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.playing_fragment_layout, mPlayingFragment).commit();

        mTabLayout.addTab(mTabLayout.newTab().setIcon(R.drawable.ic_all_list).setText("전체"));
        mTabLayout.addTab(mTabLayout.newTab().setIcon(R.drawable.ic_play_list).setText("재생목록"));

        mTabLayout.setOnTabSelectedListener(this);

        mPagerAdapter = new MusicPagerAdapter(this.getSupportFragmentManager());

        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

        if (mMediaPlayer == null) {

            mSharedPreferences = getSharedPreferences("sharedFile", MODE_PRIVATE);
            //서비스에서 저장된 가져와서 셋팅
            mMusicId = mSharedPreferences.getLong("musicId", -1);

            mDBPlaylistId = mSharedPreferences.getString("playlistId", null);
            mMusicPosition = mSharedPreferences.getInt("position", -1);

            if (mMusicId != -1) {

                MusicInfo sharedMusicInfo = new MusicInfo(this, mMusicId);

                if (sharedMusicInfo.getAlbumArt() != null) {
                    mImage.setImageBitmap(sharedMusicInfo.getAlbumArt());
                } else {
                    mImage.setImageResource(R.drawable.ic_music_image_24dp);
                }
                mTextSinger.setText(sharedMusicInfo.getArtist());
                mTextSong.setText(sharedMusicInfo.getTitle());
            } else {
                Cursor cursor = mFacade.queryAllMusic(this);

                cursor.moveToFirst();
                mMusicId = Long.parseLong(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));

                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putInt("position", 0).commit();
                editor.putString("playlistId", null).commit();

                MusicInfo sharedMusicInfo = new MusicInfo(this, mMusicId);

                if (sharedMusicInfo.getAlbumArt() != null) {
                    mImage.setImageBitmap(sharedMusicInfo.getAlbumArt());
                }
                mTextSinger.setText(sharedMusicInfo.getArtist());
                mTextSong.setText(sharedMusicInfo.getTitle());
            }
        } else {
            Intent MusicDataIntent = new Intent(this, MusicService.class);
            MusicDataIntent.setAction(MusicService.ACTION_MUSIC_DATA);
            startService(MusicDataIntent);
        }
    }

    @Override
    public void onBackPressed() {
        if (mSearchView.isIconified() == false) {
            getSupportFragmentManager().beginTransaction().remove(mSearchMusicFragment).commit();
            mSearchView.setIconified(true);
            mMusicController.setVisibility(View.VISIBLE);
        } else if (mActivityParent.getVisibility() == View.GONE) {
            mActivityParent.setVisibility(View.VISIBLE);
            mMenuItem.setVisible(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_play:
                Intent playIntent = new Intent(this, MusicService.class);
                playIntent.setAction(MusicService.ACTION_PLAY);
                startService(playIntent);
                break;

            case R.id.button_previous:
                Intent previousIntent = new Intent(this, MusicService.class);
                previousIntent.setAction(MusicService.ACTION_PREVIOUS);
                startService(previousIntent);
                break;

            case R.id.button_next:
                Intent nextIntent = new Intent(this, MusicService.class);
                nextIntent.setAction(MusicService.ACTION_NEXT);
                startService(nextIntent);
                break;

            case R.id.image:
                mActivityParent.setVisibility(View.GONE);
                mMenuItem.setVisible(false);
                break;
        }
    }

    @Subscribe
    public void onEvent(Event event) {
        if (event instanceof PlayingMusicSignEvent) {
            playingMusicSignEvent = (PlayingMusicSignEvent) event;
            mMusicId = playingMusicSignEvent.getMusicId();
            mDBPlaylistId = playingMusicSignEvent.getPlayListId();
            mMusicPosition = playingMusicSignEvent.getPosition();
            mMediaPlayer = playingMusicSignEvent.getMediaPlayer();
            mMusicInfo = new MusicInfo(this, mMusicId);
            if (mMusicInfo.getAlbumArt() != null) {
                mImage.setImageBitmap(mMusicInfo.getAlbumArt());
            } else {
                mImage.setImageResource(R.drawable.ic_music_image_24dp);
            }
            mTextSinger.setText(mMusicInfo.getArtist());
            mTextSong.setText(mMusicInfo.getTitle());
            mMediaPlayer = playingMusicSignEvent.getMediaPlayer();
            if (mMediaPlayer.isPlaying()) {
                mPlayButton.setImageResource(R.drawable.ic_pause_button);
            } else {
                mPlayButton.setImageResource(R.drawable.ic_play_button);
            }
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

    @Override
    public void PlayListMusicData(long playlistId) {

        PlayListSongFragment playListSongFragment = PlayListSongFragment.newInstance(playlistId);
        getSupportFragmentManager().beginTransaction().addToBackStack("3").add(R.id.parent_fragment, playListSongFragment).commit();
    }

    @Override
    public void playlistSongData(long playlistId) {

        Intent dialogIntent = new Intent(this, SongChoiceDialog.class);
        dialogIntent.putExtra("playlistId", playlistId);
        startActivity(dialogIntent);
    }

    /**
     * AllListFragment 리스트뷰의 아이템 클릭 리시버
     *
     * @param playlist
     * @param position
     * @param id
     */
    @Override
    public void AllListMusicData(ArrayList playlist, int position, long id) {

        Intent ClickMusicIntent = new Intent(this, MusicService.class);
        ClickMusicIntent.setAction(MusicService.ACTION_CLICK_MUSIC);
        ClickMusicIntent.putExtra("musicId", id);
        ClickMusicIntent.putExtra("playlist", playlist);
        startService(ClickMusicIntent);

    }

    @Override
    public void fragmentData(String playlistId, int position, int musicId) {
        Intent playlistIntent = new Intent(this, PlaylistDialog.class);
        playlistIntent.putExtra("playlistId", playlistId);
        playlistIntent.putExtra("position", position);
        playlistIntent.putExtra("musicId", musicId);
        startActivity(playlistIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.search_menu, menu);

        mMenuItem = menu.findItem(R.id.acition_seach);

        mSearchView = (SearchView) mMenuItem.getActionView();

        mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchMusicFragment = SearchMusicFragment.newInstance(null);
                getSupportFragmentManager().beginTransaction().addToBackStack("0").replace(R.id.parent_activity, mSearchMusicFragment).commit();
                mMusicController.setVisibility(View.GONE);
            }
        });

        mSearchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        EventBus.getDefault().post(new SearchMusicEvent(query));
        mSearchView.setIconified(true);
        mSearchView.clearFocus();

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        return true;
    }

private class MusicPagerAdapter extends FragmentPagerAdapter {

    public MusicPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new AllListFragment();
            default:
                return new PlayListFragment();
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}