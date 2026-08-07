package com.chaerok.backend.visit.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.user.entity.UserRole;
import com.chaerok.backend.visit.dto.VisitCreateRequest;
import com.chaerok.backend.visit.dto.VisitCreateResponse;
import com.chaerok.backend.visit.dto.VisitListResponse;
import com.chaerok.backend.visit.service.VisitCommandService;
import com.chaerok.backend.visit.service.VisitQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitControllerTest {

    @Mock
    private VisitCommandService visitCommandService;

    @Mock
    private VisitQueryService visitQueryService;

    private VisitController controller;
    private AuthenticatedUser authenticatedUser;

    @BeforeEach
    void setUp() {
        controller = new VisitController(
                visitCommandService,
                visitQueryService
        );
        authenticatedUser =
                new AuthenticatedUser(1L, UserRole.USER);
    }

    @Test
    @DisplayName("방문 등록 성공을 201로 반환한다")
    void createVisit() {
        VisitCreateRequest request = new VisitCreateRequest(200L);
        VisitCreateResponse expected = new VisitCreateResponse(
                300L,
                100L,
                200L,
                "공산성",
                "TOURISM",
                1,
                3,
                false,
                LocalDateTime.of(2026, 8, 7, 15, 0)
        );

        when(visitCommandService.createVisit(
                1L,
                100L,
                request
        )).thenReturn(expected);

        ResponseEntity<VisitCreateResponse> response =
                controller.createVisit(
                        authenticatedUser,
                        100L,
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(expected);
        verify(visitCommandService)
                .createVisit(1L, 100L, request);
    }

    @Test
    @DisplayName("방문 현황 조회를 200으로 반환한다")
    void getVisits() {
        VisitListResponse expected = new VisitListResponse(
                100L,
                2,
                3,
                false,
                List.of()
        );

        when(visitQueryService.getVisits(1L, 100L))
                .thenReturn(expected);

        ResponseEntity<VisitListResponse> response =
                controller.getVisits(
                        authenticatedUser,
                        100L
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(visitQueryService).getVisits(1L, 100L);
    }
}
