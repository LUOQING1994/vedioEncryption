package com.example.vedioencryption;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_RGBA;


public class MainActivity extends Activity implements View.OnClickListener{
    // 为当前Activity取一个名字 方便调试
    private String TAG = "MainActivity";
    private CommonUtils commonUtils = new CommonUtils();
    private TextView textView;
    private boolean inOrDeCodeShow = false;
    private boolean inEndInCode = false;
    /*
     *  本实例创建时，自动执行的函数
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean status = OpenCVLoader.initDebug();

        if (status) {
            Log.e(TAG, "onCreate: Succese");
        } else {
            Log.e(TAG, "onCreate: Failed");
        }

        if (Build.VERSION.SDK_INT >= 23){
            commonUtils.requestPermissions(this);
        }

        // 创建一个Button对象 并指向前面UI界面中的button组件
        Button inCodeBtn = (Button)findViewById(R.id.incode_button);
        Button deCodeBtn = (Button)findViewById(R.id.decode_button);
        // 根据id 获取UI界面中的ImageView对象 并把操作结果展示到该对象中
        imageView = (ImageView)findViewById(R.id.imageView);
        textView = (TextView)findViewById(R.id.text);
        // 为id等于test_button的按钮对象添加一个点击事件
        // 继承View.OnClickListener接口
        inCodeBtn.setOnClickListener(this);
        deCodeBtn.setOnClickListener(this);
        // 读取秘钥图片
        tmpMat = Imgcodecs.imread("sdcard/a_vedio/img_key.jpg");
        xor_img = new Mat();
        readLocalVedioByThread();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    /*
     * 继承View.OnClickListener接口后 出现的需要重写的方法
     * */
    @Override
    public void onClick(View v) {
        // 根据用户点击的不同按钮 执行不同的逻辑方法
        if (!inEndInCode){
            return;
        }
        switch (v.getId()) {
            case R.id.incode_button:
                inOrDeCodeShow = false;
                showInCodeOption("sdcard/a_vedio/keyImage/");
                break;
            case R.id.decode_button:
                // 解密
                Log.d("解密 ", "解密开始======================   ");
                inOrDeCodeShow = true;
                textView.setText("播放解密后的视频");
                deCodeOption("sdcard/a_vedio/keyImage/");
                break;
        }
    }
    private FFmpegFrameGrabber grabber;
    private AndroidFrameConverter converter;
    private ImageView imageView;
    private Frame frame;
    private Bitmap bmp;
    private Mat tmpMat;
    Mat xor_img ;
    public void readLocalVedioByThread() {
        String vedioUrl = "pro_dump_data.mp4";
        try {
            String file = Environment.getExternalStorageDirectory().toString() + "/" + vedioUrl;
            grabber = new FFmpegFrameGrabber(file);
            grabber.setImageWidth(640);
            grabber.setImageHeight(480);
            //为了加快转bitmap这句一定要写
            grabber.setPixelFormat(AV_PIX_FMT_RGBA);
            grabber.start();
            converter = new AndroidFrameConverter();
            frame = grabber.grabImage();

        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (frame!= null) {
                    try {
                        frame = grabber.grabImage();
                        if ( frame == null){
                            break;
                        }
                        bmp = converter.convert(frame);
                        inCodeOption(bmp,"sdcard/a_vedio/keyImage/");
                        showToast("加密开始。。。。",bmp);
                        inEndInCode = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                try {
                    grabber.release();
                    showToast("加密完毕",null);
                    inEndInCode = true;
                } catch (FrameGrabber.Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    // ====================================

    //  播放加密视频和解密视频时 画面相互调换了 （待修改）
    /**
     *  图片存储
     */
    public void saveImages(Mat imgMat,String path){
        Date date = new Date(System.currentTimeMillis());
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        File file = new File( "sdcard/a_vedio/"+ sdf.format(date) + ".txt");
        Log.d("photoPath -->> ", "存储开始======================   ");
        File dir1 = new File(path);
        if (!dir1.exists()) {
            dir1.mkdirs();
        }
        // 删除原有的文件
        if (!inOrDeCodeShow) {
            commonUtils.deleteLocal(dir1);
            if (file.exists()) {
                file.delete();
            }
            inOrDeCodeShow = true;
        }
        // 存储图片
        String fileName = System.currentTimeMillis() + ".png";
        Imgcodecs.imwrite(path + fileName, imgMat);

        try {
            if(!file.exists()) {
                file.createNewFile(); // 创建新文件,有同名的文件的话直接覆盖
            }
            FileOutputStream fos = new FileOutputStream(file,true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write(path + fileName);
            bw.newLine();
            bw.flush();
            bw.close();
            osw.close();
            fos.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * 加密算法
     */
    public void inCodeOption(Bitmap bitmap,String path1){
        Mat orige_img = new Mat();
        Utils.bitmapToMat(bitmap, orige_img);

        if (orige_img.channels() == 4){
            List<Mat> mv = new ArrayList<Mat>();// 分离出来的彩色通道数据
            Core.split(orige_img, mv);// 分离色彩通道
            mv.remove(3);
            Core.merge(mv, orige_img);// 合并split()方法分离出来的彩色通道数据
        }

        Core.bitwise_xor(orige_img,tmpMat,xor_img);
        saveImages(xor_img,path1);
        orige_img.release();
    }

    /**
     * 解密算法
     */
    public void deCodeOption(final String path1){
        // 循环读取存储本地的图片
        final File org_image = new File(path1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 按txt文件中的数据进行图片的读取
                File dir1 = new File("sdcard/a_vedio/");
                for (File file : dir1.listFiles()){
                    if (file.isFile() && file.getName().split("\\.")[1].equals("txt")){
                        try {
                            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file),"UTF-8");
                            BufferedReader bf = new BufferedReader(inputReader);
                            // 按行读取字符串
                            String str;
                            while ((str = bf.readLine()) != null) {
                                Mat orige_img = Imgcodecs.imread(str);
                                if (orige_img.channels() < 3){
                                    continue;
                                }
                                Core.bitwise_xor(orige_img,tmpMat,xor_img);
                                Bitmap tmp_bitmap = Bitmap.createBitmap(xor_img.cols(), xor_img.rows(),
                                        Bitmap.Config.ARGB_8888);
                                Utils.matToBitmap(xor_img, tmp_bitmap);
                                try {
                                    Thread.sleep(40);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                showToast("播放解密视频", tmp_bitmap);
                            }
                            bf.close();
                            inputReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();

    }
    /**
     *  播放加密视频
     */
    public void showInCodeOption(final String path1){
        // 循环读取存储本地的图片
        final File org_image = new File(path1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (File file : org_image.listFiles()){
                    if (inOrDeCodeShow){
                        break;
                    }
                    Mat orige_img = Imgcodecs.imread(file.getAbsolutePath());
                    Bitmap tmp_bitmap = Bitmap.createBitmap(orige_img.cols(), orige_img.rows(),
                            Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(orige_img, tmp_bitmap);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    showToast("播放加密视频", tmp_bitmap);
                }
            }
        }).start();

    }
    /**
     * Shows a {@link } on the UI thread for the classification results.
     *
     * @param text The message to show
     */
    private void showToast(final String text, final Bitmap show_frame_data) {
        if (this != null) {
            this.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (text != null){
                                textView.setText(text);
                            }
                            if (show_frame_data != null){
                                imageView.setImageBitmap(show_frame_data);
                            }
                        }
                    });
        }
    }
}