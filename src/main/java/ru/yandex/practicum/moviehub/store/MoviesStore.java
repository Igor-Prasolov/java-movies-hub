package ru.yandex.practicum.moviehub.store;

import ru.yandex.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {
    private Map<Integer, Movie> movieMap = new HashMap<>();
    private int nextId = 1;


    public List<Movie> getAll() {
        return new ArrayList<>(movieMap.values());
    }

    public Movie addMovie(Movie movie) {
        movie.setId(nextId);
        movieMap.put(nextId, movie);
        nextId++;
        return movie;
    }

    public void clearAll() {
        movieMap.clear();
    }

    public Optional<Movie> getById(int id) {
        return Optional.ofNullable(movieMap.get(id));
    }

    public void deleteById(int id) {
        movieMap.remove(id);
    }
}
