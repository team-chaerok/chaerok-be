package com.chaerok.backend.filmroll.exception;

public class FilmRollExitRequiredException
        extends FilmRollConflictException {

    public FilmRollExitRequiredException() {
        super("지역 이탈이 확정된 뒤에 현상할 수 있습니다.");
    }
}
