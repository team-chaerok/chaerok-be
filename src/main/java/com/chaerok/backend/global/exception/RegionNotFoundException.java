package com.chaerok.backend.global.exception;

public class RegionNotFoundException extends RuntimeException {

    public RegionNotFoundException() {
        super("지역을 찾을 수 없습니다.");
    }
}