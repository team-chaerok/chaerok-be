package com.chaerok.backend.filmroll.exception;

public class FilmRollDevelopmentWaitException
        extends FilmRollConflictException {

    public FilmRollDevelopmentWaitException() {
        super("지역 이탈 후 1시간이 지나야 현상할 수 있습니다.");
    }
}
