package com.chaerok.backend.filmroll.exception;

public class FilmRollNotFoundException extends RuntimeException {

    public FilmRollNotFoundException() {
        super("필름 롤을 찾을 수 없습니다.");
    }
}
