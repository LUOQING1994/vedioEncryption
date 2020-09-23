package com.example.vedioencryption;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.io.File;

public class CommonUtils {
    public static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE }; //Android6.0以后操作系统的动态权限申请


    /**
     * 用于Android6.0以后的操作系统，动态申请存储的读写权限
     * @param context
     */
    public static void requestPermissions(Activity context){
        //用于Android6.0以后的操作系统，动态申请存储的读写权限
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(context,PERMISSIONS_STORAGE, 1 );
        }
    }
    /**
     *删除本地文件
     */
    public boolean deleteLocal(File file){
        if(file.exists()){
            if(file.isFile()){
                file.delete();//如果为文件，直接删除
            }else if(file.isDirectory()){
                File []files=file.listFiles();
                for(File file1:files){
                    deleteLocal(file1);//如果为文件夹，递归调用
                }
            }
            file.delete();
            return true;
        }
        return false;
    }
}
