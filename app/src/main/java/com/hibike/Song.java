package com.hibike;

import java.io.File;

public class Song {
    //Technical
    public int id;
    public String path;

    //For user
    public String name;
    public String author;
    public String album;
    public int duration;

    Song(File song){

    }

    Song(String songPath){

    }

    //Save song data in device memory, return true if saving is successful and false if not
    public boolean saveSong(){
        //TODO: make method that return true if saving is successful and false if not
        return true;
    }

}
