package com.hibike;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.hibike.Keys.Notification.PATH_EXTRA;
import static com.hibike.Keys.Notification.PLAY_THIS_SONG;

public class SongButton extends RelativeLayout {

    private final Song song;
    private final Context context;

    SongButton(Song _song, Context _context) {
        super(_context);
        ImageView image=(ImageView) findViewById(R.id.imageView);
        TextView name=(TextView) findViewById(R.id.nameView);
        TextView authorAndAlbum=(TextView) findViewById(R.id.authorAndAlbumView);
        TextView duration=(TextView) findViewById(R.id.durationView);
        Button newSong=(Button) findViewById(R.id.songButton);

        context=_context;
        song=_song;

        //Creating empty Song button
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sample_song_button, this);

        //Filling Song button
        name.setText(song.getName());
        String authorAndAlbumString= song.getAuthor();
        if (song.getAlbum()!=null) authorAndAlbumString+=" - "+song.getAlbum();
        authorAndAlbum.setText(authorAndAlbumString);
        if (song.getImage()!=null) {
            image.setImageDrawable(Drawable.createFromPath(song.getImage()));
        }
        duration.setText(convertToTimeFormat(song.getDuration()));

        newSong.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Settings.getSelectedSongs().isEmpty()){
                    Intent toService=new Intent(context,HibikeService.class);
                    toService.setAction(PLAY_THIS_SONG);
                    toService.putExtra(PATH_EXTRA,song.getPath());
                    context.startService(toService);
                }else{
                    ArrayList<Song> selectedSongs=Settings.getSelectedSongs();
                    if(selectedSongs.contains(song)){
                        selectedSongs.remove(song);
                        getChildAt(0).setBackground(context.getDrawable(R.drawable.button));
                    }else{
                        getChildAt(0).setBackgroundColor(context.getColor(R.color.colorAccent));
                        selectedSongs.add(song);
                    }
                    Settings.setSelectedSongs(selectedSongs);
                }
            }
        });
    }

    SongButton(File music, Context _context) throws IOException{
        this(new Song(music, _context), _context);
    }

    SongButton(String path, Context _context) throws IOException{
        this(new Song(path, _context), _context);
    }

    SongButton(int id, Context _context) throws IOException{
        this(new Song(id, _context), _context);
    }

    public void setOnLongClickListener(OnLongClickListener listener){
        Button newSong=(Button) findViewById(R.id.songButton);
        newSong.setOnLongClickListener(listener);
    }
    private String convertToTimeFormat(long time){
        long seconds, minutes, hours;

        String timeString;
        seconds=(time/1000)%60;
        minutes=(time/(60*1000))%60;
        hours=time/(24*60*1000);

        if (hours>0) {
            timeString=hours+":";
            if (minutes>9) timeString+=minutes;
            else timeString+="0"+minutes;
        }else timeString=minutes+":";
        if (seconds>9) timeString+=seconds;
        else timeString+="0"+seconds;

        return timeString;
    }
}
