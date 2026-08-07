package com.chaerok.backend.visit.exception;

public class FilmRollNotVisitableException extends RuntimeException {

    public FilmRollNotVisitableException() {
        super("촬영 중인 필름 롤에서만 방문을 인증할 수 있습니다.");
    }
}
