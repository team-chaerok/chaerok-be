package com.chaerok.backend.visit.exception;

public class VisitAlreadyExistsException extends RuntimeException {

    public VisitAlreadyExistsException() {
        super("이미 방문 인증한 장소입니다.");
    }
}
