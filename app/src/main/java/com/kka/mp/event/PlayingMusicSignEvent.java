package com.kka.mp.event;

import android.media.MediaPlayer;

public class PlayingMusicSignEvent implements Event {
    private String playListId;
    private int position;
    private long musicId;
    private MediaPlayer mediaPlayer;

    public PlayingMusicSignEvent(String playListId, int position, long musicId, MediaPlayer mediaPlayer) {
        this.playListId = playListId;
        this.musicId = musicId;
        this.position = position;
        this.mediaPlayer = mediaPlayer;
    }

    public long getMusicId() {
        return musicId;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public long getDurationTime() {
        return mediaPlayer.getDuration();
    }

    public long getCurrentTime() {
        return mediaPlayer.getCurrentPosition();
    }

    public String getPlayListId() {
        return playListId;
    }

    public int getPosition() {
        return position;
    }

    public String getDurationTimeText() {

        long timeInMillisec = mediaPlayer.getDuration();
        long time = timeInMillisec / 1000;
        long hours = time / 3600;
        long minutes = (time - hours * 3600) / 60;
        long seconds = time - (hours * 3600 + minutes * 60);
        if (seconds < 10) {
            return minutes + ":0" + seconds;
        }
        return minutes + ":" + seconds;
    }

    public String getCurrentTimeText() {

        long timeInMillisec = mediaPlayer.getCurrentPosition();
        long time = timeInMillisec / 1000;
        long hours = time / 3600;
        long minutes = (time - hours * 3600) / 60;
        long seconds = time - (hours * 3600 + minutes * 60);
        if (seconds < 10) {
            return minutes + ":0" + seconds;
        }
        return minutes + ":" + seconds;

    }
}
