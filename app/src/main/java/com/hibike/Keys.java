package com.hibike;

public class Keys {
    public interface ReplayMods{
        int NO_REPLAY=0;
        int REPLAY_PLAYLIST=1;
        int REPLAY_SONG=2;
    }

    public interface Songs{
        String SONGS_SETTINGS_NAME="com.hibikeplayer.settings.songsettingsname";
        String PLAYLISTS="playlists";
        String PLAYLISTS_ID="playlistsid";
        String PLAYLISTS_NAMES="playlistsnames";
        String PLAYLIST_SONGS="_playlistsongs";
        String PLAYLIST_NAME="_playlistname";
        int ALL_SONGS_PLAYLIST_ID=-1;
        int CURRENT_PLAYLIST_ID=-2;
        int SELECTED_SONGS_PLAYLIST_ID=-3;
        String PLAYLIST_CURRENT="Текущий";
        String SONG_PATH="_PATH";
        String SONG_NAME="_NAME";
        String SONG_AUTHOR="_AUTHOR";
        String SONG_ALBUM="_ALBUM";
        String SONG_DURATION="_DURATION";

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
        String IS_PLAY="com.hibikeplayer.settings.isplay";
        String IS_RANDOM="com.hibikeplayer.settings.israndom";
        String FIRST_LOAD="com.hibikeplayer.settings.allsongs.israndom";
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
