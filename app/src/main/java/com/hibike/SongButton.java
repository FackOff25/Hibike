package com.hibike;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static com.hibike.Keys.Notification.PATH_EXTRA;
import static com.hibike.Keys.Notification.PLAY_THIS_SONG;
import static com.hibike.Keys.Settings.*;
public class SongButton extends RelativeLayout {
    private File buttonSong;
    private String songName;
    private String songAuthor;
    private int songDuration;
    private String songAlbum;
    private Context context;
    public SongButton(Context context1,File song) {
        super(context1);
        context=context1;
        buttonSong=song;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sample_song_button, this);
        setAttributes(song);
    }

    private void setAttributes(final File song){
        PrimaryMusicData musicData=new PrimaryMusicData();
        ImageView image=(ImageView) findViewById(R.id.imageView);
        TextView name=(TextView) findViewById(R.id.nameView);
        TextView authorAndAlbum=(TextView) findViewById(R.id.authorAndAlbumView);
        TextView duration=(TextView) findViewById(R.id.durationView);
        try {musicData.primarySearch(song,context);} catch (IOException e){}catch(NullPointerException e){}
        if (musicData.musicName!=null) name.setText(musicData.musicName);
        else {
            String fullFileName=song.getName();
            String str="";
            for (int count=0;fullFileName.toCharArray()[count]!='.';count++) str+=fullFileName.toCharArray()[count];
            name.setText(str);
        } songName=name.getText().toString();
        if (musicData.imageName!=null) {
            image.setImageDrawable(Drawable.createFromPath(context.getFilesDir().getAbsolutePath()+"/"+musicData.imageName));
            File imageFileToDelete=new File(context.getFilesDir().getAbsolutePath()+"/"+musicData.imageName);
            imageFileToDelete.delete();
        }
        else image.setImageDrawable(context.getDrawable(R.drawable.hibike_black));
        authorAndAlbum.setText(musicData.musicAuthor); songAuthor=musicData.musicAuthor;
        if (musicData.musicAlbum!=null) authorAndAlbum.setText(authorAndAlbum.getText()+" - "+musicData.musicAlbum); songAlbum=musicData.musicAlbum;
        MediaMetadataRetriever data=new MediaMetadataRetriever();
        data.setDataSource(song.getAbsolutePath());
        String durStr=data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long dur = Long.parseLong(durStr);
        int minuts=Math.round(dur/60000);
        int seconds=Math.round((dur%60000)/1000);
        if (seconds>=10) durStr=minuts+":"+seconds;
        else durStr=minuts+":0"+seconds;
        duration.setText(durStr);

        final Button newSong=(Button) findViewById(R.id.songButton);
        newSong.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getSelectedSongs().isEmpty()){
                    if (getIsRandom()){
                        File[] playingPlayList=getPlayListByName(getOpenedPlaylist());
                        Collections.shuffle(Arrays.asList(playingPlayList));
                        setPlaylist(getOpenedPlaylist(),playingPlayList);
                    }else setPlaylist(getOpenedPlaylist(),getPlayListByName(getOpenedPlaylist()));
                    Intent toService=new Intent(context,HibikeService.class);
                    toService.setAction(PLAY_THIS_SONG);
                    toService.putExtra(PATH_EXTRA,song.getAbsolutePath());
                    context.startService(toService);
                }else{
                    ArrayList<File> selectedSongs=getSelectedSongs();
                    if(selectedSongs.contains(getSong())){
                        selectedSongs.remove(getSong());
                        getChildAt(0).setBackground(context.getDrawable(R.drawable.button));
                    }else{
                        getChildAt(0).setBackgroundColor(context.getColor(R.color.colorAccent));
                        selectedSongs.add(getSong());
                    }
                    setSelectedSongs(selectedSongs);
                }
            }
        });
    }
    public void setOnLongClickListener(OnLongClickListener listener){
        Button newSong=(Button) findViewById(R.id.songButton);
        newSong.setOnLongClickListener(listener);
    }
    private void setSelectedSongs(ArrayList<File> selectedSongsFiles){
        ArrayList<String> selectedSongs=new ArrayList<>();
        for(int count=0;count<selectedSongsFiles.size();count++){
            selectedSongs.add(selectedSongsFiles.get(count).getAbsolutePath());
        }
        SharedPreferences settings=context.getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(SELECTED_SONGS,new HashSet<>(selectedSongs));
        editor.apply();
    }
    private ArrayList<File> getSelectedSongs(){
        SharedPreferences settings=context.getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
        ArrayList<File> selectedSongs=new ArrayList<>();
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
    public void setPlaylist(String playlistName,File[] playlist){
        String playlistString="";
        for (File song:playlist) playlistString+=">>"+song.getAbsolutePath();
        SharedPreferences settings=context.getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PLAYING_PLAYLIST_NAME,playlistName);
        editor.putString(PLAYING_PLAYLIST,playlistString);
        Log.e("Playling",playlistString);
        editor.apply();
    }
    public File[] getPlayListByName(String playlistName){
        SharedPreferences settings=context.getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
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
    public File[] getAllSongs(){
        SharedPreferences settings=context.getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
        File[] allSongs=null;
        if (settings.contains(ALL_SONGS)){
            String[] paths=settings.getString(ALL_SONGS,null).split(">>");
            allSongs=new File[paths.length];
            for (int count=0;count<paths.length;count++){
                allSongs[count]=new File(paths[count]);
            }
        }
        return allSongs;
    }
    private String getOpenedPlaylist(){
        SharedPreferences settings=context.getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
        String playlistName=ALL_SONGS;
        if (settings.contains(OPENED_PLAYLIST)){
            playlistName=settings.getString(OPENED_PLAYLIST,null);
        }
        return playlistName;
    }
    private boolean getIsRandom(){
        boolean isRandom=false;
        SharedPreferences settings=context.getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
        if (settings.contains(IS_RANDOM)) isRandom=settings.getBoolean(IS_RANDOM,false);
        return isRandom;
    }

    public File getSong(){
        return buttonSong;
    }
}
