package com.hibike;

import android.content.Context;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ReplaceMusicData {
    public byte[] arrayOf2EmputyBytes={0,0};
    public void replace(File music, String musicName, String musicAuthor, String musicAlbum, String musicKind, String musicLyrics, String musicLyricsLanguage, String imageName, Context context)
    throws IOException{
        TertiaryMusicData musicData = new TertiaryMusicData();
        musicData.tertiarySearch(music, context);
            Charset charsets[]={StandardCharsets.ISO_8859_1,StandardCharsets.UTF_16};
            int allTagsSize=10;
            if (musicName!=null) allTagsSize+=11+musicName.getBytes(charset(musicName,charsets)).length;
            if (musicAuthor!=null) allTagsSize+=11+musicAuthor.getBytes(charset(musicAuthor,charsets)).length;
            if (musicAlbum!=null) allTagsSize+=11+musicAlbum.getBytes(charset(musicAlbum,charsets)).length;
            if (musicKind!=null) allTagsSize+=11+musicKind.getBytes(charset(musicKind,charsets)).length;
            if (musicLyrics!=null) allTagsSize+=11+musicLyrics.getBytes(charset(musicLyrics,charsets)).length+musicLyricsLanguage.getBytes(charset(musicLyricsLanguage,charsets)).length;
            if (imageName!=null) {
                FileInputStream image=new FileInputStream(imageName);
                String mymeType="image/";
                String end="";
                for(int count=imageName.length()-1;imageName.toCharArray()[count]!='.';count--) end=imageName.toCharArray()[count]+end;
                mymeType+=end;
                allTagsSize+=11+mymeType.getBytes().length+1+image.available()+2;
            }
            FileInputStream toGetOtherTags=context.openFileInput("TagsRecord.cut");
            File tagsRecord=new File(context.getFilesDir().getAbsolutePath()+"/TagsRecord.cut");
            allTagsSize+=toGetOtherTags.available();
            FileInputStream fin=new FileInputStream(music);
            fin.skip(musicData.allTagsSize);
            byte[] arrayForRecord=new byte[fin.available()];

            FileOutputStream fos=context.openFileOutput("record.cut",Context.MODE_APPEND);
            File record=new File(context.getFilesDir().getAbsolutePath()+"/record.cut");
            fin.read(arrayForRecord);
            fos.write(arrayForRecord);
            fos.close();
            fin.close();

            fos=new FileOutputStream(music);
            fos.write("ID3".getBytes()); fos.write(3); fos.write(0); fos.write(0); fos.write(getSizeOfTags(allTagsSize));
            if(musicName!=null){
                Charset encoder=charset(musicName,charsets);
                fos.write("TIT2".getBytes());
                fos.write(getSizeOfFrame(musicName.getBytes(encoder).length));
                fos.write(arrayOf2EmputyBytes);
                switch (encoder.name()){
                    case "ISO-8859-1":
                        fos.write(0);
                        break;
                    case "UTF-16":
                        fos.write(1);
                        break;
                }
                fos.write(musicName.getBytes(encoder));
            }
            if(musicAuthor!=null){
                fos.write("TPE1".getBytes());
                fos.write(getSizeOfFrame(musicAuthor.getBytes(charset(musicAuthor,charsets)).length));
                fos.write(arrayOf2EmputyBytes);
                switch (charset(musicAuthor,charsets).name()){
                    case "ISO-8859-1":
                        fos.write(0);
                        break;
                    case "UTF-16":
                        fos.write(1);
                        break;
                }
                fos.write(musicAuthor.getBytes(charset(musicAuthor,charsets)));
            }
            if(musicAlbum!=null){
                fos.write("TALB".getBytes());
                fos.write(getSizeOfFrame(musicAlbum.getBytes(charset(musicAlbum,charsets)).length));
                fos.write(arrayOf2EmputyBytes);
                switch (charset(musicAlbum,charsets).name()){
                    case "ISO-8859-1":
                        fos.write(0);
                        break;
                    case "UTF-16":
                        fos.write(1);
                        break;
                }
                fos.write(musicAlbum.getBytes(charset(musicAlbum,charsets)));
            }
            if(musicKind!=null){
                fos.write("TALB".getBytes());
                fos.write(getSizeOfFrame(musicKind.getBytes(charset(musicKind,charsets)).length));
                fos.write(arrayOf2EmputyBytes);
                switch (charset(musicKind,charsets).name()){
                    case "ISO-8859-1":
                        fos.write(0);
                        break;
                    case "UTF-16":
                        fos.write(1);
                        break;
                }
                fos.write(musicKind.getBytes(charset(musicKind,charsets)));
            }
            if(musicLyrics!=null){
                fos.write("USLT".getBytes());
                fos.write(getSizeOfFrame(musicLyrics.getBytes(charset(musicLyrics,charsets)).length+musicLyricsLanguage.getBytes(charset(musicLyrics,charsets)).length+1));
                fos.write(arrayOf2EmputyBytes);
                switch (charset(musicLyrics,charsets).name()){
                    case "ISO-8859-1":
                        fos.write(0);
                        break;
                    case "UTF-16":
                        fos.write(1);
                        break;
                }
                fos.write(musicLyricsLanguage.getBytes(charset(musicLyrics,charsets)));
                fos.write(0);
                fos.write(musicLyrics.getBytes(charset(musicLyrics,charsets)));
            }
            arrayForRecord=new byte[toGetOtherTags.available()];
            toGetOtherTags.read(arrayForRecord);
            fos.write(arrayForRecord);
            toGetOtherTags.close();
            if(imageName!=null){
                FileInputStream toGetImage=new FileInputStream(imageName);
                arrayForRecord=new byte[toGetImage.available()];
                toGetImage.read(arrayForRecord);
                String mymeType="image/";
                String end="";
                for(int count=imageName.length()-1;imageName.toCharArray()[count]!='.';count--) end=imageName.toCharArray()[count]+end;
                mymeType+=end;
                fos.write("APIC".getBytes());
                fos.write(getSizeOfFrame(mymeType.getBytes().length+1+1+arrayForRecord.length));
                fos.write(arrayOf2EmputyBytes);
                fos.write(0);
                fos.write(mymeType.getBytes()); fos.write(0);
                fos.write(3);fos.write(0);
                fos.write(arrayForRecord);
                toGetImage.close();
            }


            fin=context.openFileInput("record.cut");
            arrayForRecord=new byte[fin.available()];
            fin.read(arrayForRecord);
            fos.write(arrayForRecord);
            fos.close();
            fin.close();
            record.delete();
            tagsRecord.delete();
    }
    public byte[] getSizeOfTags(int allTagsSize){
        byte[] array={0,0,0,0};
        int maxStap;
        allTagsSize-=10;
        for(maxStap=0;allTagsSize>Math.pow(2,maxStap+1);maxStap++);
        while (maxStap>=0){
            if(allTagsSize>=Math.pow(2,maxStap)){
                allTagsSize-=Math.pow(2,maxStap);
                switch(maxStap){
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6: array[3]+=Math.pow(2,maxStap); break;
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13: array[2]+=Math.pow(2,maxStap-7);  break;
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20: array[1]+=Math.pow(2,maxStap-14); break;
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27: array[0]+=Math.pow(2,maxStap-21); break;
                }
            }
            maxStap--;
        }
        return array;
    }
    public byte[] getSizeOfFrame(int size){
        size++;
        byte[] array={0,0,0,0};
        int maxStap;
        for(maxStap=0;size>=Math.pow(2,maxStap+1);maxStap++);
        while (maxStap>=0){
            if (size>=Math.pow(2,maxStap)){
                size-=Math.pow(2,maxStap);
                switch(maxStap){
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7: array[3]+=Math.pow(2,maxStap); break;
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15: array[2]+=Math.pow(2,maxStap-8); break;
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23: array[1]+=Math.pow(2,maxStap-16); break;
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31: array[0]+=Math.pow(2,maxStap-24); break;
                }}
            maxStap--;

        }
        return array;
    }
    public Charset charset(String value, Charset charsets[]) {
        Charset probe = StandardCharsets.UTF_8;
        for(int count=0;count<charsets.length;count++) if(value.equals(convert(convert(value, charsets[count], probe), probe,charsets[count]))) {
            return charsets[count];
        }
        return StandardCharsets.UTF_16;
    }
    public String convert(String value, Charset fromEncoding, Charset toEncoding) {
        return new String(value.getBytes(fromEncoding),toEncoding);
    }
}