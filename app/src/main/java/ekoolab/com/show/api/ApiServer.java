package ekoolab.com.show.api;

import android.content.Context;

import com.androidnetworking.common.Priority;
import com.google.gson.reflect.TypeToken;
import com.rx2androidnetworking.Rx2AndroidNetworking;
import com.uber.autodispose.FlowableSubscribeProxy;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ekoolab.com.show.activities.BaseActivity;
import ekoolab.com.show.fragments.BaseFragment;
import ekoolab.com.show.utils.RxUtils;
import io.reactivex.Flowable;

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/9/22
 * @description
 */
public class ApiServer {

    public static <T> FlowableSubscribeProxy<T> basePostRequest(BaseActivity activity,
                                                                String url,
                                                                HashMap<String, String> map,
                                                                TypeToken<T> typeToken) {
        return Rx2AndroidNetworking
                .post(url)
                .addBodyParameter(map)
                .setPriority(Priority.MEDIUM)
                .build()
                .getParseFlowable(typeToken)
                .compose(RxUtils.rxSchedulerHelper())
                .as(activity.autoDisposable());
    }

    public static <T> Flowable<T> basePostRequestNoDisposable(String url,
                                                              HashMap<String, Object> map,
                                                              TypeToken<T> typeToken) {
        return Rx2AndroidNetworking
                .post(url)
                .addBodyParameter(map)
                .setPriority(Priority.MEDIUM)
                .build()
                .getParseFlowable(typeToken)
                .compose(RxUtils.rxSchedulerHelper());
    }

    public static <T> FlowableSubscribeProxy<T> basePostRequest(BaseFragment fragment,
                                                                String url,
                                                                HashMap<String, String> map,
                                                                TypeToken<T> typeToken) {
        return Rx2AndroidNetworking
                .post(url)
                .addBodyParameter(map)
                .setPriority(Priority.MEDIUM)
                .build()
                .getParseFlowable(typeToken)
                .compose(RxUtils.rxSchedulerHelper())
                .as(fragment.autoDisposable());
    }

    public static <T> FlowableSubscribeProxy<T> baseUploadRequest(BaseActivity activity,
                                                                  String url,
                                                                  Map<String, String> valueMap,
                                                                  Map<String, File> fileMap,
                                                                  TypeToken<T> typeToken) {
        return Rx2AndroidNetworking
                .upload(url)
                .addMultipartParameter(valueMap)
                .addMultipartFile(fileMap)
                .setPriority(Priority.MEDIUM)
                .build()
                .getParseFlowable(typeToken)
                .compose(RxUtils.rxSchedulerHelper())
                .as(activity.autoDisposable());
    }

    public static <T> FlowableSubscribeProxy<T> baseUploadMoreFilesRequest(BaseActivity activity,
                                                                           String url,
                                                                           Map<String, String> valueMap,
                                                                           String fileKey,
                                                                           List<File> files,
                                                                           TypeToken<T> typeToken) {
        return Rx2AndroidNetworking
                .upload(url)
                .addMultipartParameter(valueMap)
                .addMultipartFileList(fileKey, files)
                .setPriority(Priority.MEDIUM)
                .build()
                .getParseFlowable(typeToken)
                .compose(RxUtils.rxSchedulerHelper())
                .as(activity.autoDisposable());
    }
}
