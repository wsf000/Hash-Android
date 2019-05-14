package com.fanjiee.thingsone;

/**
 * Created by shaofeng.wang on 2019/4/16.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.Bitmap;
import android.os.Environment;

public class FormatUtil {


    /**
     * 将缓存文件夹的数据转存到vedio文件下
     * @param recAudioFile
     */
    public static void videoRename(File recAudioFile) {
        String path = Environment.getExternalStorageDirectory()
                .getAbsolutePath()+ "/mahc/video/"+ "0" + "/";
        String fileName = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date()) + ".3gp";
        File out = new File(path);
        if (!out.exists()) {
            out.mkdirs();
        }
        out = new File(path, fileName);
        if (recAudioFile.exists())
            recAudioFile.renameTo(out);
    }

    /**
     * 用以计时操作的相关方法
     * @param num
     * @return
     */
    public static String format(int num){
        String s = num + "";
        if (s.length() == 1) {
            s = "0" + s;
        }
        return s;
    }

    /**
     * 把batmap 转file
     * @param bitmap
     */
    public static File saveBitmapFile(Bitmap bitmap){
        //在这里抽取了一个方法 可以封装到自己的工具类中...
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);

        File file = new File(Environment.getExternalStorageDirectory() + "/temp.jpg");
        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            int x = 0;
            byte[] b = new byte[1024 * 100];
            while ((x = is.read(b)) != -1) {
                fos.write(b, 0, x);
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

}
