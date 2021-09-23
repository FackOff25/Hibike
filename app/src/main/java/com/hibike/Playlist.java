package com.hibike;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.hibike.Keys.Songs.PLAYLISTS;
import static com.hibike.Keys.Songs.PLAYLISTS_ID;
import static com.hibike.Keys.Songs.PLAYLISTS_NAMES;
import static com.hibike.Keys.Songs.PLAYLIST_SONGS;
import static com.hibike.Keys.Songs.PLAYLIST_NAME;

public class Playlist extends ArrayList<Song> {
    private int id;
    public String name;
    public int currentSong=1;
    public int songCounter=0;

    private Context context;

    Playlist(int _id, Context _context) throws Exception{
        super();
        context=_context;
        CopyOnWriteArraySet<String> playlists=new CopyOnWriteArraySet<String>();
        SharedPreferences settings=context.getSharedPreferences(PLAYLISTS,Context.MODE_PRIVATE);
        settings.getStringSet(PLAYLISTS_NAMES, playlists);

        if(!playlists.contains(id)) throw new Exception("There is no such playlist");

        id=_id;
        settings.getString(id+PLAYLIST_NAME, name);

        CopyOnWriteArraySet<String> temp_songs=new CopyOnWriteArraySet<String>();
        settings.getStringSet(id+PLAYLIST_SONGS,temp_songs);
        ArrayList<String> songs = new ArrayList<String>(temp_songs);
        songCounter=songs.size();
        for (int count=1; count<=songCounter; count++) add(Integer.parseInt(songs.get(count)));
    }

    Playlist(String _name, Context _context){
        super();
        name=_name;
        context=_context;
        id=makeId();
    }

    public boolean add(int id) {
        Song song;
        try{
            song=new Song(id, context);
        }catch (NoMoreSongException e){
            return false;
        }catch (NoSongFileException e){
            return false;
        }
        return super.add(song);
    }

    @Override
    public boolean add(Song song) {
        songCounter++;
        return super.add(song);
    }

    public void addNext(Song song){
        add(currentSong+1, song);
    }

    public void play(){
        play(currentSong);
    }

    public void play(int position){
        get(position).play();
    }

    public boolean move(int curPosition, int newPosition){
        if (curPosition<newPosition) newPosition--;
        Song song=get(curPosition);

        remove(curPosition);
        add(newPosition, song);

        return true;
    }

    public void save(){
        CopyOnWriteArraySet<String> songs=new CopyOnWriteArraySet<String>();
        CopyOnWriteArraySet<String> playlists=new CopyOnWriteArraySet<String>();
        for (int count=1; count<=songCounter; count++)  songs.add(Integer.toString(get(count).id));

        SharedPreferences settings=context.getSharedPreferences(PLAYLISTS,Context.MODE_PRIVATE);
        settings.getStringSet(PLAYLISTS_ID, playlists);

        if(!playlists.contains(id)) playlists.add(Integer.toString(id));

        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(PLAYLISTS_ID, playlists);
        editor.putString(id+PLAYLIST_NAME, name);
        editor.putStringSet(id+PLAYLIST_SONGS, new CopyOnWriteArraySet<String>(songs));
        editor.apply();
    }

    public void delete(){
        CopyOnWriteArraySet<String> playlists=new CopyOnWriteArraySet<String>();
        SharedPreferences settings=context.getSharedPreferences(PLAYLISTS,Context.MODE_PRIVATE);
        settings.getStringSet(PLAYLISTS_ID, playlists);

        if(!playlists.contains(id)) return;

        SharedPreferences.Editor editor = settings.edit();
        playlists.remove(id);
        editor.putStringSet(PLAYLISTS_ID, playlists);
        editor.remove(id+PLAYLIST_NAME);
        editor.remove(id+PLAYLIST_SONGS);
        editor.apply();
    }

    private int makeId(){
        int number= (int) Math.random()*1000+1;
        CopyOnWriteArraySet<String> playlists=new CopyOnWriteArraySet<String>();
        SharedPreferences settings=context.getSharedPreferences(PLAYLISTS,Context.MODE_PRIVATE);
        settings.getStringSet(PLAYLISTS_ID, playlists);
        while(playlists.contains(number)) number++;
        return number;
    }
}
