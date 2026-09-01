package com.chaerok.backend.visit.exception;

public class VisitPhotoAlreadyUsedException extends RuntimeException {

    public VisitPhotoAlreadyUsedException() {
        super("이미 다른 방문 인증에 사용한 사진입니다.");
    }
}