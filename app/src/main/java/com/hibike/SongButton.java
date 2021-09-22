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

    private Song song;
    private Context context;

    SongButton(Context _context,Song _song) {
        super(_context);

        context=_context;
        song=_song;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sample_song_button, this);
    }

    private void setAttributes(){
        PrimaryMusicData musicData=new PrimaryMusicData();
        ImageView image=(ImageView) findViewById(R.id.imageView);
        TextView name=(TextView) findViewById(R.id.nameView);
        TextView authorAndAlbum=(TextView) findViewById(R.id.authorAndAlbumView);
        TextView duration=(TextView) findViewById(R.id.durationView);
        try {musicData.primarySearch(new File(song.path),context);} catch (IOException e){}catch(NullPointerException e){}
        if (musicData.imageName!=null) {
            image.setImageDrawable(Drawable.createFromPath(context.getFilesDir().getAbsolutePath()+"/"+musicData.imageName));
            File imageFileToDelete=new File(context.getFilesDir().getAbsolutePath()+"/"+musicData.imageName);
            imageFileToDelete.delete();
        }
        else image.setImageDrawable(context.getDrawable(R.drawable.hibike_black));

        final Button newSong=(Button) findViewById(R.id.songButton);
        newSong.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getSelectedSongs().isEmpty()){
                    Intent toService=new Intent(context,HibikeService.class);
                    toService.setAction(PLAY_THIS_SONG);
                    toService.putExtra(PATH_EXTRA,song.path);
                    context.startService(toService);
                }else{
                    ArrayList<File> selectedSongs=getSelectedSongs();
                    if(selectedSongs.contains(song.id)){
                        selectedSongs.remove(song.id);
                        getChildAt(0).setBackground(context.getDrawable(R.drawable.button));
                    }else{
                        getChildAt(0).setBackgroundColor(context.getColor(R.color.colorAccent));
                        selectedSongs.add(song.id);
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
}
