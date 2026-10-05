package com.example.marvel.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import com.example.marvel.R;
import com.example.marvel.data.api.ApiClient;
import com.example.marvel.data.api.ComicVineService;
import com.example.marvel.data.model.ApiResponse;
import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.CharacterPage;
import com.example.marvel.data.model.Team;
import com.google.gson.JsonParseException;
import com.google.gson.stream.MalformedJsonException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CharacterRepository {

    private static final int MIN_RESULTS_PER_LOAD = 12;
    private static final int MAX_REQUESTS_PER_LOAD = 3;
    private static final int MAX_CACHED_PAGES = 60;
    private static final int MAX_CACHED_DETAILS = 150;
    private static final int MAX_CACHED_MEMBER_BATCHES = 20;

    private static CharacterRepository instance;

    private final Context appContext;
    private final ComicVineService api;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, CharacterPage> pageCache =
            new LinkedHashMap<String, CharacterPage>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CharacterPage> eldest) {
                    return size() > MAX_CACHED_PAGES;
                }
            };
    private final LruCache<Integer, Character> detailCache = new LruCache<>(MAX_CACHED_DETAILS);
    private final Map<Integer, Team> teamCache = new HashMap<>();
    private final LruCache<String, List<Character>> memberBatchCache =
            new LruCache<>(MAX_CACHED_MEMBER_BATCHES);

    public static synchronized CharacterRepository getInstance(Context context) {
        if (instance == null) {
            instance = new CharacterRepository(context.getApplicationContext());
        }
        return instance;
    }

    private CharacterRepository(Context appContext) {
        this.appContext = appContext;
        this.api = ApiClient.getService(appContext);
    }

    public RequestHandle loadCharacters(String query, SortOption sort, int offset,
                                        RepoCallback<CharacterPage> callback) {
        RequestHandle handle = new RequestHandle();
        String cleanQuery = cleanQuery(query);
        String cacheKey = sort.name() + "|" + cleanQuery.toLowerCase(Locale.ROOT) + "|" + offset;

        CharacterPage cached = pageCache.get(cacheKey);
        if (cached != null) {
            deliver(handle, () -> callback.onSuccess(cached));
            return handle;
        }
        if (!ApiClient.hasApiKey()) {
            deliver(handle, () -> callback.onError(text(R.string.error_missing_api_key)));
            return handle;
        }

        String filter = cleanQuery.isEmpty() ? null : "name:" + cleanQuery;
        fetchMarvelBatch(handle, sort, filter, offset, new ArrayList<>(), 1,
                new RepoCallback<CharacterPage>() {
                    @Override
                    public void onSuccess(CharacterPage page) {
                        pageCache.put(cacheKey, page);
                        callback.onSuccess(page);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
        return handle;
    }

    public RequestHandle loadCharacterDetail(int id, RepoCallback<Character> callback) {
        RequestHandle handle = new RequestHandle();

        Character cached = detailCache.get(id);
        if (cached != null) {
            deliver(handle, () -> callback.onSuccess(cached));
            return handle;
        }
        if (!ApiClient.hasApiKey()) {
            deliver(handle, () -> callback.onError(text(R.string.error_missing_api_key)));
            return handle;
        }

        enqueue(api.getCharacter(id, ComicVineService.DETAIL_FIELDS), handle,
                new RepoCallback<Character>() {
                    @Override
                    public void onSuccess(Character character) {
                        detailCache.put(id, character);
                        callback.onSuccess(character);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
        return handle;
    }

    public RequestHandle loadTeam(int id, RepoCallback<Team> callback) {
        RequestHandle handle = new RequestHandle();

        Team cached = teamCache.get(id);
        if (cached != null) {
            deliver(handle, () -> callback.onSuccess(cached));
            return handle;
        }
        if (!ApiClient.hasApiKey()) {
            deliver(handle, () -> callback.onError(text(R.string.error_missing_api_key)));
            return handle;
        }

        enqueue(api.getTeam(id, ComicVineService.TEAM_FIELDS), handle, new RepoCallback<Team>() {
            @Override
            public void onSuccess(Team team) {
                teamCache.put(id, team);
                callback.onSuccess(team);
            }

            @Override
            public void onError(String message) {
                boolean notFound = message.equals(text(R.string.error_not_found));
                callback.onError(notFound ? text(R.string.error_team_not_found) : message);
            }
        });
        return handle;
    }

    public Team getCachedTeam(int id) {
        return teamCache.get(id);
    }

    public RequestHandle loadCharactersByIds(List<Integer> ids,
                                             RepoCallback<List<Character>> callback) {
        RequestHandle handle = new RequestHandle();
        List<Integer> batch = ids.subList(0, Math.min(ids.size(), ComicVineService.MAX_PAGE_SIZE));
        if (batch.isEmpty()) {
            deliver(handle, () -> callback.onSuccess(Collections.emptyList()));
            return handle;
        }
        StringBuilder joined = new StringBuilder();
        for (Integer id : batch) {
            if (joined.length() > 0) joined.append('|');
            joined.append(id);
        }
        String key = joined.toString();

        List<Character> cached = memberBatchCache.get(key);
        if (cached != null) {
            deliver(handle, () -> callback.onSuccess(cached));
            return handle;
        }
        if (!ApiClient.hasApiKey()) {
            deliver(handle, () -> callback.onError(text(R.string.error_missing_api_key)));
            return handle;
        }

        enqueue(api.listCharacters(ComicVineService.MEMBER_FIELDS, ComicVineService.MAX_PAGE_SIZE,
                0, null, "id:" + key), handle, new RepoCallback<List<Character>>() {
            @Override
            public void onSuccess(List<Character> characters) {
                memberBatchCache.put(key, characters);
                callback.onSuccess(characters);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
        return handle;
    }

    private <T> void enqueue(Call<ApiResponse<T>> call, RequestHandle handle,
                             RepoCallback<T> callback) {
        handle.setCurrentCall(call);
        call.enqueue(new Callback<ApiResponse<T>>() {
            @Override
            public void onResponse(Call<ApiResponse<T>> c, Response<ApiResponse<T>> response) {
                if (handle.isCancelled()) return;
                ApiResponse<T> body = response.body();
                String error = errorMessage(response, body);
                if (error != null) {
                    callback.onError(error);
                    return;
                }
                T results = body.getResults();
                if (results == null) {
                    callback.onError(text(R.string.error_not_found));
                    return;
                }
                callback.onSuccess(results);
            }

            @Override
            public void onFailure(Call<ApiResponse<T>> c, Throwable t) {
                if (handle.isCancelled()) return;
                callback.onError(failureMessage(t));
            }
        });
    }

    public Character getCachedDetail(int id) {
        return detailCache.get(id);
    }

    private void fetchMarvelBatch(RequestHandle handle, SortOption sort, String filter, int offset,
                                  List<Character> found, int requestNumber,
                                  RepoCallback<CharacterPage> callback) {
        Call<ApiResponse<List<Character>>> call = api.listCharacters(
                ComicVineService.LIST_FIELDS, ComicVineService.MAX_PAGE_SIZE,
                offset, sort.getApiValue(), filter);
        handle.setCurrentCall(call);

        call.enqueue(new Callback<ApiResponse<List<Character>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Character>>> c,
                                   Response<ApiResponse<List<Character>>> response) {
                if (handle.isCancelled()) return;
                ApiResponse<List<Character>> body = response.body();
                String error = errorMessage(response, body);
                if (error != null) {
                    callback.onError(error);
                    return;
                }

                List<Character> results = body.getResults() == null
                        ? Collections.emptyList() : body.getResults();
                for (Character character : results) {
                    if (character != null && character.isMarvel()) {
                        found.add(character);
                    }
                }

                int nextOffset = offset + results.size();
                boolean hasMore = !results.isEmpty() && nextOffset < body.getNumberOfTotalResults();
                boolean needsMore = found.size() < MIN_RESULTS_PER_LOAD;

                if (needsMore && hasMore && requestNumber < MAX_REQUESTS_PER_LOAD) {
                    fetchMarvelBatch(handle, sort, filter, nextOffset, found, requestNumber + 1, callback);
                } else {
                    callback.onSuccess(new CharacterPage(found, nextOffset, hasMore));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Character>>> c, Throwable t) {
                if (handle.isCancelled()) return;
                callback.onError(failureMessage(t));
            }
        });
    }

    private String errorMessage(Response<?> response, ApiResponse<?> body) {
        if (!response.isSuccessful()) {
            int code = response.code();
            if (code == 401) return text(R.string.error_invalid_api_key);
            if (code == 420 || code == 429) return text(R.string.error_rate_limit);
            return text(R.string.error_server, code);
        }
        if (body == null) {
            return text(R.string.error_unexpected);
        }
        if (!body.isOk()) {
            ApiClient.forget(response.raw().request().url());
            if (body.getStatusCode() == ApiResponse.STATUS_INVALID_API_KEY) {
                return text(R.string.error_invalid_api_key);
            }
            if (body.getStatusCode() == ApiResponse.STATUS_OBJECT_NOT_FOUND) {
                return text(R.string.error_not_found);
            }
            return text(R.string.error_api, Objects.toString(body.getError(), "?"));
        }
        return null;
    }

    private String failureMessage(Throwable t) {
        if (t instanceof MalformedJsonException || t instanceof JsonParseException) {
            return text(R.string.error_unexpected);
        }
        if (t instanceof IOException) {
            return text(R.string.error_no_connection);
        }
        return text(R.string.error_unexpected);
    }

    private void deliver(RequestHandle handle, Runnable action) {
        mainHandler.post(() -> {
            if (!handle.isCancelled()) action.run();
        });
    }

    private String text(int resId, Object... args) {
        return appContext.getString(resId, args);
    }

    static String cleanQuery(String query) {
        if (query == null) return "";
        return query.replace(',', ' ').replace(':', ' ').trim().replaceAll("\\s+", " ");
    }
}
