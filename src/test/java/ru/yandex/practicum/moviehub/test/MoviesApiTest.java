package ru.yandex.practicum.moviehub.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.moviehub.server.BaseHttpHandler;
import ru.yandex.practicum.moviehub.server.MoviesServer;
import ru.yandex.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;


    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void clearStore() {
        store.clearAll();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                response.headers().firstValue("Content-Type").orElse("");
        assertEquals(BaseHttpHandler.CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = response.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }


    @Test
    void addMovie_whenValid_Return201() throws Exception {
        String json = "{\"title\":\"Fast&Furios1\",\"year\":2001}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, response.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                response.headers().firstValue("Content-Type").orElse("");
        assertEquals(BaseHttpHandler.CT_JSON, contentTypeHeaderValue,
                "Content-Type должен быть application/json; charset=UTF-8");
    }

    @Test
    void addMovie_whenInvalidContentType_Return415() throws Exception {
        String json = "{\"title\":\"Fast&Furios1\",\"year\":2001}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, response.statusCode(), "POST /movies должен вернуть 415");
    }

    @Test
    void addMovie_whenTitleIsEmpty_return422() throws Exception {
        String json = "{\"title\":\"\",\"year\":2001}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode());

        JsonElement jsonElement = JsonParser.parseString(response.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String error = jsonObject.get("error").getAsString();
        JsonArray details = jsonObject.get("details").getAsJsonArray();

        assertTrue(details.size() > 0);
        assertEquals("Validation failed", error);
    }

    @Test
    void addMovie_whenTitle100Char_return201() throws Exception {
        String longTitle = "a".repeat(100);
        String json = "{\"title\":\"" + longTitle + "\",\"year\":2001}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonElement jsonElement = JsonParser.parseString(response.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String title = jsonObject.get("title").getAsString();
        assertEquals(201, response.statusCode());
        assertEquals(100, title.length());
    }

    @Test
    void addMovie_whenTitle101Char_return422() throws Exception {
        String longTitle = "a".repeat(101);
        String json = "{\"title\":\"" + longTitle + "\",\"year\":2001}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonElement jsonElement = JsonParser.parseString(response.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String error = jsonObject.get("error").getAsString();
        JsonArray details = jsonObject.get("details").getAsJsonArray();

        assertTrue(details.size() > 0);
        assertEquals("Validation failed", error);
        assertEquals(422, response.statusCode());
    }

    @Test
    void addMovie_whenYearTooSmall_return422() throws Exception {
        String json = "{\"title\":\"Fast&Furios1\",\"year\":1887}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonElement jsonElement = JsonParser.parseString(response.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonArray details = jsonObject.get("details").getAsJsonArray();

        assertTrue(details.size() > 0);
        assertEquals(422, response.statusCode());
    }

    @Test
    void addMovie_whenYearToBig_return422() throws Exception {
        int currentYear = LocalDate.now().getYear();
        String json = "{\"title\":\"Fast\",\"year\":" + (currentYear + 2) + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonElement jsonElement = JsonParser.parseString(response.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonArray details = jsonObject.get("details").getAsJsonArray();

        assertTrue(details.size() > 0);
        assertEquals(422, response.statusCode());
    }

    @Test
    void addMovie_whenNoContentType_return415() throws Exception {
        String json = "{\"title\":\"Fast\",\"year\":2001}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(415, response.statusCode());
    }

    @Test
    void addMovie_whenInvalidJson_return400() throws Exception {
        String invalidJson = "{lalallala";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }


    @Test
    void getMovieById_whenExists_return200() throws Exception {
        String json = "{\"title\":\"Fast&Furios1\",\"year\":2001}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonElement jsonElement = JsonParser.parseString(response.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        assertEquals(201, response.statusCode());

        int id = jsonObject.get("id").getAsInt();

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> getResponse =
                client.send(getRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, getResponse.statusCode());
    }

    @Test
    void getMovieById_whenIdNotNumber_return400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/lalal"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode());
    }

    @Test
    void getMovieById_whenNotFound_return404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999999"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, response.statusCode());
    }


    @Test
    void deleteMovie_whenExists_return204() throws Exception {
        String json = "{\"title\":\"Fast&Furios1\",\"year\":2001}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonElement jsonElement = JsonParser.parseString(response.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        assertEquals(201, response.statusCode());

        int id = jsonObject.get("id").getAsInt();

        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .DELETE()
                .build();

        HttpResponse<String> deleteResponse =
                client.send(deleteRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, deleteResponse.statusCode());
    }

    @Test
    void deleteMovie_whenNotFound_return404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999999"))
                .DELETE()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, response.statusCode());
    }

    @Test
    void deleteMovie_whenNotNumber_return400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/lalal"))
                .DELETE()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode());
    }


    @Test
    void getMovies_whenYearValid_return200() throws Exception {
        String json = "{\"title\":\"Fast&Furios1\",\"year\":2001}";
        String json2 = "{\"title\":\"Fast&Furios2\",\"year\":2003}";

        HttpRequest post1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response =
                client.send(post1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, response.statusCode());

        HttpRequest post2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();
        HttpResponse<String> response2 =
                client.send(post2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, response2.statusCode());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2001"))
                .GET()
                .build();

        HttpResponse<String> response3 =
                client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response3.statusCode());

        JsonArray jsonArray = JsonParser.parseString(response3.body()).getAsJsonArray();
        assertEquals(1, jsonArray.size());
        assertEquals(2001, jsonArray.get(0).getAsJsonObject().get("year").getAsInt());
    }

    @Test
    void getMovies_whenYearInvalid_return400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    void deleteMovie_whenNoId_return404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }
}
