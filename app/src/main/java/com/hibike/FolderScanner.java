package com.hibike;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import static com.hibike.Keys.Settings.ALL_SONGS;
import static com.hibike.Keys.Settings.SETTINGS_NAME;

class FolderScanner extends AsyncTask<File, Void, Void> {
    private final MainActivity mainActivity;
    private ArrayList<File> allSongsPlaylist = new ArrayList<File>();

    public FolderScanner(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mainActivity.getPermissoin();
        int permissionStatus = mainActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) onPreExecute();
    }

    @Override
    protected Void doInBackground(File... files) {
        for (File folder : files) {
            scanFolder(folder);
        }
        allSongsPlaylist = mainActivity.sortByName(allSongsPlaylist);
        SharedPreferences settings2 = mainActivity.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings2.edit();
        File[] playlist = allSongsPlaylist.toArray(new File[allSongsPlaylist.size()]);
        String playlistString = "";
        for (File file : playlist) {
            if (file != null) playlistString += ">>" + file.getAbsolutePath();
        }
        editor.putString(ALL_SONGS, playlistString);
        editor.apply();
        return null;
    }

    private void scanFolder(File folder) {
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
