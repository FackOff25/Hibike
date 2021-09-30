package com.hibike;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.hibike.Keys.Songs.*;

public class Song {
    //Technical
    private int id;
    private String path;
    final private Context context;

    //For user
    private String name=null;
    private String author=null;
    private String album=null;
    private long duration=0;
    private String image=null;

    public Song(File song, Context _context) throws IOException {
        context=_context;
        id=makeId();

        name=PrimaryMusicData.getName(song);
        author=PrimaryMusicData.getAuthor(song);
        album=PrimaryMusicData.getAlbum(song);
        image=CoverEditor.shrink(PrimaryMusicData.getImage(song, context));

        MediaMetadataRetriever data=new MediaMetadataRetriever();
        data.setDataSource(song.getAbsolutePath());
        String durStr=data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        duration = Integer.parseInt(durStr);
    }

    public Song(String songPath, Context context) throws IOException{
           this(new File(songPath), context);
    }

    public Song(int _id,Context _context) throws NoMoreSongException, NoSongFileException{
        id=_id;
        context=_context;

        SharedPreferences settings=context.getSharedPreferences(SONGS_SETTINGS_NAME,Context.MODE_PRIVATE);
        CopyOnWriteArraySet<String> parametersSet=new CopyOnWriteArraySet<>();

        if(!settings.contains(Integer.toString(id))) throw new NoMoreSongException(id);
        String[] parameters=new String[5];

        parametersSet = (CopyOnWriteArraySet<String>) settings.getStringSet(Integer.toString(id), null);
        parametersSet.toArray(parameters);

        path=parameters[0];
        if (!(new File(path)).exists()) throw new NoSongFileException(path);
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
        Settings settings=new Settings(context);
        settings.setPlayingSong(id);
    }

    /* Methods to give parameters*/
    public int getId() {
        return id;
    }
    public String getPath(){
        return path;
    }

    public String getName(){
        return name;
    }

    public String getAuthor(){
        return author;
    }

    public String getAlbum(){
        return album;
    }

    public long getDuration(){
        return duration;
    }

    public String getImage(){
        return image;
    }
    /*End of "get" methods*/

    private int makeId(){
        int number= (int) Math.random()*100000+1;
        CopyOnWriteArraySet<String> playlists=new CopyOnWriteArraySet<String>();
        SharedPreferences settings=context.getSharedPreferences(SONGS_SETTINGS_NAME,Context.MODE_PRIVATE);
        playlists = (CopyOnWriteArraySet) settings.getStringSet(Integer.toString(id), null);
        while(playlists.contains(Integer.toString(number))) number++;
        return number;
    }
}
