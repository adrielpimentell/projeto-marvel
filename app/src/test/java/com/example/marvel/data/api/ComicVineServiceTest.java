package com.example.marvel.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.marvel.data.model.ApiResponse;
import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.Team;
import com.example.marvel.data.repository.SortOption;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;

public class ComicVineServiceTest {

    private static final String LIST_JSON = "{"
            + "\"error\":\"OK\",\"limit\":100,\"offset\":0,"
            + "\"number_of_page_results\":2,\"number_of_total_results\":250,\"status_code\":1,"
            + "\"results\":["
            + "{\"id\":1,\"name\":\"Heroi Teste A\",\"real_name\":\"Pessoa A\","
            + "\"image\":{\"small_url\":\"https://exemplo.com/a.jpg\"},"
            + "\"publisher\":{\"id\":31,\"name\":\"Marvel\"},"
            + "\"count_of_issue_appearances\":900},"
            + "{\"id\":2,\"name\":\"Heroi Teste B\",\"real_name\":\"Pessoa B\","
            + "\"publisher\":{\"id\":10,\"name\":\"DC Comics\"},"
            + "\"count_of_issue_appearances\":800}"
            + "],\"version\":\"1.0\"}";

    private static final String DETAIL_JSON = "{"
            + "\"error\":\"OK\",\"status_code\":1,"
            + "\"results\":{\"id\":1443,\"name\":\"Heroi Teste\","
            + "\"image\":{\"small_url\":\"https://exemplo.com/uploads/123-blank.png\"},"
            + "\"publisher\":{\"id\":31,\"name\":\"Marvel\"},"
            + "\"powers\":[{\"id\":1,\"name\":\"Poder 1\"},{\"id\":2,\"name\":\"Poder 2\"}],"
            + "\"teams\":[{\"id\":3,\"name\":\"Time 1\"}],"
            + "\"movies\":[]"
            + "}}";

    private static final String NULL_FIELDS_JSON = "{"
            + "\"error\":\"OK\",\"status_code\":1,"
            + "\"results\":{\"id\":7,\"name\":null,\"real_name\":null,\"deck\":null,\"aliases\":null,"
            + "\"site_detail_url\":null,\"image\":null,\"publisher\":null,\"powers\":null}}";

    private static final String TEAM_JSON = "{"
            + "\"error\":\"OK\",\"status_code\":1,"
            + "\"results\":{\"id\":3806,\"name\":\"Equipe Teste\",\"deck\":\"Resumo\","
            + "\"image\":{\"small_url\":\"https://exemplo.com/equipe.jpg\"},"
            + "\"publisher\":{\"id\":31,\"name\":\"Marvel\"},"
            + "\"count_of_team_members\":3,"
            + "\"characters\":[{\"id\":1440,\"name\":\"A\"},{\"id\":1442,\"name\":\"B\"},"
            + "{\"id\":1443,\"name\":\"C\"}]"
            + "}}";

    private MockWebServer server;
    private ComicVineService service;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        service = ApiClient.create(server.url("/api/").toString(), "CHAVE_TESTE", null);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void listCharacters_sendsKeyFormatFieldListAndUserAgent() throws Exception {
        server.enqueue(new MockResponse().setBody(LIST_JSON));

        Response<ApiResponse<List<Character>>> response = service.listCharacters(
                ComicVineService.LIST_FIELDS, ComicVineService.MAX_PAGE_SIZE, 0,
                SortOption.CLASSIC.getApiValue(), null).execute();

        RecordedRequest request = server.takeRequest();
        HttpUrl url = request.getRequestUrl();
        assertNotNull(url);
        assertEquals("/api/characters/", url.encodedPath());
        assertEquals("CHAVE_TESTE", url.queryParameter("api_key"));
        assertEquals("json", url.queryParameter("format"));
        assertEquals(ComicVineService.LIST_FIELDS, url.queryParameter("field_list"));
        assertEquals("100", url.queryParameter("limit"));
        assertEquals("id:asc", url.queryParameter("sort"));
        assertNull(url.queryParameter("filter"));
        assertEquals(ApiClient.USER_AGENT, request.getHeader("User-Agent"));

        ApiResponse<List<Character>> body = response.body();
        assertNotNull(body);
        assertTrue(body.isOk());
        assertEquals(250, body.getNumberOfTotalResults());
        assertEquals(2, body.getResults().size());
        assertTrue(body.getResults().get(0).isMarvel());
        assertFalse(body.getResults().get(1).isMarvel());
        assertEquals("https://exemplo.com/a.jpg", body.getResults().get(0).getCardImageUrl());
    }

    @Test
    public void getCharacter_usesDetailPathAndParsesLists() throws Exception {
        server.enqueue(new MockResponse().setBody(DETAIL_JSON));

        Response<ApiResponse<Character>> response =
                service.getCharacter(1443, ComicVineService.DETAIL_FIELDS).execute();

        RecordedRequest request = server.takeRequest();
        assertNotNull(request.getRequestUrl());
        assertEquals("/api/character/4005-1443/", request.getRequestUrl().encodedPath());

        Character character = response.body().getResults();
        assertTrue(character.hasDetails());
        assertEquals(2, character.getPowers().size());
        assertEquals(1, character.getTeams().size());
        assertTrue(character.getMovies().isEmpty());
        assertNull(character.getCardImageUrl());
    }

    @Test
    public void isMarvel_acceptsIdOrNameVariants() throws Exception {
        String json = "{\"error\":\"OK\",\"status_code\":1,\"results\":["
                + "{\"id\":1,\"publisher\":{\"id\":31,\"name\":\"Qualquer\"}},"
                + "{\"id\":2,\"publisher\":{\"name\":\"Marvel Comics\"}},"
                + "{\"id\":3,\"publisher\":{\"name\":\"marvel\"}},"
                + "{\"id\":4,\"publisher\":{\"id\":10,\"name\":\"DC Comics\"}},"
                + "{\"id\":5,\"publisher\":null}]}";
        server.enqueue(new MockResponse().setBody(json));

        List<Character> results = service.listCharacters(
                ComicVineService.LIST_FIELDS, 100, 0, null, null).execute().body().getResults();

        assertTrue(results.get(0).isMarvel());
        assertTrue(results.get(1).isMarvel());
        assertTrue(results.get(2).isMarvel());
        assertFalse(results.get(3).isMarvel());
        assertFalse(results.get(4).isMarvel());
    }

    @Test
    public void nullFields_doNotCrash() throws Exception {
        server.enqueue(new MockResponse().setBody(NULL_FIELDS_JSON));

        Character character = service.getCharacter(7, ComicVineService.DETAIL_FIELDS)
                .execute().body().getResults();

        assertEquals("Sem nome", character.getName());
        assertEquals("", character.getRealName());
        assertEquals("", character.getDeck());
        assertTrue(character.getAliases().isEmpty());
        assertNull(character.getSiteDetailUrl());
        assertNull(character.getCardImageUrl());
        assertNull(character.getLargeImageUrl());
        assertFalse(character.isMarvel());
        assertTrue(character.getPowers().isEmpty());
        assertTrue(character.getMovies().isEmpty());
    }

    @Test
    public void getTeam_usesTeamPathAndParsesAllMembers() throws Exception {
        server.enqueue(new MockResponse().setBody(TEAM_JSON));

        Team team = service.getTeam(3806, ComicVineService.TEAM_FIELDS).execute().body().getResults();

        RecordedRequest request = server.takeRequest();
        assertNotNull(request.getRequestUrl());
        assertEquals("/api/team/4060-3806/", request.getRequestUrl().encodedPath());
        assertEquals(ComicVineService.TEAM_FIELDS, request.getRequestUrl().queryParameter("field_list"));

        assertEquals("Equipe Teste", team.getName());
        assertTrue(team.isMarvel());
        assertEquals(3, team.getMemberCount());
        assertEquals(List.of(1440, 1442, 1443), team.getMemberIds());
        assertEquals("https://exemplo.com/equipe.jpg", team.getCardImageUrl());
    }

    @Test
    public void team_withoutMembersOrPublisherIsSafe() throws Exception {
        server.enqueue(new MockResponse().setBody(
                "{\"error\":\"OK\",\"status_code\":1,\"results\":{\"id\":9,\"characters\":null}}"));

        Team team = service.getTeam(9, ComicVineService.TEAM_FIELDS).execute().body().getResults();

        assertFalse(team.isMarvel());
        assertTrue(team.getMembers().isEmpty());
        assertEquals(0, team.getMemberCount());
        assertNull(team.getCardImageUrl());
    }

    @Test
    public void listCharacters_filterByIdsKeepsThePipes() throws Exception {
        server.enqueue(new MockResponse().setBody(LIST_JSON));

        service.listCharacters(ComicVineService.MEMBER_FIELDS, 100, 0, null, "id:1440|1442").execute();

        HttpUrl url = server.takeRequest().getRequestUrl();
        assertNotNull(url);
        assertEquals("id:1440|1442", url.queryParameter("filter"));
        assertNull(url.queryParameter("sort"));
    }
}
