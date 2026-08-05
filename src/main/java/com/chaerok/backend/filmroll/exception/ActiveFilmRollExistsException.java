package com.chaerok.backend.filmroll.exception;

public class ActiveFilmRollExistsException
        extends FilmRollConflictException {

    public ActiveFilmRollExistsException() {
        super("이미 진행 중인 필름 롤이 있습니다.");
    }
}
