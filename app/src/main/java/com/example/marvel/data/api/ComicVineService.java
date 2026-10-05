package com.example.marvel.data.api;

import com.example.marvel.data.model.ApiResponse;
import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.Team;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ComicVineService {

    int MAX_PAGE_SIZE = 100;

    String LIST_FIELDS = "id,name,real_name,deck,image,publisher,count_of_issue_appearances";

    String DETAIL_FIELDS = "id,name,real_name,aliases,deck,image,publisher,site_detail_url,"
            + "count_of_issue_appearances,first_appeared_in_issue,origin,powers,teams,movies";

    String MEMBER_FIELDS = "id,name,real_name,image,publisher";

    String TEAM_FIELDS = "id,name,deck,image,publisher,count_of_team_members,characters";

    @GET("characters/")
    Call<ApiResponse<List<Character>>> listCharacters(
            @Query("field_list") String fieldList,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("sort") String sort,
            @Query("filter") String filter);

    @GET("character/4005-{id}/")
    Call<ApiResponse<Character>> getCharacter(
            @Path("id") int id,
            @Query("field_list") String fieldList);

    @GET("team/4060-{id}/")
    Call<ApiResponse<Team>> getTeam(
            @Path("id") int id,
            @Query("field_list") String fieldList);
}
