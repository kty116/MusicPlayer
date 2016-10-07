package com.kka.mp.event;

public class SearchMusicEvent implements Event {
    private String word;

    public SearchMusicEvent(String word) {
        this.word = word;
    }

    public String getWord() {
        return word;
    }
}
