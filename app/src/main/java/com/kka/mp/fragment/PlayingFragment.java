package com.kka.mp.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kka.mp.R;
import com.kka.mp.db.Facade;
import com.kka.mp.event.Event;
import com.kka.mp.event.PlayingMusicSignEvent;
import com.kka.mp.model.MusicInfo;
import com.kka.mp.service.MusicService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class PlayingFragment extends Fragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private TextView mTitle;
    private TextView mArtist;
    private SeekBar mSeekBar;
    private ImageButton mRepeatButton;
    private ImageButton mPreviousButton;
    private ImageButton mPlayButton;
    private ImageButton mNextButton;
    private ImageButton mPlaylistButton;
    private long mMusicId;
    private boolean mRepeatMode;
    private SharedPreferences mSharedPreferences;
    private TextView mDurationText;
    private MediaPlayer mMediaPlayer;
    private TextView mCurrentTimeText;
    private ImageView mAlbum;
    private String mPlaylistId;
    private int mMusicPosition;
    private MusicInfo mMusicInfo;
    private fragmentListener mListener;
    private Thread mMusicThread;
    private Handler mHandler;
    private Facade mFacade;
    private Cursor mMusicData;
    private PlayingMusicSignEvent mPlayingMusicSignEvent;

    public interface fragmentListener {
        void fragmentData(String playlistId, int position, int musicId);
    }

    public static PlayingFragment newInstance() {
        PlayingFragment playingFragment = new PlayingFragment();
        return playingFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        EventBus.getDefault().register(this);

        try {
            mListener = (fragmentListener) context;
        } catch (ClassCastException err) {
            throw new ClassCastException(context.toString() + "리스너 구현 x");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playing, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //플레잉이미지의 view들
        mCurrentTimeText = (TextView) view.findViewById(R.id.current_time);
        mDurationText = (TextView) view.findViewById(R.id.duration);
        mAlbum = (ImageView) view.findViewById(R.id.album_image);
        mTitle = (TextView) view.findViewById(R.id.title_text);
        mArtist = (TextView) view.findViewById(R.id.artist_text);
        mSeekBar = (SeekBar) view.findViewById(R.id.seekBar);
        mRepeatButton = (ImageButton) view.findViewById(R.id.button_repeat);
        mPreviousButton = (ImageButton) view.findViewById(R.id.button_previous);
        mPlayButton = (ImageButton) view.findViewById(R.id.button_play);
        mNextButton = (ImageButton) view.findViewById(R.id.button_next);
        mPlaylistButton = (ImageButton) view.findViewById(R.id.button_playlist);

        //플레잉이미지 set
        mRepeatButton.setOnClickListener(this);
        mPreviousButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setOnClickListener(this);
        mPlaylistButton.setOnClickListener(this);

        mFacade = new Facade(getActivity());

        if (mMediaPlayer == null) {

            mSharedPreferences = getActivity().getSharedPreferences("sharedFile", Context.MODE_PRIVATE);
            //서비스에서 저장된 가져와서 셋팅
            mMusicId = mSharedPreferences.getLong("musicId", -1);
            mPlaylistId = mSharedPreferences.getString("playlistId", null);
            mMusicPosition = mSharedPreferences.getInt("position", -1);

            //맨 처음
            if (mMusicId != -1) {

                mMusicInfo = new MusicInfo(getActivity(), mMusicId);

                if (mMusicInfo.getAlbumArt() != null) {
                    mAlbum.setImageBitmap(mMusicInfo.getAlbumArt());
                } else {
                    mAlbum.setImageResource(R.drawable.ic_music_image_24dp);
                }
                mArtist.setText(mMusicInfo.getArtist());
                mTitle.setText(mMusicInfo.getTitle());
            } else {
                //저장된 뮤직아이디 있을 때
                mMusicData = mFacade.queryAllMusic(getActivity());

                if (mMusicData != null) {
                    mMusicData.moveToFirst();
                    mMusicId = Long.parseLong(mMusicData.getString(mMusicData.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));

                    mMusicData.close();
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putInt("position", 0).commit();
                    editor.putString("playlistId", null).commit();

                    mMusicInfo = new MusicInfo(getActivity(), mMusicId);

                    if (mMusicInfo.getAlbumArt() != null) {
                        mAlbum.setImageBitmap(mMusicInfo.getAlbumArt());
                    } else {
                        mAlbum.setImageResource(R.drawable.ic_music_image_24dp);
                    }
                    mArtist.setText(mMusicInfo.getArtist());
                    mTitle.setText(mMusicInfo.getTitle());
                }
            }
        } else {
            //실행중인 음악 있을 때
            Intent MusicDataIntent = new Intent(getActivity(), MusicService.class);
            MusicDataIntent.setAction(MusicService.ACTION_MUSIC_DATA);
            getActivity().startService(MusicDataIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMediaPlayer != null) {
            mSeekBar.setMax(mMediaPlayer.getDuration());
            mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
            setCurrentTime();
        }
    }

    @Subscribe
    public void onEvent(Event event) {
        if (event instanceof PlayingMusicSignEvent) {

            mPlayingMusicSignEvent = (PlayingMusicSignEvent) event;
            mPlaylistId = mPlayingMusicSignEvent.getPlayListId();
            mMusicPosition = mPlayingMusicSignEvent.getPosition();
            mMusicId = mPlayingMusicSignEvent.getMusicId();
            mMediaPlayer = mPlayingMusicSignEvent.getMediaPlayer();

            //음악 정보 넣기
            mMusicInfo = new MusicInfo(getContext(), mMusicId);

            if (mMusicInfo.getAlbumArt() != null) {
                mAlbum.setImageBitmap(mMusicInfo.getAlbumArt());
            } else {
                mAlbum.setImageResource(R.drawable.ic_music_image_24dp);
            }
            mTitle.setText(mMusicInfo.getTitle());
            mArtist.setText(mMusicInfo.getArtist());

            if (mMediaPlayer.isPlaying()) {
                mPlayButton.setImageResource(R.drawable.ic_pause_circle_button);
            } else {
                mPlayButton.setImageResource(R.drawable.ic_play_circle_button);
            }

            mSeekBar.setMax((int) mPlayingMusicSignEvent.getDurationTime());
            mDurationText.setText(mPlayingMusicSignEvent.getDurationTimeText());
            mSeekBar.setProgress((int) mPlayingMusicSignEvent.getCurrentTime());
            mCurrentTimeText.setText(mPlayingMusicSignEvent.getCurrentTimeText());
            setCurrentTime();

        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_play:
                Intent playIntent = new Intent(getActivity(), MusicService.class);
                playIntent.setAction(MusicService.ACTION_PLAY);
                getActivity().startService(playIntent);
                break;

            case R.id.button_previous:
                Intent previousIntent = new Intent(getActivity(), MusicService.class);
                previousIntent.setAction(MusicService.ACTION_PREVIOUS);
                getActivity().startService(previousIntent);
                break;

            case R.id.button_next:
                Intent nextIntent = new Intent(getActivity(), MusicService.class);
                nextIntent.setAction(MusicService.ACTION_NEXT);
                getActivity().startService(nextIntent);
                break;

            case R.id.button_repeat:
                mRepeatMode = mSharedPreferences.getBoolean("repeat", false);
                if (mRepeatMode == false) {
                    //epeatMode false 일때
                    mRepeatButton.setImageResource(R.drawable.ic_repeat_single_song_button_t);
                    mSharedPreferences.edit().putBoolean("repeat", true).commit();
                } else {
                    //epeatMode true 일때
                    mRepeatButton.setImageResource(R.drawable.ic_repeat_single_song_button_f);
                    mSharedPreferences.edit().putBoolean("repeat", false).commit();
                }
                break;

            case R.id.button_playlist:
                mListener.fragmentData(mPlaylistId, mMusicPosition, (int) mMusicId);
                break;
        }
    }

    public void setCurrentTime() {
        mHandler = new Handler();
        mMusicThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mMediaPlayer.isPlaying()) {

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
                            mCurrentTimeText.setText(mPlayingMusicSignEvent.getCurrentTimeText());
                        }
                    });

                }
            }
        });
        mMusicThread.start();
    }

    //시크바 설정 메소드
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            //플레이어
            if (mMediaPlayer != null) {
                mMediaPlayer.seekTo(progress);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
