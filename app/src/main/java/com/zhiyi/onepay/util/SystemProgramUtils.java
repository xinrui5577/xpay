package com.zhiyi.onepay.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;
/**
 * 类：SystemProgramUtils 系统程序适配帮助类
 * 1. 拍照
 * 2. 相册
 * 3. 裁切
 * 4. apk安装
 * 作者： qxc
 * 日期：2018/2/23.
 */
public class SystemProgramUtils {
    public static final int REQUEST_CODE_PAIZHAO = 1;
    public static final int REQUEST_CODE_ZHAOPIAN = 2;
    public static final int REQUEST_CODE_CAIQIE = 3;

    /**
     * 打开相机拍照
     */
    public static void paizhao(Activity activity, File outputFile){
        Intent intent = new Intent();
        intent.setAction("android.media.action.IMAGE_CAPTURE");
        intent.addCategory("android.intent.category.DEFAULT");
        Uri uri = FileProviderUtils.uriFromFile(activity, outputFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        activity.startActivityForResult(intent, REQUEST_CODE_PAIZHAO);
    }

    /**
     * 打开相册
     */
    public static void zhaopian(Activity activity){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction("android.intent.action.PICK");
        intent.addCategory("android.intent.category.DEFAULT");
        activity.startActivityForResult(intent, REQUEST_CODE_ZHAOPIAN);
    }

    /**
     * 打开图片裁切
     */
    public static void Caiqie(Activity activity, Uri uri, File outputFile) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        FileProviderUtils.setIntentDataAndType(activity, intent, "image/*", uri, true);
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        //return-data为true时，直接返回bitmap，可能会很占内存，不建议，小米等个别机型会出异常！！！
        //所以适配小米等个别机型，裁切后的图片，不能直接使用data返回，应使用uri指向
        //裁切后保存的URI，不属于我们向外共享的，所以可以使用fill://类型的URI
        Uri outputUri = Uri.fromFile(outputFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        activity.startActivityForResult(intent, REQUEST_CODE_CAIQIE);
    }

}
