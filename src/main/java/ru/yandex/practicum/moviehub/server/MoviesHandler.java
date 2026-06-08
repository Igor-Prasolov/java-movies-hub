package ru.yandex.practicum.moviehub.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.yandex.practicum.moviehub.model.ErrorResponse;
import ru.yandex.practicum.moviehub.model.Movie;
import ru.yandex.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private final Gson gson = new Gson();


    MoviesHandler(MoviesStore store) {
        this.store = store;
    }


    @Override
    public void handle(HttpExchange ex) throws IOException {
        List<Movie> movies = store.getAll();
        int currentYear = LocalDate.now().getYear();
        String method = ex.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/movies")) {
                String query = ex.getRequestURI().getQuery();
                if (query == null) {
                    String json = gson.toJson(movies);
                    sendJson(ex, 200, json);
                } else if (query.startsWith("year=")) {
                    String yearStr = query.substring(5);
                    try {
                        int year = Integer.parseInt(yearStr);
                        List<Movie> filtered = movies.stream()
                                .filter(m -> m.getYear() == year)
                                .collect(Collectors.toList());
                        String json = gson.toJson(filtered);
                        sendJson(ex, 200, json);
                    } catch (NumberFormatException e) {
                        sendJson(ex, 400, "{\"error\":\"Invalid year\"}");
                    }
                } else {
                    sendJson(ex, 400, "{\"error\":\"Unknown parameter\"}");
                }
            } else if (path.startsWith("/movies/")) {
                String[] parts = path.split("/");
                String idStr = parts[2];
                int id;
                try {
                    id = Integer.parseInt(idStr);
                } catch (NumberFormatException e) {
                    sendJson(ex, 400,
                            "{\"error\":\"Invalid id\"}");
                    return;
                }
                Optional<Movie> movie = store.getById(id);
                if (movie.isPresent()) {
                    sendJson(ex, 200, gson.toJson(movie.get()));
                } else {
                    sendJson(ex, 404, "{\"error\":\"Not found\"}");
                }
            } else {
                sendJson(ex, 404, "{\"error\":\"Not found\"}");
            }
        } else if (method.equalsIgnoreCase("POST")) {
            List<String> errors = new ArrayList<>();
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || contentType.isEmpty() || !contentType.startsWith("application/json")) {
                sendJson(ex, 415, "{\"error\": \"Unsupported Media Type\"}");
                return;
            }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Movie json;
            try {
                json = gson.fromJson(body, Movie.class);
            } catch (com.google.gson.JsonSyntaxException e) {
                sendJson(ex, 400, "{\"error\":\"Invalid JSON\"}");
                return;
            }
            if (json.getTitle() == null || json.getTitle().trim().isEmpty()) {
                errors.add("Title must not be empty");
            }
            if (json.getTitle() != null && json.getTitle().length() > 100) {
                errors.add("Title must not exceed 100 characters");
            }
            if (json.getYear() < 1888) {
                errors.add("Year must be at least 1888");
            }
            if (json.getYear() > (currentYear + 1)) {
                errors.add("Year must not be later than next year");
            }
            if (!errors.isEmpty()) {
                ErrorResponse errorResponse = new ErrorResponse("Validation failed", errors);
                String jsonErrors = gson.toJson(errorResponse);
                sendJson(ex, 422, jsonErrors);
                return;
            }
            Movie savedMovie = store.addMovie(json);
            String responseJson = gson.toJson(savedMovie);
            sendJson(ex, 201, responseJson);
        } else if (method.equalsIgnoreCase("DELETE")) {
            String path = ex.getRequestURI().getPath();
            if (path.startsWith("/movies")) {
                String[] parts = path.split("/");
                if (parts.length < 3) {
                    sendJson(ex, 404, "{\"error\":\"Not found\"}");
                    return;
                }
                String idStr = parts[2];
                int id;
                try {
                    id = Integer.parseInt(idStr);
                } catch (NumberFormatException e) {
                    sendJson(ex, 400, "{\"error\":\"Invalid id\"}");
                    return;
                }
                Optional<Movie> movie = store.getById(id);
                if (movie.isPresent()) {
                    store.deleteById(id);
                    sendNoContent(ex);
                } else {
                    sendJson(ex, 404, "{\"error\":\"Not found\"}");
                }
            } else {
                sendJson(ex, 404, "{\"error\":\"Not found\"}");
            }

        } else {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
        }
    }
}
