package com.hibike;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.hibike.Keys.ReplayMods.REPLAY_PLAYLIST;
import static com.hibike.Keys.ReplayMods.REPLAY_SONG;
import static com.hibike.Keys.Settings.*;
import static com.hibike.Keys.Songs.ALL_SONGS_PLAYLIST_ID;
import static com.hibike.Keys.Songs.CURRENT_PLAYLIST_ID;
import static com.hibike.Keys.Songs.PLAYLISTS;
import static com.hibike.Keys.Songs.PLAYLISTS_ID;
import static com.hibike.Keys.Songs.PLAYLIST_NAME;
import static com.hibike.Keys.Songs.PLAYLIST_SONGS;
import static com.hibike.Keys.Songs.SELECTED_SONGS_PLAYLIST_ID;

public class Settings {
    final private Context context;
    final private SharedPreferences settings;
    final private SharedPreferences.Editor editor;

    Settings(Context appContext){
        context=appContext;
        settings=context.getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
        editor=settings.edit();
    }

    public void setPlayingSong(int id){
        editor.putInt(PLAYING_SONG,id);
        editor.apply();
    }

    public void setCurrentPlaylist(Playlist playlist){
        playlist.makePlaying();
        playlist.save();
    }

    //Sets which playlist is playing
    public void setOpenedPlaylist(int id){
        editor.putInt(OPENED_PLAYLIST, id);
        editor.apply();
    }

    public int getPlayingSong(){
        if (settings.contains(PLAYING_SONG)) return Integer.parseInt(settings.getString(PLAYING_SONG,null));
        else return getAllSongs().get(0);
    }

    public Playlist getAllSongs() throws NoSuchPlaylistException {
        return new Playlist(ALL_SONGS_PLAYLIST_ID,context);
    }

    public Playlist getPlayingPlayList(){
        try {
            return new Playlist(CURRENT_PLAYLIST_ID, context);
        }catch (NoSuchPlaylistException e){
            return new Playlist(ALL_SONGS_PLAYLIST_ID,context);
        }
    }

    public  ArrayList<Playlist> getPlaylists(){
        ArrayList<String> playlistsIds=new ArrayList<>();
        ArrayList<Playlist> playlists=null;
        if (settings.contains(PLAYLISTS)) {
            playlistsIds.addAll(settings.getStringSet(PLAYLISTS,null));
            playlists=new ArrayList<>();
            for (int count=1;count<=playlistsIds.size(); count++) playlists.add(new Playlist(Integer.parseInt(playlistsIds.get(count)),context));
        }
        return playlists;
    }

    //Setting and getting selecting songs as playlist
    public void setSelectedSongs(Playlist selectedSongs){
        CopyOnWriteArraySet<String> songs=new CopyOnWriteArraySet<String>();
        CopyOnWriteArraySet<String> playlists;
        for (int count=1; count<=selectedSongs.size(); count++)  songs.add(Integer.toString(selectedSongs.get(count)));

        SharedPreferences settings=context.getSharedPreferences(PLAYLISTS,Context.MODE_PRIVATE);
        playlists = (CopyOnWriteArraySet<String>) settings.getStringSet(PLAYLISTS_ID, null);

        editor.putStringSet(PLAYLISTS_ID, playlists);
        editor.putStringSet(CURRENT_PLAYLIST_ID+PLAYLIST_SONGS, new CopyOnWriteArraySet<String>(songs));
        editor.apply();
    }
    public void setNoSelectedSongs(){
        CopyOnWriteArraySet<String> playlists;

        SharedPreferences settings=context.getSharedPreferences(PLAYLISTS,Context.MODE_PRIVATE);
        playlists = (CopyOnWriteArraySet<String>) settings.getStringSet(PLAYLISTS_ID, null);

        editor.putStringSet(PLAYLISTS_ID, playlists);
        editor.putStringSet(CURRENT_PLAYLIST_ID+PLAYLIST_SONGS, new CopyOnWriteArraySet<String>());
        editor.apply();
    }

    public Playlist getSelectedSongs(){
        return new Playlist(SELECTED_SONGS_PLAYLIST_ID, context);
    }

    public int getOpenedPlaylist(){
        int playlistName=playlistName=settings.getInt(OPENED_PLAYLIST, ALL_SONGS_PLAYLIST_ID);
        return playlistName;
    }
    //Setting and getting time of playing song
    public void setTime(int time){
        editor.putInt(SONG_TIME, time);
        editor.apply();
    }

    public int getTime(){
        return settings.getInt(SONG_TIME,0);
    }

    //Setting and getting a state of "Play Random" button
    public void setIsRandom(boolean isRandom){
        editor.putBoolean(IS_RANDOM,isRandom);
        editor.apply();
    }
    public boolean getIsRandom(){
        boolean isRandom=settings.getBoolean(IS_RANDOM,false);
        return isRandom;
    }

    //Setting and getting a state of "Replay mode" button
    public void setReplayMode(int replayMode){
        editor.putInt(REPLAY_MODE, replayMode);
        editor.apply();
    }
    public int getReplayMode(){
        int replayMode=settings.getInt(REPLAY_MODE,REPLAY_SONG);
        return replayMode;
    }

    //Setting and getting first load parameter
    public void setNotFirstLoad(){
        editor.putBoolean(FIRST_LOAD,false);
        editor.apply();
    }
    public boolean getIsFirstLoad(){
        boolean isFirstLoad=settings.getBoolean(FIRST_LOAD,true);
        return isFirstLoad;
    }

    //Put info is any song playing. Needed for notification
    public void setIsPlay(boolean isPlay){
        editor.putBoolean(IS_PLAY, isPlay);
        editor.apply();
    }
    public boolean getIsPlay(){
        boolean isPlay=false;
        if (settings.contains(IS_PLAY)) isPlay=settings.getBoolean(IS_PLAY,false);
        return isPlay;
    }

    /*
    public File[] getPlayListByName(String playlistName){
        File[] playlist;
        if (settings.contains(PLAYLISTS_NAMES)) {
            if (playlistName.equals(ALL_SONGS)) return getAllSongs();
            if (settings.contains(PLAYLISTS)){
                ArrayList<String> playlistsNames=new ArrayList<>();
                playlistsNames.addAll(settings.getStringSet(PLAYLISTS_NAMES,null));
                ArrayList<String> playlists=new ArrayList<>();
                playlists.addAll(settings.getStringSet(PLAYLISTS,null));
                String[] paths=playlists.get(playlistsNames.indexOf(playlistName)).split(">>");
                playlist=new File[paths.length-1];
                for (int count=0;count<playlist.length;count++){
                    playlist[count]=new File(paths[count+1]);
                }
                return playlist;
            }
        }
        return getAllSongs();
    }
     */

}
