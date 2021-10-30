package com.hibike;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static com.hibike.Keys.Settings.*;
import static com.hibike.Keys.Notification.*;
import static com.hibike.Keys.Broadcast.*;
import static com.hibike.Keys.ReplayMods.*;
import static com.hibike.Keys.Songs.ALL_SONGS_PLAYLIST_ID;

public class MainActivity extends AppCompatActivity implements OnAudioFocusChangeListener{
    private final int Pick_image = 1;

    private BroadcastReceiver receiver;

    private boolean touched=false;
    private Song editingSong;
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
            getPermission();
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
                getPermission();
            }

            File[] files={Environment.getExternalStorageDirectory()};
            Playlist allSongsPlaylist;
            Scanning scanning=new Scanning(this);
            for (File folder:files){
                try { scanning.scanFolder(folder); }catch (IOException e) { e.printStackTrace(); }
            }
            allSongsPlaylist=scanning.getAllSongsPlaylist();
            try {allSongsPlaylist.sortByName();} catch (NoSongFileException e) { e.printStackTrace(); }
            allSongsPlaylist.save();

            settings.setNotFirstLoad();
        }else{
            FolderScanner scanner=new FolderScanner(this);
            scanner.execute(Environment.getExternalStorageDirectory());
        }
        //Starting Service
        Intent startIntent = new Intent(MainActivity.this, HibikeService.class);
        startIntent.setAction(START_FOREGROUND);
        startService(startIntent);
        //Intent receiver starting
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
                        try {
                            Song song = new Song(path, context);
                            setSong(song.getId());
                        } catch (IOException e) { e.printStackTrace(); }
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
        setSong(settings.getPlayingSong());

        addPlaylist(ALL_SONGS,ALL_SONGS_PLAYLIST_ID);
        ArrayList<Playlist> playlists=settings.getPlaylists();
        if(playlists!=null){
            for(int count=1;count<=playlists.size();count++){
                Playlist thisPlaylist=playlists.get(count);
                addPlaylist(thisPlaylist.getName(),thisPlaylist.getId());
            }
        }
        //Setting
        Playlist openedPlaylist=new Playlist(settings.getOpenedPlaylist(), this);
        String playlistName=openedPlaylist.getName();
        if (openedPlaylist.getId()==ALL_SONGS_PLAYLIST_ID) playlistName=getString(R.string.all_songs);
        changeSlidingPlaylistSign(playlistName);

        LinearLayout playlistLayout=(LinearLayout) findViewById(R.id.playlistsLayout);
        //Setting which playlist is playing for left panel
        for (int count=0;count<playlistLayout.getChildCount();count++){
            Button btn=(Button) playlistLayout.getChildAt(count);
            String btnText=btn.getText().toString();
            if (btnText.equals(playlistName)) btn.setTextColor(getColor(R.color.colorAccent));
            else btn.setTextColor(getColor(R.color.lightText));
        }

        if (openedPlaylist.getId()==ALL_SONGS_PLAYLIST_ID) {
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.GONE);
        }else {
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.VISIBLE);
        }
        UpdatePlaylistTread updatePlaylistTread=new UpdatePlaylistTread();
        updatePlaylistTread.execute(openedPlaylist.toFileArray());
    }
    public void onStart(){
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),new IntentFilter(BROADCAST_ACTION));
    }

    public void initViews(){
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        SlidingUpPanelLayout umano=(SlidingUpPanelLayout) findViewById(R.id.umano);                 //Umano is bottom sliding panel
        umano.setPanelHeight(getNavBarHeight()+175);
        umano.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) { }

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
                songTime.setText(convertFromTimeFormat(progress));
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
    //Change UI according to new song
    public void setSong(int id){
        Song song = null;
        try { song = new Song(id, this); } catch (NoSongFileException e) { e.printStackTrace(); }
        ScrollView scrollView=(ScrollView) findViewById(R.id.lirycsScroll);
        scrollView.scrollTo(0,0);
        SecondaryMusicData musicData=new SecondaryMusicData();
        try { musicData.secondarySearch(new File(song.getPath()), this); } catch (IOException e) { e.printStackTrace(); }

        ImageButton btn1=(ImageButton) findViewById(R.id.playButton);
        ImageButton btn2=(ImageButton) findViewById(R.id.playButton2);
        if (settings.getIsPlay()){
            btn1.setImageDrawable(getDrawable(R.drawable.pause));
            btn2.setImageDrawable(getDrawable(R.drawable.pause));
        }else{
            btn1.setImageDrawable(getDrawable(R.drawable.play));
            btn2.setImageDrawable(getDrawable(R.drawable.play));
        }
        //setting
        TextView songName=(TextView) findViewById(R.id.songName);
        TextView songAuthor=(TextView) findViewById(R.id.songAuthor);
        songName.setText(song.getName());
        songAuthor.setText(song.getAuthor());

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
        songDuration.setText(convertFromTimeFormat(song.getDuration()));

        SeekBar playBar=(SeekBar) findViewById(R.id.seekBar);
        playBar.setMax((int) song.getDuration());
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
        Intent toService=new Intent(MainActivity.this,HibikeService.class);
        toService.setAction(SHAKE);
        startService(toService);

        ImageButton btn=(ImageButton) v;
        boolean isRandom=!settings.getIsRandom();
        if (isRandom) {
            btn.setImageDrawable(getDrawable(R.drawable.shake));
            makeMessage("Перемешивание включено");
        }else {
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
    //Unselect song buttons
    private void clearBackgrounds(){
        LinearLayout mainLayout=(LinearLayout) findViewById(R.id.mainLayout);
        for (int count=0;count<mainLayout.getChildCount();count++){
            SongButton btn=(SongButton) mainLayout.getChildAt(count);
            btn.getChildAt(0).setBackground(getDrawable(R.drawable.button));
        }
    }
    //Adds songs to Playlist
    private void addToPlaylist(int playlistId, Playlist songs){
        Playlist playlist=new Playlist(playlistId, this);
        addToPlaylist(playlist, songs);
    }
    private void addToPlaylist(int playlistId, ArrayList<Song> songs){
        Playlist playlist=new Playlist(playlistId, this);
        addToPlaylist(playlist, songs);
    }
    private void addToPlaylist(Playlist playlist, Playlist songs){
        for (Integer song:songs) playlist.add(song);
        playlist.save();
    }
    private void addToPlaylist(Playlist playlist, ArrayList<Song> songs){
        for (Song song:songs) playlist.add(song.getId());
        playlist.save();
    }

    public void deleteFromPlaylist(View v){
        Playlist openedPlaylist =new Playlist(settings.getOpenedPlaylist(), this);
        Playlist selectedSongs=settings.getSelectedSongs();

        for (int song:selectedSongs) openedPlaylist.remove(song);
        openedPlaylist.save();


        //File[] playingPlayList;
        LinearLayout playlistLayout=(LinearLayout) findViewById(R.id.playlistsLayout);
        /*for (int count=0;count<playlistLayout.getChildCount();count++){
            Button btn=(Button) playlistLayout.getChildAt(count);
            String btnText=btn.getText().toString();

            if (btnText.equals(getString(R.string.all_songs))) btnText=ALL_SONGS;
            if (btnText.equals(openedPlaylist.getName())) btn.setTextColor(getColor(R.color.colorAccent));
            else btn.setTextColor(getColor(R.color.lightText));
        }
        if (playlist.equals(ALL_SONGS)|| playlist.equals(getString(R.string.all_songs))) {
            playingPlayList=settings.getAllSongs();
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.GONE);
        }
        else {
            index=settings.getPlaylistsNames().indexOf(playlist);
            String[] paths=settings.getPlaylists().get(index).split(">>");
            playingPlayList=toPlaylist(paths);
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.VISIBLE);
        }*/
        //Most likely it will fuck up updating

        UpdatePlaylistTread updatePlaylistTread=new UpdatePlaylistTread();
        updatePlaylistTread.execute(openedPlaylist.toFileArray());

        shadowListener(null);
    }
    //TODO: make normal buttons deleting
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
        Playlist newPlaylist =new Playlist(newPlaylistEdit.getText().toString(), this);
        newPlaylist.addAll(settings.getSelectedSongs());
        newPlaylist.save();
        /*
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
        */
        addPlaylist(newPlaylistEdit.getText().toString(), newPlaylist);
        clearBackgrounds();
        closeNewPlaylistMenu(null);
    }

    //Creating new playlist button on left panel
    public void addPlaylist(final String playlistName, int playlistId){
        addPlaylist(playlistName, new Playlist(playlistId,this));
    }
    public void addPlaylist(final String playlistName,final Playlist playlist){
        LinearLayout playlistsLayout=(LinearLayout) findViewById(R.id.playlistsLayout);
        Button newPlaylist=new Button(this);
        if (!playlistName.equals(ALL_SONGS)) newPlaylist.setText(playlistName);
        else newPlaylist.setText(R.string.all_songs);

        newPlaylist.setGravity(Gravity.CENTER);
        /*final File[] songsInPlaylist=new File[playlist.length];
        for(int count=0;count<songsInPlaylist.length;count++){
            songsInPlaylist[count]=new File(playlist[count]);
            if (!songsInPlaylist[count].exists()) songsInPlaylist[count]=null;
        }*/
        //Button settings
        newPlaylist.setPadding(20,20,20,20);
        newPlaylist.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                choicePlaylist(playlist.getId());
                slidingPaneLayout.closePane();
            }
        });
        playlistsLayout.addView(newPlaylist,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        //Adding option to playlist adding panel
        if (!playlistName.equals(ALL_SONGS)){
            LinearLayout playlistMenu=(LinearLayout) findViewById(R.id.playlistMenu);
            Button newPlaylistInMenu=new Button(this);
            newPlaylistInMenu.setPadding(10,10,10,10);
            newPlaylistInMenu.setText(playlistName);
            newPlaylistInMenu.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    addToPlaylist(playlist,settings.getSelectedSongs());
                    LinearLayout playlistMenu=(LinearLayout) findViewById(R.id.playlistMenu);
                    playlistMenu.setVisibility(View.GONE);
                    Button shadow=(Button) findViewById(R.id.shadowBehindMenu);
                    shadow.setVisibility(View.GONE);
                    settings.setNoSelectedSongs();
                    clearBackgrounds();
                }
            });
            playlistMenu.addView(newPlaylistInMenu,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        }
    }
    //Shows menu for editing menu
    public void showEditPlaylist(View v){
        shadowListener(null);
        LinearLayout editPlaylistMenu=(LinearLayout) findViewById(R.id.editPlaylistMenu);
        editPlaylistMenu.setVisibility(View.VISIBLE);
    }
    public void editPlaylist(View v){
        EditText newName=(EditText) findViewById(R.id.playlistNameEdit);
        String newNameString=newName.getText().toString();
        Playlist openedPlaylist= new Playlist(settings.getOpenedPlaylist(), this);
        String oldNameString=openedPlaylist.getName();
        openedPlaylist.setName(newNameString);
        openedPlaylist.save();
        changeMainPlaylistSign(newNameString);
        /*
        ArrayList<String> playlistsNames=settings.getPlaylistsNames();
        playlistsNames.set(playlistsNames.indexOf(settings.getOpenedPlaylist()),newName.getText().toString());
        settings.setPlaylists(playlistsNames);

        if(settings.getPlayingPlayListName().equals(settings.getOpenedPlaylist())){
            changeSlidingPlaylistSign(newName.getText().toString());
        }
        */
        LinearLayout playlistLayout=(LinearLayout) findViewById(R.id.playlistsLayout);
        Button btn;

        for (int count=0;count<playlistLayout.getChildCount();count++){
            btn=(Button) playlistLayout.getChildAt(count);
            if (btn.getText().toString().equals(oldNameString)) {
                btn.setText(newName.getText().toString());
                break;
            }
        }

        newName.setText("");
        shadowListener(null);
    }
    public void deletePlaylist(View v){
        shadowListener(null);
        Playlist openedPlaylist=new Playlist(settings.getOpenedPlaylist(), this);

        openedPlaylist.delete();
        /*ArrayList<String> playlistsNames=settings.getPlaylistsNames();
        ArrayList<String> playlists=settings.getPlaylists();
        int index=playlistsNames.indexOf(settings.getOpenedPlaylist());
        playlistsNames.remove(index);
        playlists.remove(index);
        settings.setPlaylistsNames(playlistsNames);
        settings.setPlaylists(playlists);
        */
        LinearLayout playlistLayout=(LinearLayout) findViewById(R.id.playlistsLayout);
        playlistLayout.removeViewAt(settings.getPlaylists().indexOf(openedPlaylist)+1);
        choicePlaylist(ALL_SONGS_PLAYLIST_ID);
    }
    public void showThisPlaylistMenu(View v){
        LinearLayout menu=(LinearLayout) findViewById(R.id.mainMenu);
        menu.setVisibility(View.GONE);
        menu=(LinearLayout) findViewById(R.id.thisPlaylistMenu);
        menu.setVisibility(View.VISIBLE);
    }
    private void choicePlaylist(int playlistId){

        if (playlistId==settings.getOpenedPlaylist()){
            slidingPaneLayout.closePane();
            return;
        }
        //settings.setNoSelectedSongs();
        //File[] playingPlayList;

        Playlist playlist=new Playlist(playlistId, this);
        //Changing playlistName sign
        if (playlistId==ALL_SONGS_PLAYLIST_ID) {
            changeMainPlaylistSign(getString(R.string.all_songs));
            changeSlidingPlaylistSign(getString(R.string.all_songs));
        }else{
            changeMainPlaylistSign(playlist.getName());
            changeSlidingPlaylistSign(playlist.getName());
        }
        //Select playlist on left panel
        LinearLayout playlistLayout=(LinearLayout) findViewById(R.id.playlistsLayout);
        Button btn;
        for (int count=0;count<playlistLayout.getChildCount();count++){
            btn=(Button) playlistLayout.getChildAt(count);
            String btnText=btn.getText().toString();
            if (btnText.equals(getString(R.string.all_songs))) btnText=ALL_SONGS;
            if (btnText.equals(playlist.getName())) btn.setTextColor(getColor(R.color.colorAccent));
            else btn.setTextColor(getColor(R.color.lightText));
        }

        if (playlist.getName().equals(ALL_SONGS)) {
            //playingPlayList=settings.getAllSongs();
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.GONE);
        }
        else {
            /*int index=settings.getPlaylistsNames().indexOf(playlistName);
            String[] paths=settings.getPlaylists().get(index).split(">>");
            playingPlayList=toPlaylist(paths);*/
            ImageButton moreButton=(ImageButton) findViewById(R.id.mainMenuButton);
            moreButton.setVisibility(View.VISIBLE);
        }

        settings.setOpenedPlaylist(playlistId);
        UpdatePlaylistTread updatePlaylistTread=new UpdatePlaylistTread();
        updatePlaylistTread.execute(playlist.toFileArray());
    }

    private void changeMainPlaylistSign(String newName){
        Button playlistNameSign=(Button) findViewById(R.id.playlistMainSign);
        playlistNameSign.setText(newName);
    }
    private void changeSlidingPlaylistSign(String newName){
        TextView playlistNameSignSliding=(TextView) findViewById(R.id.playlistSlidingSign);
        playlistNameSignSliding.setText(newName);
    }

    /*From old playlist system
    private File[] toPlaylist(@NonNull String[] paths){
        File[] playlist=new File[paths.length];
        for(int count=0;count<playlist.length;count++)playlist[count]=new File(paths[count]);
        return playlist;
    }*/

    public void shadowListener(View v){
        RelativeLayout contextLayout=(RelativeLayout) findViewById(R.id.contextLayout);
        for(int count=2;count<contextLayout.getChildCount();count++){
            contextLayout.getChildAt(count).setVisibility(View.GONE);
        }
    }
    public void openPlaylistPane(View v){slidingPaneLayout.openPane();}
    //Getting Permission from android
    public void getPermission(){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.WAKE_LOCK},2);
    }

    public void showEditPanel(View v) {
        shadowListener(null);
        TertiaryMusicData musicData=new TertiaryMusicData();
        try{
            if (settings.getSelectedSongs().isEmpty()) editingSong=new Song(settings.getPlayingSong(), this);
            else editingSong=new Song(settings.getSelectedSongs().get(0), this);
            musicData.tertiarySearch(editingSong,this);
        }catch (IOException e){e.printStackTrace();}

        EditText editText=(EditText) findViewById(R.id.songNameEdit);
        editText.setText(editingSong.getName());
        editText=(EditText) findViewById(R.id.songAuthorEdit);
        editText.setText(editingSong.getAuthor());
        editText=(EditText) findViewById(R.id.songAlbumEdit);
        editText.setText(editingSong.getAlbum());

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
        EditText editText;
        String newName;
        String newAuthor;
        String newAlbum;
        String newKind;
        String newImage=null;
        String newLyrics;

        editText=(EditText) findViewById(R.id.songNameEdit);
        newName=editText.getText().toString();
        if (newName.equals("")) newName=null;

        editText=(EditText) findViewById(R.id.songAuthorEdit);
        newAuthor=editText.getText().toString();
        if (newAuthor.equals("")) newAuthor=null;

        editText=(EditText) findViewById(R.id.songAlbumEdit);
        newAlbum=editText.getText().toString();
        if (newAlbum.equals("")) newAlbum=null;

        editText=(EditText) findViewById(R.id.songKindEdit);
        newKind=editText.getText().toString();
        if (newKind.equals("")) newKind=null;

        if (imagePath!=null) newImage=imagePath;

        editText=(EditText) findViewById(R.id.songLyricsEdit);
        newLyrics=editText.getText().toString();

        ReplaceMusicData replacer=new ReplaceMusicData();
        try{
            makeBackUp(editingSong.toFile());
            replacer.replace(editingSong.toFile(),newName,newAuthor,newAlbum,newKind,newLyrics,"   ",newImage,this);
            if (settings.getSelectedSongs().isEmpty()) setSong(editingSong.getId());
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
    //Shows menu appeared after long click on SongBatton
    public void showSongMenu(){
        findViewById(R.id.songMenu).setVisibility(View.VISIBLE);

        Button editButton=(Button) findViewById(R.id.editButton2);
        if (settings.getSelectedSongs().isEmpty()) editButton.setVisibility(View.VISIBLE);
        else editButton.setVisibility(View.GONE);

        Button deleteFromPlaylistButton=(Button) findViewById(R.id.deleteFromPlaylistButton);
        if (settings.getOpenedPlaylist()==ALL_SONGS_PLAYLIST_ID) deleteFromPlaylistButton.setVisibility(View.GONE);
        else deleteFromPlaylistButton.setVisibility(View.VISIBLE);

        findViewById(R.id.shadowBehindMenu).setVisibility(View.VISIBLE);
    }
    public void showMainMenu(View v){
        findViewById(R.id.shadowBehindMenu).setVisibility(View.VISIBLE);
        findViewById(R.id.mainMenu).setVisibility(View.VISIBLE);
    }
    //Getting of top and bottom android bars
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

    /*public ArrayList<File> sortByName(ArrayList<File> musics) throws NullPointerException{
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
    }*/

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
        }
    }
    //using on click for getting image by GetImage button on song edit panel
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
    //Making popping massage
    public void makeMessage(String mes){
        Toast.makeText(this, mes, Toast.LENGTH_LONG).show();
    }
    //Stop playing when something other starts playing and vice versa
    public void onAudioFocusChange(int focusChange) {
        playListener(null);
    }

    private String convertFromTimeFormat(long time){
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
    //Iirc its multiTread thing
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
            final SongButton newButton=new SongButton(song, MainActivity.this);newButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Playlist selectedSongs=settings.getSelectedSongs();
                        if(!selectedSongs.contains(newButton.getSong().getId())){
                            newButton.getChildAt(0).setBackgroundColor(getColor(R.color.colorAccent));
                            selectedSongs.add(newButton.getSong().getId());
                            settings.setSelectedSongs(selectedSongs);
                        }
                        showSongMenu();
                        return true;
                    }
                });
                songCount++;
                playlistLayout.addView(newButton,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,195));
            }catch (RuntimeException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
            }
    }
}
