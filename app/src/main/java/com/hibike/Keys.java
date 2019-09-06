package com.hibike;

public class Keys {
    public interface ReplayMods{
        String NO_REPLAY="without replay";
        String REPLAY_PLAYLIST="replay playlist";
        String REPLAY_SONG="replay song";
    }
    public interface Settings {
        String SETTINGS_NAME="com.hibikeplayer.settings.settingsname";
        String SONG_TIME="com.hibikeplayer.settings.songtime";
        String PLAYING_SONG="com.hibikeplayer.settings.playingsong";
        String PLAYING_PLAYLIST_NAME="com.hibikeplayer.settings.playingplaylistname";
        String PLAYING_PLAYLIST="com.hibikeplayer.settings.playingplaylist";
        String OPENED_PLAYLIST="com.hibikeplayer.settings.allsongs.openedplaylist";
        String REPLAY_MODE="com.hibikeplayer.settings.replaymode";
        String PLAYLISTS_NAMES ="com.hibikeplayer.settings.playlistsnames";
        String PLAYLISTS="com.hibikeplayer.settings.playlist";
        String SELECTED_SONGS="com.hibikeplayer.settings.selectedsongs";
        String ALL_SONGS="com.hibikeplayer.settings.allsongs";
        String IS_PLAY="com.hibikeplayer.settings.allsongs.isplay";
        String IS_RANDOM="com.hibikeplayer.settings.allsongs.israndom";
    }

    public interface Notification {
        int NOTIFICATION_ID = 101;
        String CHANNEL="Hibike notification channel";
        String START_FOREGROUND="Start foreground";
        String STOP_FOREGROUND="Stop foreground";
        String NEXT_SONG="Next song";
        String PREVIOUS_SONG="Previous song";
        String PLAY_SONG="Play/pause song";
        String PLAY_THIS_SONG="Play this song";
        String SEEK_TO="Seek to";

        String PATH_EXTRA="Path extra";
        String TIME_EXTRA="Time extra";
    }

    public interface Broadcast {
        String BROADCAST_ACTION="com.hibikeplayer.background.bradcast";
        String UPDATE_UI="com.hibikeplayer.background.updateui";
        String PLAY_CHANGED="com.hibikeplayer.background.playchanged";
        String SET_TIME="com.hibikeplayer.background.settime";
        String CLOSE="com.hibikeplayer.background.close";
    }
}
