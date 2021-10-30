package com.hibike;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static android.support.v4.app.NotificationCompat.PRIORITY_LOW;
import static com.hibike.Keys.Notification.*;
import static com.hibike.Keys.Broadcast.*;
import static com.hibike.Keys.ReplayMods.*;
import static com.hibike.Keys.Songs.SONG_ID;

public class HibikeService extends Service implements AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener{

    public MediaPlayer mediaPlayer=new MediaPlayer();
    public AudioManager manager;
    private AudioManager.OnAudioFocusChangeListener changeListener;
    public LocalBroadcastManager broadcaster;

    private Settings settings;
    private Playlist playlist;
    //Timer
    private Timer playTimer=new Timer();
    final private TimerTask timerTask=new TimerTask() {
        final Intent intent = new Intent(BROADCAST_ACTION);
        @Override
        public void run() {
            settings.setTime(mediaPlayer.getCurrentPosition());
            intent.putExtra(UPDATE_UI,SET_TIME);
            broadcaster.sendBroadcast(intent);
        }
    };

    public HibikeService() { }
    public void onCreate(){
        super.onCreate();

        settings=new Settings(this);
        broadcaster = LocalBroadcastManager.getInstance(this);
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(this);
        manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        changeListener=this;
        playlist=settings.getPlayingPlayList();
        settings.setIsPlay(false);

        try{
            setSong(playlist.getCurrentSong());
        }catch (IOException e){
            e.printStackTrace();
        }
        mediaPlayer.seekTo(settings.getTime());

    }
    @SuppressLint("LongLogTag")
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()){
            case START_FOREGROUND:

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel();
                try {
                    startForeground(NOTIFICATION_ID, getForegroundNotification(NOTIFICATION_ID, new Song(playlist.getCurrentSong(), this)));
                }catch (NoSongFileException e){e.printStackTrace();}
                break;
            case STOP_FOREGROUND:
                //Saving time
                settings.setTime(mediaPlayer.getCurrentPosition());
                playTimer.cancel();
                settings.setIsPlay(false);
                Log.i("Hibike Foreground Service","Save Complete");
                //turning off mediaPlayer
                if (mediaPlayer != null) mediaPlayer.release();
                manager.abandonAudioFocus(changeListener);
                //sending Intent to activity
                Intent toActivity = new Intent(BROADCAST_ACTION);
                toActivity.putExtra(UPDATE_UI,CLOSE);
                broadcaster.sendBroadcast(toActivity);
                //Stopping service
                stopForeground(true);
                stopSelf();
                break;
            case NEXT_SONG:
                nextSong();
                settings.setTime(0);
                break;
            case PREVIOUS_SONG:
                previousSong();
                settings.setTime(0);
                break;
            case PLAY_SONG:
                playListener();
                break;
            case PLAY_THIS_SONG:
                try{setSong(intent.getIntExtra(SONG_ID,0));}catch (IOException e){e.printStackTrace();}
                settings.setTime(0);
                startPlay();
                break;
            case SEEK_TO:
                mediaPlayer.seekTo(intent.getIntExtra(TIME_EXTRA,0));
                settings.setTime(mediaPlayer.getCurrentPosition());
                break;
            case SHAKE:
                if (!settings.getIsRandom()) playlist.shake();
                else {
                    playlist=new Playlist(playlist.getId(), this);
                }
                settings.setCurrentPlaylist(playlist);
                break;
        }
        return START_STICKY;
    }
    public IBinder onBind(Intent intent) {return null;}

    public void playListener(){
        boolean isPlay=settings.getIsPlay();
        if(isPlay) stopPlay();
        else startPlay();
    }

    private void startPlay(){
        //Start actual playing
        int result = manager.requestAudioFocus(changeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mediaPlayer.start();
        }
        //Start timer
        playTimer.cancel();
        playTimer=new Timer();
        playTimer.schedule(timerTask,0,1000);

        //updating playing state
        settings.setIsPlay(true);
        //Sending intent to main activity
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(UPDATE_UI,PLAY_CHANGED);
        broadcaster.sendBroadcast(intent);
        //Updating or starting notification
        try{startForeground(NOTIFICATION_ID,getForegroundNotification(NOTIFICATION_ID,new Song(playlist.getCurrentSong(), this)));}catch (IOException e) {e.printStackTrace();}
    }
    private void stopPlay(){
        //stopping actual playing
        mediaPlayer.pause();
        manager.abandonAudioFocus(changeListener);
        //stopping timer
        playTimer.cancel();

        //Updating playing state
        settings.setIsPlay(false);
        //Sending intent to main activity
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(UPDATE_UI,PLAY_CHANGED);
        broadcaster.sendBroadcast(intent);
        //Updating notification
        try{startForeground(NOTIFICATION_ID,getForegroundNotification(NOTIFICATION_ID,new Song(playlist.getCurrentSong(), this)));}catch (IOException e) {e.printStackTrace();}
    }

    public void setSong(int songId) throws IOException{
        settings.setPlayingSong(songId);
        Song song=new Song(songId, this);
        mediaPlayer.reset();
        mediaPlayer.setDataSource(song.getPath());
        mediaPlayer.prepare();
        updateUI();
    }

    private void nextSong(){

        int songIsPlay=playlist.next();

        //boolean gotya=false;
        //for (int count=0;!gotya&&count<10;count++){
         try{
            setSong(songIsPlay);
            //gotya=true;
        }catch (IOException e){e.printStackTrace();}
        //}
        //if(!gotya) nextSong();
        startPlay();

    }
    private void previousSong(){
        if (mediaPlayer.getCurrentPosition()>10000)mediaPlayer.seekTo(0);
        else{
            int songIsPlay=playlist.prev();
            try{setSong(songIsPlay);}catch (IOException e){e.printStackTrace();}
            /*int songIsPlay=Arrays.asList(settings.getPlayingPlayList()).indexOf(settings.getPlayingSong())-1;
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
            startPlay();*/
            startPlay();
        }
    }
    public void onCompletion(MediaPlayer mp) {
        switch (settings.getReplayMode()){
            case REPLAY_SONG:
                try{setSong(settings.getPlayingSong());}catch(IOException e){e.printStackTrace();}
                startPlay();
                break;
            case NO_REPLAY:
                if (playlist.currentSong==playlist.songCounter){
                    stopPlay();
                    settings.setTime(0);
                }else nextSong();
                break;
            case REPLAY_PLAYLIST:
            default:
                nextSong();
                break;
        }
    }

    public void onAudioFocusChange(int focusChange) {playListener();}
    //Send intent to main activity to update UI
    public void updateUI() {
        try {
            Intent intent = new Intent(BROADCAST_ACTION);
            intent.putExtra(UPDATE_UI, new Song(playlist.getCurrentSong(), this).getPath());
            broadcaster.sendBroadcast(intent);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    //
    //Notification methods
    //
    //Creating notification
    private Notification getForegroundNotification(int id,Song song){
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
        return builder.build();
    }
    //Fot Oreo android version
    @RequiresApi(android.os.Build.VERSION_CODES.O)
    private void createNotificationChannel(){
        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, CHANNEL,NotificationManager.IMPORTANCE_DEFAULT);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        channel.setImportance(NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null,null);
        nm.createNotificationChannel(channel);
    }
    //Creating UI
    private RemoteViews getRemoteViews(Song song){
        String authorAndAlbum;
        Drawable drawable;

        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.hibike_notify);
        setIntets(remoteViews);
        //Buttons
        remoteViews.setImageViewIcon(R.id.nextSongNotification, Icon.createWithResource(this,android.R.drawable.ic_media_next));
        if (!settings.getIsPlay()) remoteViews.setImageViewIcon(R.id.playButtonNotification, Icon.createWithResource(this,android.R.drawable.ic_media_play));
        else remoteViews.setImageViewIcon(R.id.playButtonNotification, Icon.createWithResource(this,android.R.drawable.ic_media_pause));
        remoteViews.setImageViewIcon(R.id.prevSongNotification, Icon.createWithResource(this,android.R.drawable.ic_media_previous));
        remoteViews.setImageViewIcon(R.id.closeButtonNotification, Icon.createWithResource(this,android.R.drawable.ic_menu_close_clear_cancel));
        //Image
        drawable=this.getDrawable(R.drawable.hibike_black);
        remoteViews.setImageViewBitmap(R.id.imageViewNotification, ((BitmapDrawable)drawable).getBitmap());
        if (song.getImage()!=null) {drawable=Drawable.createFromPath(this.getFilesDir().getAbsolutePath()+"/"+song.getImage());
        remoteViews.setImageViewBitmap(R.id.imageViewNotification, ((BitmapDrawable)drawable).getBitmap());}
        //Text: name, author and album
        remoteViews.setTextViewText(R.id.nameViewNotification, song.getName());
        authorAndAlbum=song.getAuthor();
        if (song.getAlbum()!=null) authorAndAlbum+=" - "+song.getAlbum();
        remoteViews.setTextViewText(R.id.authotAndAlbumNotification,authorAndAlbum);

        return remoteViews;
    }
    //Sets event on buttons
    private void setIntets(RemoteViews remoteViews){
        //Closing button
        Intent closeIntent=new Intent(this,HibikeService.class);
        closeIntent.setAction(STOP_FOREGROUND);
        PendingIntent closePendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            closePendingIntent = PendingIntent.getForegroundService(this,1,closeIntent,0);
        }else{closePendingIntent = PendingIntent.getService(this,1,closeIntent,0);}
        remoteViews.setOnClickPendingIntent(R.id.closeButtonNotification,closePendingIntent);
        //Next song button
        Intent nextSongIntent=new Intent(this,HibikeService.class);
        nextSongIntent.setAction(NEXT_SONG);
        PendingIntent nextSongPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            nextSongPendingIntent = PendingIntent.getForegroundService(this,1,nextSongIntent,0);
        }else{nextSongPendingIntent = PendingIntent.getService(this,1,nextSongIntent,0);}
        remoteViews.setOnClickPendingIntent(R.id.nextSongNotification,nextSongPendingIntent);
        //Previous song button
        Intent previousSongIntent=new Intent(this,HibikeService.class);
        previousSongIntent.setAction(PREVIOUS_SONG);
        PendingIntent previousSongPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            previousSongPendingIntent = PendingIntent.getForegroundService(this,1,previousSongIntent,0);
        }else{previousSongPendingIntent = PendingIntent.getService(this,1,previousSongIntent,0);}
        remoteViews.setOnClickPendingIntent(R.id.prevSongNotification,previousSongPendingIntent);
        //Play/pause button
        Intent playIntent=new Intent(this,HibikeService.class);
        playIntent.setAction(PLAY_SONG);
        PendingIntent playPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            playPendingIntent = PendingIntent.getForegroundService(this,1,playIntent,0);
        }else{playPendingIntent = PendingIntent.getService(this,1,playIntent,0);}
        remoteViews.setOnClickPendingIntent(R.id.playButtonNotification,playPendingIntent);
    }
}
