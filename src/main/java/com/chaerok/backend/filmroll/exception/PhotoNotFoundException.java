package com.chaerok.backend.filmroll.exception;

public class PhotoNotFoundException extends RuntimeException {

    public PhotoNotFoundException() {
        super("사진 정보를 찾을 수 없습니다.");
    }
}
