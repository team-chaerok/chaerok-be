package com.chaerok.backend.visit.exception;

public class PlaceRegionMismatchException extends RuntimeException {

    public PlaceRegionMismatchException() {
        super("필름 롤 지역에 속하지 않은 장소는 방문 인증할 수 없습니다.");
    }
}
