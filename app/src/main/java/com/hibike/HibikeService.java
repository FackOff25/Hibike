package com.hibike;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static android.support.v4.app.NotificationCompat.PRIORITY_LOW;
import static com.hibike.Keys.Notification.*;
import static com.hibike.Keys.Settings.*;
import static com.hibike.Keys.Broadcast.*;
import static com.hibike.Keys.ReplayMods.*;

public class HibikeService extends Service implements AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener{

    public MediaPlayer mediaPlayer=new MediaPlayer();
    public AudioManager manager;
    private AudioManager.OnAudioFocusChangeListener changeListener;
    public LocalBroadcastManager broadcaster;
    private Timer playTimer=new Timer();
    private Settings settings;

    public HibikeService() {
    }
    public void onCreate(){
        super.onCreate();
        settings=new Settings(this);
        broadcaster = LocalBroadcastManager.getInstance(this);
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(this);
        manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        changeListener=this;
        try{
            while (settings.getAllSongs()==null){}
            setSong(settings.getPlayingSong());
        }catch (IOException e){}
        mediaPlayer.seekTo(settings.getTime());
        settings.setIsPlay(false);
    }
    @SuppressLint("LongLogTag")
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()){
            case START_FOREGROUND:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNorificationChannel();
                startForeground(NOTIFICATION_ID,getForegroundNotification(NOTIFICATION_ID,settings.getPlayingSong()));
                break;
            case STOP_FOREGROUND:
                setTime();
                Log.i("Hibike Foreground Service","Save Complete");
                playTimer.cancel();
                if (mediaPlayer != null) mediaPlayer.release();
                manager.abandonAudioFocus(changeListener);
                settings.setIsPlay(false);
                Intent toActivity = new Intent(BROADCAST_ACTION);
                toActivity.putExtra(UPDATE_UI,CLOSE);
                broadcaster.sendBroadcast(toActivity);
                stopForeground(true);
                stopSelf();
                break;
            case NEXT_SONG:
                nextSong();
                SharedPreferences.Editor editor = getSharedPreferences(SETTINGS_NAME,MODE_PRIVATE).edit();
                editor.putInt(SONG_TIME, 0);
                editor.apply();
                break;
            case PREVIOUS_SONG:
                previousSong();
                editor = getSharedPreferences(SETTINGS_NAME,MODE_PRIVATE).edit();
                editor.putInt(SONG_TIME, 0);
                editor.apply();
                break;
            case PLAY_SONG:
                playListener();
                break;
            case PLAY_THIS_SONG:
                try{setSong(new File(intent.getStringExtra(PATH_EXTRA)));}catch (IOException e){}
                editor = getSharedPreferences(SETTINGS_NAME,MODE_PRIVATE).edit();
                editor.putInt(SONG_TIME, 0);
                editor.apply();
                startPlay();
                break;
            case SEEK_TO:
                mediaPlayer.seekTo(intent.getIntExtra(TIME_EXTRA,0));
                break;
        }
        return START_STICKY;
    }
    public IBinder onBind(Intent intent) {return null;}

    public void playListener(){
        boolean isPlay=settings.getIsPlay();
        if(isPlay)stopPlay();
        else startPlay();
    }
    private void startPlay(){
        int result = manager.requestAudioFocus(changeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mediaPlayer.start();
        }
        playTimer.cancel();
        playTimer=new Timer();
        playTimer.schedule(new TimerTask() {
            Intent intent = new Intent(BROADCAST_ACTION);
            @Override
            public void run() {
                setTime();
                intent.putExtra(UPDATE_UI,SET_TIME);
                broadcaster.sendBroadcast(intent);
            }
        },0,1000);
        settings.setIsPlay(true);
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(UPDATE_UI,PLAY_CHANGED);
        broadcaster.sendBroadcast(intent);
        startForeground(NOTIFICATION_ID,getForegroundNotification(NOTIFICATION_ID,settings.getPlayingSong()));
    }
    private void stopPlay(){
        mediaPlayer.pause();
        playTimer.cancel();
        manager.abandonAudioFocus(changeListener);
        settings.setIsPlay(false);
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(UPDATE_UI,PLAY_CHANGED);
        broadcaster.sendBroadcast(intent);
        startForeground(NOTIFICATION_ID,getForegroundNotification(NOTIFICATION_ID,settings.getPlayingSong()));
    }
    public void setSong(File song) throws IOException{
        settings.setPlayingSong(song);
        mediaPlayer.reset();
        mediaPlayer.setDataSource(song.getAbsolutePath());
        mediaPlayer.prepare();
        settings.setPlayingSong(song);
        updateUi();
    }
    private void setTime(){
        SharedPreferences.Editor editor = getSharedPreferences(SETTINGS_NAME,MODE_PRIVATE).edit();
        editor.putInt(SONG_TIME, mediaPlayer.getCurrentPosition());
        editor.apply();
    }
    private void nextSong(){
        File[] playlist=settings.getPlayingPlayList();
        int songIsPlay=Arrays.asList(playlist).indexOf(settings.getPlayingSong())+1;
        if (songIsPlay>=playlist.length) songIsPlay=0;
        if (playlist[songIsPlay].getName().endsWith(".mp3")){
            boolean gotya=false;
            for (int count=0;!gotya&&count<10;count++){
            try{
                setSong(playlist[songIsPlay]);
                gotya=true;
            }catch (IOException e){}
            }
            if(!gotya) nextSong();
            startPlay();
        }


    }
    private void previousSong(){
        if (mediaPlayer.getCurrentPosition()<=10000){
        int songIsPlay=Arrays.asList(settings.getPlayingPlayList()).indexOf(settings.getPlayingSong())-1;
        if (songIsPlay<0) {
            songIsPlay=settings.getPlayingPlayList().length-1;
            boolean gotya=false;
            for (int count=0;!gotya&&count<10;count++){
                try{
                    setSong(settings.getPlayingPlayList()[songIsPlay]);
                    gotya=true;
                }catch (IOException e){}
            }
            if(!gotya) previousSong();
        }else{
            boolean gotya=false;
            for (int count=0;!gotya&&count<5;count++){
                try{
                    setSong(settings.getPlayingPlayList()[songIsPlay]);
                    gotya=true;
                }catch (IOException e){}
            }
            if(!gotya) previousSong();
        }
        startPlay();
    }else{
        boolean gotya=false;
        for (int count=0;!gotya&&count<5;count++){
            try{
                setSong(settings.getPlayingSong());
                gotya=true;
            }catch (IOException e){}
        }
        if(!gotya) previousSong();
        startPlay();
    }
    }
    public void onCompletion(MediaPlayer mp) {
    try{
        SharedPreferences settings2=getSharedPreferences(SETTINGS_NAME,MODE_PRIVATE);
        if (settings2.contains(REPLAY_MODE)){
            String replaymode=settings2.getString(REPLAY_MODE,null);
            switch (replaymode){
                case REPLAY_SONG:
                    setSong(settings.getPlayingSong());
                    startPlay();
                    break;
                case REPLAY_PLAYLIST:
                    nextSong();
                    break;
                case NO_REPLAY:
                    int songIsPlay=Arrays.asList(settings.getPlayingPlayList()).indexOf(settings.getPlayingSong())+1;
                    if (songIsPlay>=settings.getPlayingPlayList().length){
                        setSong(settings.getPlayingSong());
                        manager.abandonAudioFocus(changeListener);
                        settings.setIsPlay(false);
                        Intent intent = new Intent(BROADCAST_ACTION);
                        intent.putExtra(UPDATE_UI,PLAY_CHANGED);
                        broadcaster.sendBroadcast(intent);
                        startForeground(NOTIFICATION_ID,getForegroundNotification(NOTIFICATION_ID,settings.getPlayingSong()));
                    }else nextSong();
                    break;
            }
        }else nextSong();
    }catch(IOException e){}
    }

    public void onAudioFocusChange(int focusChange) {playListener();}

    public void updateUi() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(UPDATE_UI,settings.getPlayingSong().getAbsolutePath());
        broadcaster.sendBroadcast(intent);
    }

    private Notification getForegroundNotification(int id,File song){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, notificationIntent, 0);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_hibike_play)
                .setContent(getRemoteViews(song))
                .setPriority(PRIORITY_LOW)
                .setNumber(id)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVibrate(null)
                .setSound(null)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        Notification notification=builder.build();
        return notification;
    }
    @RequiresApi(android.os.Build.VERSION_CODES.O)
    private void createNorificationChannel(){
        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, CHANNEL,NotificationManager.IMPORTANCE_DEFAULT);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        channel.setImportance(NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null,null);
        nm.createNotificationChannel(channel);
    }
    private RemoteViews getRemoteViews(File song){
        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.hibike_notify);
        setIntets(remoteViews);
        remoteViews.setImageViewIcon(R.id.nextSongNotification, Icon.createWithResource(this,android.R.drawable.ic_media_next));
        if (!settings.getIsPlay()) remoteViews.setImageViewIcon(R.id.playButtonNotification, Icon.createWithResource(this,android.R.drawable.ic_media_play));
        else remoteViews.setImageViewIcon(R.id.playButtonNotification, Icon.createWithResource(this,android.R.drawable.ic_media_pause));
        remoteViews.setImageViewIcon(R.id.prevSongNotification, Icon.createWithResource(this,android.R.drawable.ic_media_previous));
        remoteViews.setImageViewIcon(R.id.closeButtonNotification, Icon.createWithResource(this,android.R.drawable.ic_menu_close_clear_cancel));
        Drawable drawable=this.getDrawable(R.drawable.hibike_black);
        remoteViews.setImageViewBitmap(R.id.imageViewNotification, ((BitmapDrawable)drawable).getBitmap());
        try{
            PrimaryMusicData musicData=new PrimaryMusicData();
            musicData.primarySearch(song,this);
            remoteViews.setTextViewText(R.id.nameViewNotification, musicData.getName(song));
            if (musicData.imageName!=null) {drawable=Drawable.createFromPath(this.getFilesDir().getAbsolutePath()+"/"+musicData.imageName);
            remoteViews.setImageViewBitmap(R.id.imageViewNotification, ((BitmapDrawable)drawable).getBitmap());}
            String authorAndAlbum=musicData.musicAuthor;
            if (musicData.musicAlbum!=null) authorAndAlbum+=" - "+musicData.musicAlbum;
            remoteViews.setTextViewText(R.id.authotAndAlbumNotification,authorAndAlbum);
        }catch (IOException e){}
        return remoteViews;
    }
    private void setIntets(RemoteViews remoteViews){
        Intent closeIntent=new Intent(this,HibikeService.class);
        closeIntent.setAction(STOP_FOREGROUND);
        PendingIntent closePendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            closePendingIntent = PendingIntent.getForegroundService(this,1,closeIntent,0);
        }else{closePendingIntent = PendingIntent.getService(this,1,closeIntent,0);}
        remoteViews.setOnClickPendingIntent(R.id.closeButtonNotification,closePendingIntent);

        Intent nextSongIntent=new Intent(this,HibikeService.class);
        nextSongIntent.setAction(NEXT_SONG);
        PendingIntent nextSongPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            nextSongPendingIntent = PendingIntent.getForegroundService(this,1,nextSongIntent,0);
        }else{nextSongPendingIntent = PendingIntent.getService(this,1,nextSongIntent,0);}
        remoteViews.setOnClickPendingIntent(R.id.nextSongNotification,nextSongPendingIntent);

        Intent previousSongIntent=new Intent(this,HibikeService.class);
        previousSongIntent.setAction(PREVIOUS_SONG);
        PendingIntent previousSongPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            previousSongPendingIntent = PendingIntent.getForegroundService(this,1,previousSongIntent,0);
        }else{previousSongPendingIntent = PendingIntent.getService(this,1,previousSongIntent,0);}
        remoteViews.setOnClickPendingIntent(R.id.prevSongNotification,previousSongPendingIntent);

        Intent playIntent=new Intent(this,HibikeService.class);
        playIntent.setAction(PLAY_SONG);
        PendingIntent playPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            playPendingIntent = PendingIntent.getForegroundService(this,1,playIntent,0);
        }else{playPendingIntent = PendingIntent.getService(this,1,playIntent,0);}
        remoteViews.setOnClickPendingIntent(R.id.playButtonNotification,playPendingIntent);
    }
}
