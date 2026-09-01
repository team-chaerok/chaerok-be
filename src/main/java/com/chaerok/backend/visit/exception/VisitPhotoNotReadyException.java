package com.chaerok.backend.visit.exception;

public class VisitPhotoNotReadyException extends RuntimeException {

    public VisitPhotoNotReadyException() {
        super("업로드가 완료된 필름 롤 사진만 방문 인증에 사용할 수 있습니다.");
    }
}