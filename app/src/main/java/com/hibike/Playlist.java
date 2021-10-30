package com.hibike;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.hibike.Keys.Songs.ALL_SONGS_PLAYLIST_ID;
import static com.hibike.Keys.Songs.CURRENT_PLAYLIST_ID;
import static com.hibike.Keys.Songs.PLAYLISTS;
import static com.hibike.Keys.Songs.PLAYLISTS_ID;
import static com.hibike.Keys.Songs.PLAYLISTS_NAMES;
import static com.hibike.Keys.Songs.PLAYLIST_CURRENT;
import static com.hibike.Keys.Songs.PLAYLIST_SONGS;
import static com.hibike.Keys.Songs.PLAYLIST_NAME;
import static com.hibike.Keys.Songs.SELECTED_SONGS_PLAYLIST_ID;

public class Playlist extends ArrayList<Integer> {
    private int id;
    private String name;
    public int currentSong=1;
    public int songCounter=0;

    private Context context;
    Playlist (Context _context){
        super();
        id=SELECTED_SONGS_PLAYLIST_ID;
        context=_context;
    }
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


    public boolean add(File file) throws IOException {
        Song song=new Song(file,context);
        return super.add(song.getId());
    }

    /*public void play() throws NoSongFileException {
        play(currentSong);
    }

    public void play(int position) throws NoSongFileException {
        new Song(get(position),context).play();
    }*/

    public boolean move(int curPosition, int newPosition){
        if (curPosition<newPosition) newPosition--;
        int song=get(curPosition);

        remove(curPosition);
        add(newPosition, song);

        return true;
    }

    public int next(){
        currentSong=currentSong+1>songCounter ? currentSong+1 : 1;
        return currentSong;
    }
    public int prev(){
        currentSong=currentSong>1 ? currentSong-1 : songCounter;
        return currentSong;
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
    //
    //Sorting methods
    //
    public void sortByName() throws NullPointerException, NoSongFileException {
        ArrayList<Song> playlist=new ArrayList<Song>();
        ArrayList<String> nameArray=new ArrayList<String>();
        for (int count=0; count<songCounter;count++) {
            Song song=new Song(get(count), context);
            playlist.add(song);
            nameArray.add(song.getName());
        }
        ArrayList<String> cloneArray=new ArrayList<String>();
        cloneArray.addAll(nameArray);
        Collections.sort(cloneArray);
        this.clear();

        for(int count=1;count<=songCounter;count++){
            String name=cloneArray.get(count);
            int idx=nameArray.indexOf(name);
            nameArray.set(idx,null);
            this.add(playlist.get(idx).getId());
        }
    }

    public void shake(){
        int cur=this.get(currentSong);
        Collections.shuffle(this);
        currentSong=this.indexOf(cur);
    }

    public int getCurrentSong() {
        return this.get(currentSong);
    }
    public int getId(){return id;}
    public String getName(){return name;}

    public void setName(String newName){
        name=newName;
    }

    public void makePlaying(){
        id=CURRENT_PLAYLIST_ID;
    }
    public File[] toFileArray() {
        File[] array=new File[songCounter];
        try { for(int count=0; count<songCounter;count++) array[count]=new Song(get(count+1), context).toFile(); } catch (NoSongFileException e) { e.printStackTrace(); }
        return array;
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
