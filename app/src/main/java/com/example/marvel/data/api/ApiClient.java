package com.example.marvel.data.api;

import android.content.Context;

import com.example.marvel.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    public static final String BASE_URL = "https://comicvine.gamespot.com/api/";
    public static final String USER_AGENT = "MarvelBattle/1.0 (Android; projeto academico em Java)";

    private static final long CACHE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final int CACHE_MAX_AGE_SECONDS = 60 * 60;
    private static final int OFFLINE_MAX_STALE_DAYS = 7;

    private static ComicVineService service;
    private static Cache cache;

    private ApiClient() {
    }

    public static synchronized ComicVineService getService(Context context) {
        if (service == null) {
            File cacheDir = new File(context.getApplicationContext().getCacheDir(), "comicvine_http");
            cache = new Cache(cacheDir, CACHE_SIZE_BYTES);
            service = create(BASE_URL, BuildConfig.COMICVINE_API_KEY, cache);
        }
        return service;
    }

    public static boolean hasApiKey() {
        return !BuildConfig.COMICVINE_API_KEY.trim().isEmpty();
    }

    static ComicVineService create(String baseUrl, String apiKey, Cache httpCache) {
        OkHttpClient.Builder http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(chain -> addKeyFormatAndUserAgent(chain, apiKey))
                .addInterceptor(ApiClient::useCacheWhenOffline)
                .addNetworkInterceptor(ApiClient::markResponseAsCacheable);
        if (httpCache != null) {
            http.cache(httpCache);
        }
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(http.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ComicVineService.class);
    }

    public static void forget(HttpUrl url) {
        if (cache == null || url == null) return;
        try {
            Iterator<String> urls = cache.urls();
            while (urls.hasNext()) {
                if (urls.next().equals(url.toString())) {
                    urls.remove();
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static Response addKeyFormatAndUserAgent(Interceptor.Chain chain, String apiKey)
            throws IOException {
        Request original = chain.request();
        HttpUrl url = original.url().newBuilder()
                .setQueryParameter("api_key", apiKey)
                .setQueryParameter("format", "json")
                .build();
        Request request = original.newBuilder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();
        return chain.proceed(request);
    }

    private static Response useCacheWhenOffline(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        try {
            return chain.proceed(request);
        } catch (IOException networkError) {
            Request cachedOnly = request.newBuilder()
                    .cacheControl(new CacheControl.Builder()
                            .onlyIfCached()
                            .maxStale(OFFLINE_MAX_STALE_DAYS, TimeUnit.DAYS)
                            .build())
                    .build();
            Response cached = chain.proceed(cachedOnly);
            if (cached.isSuccessful()) {
                return cached;
            }
            cached.close();
            throw networkError;
        }
    }

    private static Response markResponseAsCacheable(Interceptor.Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        String cacheControl = response.isSuccessful()
                ? "public, max-age=" + CACHE_MAX_AGE_SECONDS
                : "no-store";
        return response.newBuilder()
                .removeHeader("Pragma")
                .header("Cache-Control", cacheControl)
                .build();
    }
}
