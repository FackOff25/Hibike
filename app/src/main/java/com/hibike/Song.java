package com.hibike;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;


import static com.hibike.Keys.Exceptions.NO_SONG_ANYMORE;
import static com.hibike.Keys.Exceptions.NO_SONG_ID;
import static com.hibike.Keys.Songs.*;

public class Song {
    //Technical
    public int id;
    public String path;
    final private Context context;

    //For user
    public String name= NameSpace.Russian.SONG_NAME;
    public String author=NameSpace.Russian.SONG_AUTHOR;
    public String album=NameSpace.Russian.SONG_ALBUM;
    public long duration=0;

    public Song(File song, Context _context) throws IOException {
        context=_context;
        id=makeId();

        PrimaryMusicData musicData=new PrimaryMusicData();
        MediaMetadataRetriever data=new MediaMetadataRetriever();

        musicData.primarySearch(song, context);
        name=musicData.musicName;
        author=musicData.musicAuthor;
        album=musicData.musicAlbum;
        data.setDataSource(song.getAbsolutePath());
        String durStr=data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        duration = Integer.parseInt(durStr);
    }

    public Song(String songPath, Context context) throws IOException {
       this(new File(songPath), context);
    }

    public Song(int _id,Context _context) throws Exception{
        id=_id;
        context=_context;

        SharedPreferences settings=context.getSharedPreferences(SONGS_SETTINGS_NAME,Context.MODE_PRIVATE);
        CopyOnWriteArraySet<String> parametersSet=new CopyOnWriteArraySet<>();

        if(!settings.contains(Integer.toString(id))) throw new Exception(NO_SONG_ID);
        String[] parameters=new String[5];

        settings.getStringSet(Integer.toString(id), parametersSet);
        parametersSet.toArray(parameters);

        path=parameters[0];
        if (!(new File(path)).exists()) throw new Exception(NO_SONG_ANYMORE);
        name=parameters[1];
        author=parameters[2];
        album=parameters[3];
        duration=Long.parseLong(parameters[4]);
    }

    //Save song data in device memory
    public void save(){
        ArrayList<String> parameters=new ArrayList<>();

        parameters.add(path);
        parameters.add(name);
        parameters.add(author);
        parameters.add(album);
        parameters.add(Long.toString(duration));

        SharedPreferences settings=context.getSharedPreferences(SONGS_SETTINGS_NAME,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putStringSet(Integer.toString(id),new CopyOnWriteArraySet<String>(parameters));
        editor.apply();
    }

    public void play(){
        //TODO:Make intent method after Service revising
    }

    private int makeId(){
        int number= (int) Math.random()*100000+1;
        CopyOnWriteArraySet<String> playlists=new CopyOnWriteArraySet<String>();
        SharedPreferences settings=context.getSharedPreferences(SONGS_SETTINGS_NAME,Context.MODE_PRIVATE);
        settings.getStringSet(Integer.toString(id), playlists);
        while(playlists.contains(number)) number++;
        return number;
    }
}
