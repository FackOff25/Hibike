package com.hibike;

import java.io.FileNotFoundException;
import java.util.NoSuchElementException;

class NoMoreSongException extends NoSuchElementException {
    public NoMoreSongException(int id){
        initCause(new Throwable("Song with id="+id+" doesn't saved in memory"));
    }
}

class NoSongFileException extends FileNotFoundException {
    public NoSongFileException(String path){
        initCause(new Throwable("There is no file on "+path));
    }
}
