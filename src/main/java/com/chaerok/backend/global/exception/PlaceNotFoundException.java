package com.chaerok.backend.global.exception;

public class PlaceNotFoundException extends RuntimeException {

    public PlaceNotFoundException() {
        super("장소를 찾을 수 없습니다.");
    }
}