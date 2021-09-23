package com.hibike;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


class PrimaryMusicData{
    public boolean haveTags=true;
    public String musicName;
    public String musicAuthor="Неизвестен";
    public String musicAlbum;
    public String imageName;

    static public String getName(File music) throws IOException, NullPointerException{
        String name="";
        FileInputStream f=new FileInputStream(music);
        BufferedInputStream fin=new BufferedInputStream(f);
        byte[] arrayForTags=new byte[3];
        fin.read(arrayForTags);
        String tag=new String(arrayForTags, StandardCharsets.US_ASCII);
        if (!tag.equals("ID3")){String fullFileName=music.getName();
            for (int count=0;fullFileName.toCharArray()[count]!='.';count++) name+=fullFileName.toCharArray()[count];
            return name;
        }else {
            int chr;
            boolean hasEnd=false;
            int ver=fin.read();
            fin.skip(6);    //Skipping of uninformative bytes
            arrayForTags=new byte[4];
            fin.read(arrayForTags);
            tag=new String(arrayForTags, StandardCharsets.US_ASCII);
            while (!hasEnd){
                String tagSize="";
                for(int count=0;count<4;count++) tagSize+=String.format("%8s", Integer.toBinaryString(fin.read() & 0xFF)).replace(" ", "0");
                int size=intFromBinar(tagSize);
                fin.skip(2);
                byte[] arrayFiller;
                Charset encoder;
                switch (tag){
                    case "TIT2":
                        encoder=getCharset(ver,fin);
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        name=new String(arrayFiller,encoder);
                        hasEnd=true;
                        break;
                    case "TPE1":
                    case "TALB":
                    case "APIC":
                    case "USLT":
                    case "TCON":
                        /*rare-usable tags*/
                    case "AENC": case "COMM": case "COMR": case "ENCR": case "EQUA": case "ETCO":
                    case "GEOB": case "GRID": case "IPLS": case "LINK": case "MCDI": case "MLLT":
                    case "OWNE": case "PRIV": case "PCNT": case "POPM": case "POSS": case "RBUF":
                    case "RVAD": case "RVRB": case "SYLT": case "SYTC": case "TBPM": case "TCOM":
                    case "TCOP": case "TDAT": case "TDLY": case "TENC": case "TEXT": case "TFLT":
                    case "TIME": case "TIT1": case "TIT3": case "TKEY": case "TLAN": case "TLEN":
                    case "TMED": case "TOAL": case "TOFN": case "TOLY": case "TOPE": case "TORY":
                    case "TOWN": case "TPE2": case "TPE3": case "TPE4": case "TPOS": case "TPUB":
                    case "TRCK": case "TRDA": case "TRSN": case "TRSO": case "TSIZ": case "TSRC":
                    case "TSSE": case "TYER": case "TXXX": case "UFID": case "USER": case "WCOM":
                    case "WCOP": case "WOAF": case "WOAR": case "WOAS": case "WORS": case "WPAY":
                    case "WPUB": case "WXXX":
                    /*Tags from ID3v2.4.0, but some SOB add them to ID3v2.3.0.
                    Just skip and delete for prise of imperor!*/
                    case "TSOT": case "TSOP": case "ASPI": case "EQU2": case "RVA2": case "SEEK":
                    case "SIGN": case "TDEN": case "TDOR": case "TDRC": case "TDRL": case "TDTG":
                    case "TIPL": case "TMCL": case "TMOO": case "TPRO": case "TSOA": case "TSST":
                        fin.skip(size);
                        break;
                    default:
                        hasEnd=true;
                        break;
                }
                if (!hasEnd) {
                    arrayForTags=new byte[4];
                    fin.read(arrayForTags);
                    tag=new String(arrayForTags, StandardCharsets.US_ASCII);
                }
            }
            fin.close();
        }
        if (!name.equals("")) return name;
        else {String fullFileName=music.getName();
            for (int count=0;fullFileName.toCharArray()[count]!='.';count++) name+=fullFileName.toCharArray()[count];
            return name;
        }
    }
    static public String getName(String path)throws IOException, NullPointerException{
        return getName(new File(path));
    }

    static public String getAuthor(File music) throws IOException, NullPointerException{
        String author="";
        FileInputStream f=new FileInputStream(music);
        BufferedInputStream fin=new BufferedInputStream(f);
        byte[] arrayForTags=new byte[3];
        fin.read(arrayForTags);
        String tag=new String(arrayForTags, StandardCharsets.US_ASCII);
        if (!tag.equals("ID3")) return author;
        else {
            int chr;
            boolean hasEnd=false;
            int ver=fin.read();
            fin.skip(6);    //Skipping of uninformative bytes
            arrayForTags=new byte[4];
            fin.read(arrayForTags);
            tag=new String(arrayForTags, StandardCharsets.US_ASCII);
            while (!hasEnd){
                String tagSize="";
                for(int count=0;count<4;count++) tagSize+=String.format("%8s", Integer.toBinaryString(fin.read() & 0xFF)).replace(" ", "0");
                int size=intFromBinar(tagSize);
                fin.skip(2);
                byte[] arrayFiller;
                Charset encoder;
                switch (tag){
                    case "TPE1":
                        encoder=getCharset(ver,fin);
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        author=new String(arrayFiller,encoder);
                        break;
                    case "TIT2":
                    case "TALB":
                    case "APIC":
                    case "USLT":
                    case "TCON":
                        /*rare-usable tags*/
                    case "AENC": case "COMM": case "COMR": case "ENCR": case "EQUA": case "ETCO":
                    case "GEOB": case "GRID": case "IPLS": case "LINK": case "MCDI": case "MLLT":
                    case "OWNE": case "PRIV": case "PCNT": case "POPM": case "POSS": case "RBUF":
                    case "RVAD": case "RVRB": case "SYLT": case "SYTC": case "TBPM": case "TCOM":
                    case "TCOP": case "TDAT": case "TDLY": case "TENC": case "TEXT": case "TFLT":
                    case "TIME": case "TIT1": case "TIT3": case "TKEY": case "TLAN": case "TLEN":
                    case "TMED": case "TOAL": case "TOFN": case "TOLY": case "TOPE": case "TORY":
                    case "TOWN": case "TPE2": case "TPE3": case "TPE4": case "TPOS": case "TPUB":
                    case "TRCK": case "TRDA": case "TRSN": case "TRSO": case "TSIZ": case "TSRC":
                    case "TSSE": case "TYER": case "TXXX": case "UFID": case "USER": case "WCOM":
                    case "WCOP": case "WOAF": case "WOAR": case "WOAS": case "WORS": case "WPAY":
                    case "WPUB": case "WXXX":
                    /*Tags from ID3v2.4.0, but some SOB add them to ID3v2.3.0.
                    Just skip and delete for prise of imperor!*/
                    case "TSOT": case "TSOP": case "ASPI": case "EQU2": case "RVA2": case "SEEK":
                    case "SIGN": case "TDEN": case "TDOR": case "TDRC": case "TDRL": case "TDTG":
                    case "TIPL": case "TMCL": case "TMOO": case "TPRO": case "TSOA": case "TSST":
                        fin.skip(size);
                        break;
                    default:
                        hasEnd=true;
                        break;
                }
                if (!hasEnd) {
                    arrayForTags=new byte[4];
                    fin.read(arrayForTags);
                    tag=new String(arrayForTags, StandardCharsets.US_ASCII);
                }
            }
            fin.close();
        }
        return author;
    }
    static public String getAuthor(String path) throws IOException, NullPointerException{
        return getAuthor(new File(path));
    }

    static public String getAlbum(File music) throws IOException, NullPointerException{
        String album="";
        FileInputStream f=new FileInputStream(music);
        BufferedInputStream fin=new BufferedInputStream(f);
        byte[] arrayForTags=new byte[3];
        fin.read(arrayForTags);
        String tag=new String(arrayForTags, StandardCharsets.US_ASCII);
        if (!tag.equals("ID3")) return album;
        else {
            int chr;
            boolean hasEnd=false;
            int ver=fin.read();
            fin.skip(6);    //Skipping of uninformative bytes
            arrayForTags=new byte[4];
            fin.read(arrayForTags);
            tag=new String(arrayForTags, StandardCharsets.US_ASCII);
            while (!hasEnd){
                String tagSize="";
                for(int count=0;count<4;count++) tagSize+=String.format("%8s", Integer.toBinaryString(fin.read() & 0xFF)).replace(" ", "0");
                int size=intFromBinar(tagSize);
                fin.skip(2);
                byte[] arrayFiller;
                Charset encoder;
                switch (tag){
                    case "TALB":
                        encoder=getCharset(ver,fin);
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        album=new String(arrayFiller,encoder);
                        break;
                    case "TIT2":
                    case "TPE1":
                    case "APIC":
                    case "USLT":
                    case "TCON":
                        /*rare-usable tags*/
                    case "AENC": case "COMM": case "COMR": case "ENCR": case "EQUA": case "ETCO":
                    case "GEOB": case "GRID": case "IPLS": case "LINK": case "MCDI": case "MLLT":
                    case "OWNE": case "PRIV": case "PCNT": case "POPM": case "POSS": case "RBUF":
                    case "RVAD": case "RVRB": case "SYLT": case "SYTC": case "TBPM": case "TCOM":
                    case "TCOP": case "TDAT": case "TDLY": case "TENC": case "TEXT": case "TFLT":
                    case "TIME": case "TIT1": case "TIT3": case "TKEY": case "TLAN": case "TLEN":
                    case "TMED": case "TOAL": case "TOFN": case "TOLY": case "TOPE": case "TORY":
                    case "TOWN": case "TPE2": case "TPE3": case "TPE4": case "TPOS": case "TPUB":
                    case "TRCK": case "TRDA": case "TRSN": case "TRSO": case "TSIZ": case "TSRC":
                    case "TSSE": case "TYER": case "TXXX": case "UFID": case "USER": case "WCOM":
                    case "WCOP": case "WOAF": case "WOAR": case "WOAS": case "WORS": case "WPAY":
                    case "WPUB": case "WXXX":
                    /*Tags from ID3v2.4.0, but some SOB add them to ID3v2.3.0.
                    Just skip and delete for prise of imperor!*/
                    case "TSOT": case "TSOP": case "ASPI": case "EQU2": case "RVA2": case "SEEK":
                    case "SIGN": case "TDEN": case "TDOR": case "TDRC": case "TDRL": case "TDTG":
                    case "TIPL": case "TMCL": case "TMOO": case "TPRO": case "TSOA": case "TSST":
                        fin.skip(size);
                        break;
                    default:
                        hasEnd=true;
                        break;
                }
                if (!hasEnd) {
                    arrayForTags=new byte[4];
                    fin.read(arrayForTags);
                    tag=new String(arrayForTags, StandardCharsets.US_ASCII);
                }
            }
            fin.close();
        }
        return album;
    }
    static public String getAlbum(String path) throws IOException, NullPointerException{
        return getAlbum(new File(path));
    }
    static public String getImage(){

    }

    public void primarySearch(File music,Context context) throws IOException,NullPointerException{
        FileInputStream f=new FileInputStream(music);
        BufferedInputStream fin=new BufferedInputStream(f);
        byte[] arrayForTags=new byte[3];
        fin.read(arrayForTags);
        String tag=new String(arrayForTags, StandardCharsets.US_ASCII);
        if (!tag.equals("ID3")) haveTags=false;
        else {
            int chr;
            boolean hasEnd=false;
            int ver=fin.read();
            fin.skip(6);
            arrayForTags=new byte[4];
            fin.read(arrayForTags);
            tag=new String(arrayForTags, StandardCharsets.US_ASCII);
            while (!hasEnd){
                String tagSize="";
                for(int count=0;count<4;count++) tagSize+=String.format("%8s", Integer.toBinaryString(fin.read() & 0xFF)).replace(" ", "0");
                int size=intFromBinar(tagSize);
                fin.skip(2);
                byte[] arrayFiller;
                Charset encoder;
                switch (tag){
                    case "TIT2":
                        encoder=getCharset(ver,fin);
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        musicName=new String(arrayFiller,encoder);
                        break;
                    case "TPE1":
                        encoder=getCharset(ver,fin);
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        musicAuthor=new String(arrayFiller,encoder);
                        break;
                    case "TALB":
                        encoder=getCharset(ver,fin);
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        musicAlbum=new String(arrayFiller,encoder);
                        break;
                    case "APIC":
                        encoder=getCharset(ver,fin);
                        imageName="";
                        String fullFileName=music.getName();
                        for (int count=0;fullFileName.toCharArray()[count]!='.';count++) imageName+=fullFileName.toCharArray()[count];
                        imageName+=".";
                        fin.skip(6); size-=6;
                        do{
                            chr=fin.read();
                            size--;
                            if (chr!=0) imageName+=(char) chr;
                        }while (chr!=0);
                        fin.read(); size--;
                        if (encoder.equals(StandardCharsets.ISO_8859_1)||encoder.equals(StandardCharsets.UTF_8)){
                        do{
                            chr=fin.read();
                            size--;
                        }while (chr!=0);
                        }else{
                            chr=fin.read();
                            size--;
                            while (chr!=0){
                                fin.read();
                                chr=fin.read();
                                size-=2;
                            }
                            fin.read();
                        }
                        FileOutputStream fos=context.openFileOutput(imageName,Context.MODE_PRIVATE);
                        arrayFiller=new byte[size];
                        fin.read(arrayFiller);
                        fos.write(arrayFiller);
                        fos.close();
                        break;
                    case "USLT":
                    case "TCON":
                    case "AENC"://rare-usable tags
                    case "COMM":
                    case "COMR":
                    case "ENCR":
                    case "EQUA":
                    case "ETCO":
                    case "GEOB":
                    case "GRID":
                    case "IPLS":
                    case "LINK":
                    case "MCDI":
                    case "MLLT":
                    case "OWNE":
                    case "PRIV":
                    case "PCNT":
                    case "POPM":
                    case "POSS":
                    case "RBUF":
                    case "RVAD":
                    case "RVRB":
                    case "SYLT":
                    case "SYTC":
                    case "TBPM":
                    case "TCOM":
                    case "TCOP":
                    case "TDAT":
                    case "TDLY":
                    case "TENC":
                    case "TEXT":
                    case "TFLT":
                    case "TIME":
                    case "TIT1":
                    case "TIT3":
                    case "TKEY":
                    case "TLAN":
                    case "TLEN":
                    case "TMED":
                    case "TOAL":
                    case "TOFN":
                    case "TOLY":
                    case "TOPE":
                    case "TORY":
                    case "TOWN":
                    case "TPE2":
                    case "TPE3":
                    case "TPE4":
                    case "TPOS":
                    case "TPUB":
                    case "TRCK":
                    case "TRDA":
                    case "TRSN":
                    case "TRSO":
                    case "TSIZ":
                    case "TSRC":
                    case "TSSE":
                    case "TYER":
                    case "TXXX":
                    case "UFID":
                    case "USER":
                    case "WCOM":
                    case "WCOP":
                    case "WOAF":
                    case "WOAR":
                    case "WOAS":
                    case "WORS":
                    case "WPAY":
                    case "WPUB":
                    case "WXXX":
                        fin.skip(size);
                        break;
                    case "TSOT"://Tags from ID3v2.4.0, but some SOB add them to ID3v2.3.0. Just skip and delete for prise of imperor!
                    case "TSOP":
                    case "ASPI":
                    case "EQU2":
                    case "RVA2":
                    case "SEEK":
                    case "SIGN":
                    case "TDEN":
                    case "TDOR":
                    case "TDRC":
                    case "TDRL":
                    case "TDTG":
                    case "TIPL":
                    case "TMCL":
                    case "TMOO":
                    case "TPRO":
                    case "TSOA":
                    case "TSST":
                        fin.skip(size);
                        break;
                    default:
                        hasEnd=true;
                        break;
                }
                if (!hasEnd) {
                    arrayForTags=new byte[4];
                    fin.read(arrayForTags);
                    tag=new String(arrayForTags, StandardCharsets.US_ASCII);
                }
            }
            fin.close();
            f.close();
        }
    }
    public static int intFromBinar(String str){
        int result=0;
        for (int count=str.length()-1;count>=0;count--) {
            if (str.toCharArray()[count]=='1')  result+=Math.pow(2,str.length()-count-1);
        }
        return result;
    }
    public byte[] getSizeOfFrame(int size){
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

    private static Charset getCharset(int version, BufferedInputStream fin) throws IOException{
        Charset encoder=StandardCharsets.ISO_8859_1;
        if (version<4){
            switch(fin.read()){
                case 0:
                    encoder=StandardCharsets.ISO_8859_1;
                    break;
                case 1:
                    encoder=StandardCharsets.UTF_16;
                    break;
            }
        }else{
            switch(fin.read()){
                case 0:
                    encoder=StandardCharsets.ISO_8859_1;
                    break;
                case 1:
                    encoder=StandardCharsets.UTF_16;
                    break;
                case 2:
                    encoder=StandardCharsets.UTF_16BE;
                    break;
                case 3:
                    encoder=StandardCharsets.UTF_8;
                    break;
            }
        }
        return encoder;
    }
}
class SecondaryMusicData{
    public boolean haveTags=true;
    public String musicName;
    public String musicAuthor="Неизвестен";
    public String musicAlbum;
    public String musicLyrics;
    public String musicLyricsLanguage;
    public String imageName=null;
    public void secondarySearch(File music,Context context) throws IOException,NullPointerException{
        FileInputStream f=new FileInputStream(music);
        BufferedInputStream fin=new BufferedInputStream(f);
        byte[] arrayForTags=new byte[3];
        fin.read(arrayForTags);
        String tag=new String(arrayForTags, StandardCharsets.US_ASCII);
        if (!tag.equals("ID3")) haveTags=false;
        else {
            int chr;
            boolean hasEnd=false;
            int ver=fin.read();
            fin.skip(6);
            arrayForTags=new byte[4];
            fin.read(arrayForTags);
            tag=new String(arrayForTags, StandardCharsets.US_ASCII);
            while (!hasEnd){
                String tagSize="";
                for(int count=0;count<4;count++) tagSize+=String.format("%8s", Integer.toBinaryString(fin.read() & 0xFF)).replace(" ", "0");
                int size=intFromBinar(tagSize);
                byte[] flags=new byte[2];
                fin.read(flags);
                byte[] arrayFiller;
                Charset encoder;
                switch (tag){
                    case "TIT2":
                        encoder=getCharset(ver,fin.read());
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        musicName=new String(arrayFiller,encoder);
                        break;
                    case "TPE1":
                        encoder=getCharset(ver,fin.read());
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        musicAuthor=new String(arrayFiller,encoder);
                        break;
                    case "TALB":
                        encoder=getCharset(ver,fin.read());
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        musicAlbum=new String(arrayFiller,encoder);
                        break;
                    case "USLT":
                        encoder=getCharset(ver,fin.read());
                        arrayFiller=new byte[3];
                        fin.read(arrayFiller);
                        musicLyricsLanguage=new String(arrayFiller,encoder);
                        size-=3;
                        if (encoder.equals(StandardCharsets.ISO_8859_1)||encoder.equals(StandardCharsets.UTF_8)){
                            do{
                                chr=fin.read();
                                size--;
                            }while (chr!=0);
                        }else{
                            do{ fin.read();
                                chr=fin.read();
                                size-=2;
                            }while (chr!=0);
                        }
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        musicLyrics=new String(arrayFiller,encoder);
                        break;
                    case "APIC":
                        encoder=getCharset(ver,fin.read());
                        imageName="";
                        String fullFileName=music.getName();
                        for (int count=0;fullFileName.toCharArray()[count]!='.';count++) imageName+=fullFileName.toCharArray()[count];
                        imageName+=".";
                        fin.skip(6); size-=6;
                        do{
                            chr=fin.read();
                            size--;
                            if (chr!=0) imageName+=(char) chr;
                        }while (chr!=0);
                        fin.read(); size--;
                        if (encoder.equals(StandardCharsets.ISO_8859_1)||encoder.equals(StandardCharsets.UTF_8)){
                            do{
                                chr=fin.read();
                                size--;
                            }while (chr!=0);
                        }else{
                            do{ fin.read();
                                chr=fin.read();
                                size-=2;
                            }while (chr!=0);
                        }
                        FileOutputStream fos=context.openFileOutput(imageName,Context.MODE_APPEND);
                        arrayFiller=new byte[size];
                        fin.read(arrayFiller);
                        fos.write(arrayFiller);
                        fos.close();
                        break;
                    case "TCON":
                    case "AENC"://rare-usable tags
                    case "COMM":
                    case "COMR":
                    case "ENCR":
                    case "EQUA":
                    case "ETCO":
                    case "GEOB":
                    case "GRID":
                    case "IPLS":
                    case "LINK":
                    case "MCDI":
                    case "MLLT":
                    case "OWNE":
                    case "PRIV":
                    case "PCNT":
                    case "POPM":
                    case "POSS":
                    case "RBUF":
                    case "RVAD":
                    case "RVRB":
                    case "SYLT":
                    case "SYTC":
                    case "TBPM":
                    case "TCOM":
                    case "TCOP":
                    case "TDAT":
                    case "TDLY":
                    case "TENC":
                    case "TEXT":
                    case "TFLT":
                    case "TIME":
                    case "TIT1":
                    case "TIT3":
                    case "TKEY":
                    case "TLAN":
                    case "TLEN":
                    case "TMED":
                    case "TOAL":
                    case "TOFN":
                    case "TOLY":
                    case "TOPE":
                    case "TORY":
                    case "TOWN":
                    case "TPE2":
                    case "TPE3":
                    case "TPE4":
                    case "TPOS":
                    case "TPUB":
                    case "TRCK":
                    case "TRDA":
                    case "TRSN":
                    case "TRSO":
                    case "TSIZ":
                    case "TSRC":
                    case "TSSE":
                    case "TYER":
                    case "TXXX":
                    case "UFID":
                    case "USER":
                    case "WCOM":
                    case "WCOP":
                    case "WOAF":
                    case "WOAR":
                    case "WOAS":
                    case "WORS":
                    case "WPAY":
                    case "WPUB":
                    case "WXXX":
                    case "TSOT"://Tags from ID3v2.4.0, but some SOB add them to ID3v2.3.0. Just skip and delete for prise of imperor!
                    case "TSOP":
                    case "ASPI":
                    case "EQU2":
                    case "RVA2":
                    case "SEEK":
                    case "SIGN":
                    case "TDEN":
                    case "TDOR":
                    case "TDRC":
                    case "TDRL":
                    case "TDTG":
                    case "TIPL":
                    case "TMCL":
                    case "TMOO":
                    case "TPRO":
                    case "TSOA":
                    case "TSST":
                        fin.skip(size);
                        break;
                    default:
                        hasEnd=true;
                        break;
                }
                if (!hasEnd) {
                    arrayForTags=new byte[4];
                    fin.read(arrayForTags);
                    tag=new String(arrayForTags, StandardCharsets.US_ASCII);
                }
            }
            fin.close();
            f.close();

        }
    }
    public int intFromBinar(String str){
        int result=0;
        for (int count=str.length()-1;count>=0;count--) {
            if (str.toCharArray()[count]=='1')  result+=Math.pow(2,str.length()-count-1);
        }
        return result;
    }
    private Charset getCharset(int version,int chr) {
        Charset encoder=StandardCharsets.ISO_8859_1;
        if (version<4){
            switch(chr){
                case 0:
                    encoder=StandardCharsets.ISO_8859_1;
                    break;
                case 1:
                    encoder=StandardCharsets.UTF_16;
                    break;
            }
        }else{
            switch(chr){
                case 0:
                    encoder=StandardCharsets.ISO_8859_1;
                    break;
                case 1:
                    encoder=StandardCharsets.UTF_16;
                    break;
                case 2:
                    encoder=StandardCharsets.UTF_16BE;
                    break;
                case 3:
                    encoder=StandardCharsets.UTF_8;
                    break;
            }
        }
        return encoder;
    }
    public byte[] getSizeOfFrame(int size){
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
}
class TertiaryMusicData{
    public boolean haveTags=true;
    public String musicName=null;
    public String musicAuthor=null;
    public String musicAlbum=null;
    public String musicLyrics=null;
    public String musicLyricsLanguage=null;
    public String imageName=null;
    public String musicKind;
    public File unReadableTags;
    public int allTagsSize;
    public void tertiarySearch(File music,Context context) throws IOException,NullPointerException{
        FileInputStream f=new FileInputStream(music);
        BufferedInputStream fin=new BufferedInputStream(f);
        byte[] arrayForTags=new byte[3];
        fin.read(arrayForTags);
        String tag=new String(arrayForTags, StandardCharsets.US_ASCII);
        FileOutputStream forOtherTags=context.openFileOutput("TagsRecord.cut",Context.MODE_APPEND);
        if (!tag.equals("ID3")) haveTags=false;
        else {
            int chr;
            boolean hasEnd=false;
            allTagsSize=0;
            int ver=fin.read();
            fin.skip(6);
            arrayForTags=new byte[4];
            fin.read(arrayForTags);
            tag=new String(arrayForTags, StandardCharsets.US_ASCII);
            while (!hasEnd){
                String tagSize="";
                for(int count=0;count<4;count++) tagSize+=String.format("%8s", Integer.toBinaryString(fin.read() & 0xFF)).replace(" ", "0");
                int size=intFromBinar(tagSize);
                allTagsSize+=10;
                allTagsSize+=size;
                byte[] flags=new byte[2];
                fin.read(flags);
                byte[] arrayFiller;
                Charset encoder;
                switch (tag){
                    case "TIT2":
                        encoder=getCharset(ver,fin);
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        musicName=new String(arrayFiller,encoder);
                        break;
                    case "TPE1":
                        encoder=getCharset(ver,fin);
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        musicAuthor=new String(arrayFiller,encoder);
                        break;
                    case "TALB":
                        encoder=getCharset(ver,fin);
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        musicAlbum=new String(arrayFiller,encoder);
                        break;
                    case "USLT":
                        encoder=getCharset(ver,fin);
                        arrayFiller=new byte[3];
                        fin.read(arrayFiller);
                        musicLyricsLanguage=new String(arrayFiller,encoder);
                        size-=3;
                        if (encoder.equals(StandardCharsets.ISO_8859_1)||encoder.equals(StandardCharsets.UTF_8)){
                            do{
                                chr=fin.read();
                                size--;
                            }while (chr!=0);
                        }else{
                            do{ fin.read();
                                chr=fin.read();
                                size-=2;
                            }while (chr!=0);
                        }
                        arrayFiller=new byte[size-1];
                        fin.read(arrayFiller);
                        musicLyrics=new String(arrayFiller,encoder);
                        break;
                    case "APIC":
                        encoder=getCharset(ver,fin);
                        imageName="";
                        String fullFileName=music.getName();
                        for (int count=0;fullFileName.toCharArray()[count]!='.';count++) imageName+=fullFileName.toCharArray()[count];
                        imageName+=".";
                        fin.skip(6); size-=6;
                        do{
                            chr=fin.read();
                            size--;
                            if (chr!=0) imageName+=(char) chr;
                        }while (chr!=0);
                        fin.read(); size--;
                        if (encoder.equals(StandardCharsets.ISO_8859_1)||encoder.equals(StandardCharsets.UTF_8)){
                            do{
                                chr=fin.read();
                                size--;
                            }while (chr!=0);
                        }else{
                            do{ fin.read();
                                chr=fin.read();
                                size-=2;
                            }while (chr!=0);
                        }
                        FileOutputStream fos=context.openFileOutput(imageName,Context.MODE_APPEND);
                        arrayFiller=new byte[size];
                        fin.read(arrayFiller);
                        fos.write(arrayFiller);
                        fos.close();
                        break;
                    case "TCON":
                    case "AENC"://rare-usable tags
                    case "COMM":
                    case "COMR":
                    case "ENCR":
                    case "EQUA":
                    case "ETCO":
                    case "GEOB":
                    case "GRID":
                    case "IPLS":
                    case "LINK":
                    case "MCDI":
                    case "MLLT":
                    case "OWNE":
                    case "PRIV":
                    case "PCNT":
                    case "POPM":
                    case "POSS":
                    case "RBUF":
                    case "RVAD":
                    case "RVRB":
                    case "SYLT":
                    case "SYTC":
                    case "TBPM":
                    case "TCOM":
                    case "TCOP":
                    case "TDAT":
                    case "TDLY":
                    case "TENC":
                    case "TEXT":
                    case "TFLT":
                    case "TIME":
                    case "TIT1":
                    case "TIT3":
                    case "TKEY":
                    case "TLAN":
                    case "TLEN":
                    case "TMED":
                    case "TOAL":
                    case "TOFN":
                    case "TOLY":
                    case "TOPE":
                    case "TORY":
                    case "TOWN":
                    case "TPE2":
                    case "TPE3":
                    case "TPE4":
                    case "TPOS":
                    case "TPUB":
                    case "TRCK":
                    case "TRDA":
                    case "TRSN":
                    case "TRSO":
                    case "TSIZ":
                    case "TSRC":
                    case "TSSE":
                    case "TYER":
                    case "TXXX":
                    case "UFID":
                    case "USER":
                    case "WCOM":
                    case "WCOP":
                    case "WOAF":
                    case "WOAR":
                    case "WOAS":
                    case "WORS":
                    case "WPAY":
                    case "WPUB":
                    case "WXXX":
                        arrayForTags=new byte[size];
                        fin.read(arrayForTags);
                        forOtherTags.write(tag.getBytes());
                        forOtherTags.write(getSizeOfFrame(size));
                        forOtherTags.write(flags);
                        forOtherTags.write(arrayForTags);
                        break;
                    case "TSOT"://Tags from ID3v2.4.0, but some SOB add them to ID3v2.3.0. Just skip and delete for prise of imperor!
                    case "TSOP":
                    case "ASPI":
                    case "EQU2":
                    case "RVA2":
                    case "SEEK":
                    case "SIGN":
                    case "TDEN":
                    case "TDOR":
                    case "TDRC":
                    case "TDRL":
                    case "TDTG":
                    case "TIPL":
                    case "TMCL":
                    case "TMOO":
                    case "TPRO":
                    case "TSOA":
                    case "TSST":
                        fin.skip(size);
                        break;
                    default:
                        hasEnd=true;
                        break;
                }
                if (!hasEnd) {
                    arrayForTags=new byte[4];
                    fin.read(arrayForTags);
                    tag=new String(arrayForTags, StandardCharsets.US_ASCII);
                }
            }
            fin.close();
            f.close();

        }
    }
    static public int intFromBinar(String str){
        int result=0;
        for (int count=str.length()-1;count>=0;count--) {
            if (str.toCharArray()[count]=='1')  result+=Math.pow(2,str.length()-count-1);
        }
        return result;
    }
    public byte[] getSizeOfFrame(int size){
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
    static private Charset getCharset(int version,BufferedInputStream fin) throws IOException{
        Charset encoder=StandardCharsets.ISO_8859_1;
        if (version<4){
            switch(fin.read()){
                case 0:
                    encoder=StandardCharsets.ISO_8859_1;
                    break;
                case 1:
                    encoder=StandardCharsets.UTF_16;
                    break;
            }
        }else{
            switch(fin.read()){
                case 0:
                    encoder=StandardCharsets.ISO_8859_1;
                    break;
                case 1:
                    encoder=StandardCharsets.UTF_16;
                    break;
                case 2:
                    encoder=StandardCharsets.UTF_16BE;
                    break;
                case 3:
                    encoder=StandardCharsets.UTF_8;
                    break;
            }
        }
        return encoder;
    }
}
