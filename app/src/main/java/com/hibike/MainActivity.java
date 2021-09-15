package com.hibike;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.hibike.Keys.Settings.*;
import static com.hibike.Keys.Notification.*;
import static com.hibike.Keys.Broadcast.*;
import static com.hibike.Keys.ReplayMods.*;

public class MainActivity extends AppCompatActivity implements OnAudioFocusChangeListener{
    private final int Pick_image = 1;

    private BroadcastReceiver receiver;

    private boolean touched=false;
    private File editingSong;
    private String imagePath;
    private Settings settings;

    private LinearLayout playlistLayout;
    private ProgressBar progressBar;
    private TextView songCountView;
    private SlidingPaneLayout slidingPaneLayout;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        settings=new Settings(this);
        if (settings.getIsFirstLoad()){
            getPermissoin();
            int permissionStatus = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            while (permissionStatus!=PackageManager.PERMISSION_GRANTED) {
                try {
                    synchronized(this) {
                        this.wait(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                permissionStatus = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                getPermissoin();
            }
            File[] files={Environment.getExternalStorageDirectory()};
            ArrayList<File> allSongsPlaylist;
            class Scanning{
                public ArrayList<File> allSongsPlaylist=new ArrayList<>();
                public void scanFolder(File folder){
                    if (folder.isDirectory()){
                        FilenameFilter filter = new FilenameFilter() {
                            public boolean accept(File directory, String fileName) {
                                File file=new File(directory+"/"+fileName);
                                return fileName.endsWith(".mp3")||file.isDirectory();
                            }
                        };
                        File[] folderFiles=folder.listFiles(filter);
                        if (folderFiles.length>0){
                            for (File item:folderFiles){
                                if (item.isDirectory()) {scanFolder(item);}
                                else {allSongsPlaylist.add(item);}
                            }}
                    }
                }
                public ArrayList<File> getAllSongsPlaylist(){
                    return allSongsPlaylist;
                }
            }
            Scanning scanning=new Scanning();
            for (File folder:files){
                scanning.scanFolder(folder);
            }
            allSongsPlaylist=sortByName(scanning.getAllSongsPlaylist());
            File[] playlist=allSongsPlaylist.toArray(new File[allSongsPlaylist.size()]);
            String playlistString="";
            for (File file:playlist){
                if (file!=null) playlistString+=">>"+file.getAbsolutePath();
            }
            SharedPreferences settings2=getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings2.edit();
            editor.putString(ALL_SONGS,playlistString);
            editor.apply();
            settings.setNotFirstLoad();
        }else{
            FolderScanner scanner=new FolderScanner();
            scanner.execute(Environment.getExternalStorageDirectory());
        }
        settings.setPlaylist(settings.getPlayingPlayListName(),settings.getPlayingPlayList());
        Intent startIntent = new Intent(MainActivity.this, HibikeService.class);
        startIntent.setAction(START_FOREGROUND);
        startService(startIntent);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String path = intent.getStringExtra(UPDATE_UI);
                switch (path){
                    case PLAY_CHANGED:
                        ImageButton btn1=(ImageButton) findViewById(R.id.playButton);
                        ImageButton btn2=(ImageButton) findViewById(R.id.playButton2);
                        if(!settings.getIsPlay()){
                            btn1.setImageDrawable(getDrawable(R.drawable.play));
                            btn2.setImageDrawable(getDrawable(R.drawable.play));
                        }else{
                            btn1.setImageDrawable(getDrawable(R.drawable.pause));
                            btn2.setImageDrawable(getDrawable(R.drawable.pause));
                        }
                        break;
                    case SET_TIME:
                        if(!touched){
                        SeekBar seekBar=(SeekBar) findViewById(R.id.seekBar);
                        seekBar.setProgress(settings.getTime());}
                        break;
                    case CLOSE:
                        finish();
                        break;
                    default:
                        File song=new File(path);
                        try{
                            setSong(song);
                        }catch (IOException e){}
                        break;

                }
            }
        };
    }
    public void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        LinearLayout mainlayout=(LinearLayout) findViewById(R.id.mainLayout);
        mainlayout.removeAllViewsInLayout();
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metricsB = new DisplayMetrics();
        display.getMetrics(metricsB);

        initViews();
        try{setSong(settings.getPlayingSong());}catch (IOException e){}

        SharedPreferences settings2=getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
        ArrayList<File> allSongsPlaylist=new ArrayList<>();
        allSongsPlaylist.addAll(Arrays.asList(settings.getAllSongs()));
        String[] allSongs=new String[allSongsPlaylist.size()];
        for(int count=0;count<allSongs.length;count++) allSongs[count]=allSongsPlaylist.get(count).getAbsolutePath();
        addPlaylist(ALL_SONGS,allSongs);
        if(settings2.contains(PLAYLISTS_NAMES)){
            ArrayList<String> playlistsNames=settings.getPlaylistsNames();
            ArrayList<String> playlists;
            if(settings2.contains(PLAYLISTS)){
                playlists=settings.getPlaylists();
                for(int count=0;count<playlists.size();count++){
                    String[] thisPlaylist= playlists.get(count).split(">>");
                    String thisPlaylistName=playlistsNames.toArray(new String[playlistsNames.size()])[count];
                    addPlaylist(thisPlaylistName,thisPlaylist);
                }
            }
        }

        String playlistName=settings.getOpenedPlaylist();
        if (playlistName.equals(ALL_SONGS)) playlistName=getString(R.string.all_songs);
        String playingPlaylistName=settings.getPlayingPlayListName();
        if (playlistName.equals(ALL_SONGS)) playlistName=getString(R.string.all_songs);
        changeSlidingPlaylistSign(playlistName);

        File[] playingPlayList;
        LinearLayout playlistLayout=(LinearLayout) findViewById(R.id.playlistsLayout);
        for (int count=0;count<playlistLayout.getChildCount();count++){
            Button btn=(Button) playlistLayout.getChildAt(count);
            String btnText=btn.getText().toString();
            if (btnText.equals(playlistName)) btn.setTextColor(getColor(R.color.colorAccent));
            else btn.setTextColor(getColor(R.color.lightText));
        }
        if (playlistName.equals(ALL_SONGS)||playlistName.equals(getString(R.string.all_songs))) {
            playingPlayList=settings.getAllSongs();
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.GONE);
        }else {
            int index=settings.getPlaylistsNames().indexOf(playlistName);
            String[] paths=settings.getPlaylists().get(index).split(">>");
            playingPlayList=toPlaylist(paths);
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.VISIBLE);
        }
        UpdatePlaylistTread updatePlaylistTread=new UpdatePlaylistTread();
        updatePlaylistTread.execute(playingPlayList);
        settings.setSelectedSongs(new ArrayList<File>());
    }
    public void onStart(){
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),new IntentFilter(BROADCAST_ACTION));
    }

    public void initViews(){
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        SlidingUpPanelLayout umano=(SlidingUpPanelLayout) findViewById(R.id.umano);
        umano.setPanelHeight(getNavBarHeight()+175);
        umano.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                switch (newState){
                    case EXPANDED:
                        findViewById(R.id.hidePanelButton).setVisibility(View.VISIBLE);
                        findViewById(R.id.playlistSlidingSign).setVisibility(View.VISIBLE);
                        findViewById(R.id.editButton).setVisibility(View.VISIBLE);
                        findViewById(R.id.shakeButton).setVisibility(View.VISIBLE);
                        findViewById(R.id.replayButton).setVisibility(View.VISIBLE);
                        findViewById(R.id.playButton).setVisibility(View.GONE);
                        RelativeLayout.LayoutParams params=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
                        params.addRule(RelativeLayout.BELOW,R.id.hidePanelButton);
                        params.addRule(RelativeLayout.ALIGN_LEFT,R.id.playlistSlidingSign);
                        findViewById(R.id.songName).setLayoutParams(params);
                        params=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,45);
                        params.addRule(RelativeLayout.BELOW,R.id.songName);
                        params.addRule(RelativeLayout.ALIGN_LEFT,R.id.playlistSlidingSign);
                        findViewById(R.id.songAuthor).setLayoutParams(params);
                        break;
                    case COLLAPSED:
                        findViewById(R.id.hidePanelButton).setVisibility(View.GONE);
                        findViewById(R.id.playlistSlidingSign).setVisibility(View.GONE);
                        findViewById(R.id.editButton).setVisibility(View.GONE);
                        findViewById(R.id.shakeButton).setVisibility(View.GONE);
                        findViewById(R.id.replayButton).setVisibility(View.GONE);
                        findViewById(R.id.playButton).setVisibility(View.VISIBLE);
                        params=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
                        params.addRule(RelativeLayout.END_OF,R.id.playButton);

                        findViewById(R.id.songName).setLayoutParams(params);
                        params=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
                        params.addRule(RelativeLayout.END_OF,R.id.playButton);
                        params.addRule(RelativeLayout.BELOW,R.id.songName);
                        findViewById(R.id.songAuthor).setLayoutParams(params);
                        break;
                    default:
                }
            }
        });

        SlidingPaneLayout slidingLayout = findViewById(R.id.slidingPaneLayout);
        slidingLayout.setSliderFadeColor(Color.TRANSPARENT);
        RelativeLayout mainLayout=(RelativeLayout) findViewById(R.id.superMainLayout);
        mainLayout.setPadding(0,getStatusBarHeight(),0,umano.getPanelHeight());

        RelativeLayout songImagePanel=(RelativeLayout) findViewById(R.id.songImagePanel);
        songImagePanel.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,metrics.widthPixels));

        RelativeLayout buttonsPanel=(RelativeLayout) findViewById(R.id.buttonsPanel);
        buttonsPanel.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,(int) (metrics.heightPixels*0.2)));

        LinearLayout slidingPanel=(LinearLayout) findViewById(R.id.slidingPanel);
        slidingPanel.setPadding(0,getStatusBarHeight(),0,0);

        SeekBar playBar=(SeekBar) findViewById(R.id.seekBar);
        playBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) {
                TextView songTime=(TextView) findViewById(R.id.songTime);
                int seconds=Math.round((progress%60000)/1000);
                if (seconds>=10) songTime.setText(Math.round(progress/60000)+":"+seconds);
                else songTime.setText(Math.round(progress/60000)+":0"+seconds);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {touched=true;}

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                touched=false;
                Intent play=new Intent(MainActivity.this,HibikeService.class);
                play.setAction(SEEK_TO);
                play.putExtra(TIME_EXTRA,seekBar.getProgress());
                startService(play);
            }
        });

        ImageButton btn=(ImageButton) findViewById(R.id.replayButton);
        switch (settings.getReplayMode()){
            case NO_REPLAY:
                btn.setImageDrawable(getDrawable(R.drawable.noreplay));
                break;
            case REPLAY_PLAYLIST:
                btn.setImageDrawable(getDrawable(R.drawable.replayplaylist));
                break;
            case REPLAY_SONG:
                btn.setImageDrawable(getDrawable(R.drawable.replaysong));
                break;
        }
        if (settings.getIsRandom()){
            ImageButton shakeButton=(ImageButton) findViewById(R.id.shakeButton);
            shakeButton.setImageDrawable(getDrawable(R.drawable.shake));
        }
        playlistLayout=(LinearLayout) findViewById(R.id.mainLayout);
        progressBar=(ProgressBar) findViewById(R.id.progressBar);
        songCountView=(TextView) findViewById(R.id.songsCount);
        slidingPaneLayout=(SlidingPaneLayout) findViewById(R.id.slidingPaneLayout);
    }
    public void setSong(File song) throws IOException,NullPointerException{

        ScrollView scrollView=(ScrollView) findViewById(R.id.lirycsScroll);
        scrollView.scrollTo(0,0);
        SecondaryMusicData musicData=new SecondaryMusicData();
        musicData.secondarySearch(song, this);

        ImageButton btn1=(ImageButton) findViewById(R.id.playButton);
        ImageButton btn2=(ImageButton) findViewById(R.id.playButton2);
        if (settings.getIsPlay()){
            btn1.setImageDrawable(getDrawable(R.drawable.pause));
            btn2.setImageDrawable(getDrawable(R.drawable.pause));
        }else{
            btn1.setImageDrawable(getDrawable(R.drawable.play));
            btn2.setImageDrawable(getDrawable(R.drawable.play));
        }
        TextView songName=(TextView) findViewById(R.id.songName);
        TextView songAuthor=(TextView) findViewById(R.id.songAuthor);

        if (musicData.musicName!=null) songName.setText(musicData.musicName);else {
            String fullFileName=song.getName();
            String str="";
            for (int count=0;fullFileName.toCharArray()[count]!='.';count++) str+=fullFileName.toCharArray()[count];
            songName.setText(str);
        }
        songAuthor.setText(musicData.musicAuthor);
        if (musicData.musicAlbum!=null) songAuthor.setText(songAuthor.getText()+" - "+musicData.musicAlbum);

        ImageView songImage=(ImageView) findViewById(R.id.songImage);
        TextView lirycs=(TextView) findViewById(R.id.lirycs);

        if (musicData.musicLyrics!=null) {
            lirycs.setText(musicData.musicLyrics);
            songImage.setForeground(getDrawable(R.drawable.shadow));
            lirycs.setMovementMethod(new ScrollingMovementMethod());
        }else{
            lirycs.setText("");
            songImage.setForeground(null);}
        if(musicData.imageName!=null) {
            songImage.setImageDrawable(Drawable.createFromPath(getApplicationContext().getFilesDir().getAbsolutePath() + "/" + musicData.imageName));
            File imageFileToDelete=new File(getApplicationContext().getFilesDir().getAbsolutePath()+"/"+musicData.imageName);
            imageFileToDelete.delete();
        }else {
            songImage.setImageDrawable(getDrawable(R.drawable.hibike_black));
            songImage.setForeground(null);
        }
        TextView songDuration=(TextView) findViewById(R.id.songDuration);
        MediaMetadataRetriever data=new MediaMetadataRetriever();
        try{
        data.setDataSource(song.getAbsolutePath());
        }catch (IllegalArgumentException e){}
        String durStr=data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long dur = Long.parseLong(durStr);
        int minuts=Math.round(dur/60000);
        int seconds=Math.round((dur%60000)/1000);
        if (seconds>=10) durStr=minuts+":"+seconds;
        else durStr=minuts+":0"+seconds;
        songDuration.setText(durStr);
        SeekBar seekBar=(SeekBar) findViewById(R.id.seekBar);
        seekBar.setMax((int)dur);
        SeekBar playBar=(SeekBar) findViewById(R.id.seekBar);
        playBar.setProgress(settings.getTime());
    }

    public void playListener(View v){
        Intent toService=new Intent(MainActivity.this,HibikeService.class);
        toService.setAction(PLAY_SONG);
        startService(toService);
    }
    public void nextSong(View v){
        Intent toService=new Intent(MainActivity.this,HibikeService.class);
        toService.setAction(NEXT_SONG);
        startService(toService);
    }
    public void prevSong(View v) {
        Intent toService=new Intent(MainActivity.this,HibikeService.class);
        toService.setAction(PREVIOUS_SONG);
        startService(toService);
    }
    public void shakeSongs(View v) {
        File[] playingPlayList=settings.getPlayingPlayList();
        ImageButton btn=(ImageButton) v;
        boolean isRandom=!settings.getIsRandom();
        if (isRandom) {
            Collections.shuffle(Arrays.asList(playingPlayList));
            btn.setImageDrawable(getDrawable(R.drawable.shake));
            settings.setPlaylist(settings.getPlayingPlayListName(),playingPlayList);
            makeMessage("Перемешивание включено");
        }else {
            ArrayList<File> playlistList=new ArrayList<>();
            playlistList.addAll(Arrays.asList(playingPlayList));
            settings.setPlaylist(settings.getPlayingPlayListName(),settings.getPlayingPlayList());
            btn.setImageDrawable(getDrawable(R.drawable.shakeoff));
            makeMessage("Перемешивание выключено");
        }
        settings.setIsRandom(isRandom);
    }
    public void nextReplayMode(View v){
        ImageButton btn=(ImageButton) v;
        switch (settings.getReplayMode()){
            case NO_REPLAY:
                settings.setReplayMode(REPLAY_PLAYLIST);
                btn.setImageDrawable(getDrawable(R.drawable.replayplaylist));
                break;
            case REPLAY_PLAYLIST:
                settings.setReplayMode(REPLAY_SONG);
                btn.setImageDrawable(getDrawable(R.drawable.replaysong));
                break;
            case REPLAY_SONG:
                settings.setReplayMode(NO_REPLAY);
                btn.setImageDrawable(getDrawable(R.drawable.noreplay));
                break;
        }
    }

    private void clearBackgrounds(){
        LinearLayout mainLayout=(LinearLayout) findViewById(R.id.mainLayout);
        for (int count=0;count<mainLayout.getChildCount();count++){
            SongButton btn=(SongButton) mainLayout.getChildAt(count);
            btn.getChildAt(0).setBackground(getDrawable(R.drawable.button));
        }
    }

    private void addToPlaylist(String playlistName,ArrayList<File> songs){
        int index=settings.getPlaylistsNames().indexOf(playlistName);
        ArrayList<String> playlists=settings.getPlaylists();
        String playlist=playlists.get(index);
        for (File song:songs) playlist+=">>"+song;
        playlists.set(index,playlist);
        settings.setPlaylists(playlists);
    }
    public void deleteFromPlaylist(View v){
        int index=settings.getPlaylistsNames().indexOf(settings.getOpenedPlaylist());
        ArrayList<String> playlists=settings.getPlaylists();
        String[] playlistStrings=playlists.get(index).split(">>");
        ArrayList<File> selectedSongs=settings.getSelectedSongs();
        ArrayList<String> playlistList=new ArrayList<>();
        playlistList.addAll(Arrays.asList(playlistStrings));
        for (File song:selectedSongs){
            String path=""+song.getAbsolutePath();
            playlistList.remove(path);
        }
        String newPlaylist="";
        for (String path:playlistList){
            newPlaylist+=">>"+path;
        }
        settings.setSelectedSongs(new ArrayList<File>());
        playlists.set(index,newPlaylist);
        settings.setPlaylists(playlists);

        String playlistName=settings.getOpenedPlaylist();
        File[] playingPlayList;
        LinearLayout playlistLayout=(LinearLayout) findViewById(R.id.playlistsLayout);
        for (int count=0;count<playlistLayout.getChildCount();count++){
            Button btn=(Button) playlistLayout.getChildAt(count);
            String btnText=btn.getText().toString();

            if (btnText.equals(getString(R.string.all_songs))) btnText=ALL_SONGS;
            if (btnText.equals(playlistName)) btn.setTextColor(getColor(R.color.colorAccent));
            else btn.setTextColor(getColor(R.color.lightText));
        }
        if (playlistName.equals(ALL_SONGS)||playlistName.equals(getString(R.string.all_songs))) {
            playingPlayList=settings.getAllSongs();
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.GONE);
        }
        else {
            index=settings.getPlaylistsNames().indexOf(playlistName);
            String[] paths=settings.getPlaylists().get(index).split(">>");
            playingPlayList=toPlaylist(paths);
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.VISIBLE);
        }
        UpdatePlaylistTread updatePlaylistTread=new UpdatePlaylistTread();
        updatePlaylistTread.execute(playingPlayList);

        shadowListener(null);
    }
    public void addToPlaylistListener(View v){
        LinearLayout songMenu=(LinearLayout) findViewById(R.id.songMenu);
        songMenu.setVisibility(View.GONE);
        LinearLayout playlistMenu=(LinearLayout) findViewById(R.id.playlistMenu);
        playlistMenu.setVisibility(View.VISIBLE);
        Button shadow=(Button) findViewById(R.id.shadowBehindMenu);
        shadow.setVisibility(View.VISIBLE);
    }
    public void newPlaylistListener(View v){
        LinearLayout playlistMenu=(LinearLayout) findViewById(R.id.playlistMenu);
        playlistMenu.setVisibility(View.GONE);
        LinearLayout newPlaylistMenu=(LinearLayout) findViewById(R.id.newPlaylistMenu);
        newPlaylistMenu.setVisibility(View.VISIBLE);
    }
    public void closeNewPlaylistMenu(View v){
        Button shadow=(Button) findViewById(R.id.shadowBehindMenu);
        shadow.setVisibility(View.GONE);
        LinearLayout newPlaylistMenu=(LinearLayout) findViewById(R.id.newPlaylistMenu);
        newPlaylistMenu.setVisibility(View.GONE);
        TextInputEditText newPlaylistEdit=(TextInputEditText) findViewById(R.id.newPlaylistEdit);
        newPlaylistEdit.clearComposingText();
    }
    public void createNewPlaylist(View v){
        TextInputEditText newPlaylistEdit=(TextInputEditText) findViewById(R.id.newPlaylistEdit);
        ArrayList<String> playlistsNames=settings.getPlaylistsNames();
        ArrayList<String> playlists=settings.getPlaylists();
        playlistsNames.add(newPlaylistEdit.getText().toString());
        String playlistString="";
        ArrayList<File> selectedSongs=settings.getSelectedSongs();
        for (int count=0;count<selectedSongs.size();count++){
            playlistString+=">>"+selectedSongs.get(count);
        }
        playlists.add(playlistString);
        settings.setPlaylistsNames(playlistsNames);
        settings.setPlaylists(playlists);
        String[] thisPlaylist= playlists.toArray(new String[playlists.size()])[playlists.size()-1].split(">>");
        addPlaylist(newPlaylistEdit.getText().toString(),thisPlaylist);
        clearBackgrounds();
        closeNewPlaylistMenu(null);
    }
    public void addPlaylist(final String playlistName, String[] playlist){
        LinearLayout playlistsLayout=(LinearLayout) findViewById(R.id.playlistsLayout);
        Button newPlaylist=new Button(this);
        if (!playlistName.equals(ALL_SONGS)) newPlaylist.setText(playlistName);
        else newPlaylist.setText(R.string.all_songs);

        newPlaylist.setGravity(Gravity.CENTER);
        final File[] songsInPlaylist=new File[playlist.length];
        for(int count=0;count<songsInPlaylist.length;count++){
            songsInPlaylist[count]=new File(playlist[count]);
            if (!songsInPlaylist[count].exists()) songsInPlaylist[count]=null;
        }
        newPlaylist.setPadding(20,20,20,20);
        newPlaylist.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                choicePlaylist(playlistName);
                slidingPaneLayout.closePane();
            }
        });
        playlistsLayout.addView(newPlaylist,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        if (!playlistName.equals(ALL_SONGS)){
            LinearLayout playlistMenu=(LinearLayout) findViewById(R.id.playlistMenu);
            Button newPlaylistInMenu=new Button(this);
            newPlaylistInMenu.setPadding(10,10,10,10);
            newPlaylistInMenu.setText(playlistName);
            newPlaylistInMenu.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    addToPlaylist(playlistName,settings.getSelectedSongs());
                    LinearLayout playlistMenu=(LinearLayout) findViewById(R.id.playlistMenu);
                    playlistMenu.setVisibility(View.GONE);
                    Button shadow=(Button) findViewById(R.id.shadowBehindMenu);
                    shadow.setVisibility(View.GONE);
                    settings.setSelectedSongs(new ArrayList<File>());
                    clearBackgrounds();
                }
            });
            playlistMenu.addView(newPlaylistInMenu,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        }
    }
    public void showEditPlaylist(View v){
        shadowListener(null);
        LinearLayout editPlaylistMenu=(LinearLayout) findViewById(R.id.editPlaylistMenu);
        editPlaylistMenu.setVisibility(View.VISIBLE);
    }
    public void editPlaylist(View v){
        ArrayList<String> playlistsNames=settings.getPlaylistsNames();
        EditText newName=(EditText) findViewById(R.id.playlistNameEdit);
        playlistsNames.set(playlistsNames.indexOf(settings.getOpenedPlaylist()),newName.getText().toString());
        settings.setPlaylists(playlistsNames);
        changeMainPlaylistSign(newName.getText().toString());
        if(settings.getPlayingPlayListName().equals(settings.getOpenedPlaylist())){
            changeSlidingPlaylistSign(newName.getText().toString());
        }
        LinearLayout playlistLayout=(LinearLayout) findViewById(R.id.playlistsLayout);
        for (int count=0;count<playlistLayout.getChildCount();count++){
            Button btn=(Button) playlistLayout.getChildAt(count);
            if (settings.getOpenedPlaylist().equals(btn.getText().toString())) {
                btn.setText(newName.getText().toString());
                settings.setOpenedPlaylist(newName.getText().toString());
                break;
            }
        }
        newName.setText("");
        shadowListener(null);
    }
    public void deletePlaylist(View v){
        shadowListener(null);
        ArrayList<String> playlistsNames=settings.getPlaylistsNames();
        ArrayList<String> playlists=settings.getPlaylists();
        int index=playlistsNames.indexOf(settings.getOpenedPlaylist());
        playlistsNames.remove(index);
        playlists.remove(index);
        settings.setPlaylistsNames(playlistsNames);
        settings.setPlaylists(playlists);
        LinearLayout playlistLayout=(LinearLayout) findViewById(R.id.playlistsLayout);
        playlistLayout.removeViewAt(index+1);
        choicePlaylist(ALL_SONGS);
    }
    public void showThisPlaylistMenu(View v){
        LinearLayout menu=(LinearLayout) findViewById(R.id.mainMenu);
        menu.setVisibility(View.GONE);
        menu=(LinearLayout) findViewById(R.id.thisPlaylistMenu);
        menu.setVisibility(View.VISIBLE);
    }
    private void choicePlaylist(@NonNull String playlistName){
        settings.setSelectedSongs(new ArrayList<File>());
        if (!playlistName.equals(settings.getOpenedPlaylist())){
        File[] playingPlayList;
        LinearLayout playlistLayout=(LinearLayout) findViewById(R.id.playlistsLayout);

        if (playlistName.equals(ALL_SONGS)) {
            changeMainPlaylistSign(getString(R.string.all_songs));
            changeSlidingPlaylistSign(getString(R.string.all_songs));
            }
        else {
            changeMainPlaylistSign(playlistName);
            changeSlidingPlaylistSign(playlistName);
        }
        for (int count=0;count<playlistLayout.getChildCount();count++){
            Button btn=(Button) playlistLayout.getChildAt(count);
            String btnText=btn.getText().toString();
            if (btnText.equals(getString(R.string.all_songs))) btnText=ALL_SONGS;
            if (btnText.equals(playlistName)) btn.setTextColor(getColor(R.color.colorAccent));
            else btn.setTextColor(getColor(R.color.lightText));
        }
        if (playlistName.equals(ALL_SONGS)||playlistName.equals(getString(R.string.all_songs))) {
            playingPlayList=settings.getAllSongs();
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.GONE);
        }
        else {
            int index=settings.getPlaylistsNames().indexOf(playlistName);
            String[] paths=settings.getPlaylists().get(index).split(">>");
            playingPlayList=toPlaylist(paths);
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.VISIBLE);
        }

        settings.setOpenedPlaylist(playlistName);
        UpdatePlaylistTread updatePlaylistTread=new UpdatePlaylistTread();
        updatePlaylistTread.execute(playingPlayList);
        }else {
            slidingPaneLayout.closePane();
        }
    }
    private void changeMainPlaylistSign(String newName){
        Button playlistNameSign=(Button) findViewById(R.id.playlistMainSign);
        playlistNameSign.setText(newName);
    }
    private void changeSlidingPlaylistSign(String newName){
        TextView playlistNameSignSliding=(TextView) findViewById(R.id.playlistSlidingSign);
        playlistNameSignSliding.setText(newName);
    }
    private File[] toPlaylist(@NonNull String[] paths){
        File[] playlist=new File[paths.length];
        for(int count=0;count<playlist.length;count++)playlist[count]=new File(paths[count]);
        return playlist;
    }

    public void shadowListener(View v){
        RelativeLayout contextLayout=(RelativeLayout) findViewById(R.id.contextLayout);
        for(int count=2;count<contextLayout.getChildCount();count++){
            contextLayout.getChildAt(count).setVisibility(View.GONE);
        }
    }
    public void openPlaylistPane(View v){slidingPaneLayout.openPane();}

    public void getPermissoin(){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.WAKE_LOCK},2);
    }

    public void showEditPanel(View v) {
        shadowListener(null);
        TertiaryMusicData musicData=new TertiaryMusicData();
        try{
            if (settings.getSelectedSongs().isEmpty()) editingSong=settings.getPlayingSong();
            else editingSong=settings.getSelectedSongs().get(0);
            musicData.tertiarySearch(editingSong,this);

        }catch (IOException e){e.printStackTrace();}
        EditText editText=(EditText) findViewById(R.id.songNameEdit);
        if (musicData.musicName!=null) editText.setText(musicData.musicName);
        editText=(EditText) findViewById(R.id.songAuthorEdit);
        if (musicData.musicAuthor!=null) editText.setText(musicData.musicAuthor);
        editText=(EditText) findViewById(R.id.songAlbumEdit);
        if (musicData.musicAlbum!=null) editText.setText(musicData.musicAlbum);
        editText=(EditText) findViewById(R.id.songKindEdit);
        if (musicData.musicKind!=null) editText.setText(musicData.musicKind);
        editText=(EditText) findViewById(R.id.songLyricsEdit);
        if (musicData.musicLyrics!=null) editText.setText(musicData.musicLyrics);
        if (musicData.imageName!=null){
            ImageButton image=(ImageButton) findViewById(R.id.songImageEdit);
            image.setImageDrawable(Drawable.createFromPath(getApplicationContext().getFilesDir().getAbsolutePath()+"/"+musicData.imageName));
            imagePath=getFilesDir().getAbsolutePath()+"/"+musicData.imageName;
            makeMessage(imagePath);
        }
        ScrollView editPanel=(ScrollView) findViewById(R.id.editPanel);
        editPanel.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }
    public void closeEditPanel(View v){
        ScrollView editPanel=(ScrollView) findViewById(R.id.editPanel);
        editPanel.setVisibility(View.GONE);
        TextInputEditText editText=(TextInputEditText) findViewById(R.id.songNameEdit);
        editText.getText().clear();
        editText=(TextInputEditText) findViewById(R.id.songAuthorEdit);
        editText.getText().clear();
        editText=(TextInputEditText) findViewById(R.id.songAlbumEdit);
        editText.getText().clear();
        editText=(TextInputEditText) findViewById(R.id.songKindEdit);
        editText.getText().clear();
        editText=(TextInputEditText) findViewById(R.id.songLyricsEdit);
        editText.getText().clear();
        ImageButton imageButton=(ImageButton) findViewById(R.id.songImageEdit);
        imageButton.setImageDrawable(getDrawable(android.R.drawable.ic_input_add));
        imagePath=null;
        editingSong=null;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }
    public void replaceTags(View v){
        EditText editText=(EditText) findViewById(R.id.songNameEdit);
        String newName=editText.getText().toString();
        if (newName.equals("")) newName=null;
        editText=(EditText) findViewById(R.id.songAuthorEdit);
        String newAuthor=editText.getText().toString();
        if (newAuthor.equals("")) newAuthor=null;
        editText=(EditText) findViewById(R.id.songAlbumEdit);
        String newAlbum=editText.getText().toString();
        if (newAlbum.equals("")) newAlbum=null;
        editText=(EditText) findViewById(R.id.songKindEdit);
        String newKind=editText.getText().toString();
        if (newKind.equals("")) newKind=null;
        String newImage=null;
        if (imagePath!=null) newImage=imagePath;
        editText=(EditText) findViewById(R.id.songLyricsEdit);
        String newLyrics=editText.getText().toString();
        ReplaceMusicData replaycer=new ReplaceMusicData();
        try{
            makeBackUp(editingSong);
            replaycer.replace(editingSong,newName,newAuthor,newAlbum,newKind,newLyrics,"   ",newImage,this);
            if (settings.getSelectedSongs().isEmpty()) setSong(editingSong);
        }catch (IOException e){e.printStackTrace();}
        closeEditPanel(null);
    }
    private void makeBackUp(File song){
        try{
            FileInputStream f=new FileInputStream(song);
            BufferedInputStream fin=new BufferedInputStream(f);
            FileOutputStream fos=openFileOutput(song.getName(),MODE_PRIVATE);
            byte[] forCopy=new byte[fin.available()];
            fin.read(forCopy);
            fin.close();
            f.close();
            fos.write(forCopy);
            fos.close();
        }catch (FileNotFoundException e){ makeMessage("Файл потерян");
        }catch (IOException e){reviveFromBackUp(song); makeMessage("Произошла какая-то ошибка, попробуйте снова...или не пробуйте, я надпись, а не начальник");}
    }
    private void reviveFromBackUp(File song){
        try{
            FileInputStream f=openFileInput(song.getName());
            BufferedInputStream fin=new BufferedInputStream(f);
            FileOutputStream fos=new FileOutputStream(song);
            byte[] forCopy=new byte[fin.available()];
            fin.read(forCopy);
            fin.close();
            f.close();
            fos.write(forCopy);
            fos.close();
        }catch (FileNotFoundException e){ makeMessage("Файл потерян");
        }catch (IOException e){makeMessage("Бекап потерян");}
    }
    public void showSongMenu(){
        LinearLayout songMenu=(LinearLayout) findViewById(R.id.songMenu);
        songMenu.setVisibility(View.VISIBLE);
        Button editButton=(Button) findViewById(R.id.editButton2);
        Button deleteFromPlaylistButton=(Button) findViewById(R.id.deleteFromPlaylistButton);
        if (settings.getSelectedSongs().size()==1) editButton.setVisibility(View.VISIBLE);
        else editButton.setVisibility(View.GONE);
        if (settings.getOpenedPlaylist().equals(ALL_SONGS)) deleteFromPlaylistButton.setVisibility(View.GONE);
        else deleteFromPlaylistButton.setVisibility(View.VISIBLE);
        Button shadow=(Button) findViewById(R.id.shadowBehindMenu);
        shadow.setVisibility(View.VISIBLE);
    }
    public void showMainMenu(View v){
        Button shadow=(Button) findViewById(R.id.shadowBehindMenu);
        shadow.setVisibility(View.VISIBLE);
        LinearLayout mainMenu=(LinearLayout) findViewById(R.id.mainMenu);
        mainMenu.setVisibility(View.VISIBLE);
    }

    private int getNavBarHeight(){
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    public ArrayList<File> sortByName(ArrayList<File> musics) throws NullPointerException{
        ArrayList<File> musics2=new ArrayList<File>();
        try{
        PrimaryMusicData musicData=new PrimaryMusicData();
        ArrayList<String> names=new ArrayList<String>();
        for (int count=0;count<musics.size();count++) names.add(musicData.getName(musics.get(count)));
        ArrayList<String> cloneArray=new ArrayList<String>();
        cloneArray.addAll(names);
        Collections.sort(cloneArray);
        for(int count=0;count<musics.size();count++){
            String name=cloneArray.get(count);
            int idx=names.indexOf(name);
            names.set(idx,null);
            musics2.add(musics.get(idx));
        }
        }catch (IOException e){}
        return musics2;
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case Pick_image:
                if(resultCode == RESULT_OK){
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        imagePath=getPath(imageUri);
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        ImageButton songImageEdit=(ImageButton) findViewById(R.id.songImageEdit);
                        songImageEdit.setImageBitmap(selectedImage);
                    } catch (FileNotFoundException e) {}
                }
        }}
    public void getSongImage(View v){
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent,Pick_image);
    }
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index =cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String s=cursor.getString(column_index);
        cursor.close();
        return s;
    }
    public void makeMessage(String mes){
        Toast.makeText(this, mes, Toast.LENGTH_LONG).show();
    }
    public void onAudioFocusChange(int focusChange) {
        playListener(null);
    }

    class UpdatePlaylistTread extends AsyncTask<File, File, Void> {
        private int songCount=0;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            playlistLayout.removeAllViews();
            progressBar.setVisibility(View.VISIBLE);
            songCountView.setVisibility(View.INVISIBLE);
            slidingPaneLayout.closePane();
        }
        @Override
        protected Void doInBackground(File... files) {
            for (final File song:files) {
                if (song!= null && song.isFile()) publishProgress(song);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            songCountView.setText(String.valueOf(songCount));
            songCountView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);

        }

        protected void onProgressUpdate(File... music) {
            super.onProgressUpdate(music);

            final File song=music[0];
            try{
            final SongButton newButton=new SongButton(MainActivity.this,song);newButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        ArrayList<File> selectedSongs=settings.getSelectedSongs();
                        if(!selectedSongs.contains(newButton.getSong())){
                            newButton.getChildAt(0).setBackgroundColor(getColor(R.color.colorAccent));
                            selectedSongs.add(newButton.getSong());
                            settings.setSelectedSongs(selectedSongs);
                        }
                        showSongMenu();
                        return true;
                    }
                });
                songCount++;
                playlistLayout.addView(newButton,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,195));
            }catch (RuntimeException e){e.printStackTrace();}
            }
    }
    class FolderScanner extends AsyncTask<File, Void, Void> {
        private ArrayList<File> allSongsPlaylist=new ArrayList<File>();
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            getPermissoin();
            int permissionStatus = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionStatus!=PackageManager.PERMISSION_GRANTED) onPreExecute();
        }
        @Override
        protected Void doInBackground(File... files) {
            for (File folder:files){
                scanFolder(folder);
            }
            allSongsPlaylist=sortByName(allSongsPlaylist);
            SharedPreferences settings2=getSharedPreferences(SETTINGS_NAME,Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings2.edit();
            File[] playlist=allSongsPlaylist.toArray(new File[allSongsPlaylist.size()]);
            String playlistString="";
            for (File file:playlist){
                if (file!=null) playlistString+=">>"+file.getAbsolutePath();
            }
            editor.putString(ALL_SONGS,playlistString);
            editor.apply();
            return null;
        }

        private void scanFolder(File folder){
            if (folder.isDirectory()){
                FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File directory, String fileName) {
                        File file=new File(directory+"/"+fileName);

                        return fileName.endsWith(".mp3")||file.isDirectory();
                    }
                };
                File[] folderFiles=folder.listFiles(filter);
                if (folderFiles.length>0){
                for (File item:folderFiles){
                    if (item.isDirectory()) scanFolder(item);
                    else {
                        allSongsPlaylist.add(item);}
                }}
            }
        }
    }
}
