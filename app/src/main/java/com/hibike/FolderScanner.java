package com.hibike;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import static com.hibike.Keys.Songs.ALL_SONGS_PLAYLIST_ID;

class FolderScanner extends AsyncTask<File, Void, Void> {
    private final MainActivity mainActivity;
    private Playlist allSongsPlaylist;

    public FolderScanner(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        allSongsPlaylist=new Playlist(ALL_SONGS_PLAYLIST_ID,mainActivity);
        allSongsPlaylist.clear();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mainActivity.getPermission();
        int permissionStatus = mainActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) onPreExecute();
    }

    @Override
    protected Void doInBackground(File... files) {
        for (File folder : files) {
            try {
                scanFolder(folder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try { allSongsPlaylist.sortByName(); } catch (NoSongFileException e) { e.printStackTrace(); }
        allSongsPlaylist.save();
        return null;
    }

    private void scanFolder(File folder) throws IOException {
        if (folder.isDirectory()) {
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File directory, String fileName) {
                    File file = new File(directory + "/" + fileName);

                    return fileName.endsWith(".mp3") || file.isDirectory();
                }
            };
            File[] folderFiles = folder.listFiles(filter);
            if (folderFiles.length > 0) {
                for (File item : folderFiles) {
                    if (item.isDirectory()) scanFolder(item);
                    else {
                        allSongsPlaylist.add(item);
                    }
                }
            }
        }
    }
}

class Scanning{
    final private Playlist allSongsPlaylist;
    final private Context context;
    Scanning(Context _context){
        context=_context;
        allSongsPlaylist=new Playlist(ALL_SONGS_PLAYLIST_ID, context);
        allSongsPlaylist.clear();
    }
    public void scanFolder(File folder) throws IOException {
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
    public Playlist getAllSongsPlaylist(){
        return allSongsPlaylist;
    }
}
