package com.master.rxlib.Rx.Net;


import android.os.Environment;
import android.util.Log;

import com.master.rxlib.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created by ck on 2017/7/31.
 */

public class RetrofitHttpManger {
    private static Map<String, File> progrssUrls = new LinkedHashMap<>();
    private File DownloadFILE = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    private Retrofit mRetrofit;

    private RetrofitHttpManger() {
    }

    public RetrofitHttpManger addDownloadUrlListener(String url) {
        progrssUrls.put(url, DownloadFILE);
        return this;
    }

    public RetrofitHttpManger addDownloadUrlListener(String url, File file) {
        progrssUrls.put(url, file);
        return this;
    }

    public RetrofitHttpManger removeDownloadListener(String url) {
        if (progrssUrls.containsKey(url)) {
            progrssUrls.remove(url);
        }
        return this;
    }

    public Retrofit get() {
        return mRetrofit;
    }

    public static class Builder {
        private int connectOut = 12;
        private int readOut = 12;
        private String baseUrl;
        private Retrofit mRetrofit;
        private boolean showlog = true;
        OkHttpClient httpclient;
        private File DownloadFILE = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        private Map<String, String> headers = new LinkedHashMap<>();
        private HttpsCerUtil.SSLParams sslParams;

        public Builder setTimeout(int connectOut, int readOut) {
            this.connectOut = connectOut;
            this.readOut = readOut;
            return this;
        }

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setDownloadFile(File downloadFile) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setShowlog(boolean showlog) {
            this.showlog = showlog;
            return this;
        }

        public Builder addHeaders(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public Builder addCert(InputStream[] certificates, InputStream bksFile, String password) {
            sslParams = HttpsCerUtil.getSslSocketFactory(certificates, bksFile, password);
            return this;
        }

        public Builder setOkhttpClient(OkHttpClient httpclient) {
            this.httpclient = httpclient;
            return this;
        }

        public RetrofitHttpManger Builder() {
            if (httpclient == null) {
                OkHttpClient.Builder builder = new OkHttpClient.Builder();
                if (showlog) {
                    builder.addInterceptor(new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                        @Override
                        public void log(String message) {
                            Log.i("HttpLoggingInterceptor", message);
                        }
                    }).setLevel(HttpLoggingInterceptor.Level.BODY));
                }
                builder.connectTimeout(connectOut, TimeUnit.SECONDS)
                        .readTimeout(readOut, TimeUnit.SECONDS)
                        // 添加公共参数拦截器
                        // 添加通用的Header
                        .addInterceptor(new Interceptor() {
                            @Override
                            public Response intercept(Chain chain) throws IOException {
                                Request.Builder builder = chain.request().newBuilder();
                                for (Map.Entry<String, String> entry : headers.entrySet()) {
                                    builder.addHeader(entry.getKey(), entry.getValue());
                                }

                                return chain.proceed(builder.build());
                            }
                        })
                        .addInterceptor(new Interceptor() {
                            @Override
                            public Response intercept(Chain chain) throws IOException {
                                Response originalResponse = chain.proceed(chain.request());
                                String url = originalResponse.request().url().url().toString();
                                if (progrssUrls.containsKey(url)) {
                                    return originalResponse.newBuilder()
                                            .body(new ProgressDownloadBody(originalResponse.body(), progrssUrls.get(url), url))
                                            .build();
                                } else {
                                    return originalResponse;
                                }
                            }
                        });
                if (sslParams != null) {
                    builder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
                }
                httpclient = builder.build();
            }
            RetrofitHttpManger retrofitHttpManger = new RetrofitHttpManger();
            mRetrofit = new Retrofit.Builder()
                    .client(httpclient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .baseUrl(baseUrl)
                    .build();
            retrofitHttpManger.DownloadFILE = DownloadFILE;
            retrofitHttpManger.mRetrofit = mRetrofit;
            return retrofitHttpManger;
        }
    }
}

