package com.hibike;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import static com.hibike.Keys.ReplayMods.REPLAY_PLAYLIST;
import static com.hibike.Keys.Settings.*;

public class Settings {
    Context context;
    SharedPreferences settings;
    public  Settings(Context appContext){
        context=appContext;
        settings=context.getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
    }

    public void setPlayingSong(File song){
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PLAYING_SONG,song.getAbsolutePath());
        editor.apply();
    }
    public void setPlaylist(String playlistName,File[] playlist){
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PLAYING_PLAYLIST_NAME,playlistName);
        String playlistString="";
        while (playlist==null) playlist=getAllSongs();
        for (File song:playlist) playlistString+=">>"+song.getAbsolutePath();
        editor.putString(PLAYING_PLAYLIST,playlistString);
        editor.apply();
    }
    public void setPlaylistsNames(ArrayList<String> playlistsNames){
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(PLAYLISTS_NAMES,new HashSet<>(playlistsNames));
        editor.apply();
    }
    public void setPlaylists(ArrayList<String> playlists){
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(PLAYLISTS,new HashSet<>(playlists));
        editor.apply();
    }
    static public void setSelectedSongs(ArrayList<Song> selectedSongsFiles){
        ArrayList<String> selectedSongs=new ArrayList<>();
        for(int count=0;count<selectedSongsFiles.size();count++){
            selectedSongs.add(selectedSongsFiles.get(count).getAbsolutePath());
        }
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(SELECTED_SONGS,new HashSet<>(selectedSongs));
        editor.apply();
    }
    public void setOpenedPlaylist(String playlistName){
        SharedPreferences.Editor editor = settings.edit();
        if (playlistName.equals(context.getString(R.string.all_songs))) playlistName=ALL_SONGS;
        editor.putString(OPENED_PLAYLIST,playlistName);
        editor.apply();
    }
    public void setIsPlay(boolean isPlay){
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(IS_PLAY,isPlay);
        editor.apply();
    }
    public void setIsRandom(boolean isRandom){
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(IS_RANDOM,isRandom);
        editor.apply();
    }
    public void setReplayMode(String replayMode){
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(REPLAY_MODE,replayMode);
        editor.apply();
    }
    public void setNotFirstLoad(){
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(FIRST_LOAD,false);
        editor.apply();
    }


    public File[] getAllSongs(){
        File[] allSongs=null;
        if (settings.contains(ALL_SONGS)){
            String[] paths=settings.getString(ALL_SONGS,null).split(">>");
            allSongs=new File[paths.length];
            for (int count=0;count<paths.length;count++){
                allSongs[count]=new File(paths[count]);
            }
        }else getAllSongs();
        return allSongs;
    }
    public File getPlayingSong(){
        if (settings.contains(PLAYING_SONG)) return new File(settings.getString(PLAYING_SONG,null));
        else return getAllSongs()[0];
    }
    public File[] getPlayingPlayList(){
        ArrayList<File> playlist=new ArrayList<>();
        if (settings.contains(PLAYING_PLAYLIST)){
            String[] paths=settings.getString(PLAYING_PLAYLIST,null).split(">>");
            for (int count=0;count<paths.length;count++){
                File song=new File(paths[count]);
                if (song.exists()&&song.isFile()) playlist.add(song);
            }
            if (playlist.size()!=0) {
                return playlist.toArray(new File[playlist.size()]);}
            else return getAllSongs();
        }else{
            return getAllSongs();
        }
    }
    public ArrayList<String> getPlaylistsNames(){
        ArrayList<String> playlistsNames=new ArrayList<>();
        if (settings.contains(PLAYLISTS_NAMES)) playlistsNames.addAll(settings.getStringSet(PLAYLISTS_NAMES,null));
        return playlistsNames;
    }
    public ArrayList<String> getPlaylists(){
        ArrayList<String> playlists=new ArrayList<>();
        if (settings.contains(PLAYLISTS)) playlists.addAll(settings.getStringSet(PLAYLISTS,null));
        return playlists;
    }
    public String getPlayingPlayListName(){
        String playlistName=ALL_SONGS;
        if (settings.contains(PLAYING_PLAYLIST_NAME)){
            playlistName=settings.getString(PLAYING_PLAYLIST_NAME,null);
        }
        return playlistName;
    }
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
    static public ArrayList<Song> getSelectedSongs(){
        ArrayList<Song> selectedSongs=new ArrayList<>();
        if (settings.contains(SELECTED_SONGS)){
            ArrayList<String> selectedSongsString=new ArrayList<>();
            selectedSongsString.addAll(settings.getStringSet(SELECTED_SONGS,null));
            if (!selectedSongsString.isEmpty()){
                for (int count=0;count<selectedSongsString.size();count++){
                    selectedSongs.add(new File(selectedSongsString.get(count)));
                }
            }
        }
        return selectedSongs;
    }
    public String getOpenedPlaylist(){
        String playlistName=ALL_SONGS;
        if (settings.contains(OPENED_PLAYLIST)){
            playlistName=settings.getString(OPENED_PLAYLIST,null);
        }
        return playlistName;
    }
    public boolean getIsPlay(){
        boolean isPlay=false;
        if (settings.contains(IS_PLAY)) isPlay=settings.getBoolean(IS_PLAY,false);
        return isPlay;
    }
    public int getTime(){
        int time=0;
        if (settings.contains(SONG_TIME)) time=settings.getInt(SONG_TIME,0);
        return time;
    }
    public boolean getIsRandom(){
        boolean isRandom=false;
        if (settings.contains(IS_RANDOM)) isRandom=settings.getBoolean(IS_RANDOM,false);
        return isRandom;
    }
    public String getReplayMode(){
        String replayMode=REPLAY_PLAYLIST;
        if (settings.contains(REPLAY_MODE)) replayMode=settings.getString(REPLAY_MODE,null);
        return replayMode;
    }
    public boolean getIsFirstLoad(){
        boolean isFirstLoad=true;
        if (settings.contains(FIRST_LOAD)) isFirstLoad=settings.getBoolean(FIRST_LOAD,false);
        return isFirstLoad;
    }
}
