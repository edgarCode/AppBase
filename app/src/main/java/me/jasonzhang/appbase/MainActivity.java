package me.jasonzhang.appbase;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import java.util.List;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.jasonzhang.appbase.net.API;
import me.jasonzhang.appbase.net.ApiService;
import me.jasonzhang.appbase.net.core.BaseResponse;
import me.jasonzhang.appbase.net.core.NetManager;
import me.jasonzhang.appbase.net.model.InstallNeceModel;
import me.jasonzhang.appbase.net.model.UpgradeModel;
import me.jasonzhang.appbase.utils.LoggerUtils;

public class MainActivity extends Activity implements View.OnClickListener {
    private ApiService apiService ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        apiService = NetManager.get(API.BASE_URL).getApiService(ApiService.class);
        findViewById(R.id.btn).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn:
                test3();
                break;
        }
    }

    private void test1() {
        apiService.checkUpgrade(5800, "LETV_X443")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse<UpgradeModel>>() {
                    @Override
                    public void accept(@NonNull BaseResponse<UpgradeModel> upgradeModelBaseResponse) throws Exception {
                        LoggerUtils.d("checkUpgradeRx onNext %s", upgradeModelBaseResponse.entity.url);
                    }
                });
    }
    private void test2() {
        apiService.getInstallNeceDetail()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse<List<InstallNeceModel>>>() {
                    @Override
                    public void accept(@NonNull BaseResponse<List<InstallNeceModel>> listBaseResponse) throws Exception {
                        LoggerUtils.d("getInstallDeceDetail onNext size = %d", listBaseResponse.entity.size());
                    }
                });
    }

    /**
     * 两个接口都返回之后做操作
     */
    private void test3() {
        Observable.zip(
                apiService.checkUpgrade(5800, "LETV_X443"),
                apiService.getInstallNeceDetail(),
                new BiFunction<BaseResponse<UpgradeModel>, BaseResponse<List<InstallNeceModel>>, String>() {
            @Override
            public String apply(@NonNull BaseResponse<UpgradeModel> upgradeModelBaseResponse,
                                @NonNull BaseResponse<List<InstallNeceModel>> listBaseResponse) throws Exception {
                LoggerUtils.d("zip apply size = %d | %s", listBaseResponse.entity.size(), Thread.currentThread().getName());
                return String.valueOf(listBaseResponse.entity.size());
            }
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onNext(String s) {
                LoggerUtils.d("onNext size = %s | %s", s, Thread.currentThread().getName());
            }

            @Override
            public void onError(Throwable e) {
                LoggerUtils.e(e, "onError ");
            }

            @Override
            public void onComplete() {

            }
        });
    }

    /**
     * test3方法的lambda写法
     */
    private void test4() {
        Observable.zip(
                apiService.checkUpgrade(5800, "LETV_X443"),
                apiService.getInstallNeceDetail(),
                (upgradeModelBaseResponse, listBaseResponse) -> {
                    LoggerUtils.d("zip apply size = %d | %s", listBaseResponse.entity.size(), Thread.currentThread().getName());
                    return String.valueOf(listBaseResponse.entity.size());
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( s -> LoggerUtils.d("onNext size = %s | %s", s, Thread.currentThread().getName()),
                        throwable -> LoggerUtils.d(throwable,"onError"),
                        () -> LoggerUtils.d("onComplete"));
    }

    /**
     * zipWith
     */
    private void test5() {
        apiService.checkUpgrade(5800, "LETV_X443").zipWith(apiService.getInstallNeceDetail(),
                (upgradeModelBaseResponse, listBaseResponse) -> {
                    LoggerUtils.d("zip apply size = %d | %s", listBaseResponse.entity.size(), Thread.currentThread().getName());
                    return String.valueOf(listBaseResponse.entity.size());
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( s -> LoggerUtils.d("onNext size = %s | %s", s, Thread.currentThread().getName()),
                        throwable -> LoggerUtils.d(throwable,"onError"),
                        () -> LoggerUtils.d("onComplete"));
    }

    private void test6() {
        /**
         * request1结束后使用request1的结果请求request2
         */
        apiService.checkUpgrade(5800, "LETV_X443")
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(new Function<BaseResponse<UpgradeModel>, ObservableSource<BaseResponse<List<InstallNeceModel>>>>() {//IO线程，由observeOn()指定
                    @Override
                    public ObservableSource<BaseResponse<List<InstallNeceModel>>> apply(
                            @NonNull BaseResponse<UpgradeModel> upgradeModelBaseResponse) throws Exception {
                        return apiService.getInstallNeceDetail();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BaseResponse<List<InstallNeceModel>>>() {//Android主线程，由observeOn()指定
                    @Override
                    public void accept(@NonNull BaseResponse<List<InstallNeceModel>> listBaseResponse) throws Exception {
                        LoggerUtils.d("getInstallDeceDetail onNext size = %d", listBaseResponse.entity.size());
                    }
                });
    }
    private void test7() {
        /**
         * request1结束后使用request1的结果请求request2，request2结果是list，对list进行分解输出
         */
        apiService.checkUpgrade(5800, "LETV_X443")
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        Toast.makeText(MainActivity.this, "Begin!!!", Toast.LENGTH_SHORT).show();
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .flatMap(new Function<BaseResponse<UpgradeModel>, Observable<BaseResponse<List<InstallNeceModel>>>>() {//IO线程，由observeOn()指定
                    @Override
                    public Observable<BaseResponse<List<InstallNeceModel>>> apply(
                            @NonNull BaseResponse<UpgradeModel> upgradeModelBaseResponse) throws Exception {
                        return apiService.getInstallNeceDetail();
                    }
                })
                .observeOn(Schedulers.io())
                .flatMap(new Function<BaseResponse<List<InstallNeceModel>>, Observable<InstallNeceModel>>() {
                    @Override
                    public Observable<InstallNeceModel> apply(@NonNull BaseResponse<List<InstallNeceModel>> listBaseResponse) throws Exception {
                        return Observable.fromIterable(listBaseResponse.entity);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<InstallNeceModel>() {
                    @Override
                    public void accept(@NonNull InstallNeceModel installNeceModel) throws Exception {
                        LoggerUtils.d("test7 onNext model.name = %s", installNeceModel.name);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        LoggerUtils.d("Error!");
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        Toast.makeText(MainActivity.this, "End!!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void test8() {
        /**
         * request1结束后使用request1的结果请求request2，request2结果是list，对list进行分解输出
         * 使用lambda, 添加在开始执行的时候显示begin的提示(可以是showProgressBar)，在结束(Complete)的时候显示End的提示(隐藏ProgressBar)
         */
        apiService.checkUpgrade(5800, "LETV_X443")
                .subscribeOn(Schedulers.io())
                .doOnSubscribe((@NonNull Disposable disposable) -> Toast.makeText(this, "Begin!!!", Toast.LENGTH_SHORT).show())
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .flatMap((@NonNull BaseResponse<UpgradeModel> upgradeModelBaseResponse) -> apiService.getInstallNeceDetail())
                .observeOn(Schedulers.io())
                .flatMap((@NonNull BaseResponse<List<InstallNeceModel>> listBaseResponse) -> Observable.fromIterable(listBaseResponse.entity))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(installNeceModel -> LoggerUtils.d("test7 onNext model.name = %s", installNeceModel.name),
                        (Throwable throwable) -> LoggerUtils.d("test8 error %s", throwable.getMessage()),
                        () -> Toast.makeText(this, "End!!", Toast.LENGTH_SHORT).show());
    }
}
