package com.hibike;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileOutputStream;
import java.io.IOException;


public class CoverEditor {

    //Makes images with 100px on one dimension from image it got
    static String shrink(String path){

        String[] pathArray=path.split("\\.");
        String res=pathArray[pathArray.length-1];
        int quality=100;

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        Bitmap original= BitmapFactory.decodeFile(path);
        Bitmap resized;
        int width = bmOptions.outWidth;
        int height = bmOptions.outHeight;

        if (width>height) resized = Bitmap.createScaledBitmap(original, 100, height*100/width, true);
        else resized = Bitmap.createScaledBitmap(original, width*100/height, 100, true);
        try {
            FileOutputStream out = new FileOutputStream(path);
            Bitmap.CompressFormat format;
            switch (res){
                case "jpg":
                case "jpeg":
                    format=Bitmap.CompressFormat.JPEG;
                    quality=50;
                    break;
                case "png":
                default:
                    format=Bitmap.CompressFormat.PNG;
                    break;
            }
            resized.compress(format, quality, out);                                                 // PNG is a lossless format, the compression factor (100) is ignored
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }


    /*Makes muted images from images it got
    static String mute(String path){

    }
     */
}
