package com.kotlin.cloud.usefulClass;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

//运行时申请权限的工具类,纯复制，郭神貌似有个库比这个好用
public class permissionsUtil {
    //权限请求码
    private final int mRequestCode = 100;

    //自身实例
    private static permissionsUtil permissionsUtil;

    private IPermissionsResult mPermissionsResult;

    public static permissionsUtil getInstance() {
        if (permissionsUtil == null) {
            permissionsUtil = new permissionsUtil();
        }
        return permissionsUtil;
    }

    //检查是否有相关权限（首先调用）
    public void checkPermissions(Activity context, String[] permissions, @NonNull IPermissionsResult permissionsResult) {
        mPermissionsResult = permissionsResult;
        //创建一个mPermissionList，逐个判断哪些权限未授予，未授予的权限存储到mPerrrmissionList中
        List<String> mPermissionList = new ArrayList<>();
        //逐个判断你要的权限是否已经通过
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permission);//添加还未授予的权限
            }
        }
        //申请权限
        if (mPermissionList.size() > 0) {//有权限没有通过，需要申请
            ActivityCompat.requestPermissions(context, permissions, mRequestCode);
        } else {
            //说明权限都已经通过，可以做你想做的事情去
            permissionsResult.passPermission();
        }
    }
    /**
     * 请求权限后回调的方法
     *
     * @param context
     * @param requestCode  是我们自己定义的权限请求码
     * @param permissions  是我们请求的权限名称数组
     * @param grantResults 是我们在弹出页面后是否允许权限的标识数组，
     *      数组的长度对应的是权限名称数组的长度，数组的数据0表示允许权限，-1表示我们点击了禁止权限
     */
    public void onRequestPermissionsResult(Activity context, int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        boolean hasPermissionDismiss = false;//有权限没有通过
        if (mRequestCode == requestCode) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    hasPermissionDismiss = true;
                    break;
                }
            }
            //如果有权限没有被允许
            if (hasPermissionDismiss) {
                mPermissionsResult.forbidPermission();
            } else {
                //全部权限通过，可以进行下一步操作。。。
                mPermissionsResult.passPermission();
            }
        }
    }

    //权限申请后两种结果的相关事件
    public interface IPermissionsResult {
        void passPermission();
        void forbidPermission();
    }
}

