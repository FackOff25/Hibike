package com.hibike;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.hibike.Keys.Songs.PLAYLISTS;
import static com.hibike.Keys.Songs.PLAYLISTS_ID;
import static com.hibike.Keys.Songs.PLAYLISTS_NAMES;
import static com.hibike.Keys.Songs.PLAYLIST_CURRENT;
import static com.hibike.Keys.Songs.PLAYLIST_SONGS;
import static com.hibike.Keys.Songs.PLAYLIST_NAME;

public class Playlist extends ArrayList<Integer> {
    private int id;
    public String name;
    public int currentSong=1;
    public int songCounter=0;

    private Context context;

    Playlist(int _id, Context _context) throws NoSuchPlaylistException{
        super();
        id=_id;
        context=_context;
        CopyOnWriteArraySet<String> playlists=new CopyOnWriteArraySet<String>();
        SharedPreferences settings=context.getSharedPreferences(PLAYLISTS,Context.MODE_PRIVATE);
        settings.getStringSet(PLAYLISTS_NAMES, playlists);

        if(!playlists.contains(id)) throw new NoSuchPlaylistException(id);

        name=settings.getString(id+PLAYLIST_NAME, null);

        ArrayList<String> songs = new ArrayList<String>(settings.getStringSet(id+PLAYLIST_SONGS, null));
        songCounter=songs.size();
        for (int count=1; count<=songCounter; count++) add(Integer.parseInt(songs.get(count)));
    }

    Playlist(String _name, Context _context){
        super();
        name=_name;
        context=_context;
        id=makeId();
    }

    public void addNext(int _id){
        add(currentSong+1, _id);
    }

    public void play() throws NoSongFileException {
        play(currentSong);
    }

    public void play(int position) throws NoSongFileException {
        new Song(get(position),context).play();
    }

    public boolean move(int curPosition, int newPosition){
        if (curPosition<newPosition) newPosition--;
        int song=get(curPosition);

        remove(curPosition);
        add(newPosition, song);

        return true;
    }

    public void save(){
        CopyOnWriteArraySet<String> songs=new CopyOnWriteArraySet<String>();
        CopyOnWriteArraySet<String> playlists;
        for (int count=1; count<=songCounter; count++)  songs.add(Integer.toString(get(count)));

        SharedPreferences settings=context.getSharedPreferences(PLAYLISTS,Context.MODE_PRIVATE);
        playlists = (CopyOnWriteArraySet<String>) settings.getStringSet(PLAYLISTS_ID, null);

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
        playlists = (CopyOnWriteArraySet<String>) settings.getStringSet(PLAYLISTS_ID, null);

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
        playlists = (CopyOnWriteArraySet<String>) settings.getStringSet(PLAYLISTS_ID, null);
        while(playlists.contains(number)) number++;
        return number;
    }
}
