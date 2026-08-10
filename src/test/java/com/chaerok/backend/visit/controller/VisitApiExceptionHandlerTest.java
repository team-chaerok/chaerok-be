package com.chaerok.backend.visit.controller;

import com.chaerok.backend.global.exception.ErrorResponse;
import com.chaerok.backend.visit.exception.FilmRollNotVisitableException;
import com.chaerok.backend.visit.exception.PlaceRegionMismatchException;
import com.chaerok.backend.visit.exception.VisitAlreadyExistsException;
import com.chaerok.backend.visit.exception.VisitApiExceptionHandler;
import com.chaerok.backend.visit.exception.VisitRequirementNotMetException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitApiExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private VisitApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new VisitApiExceptionHandler();
        when(request.getRequestURI())
                .thenReturn("/api/film-rolls/100/visits");
    }

    @Test
    @DisplayName("중복 방문을 409 VISIT_ALREADY_EXISTS로 반환한다")
    void duplicateVisit() {
        assertConflict(
                handler.handleVisitAlreadyExists(
                        new VisitAlreadyExistsException(),
                        request
                ),
                "VISIT_ALREADY_EXISTS"
        );
    }

    @Test
    @DisplayName("방문 불가능 상태를 409 VISIT_NOT_ALLOWED로 반환한다")
    void notVisitable() {
        assertConflict(
                handler.handleFilmRollNotVisitable(
                        new FilmRollNotVisitableException(),
                        request
                ),
                "VISIT_NOT_ALLOWED"
        );
    }

    @Test
    @DisplayName("지역 불일치를 409 PLACE_REGION_MISMATCH로 반환한다")
    void regionMismatch() {
        assertConflict(
                handler.handlePlaceRegionMismatch(
                        new PlaceRegionMismatchException(),
                        request
                ),
                "PLACE_REGION_MISMATCH"
        );
    }

    @Test
    @DisplayName("현상 방문 조건 미충족을 409 VISIT_REQUIREMENT_NOT_MET로 반환한다")
    void requirementNotMet() {
        assertConflict(
                handler.handleVisitRequirementNotMet(
                        new VisitRequirementNotMetException(),
                        request
                ),
                "VISIT_REQUIREMENT_NOT_MET"
        );
    }

    private void assertConflict(
            ResponseEntity<ErrorResponse> response,
            String code
    ) {
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(code);
        assertThat(response.getBody().path())
                .isEqualTo("/api/film-rolls/100/visits");
    }
}
