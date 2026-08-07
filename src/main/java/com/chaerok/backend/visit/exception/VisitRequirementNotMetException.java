package com.chaerok.backend.visit.exception;

public class VisitRequirementNotMetException extends RuntimeException {

    public VisitRequirementNotMetException() {
        super("관광지, 식당, 카페를 각각 1곳 이상 방문해야 현상할 수 있습니다.");
    }
}
