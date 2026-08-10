package com.chaerok.backend.visit.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.exception.PlaceNotFoundException;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.visit.dto.VisitCreateRequest;
import com.chaerok.backend.visit.dto.VisitCreateResponse;
import com.chaerok.backend.visit.entity.Visit;
import com.chaerok.backend.visit.exception.FilmRollNotVisitableException;
import com.chaerok.backend.visit.exception.PlaceRegionMismatchException;
import com.chaerok.backend.visit.exception.VisitAlreadyExistsException;
import com.chaerok.backend.visit.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitCommandServiceTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private VisitRequirementService visitRequirementService;

    @Mock
    private FilmRoll filmRoll;

    @Mock
    private Place place;

    @Mock
    private Region filmRollRegion;

    @Mock
    private Region placeRegion;

    private VisitCommandService service;
    private VisitCreateRequest request;

    @BeforeEach
    void setUp() {
        service = new VisitCommandService(
                filmRollRepository,
                placeRepository,
                visitRepository,
                visitRequirementService
        );

        request = new VisitCreateRequest(200L);

        org.mockito.Mockito.lenient()
                .when(filmRoll.getId())
                .thenReturn(100L);
        org.mockito.Mockito.lenient()
                .when(filmRoll.getStatus())
                .thenReturn(FilmRollStatus.CAPTURING);
        org.mockito.Mockito.lenient()
                .when(filmRoll.getRegion())
                .thenReturn(filmRollRegion);
        org.mockito.Mockito.lenient()
                .when(filmRollRegion.getId())
                .thenReturn(10L);

        org.mockito.Mockito.lenient()
                .when(place.getId())
                .thenReturn(200L);
        org.mockito.Mockito.lenient()
                .when(place.getTitle())
                .thenReturn("공주 맛집");
        org.mockito.Mockito.lenient()
                .when(place.getCategoryGroup())
                .thenReturn(PlaceCategoryGroup.FOOD);
        org.mockito.Mockito.lenient()
                .when(place.getRegion())
                .thenReturn(placeRegion);
        org.mockito.Mockito.lenient()
                .when(placeRegion.getId())
                .thenReturn(10L);
    }

    @Test
    @DisplayName("GPS 검증이 끝난 placeId를 정상 방문 기록으로 저장한다")
    void createVisit() {
        stubOwnedFilmRollAndPlace();
        when(visitRepository.existsByFilmRollIdAndPlaceId(
                100L,
                200L
        )).thenReturn(false);

        when(visitRepository.saveAndFlush(any(Visit.class)))
                .thenAnswer(invocation -> {
                    Visit visit = invocation.getArgument(0);
                    ReflectionTestUtils.setField(visit, "id", 300L);
                    ReflectionTestUtils.setField(
                            visit,
                            "visitedAt",
                            LocalDateTime.of(2026, 8, 7, 15, 30)
                    );
                    return visit;
                });

        when(visitRequirementService.getProgress(100L))
                .thenReturn(new VisitRequirementService.Progress(
                        1,
                        3,
                        false
                ));

        VisitCreateResponse response =
                service.createVisit(1L, 100L, request);

        assertThat(response.visitId()).isEqualTo(300L);
        assertThat(response.filmRollId()).isEqualTo(100L);
        assertThat(response.placeId()).isEqualTo(200L);
        assertThat(response.categoryGroup()).isEqualTo("FOOD");
        assertThat(response.visitedCategoryCount()).isEqualTo(1);
        assertThat(response.requiredCategoryCount()).isEqualTo(3);
        assertThat(response.visitRequirementMet()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않거나 소유하지 않은 필름 롤은 같은 방식으로 거부한다")
    void rejectsMissingOrUnownedFilmRoll() {
        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createVisit(1L, 100L, request)
        ).isInstanceOf(FilmRollNotFoundException.class);

        verify(placeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 Place는 방문 인증하지 않는다")
    void rejectsMissingPlace() {
        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));
        when(placeRepository.findById(200L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createVisit(1L, 100L, request)
        ).isInstanceOf(PlaceNotFoundException.class);

        verify(visitRepository, never())
                .saveAndFlush(any(Visit.class));
    }

    @Test
    @DisplayName("FilmRoll과 다른 지역의 Place는 방문 인증하지 않는다")
    void rejectsPlaceFromDifferentRegion() {
        when(placeRegion.getId()).thenReturn(99L);
        stubOwnedFilmRollAndPlace();

        assertThatThrownBy(() ->
                service.createVisit(1L, 100L, request)
        ).isInstanceOf(PlaceRegionMismatchException.class);

        verify(visitRepository, never())
                .saveAndFlush(any(Visit.class));
    }

    @Test
    @DisplayName("CAPTURING 이외 상태에서는 새 방문을 추가하지 않는다")
    void rejectsNonCapturingFilmRoll() {
        when(filmRoll.getStatus())
                .thenReturn(FilmRollStatus.FAILED);
        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));

        assertThatThrownBy(() ->
                service.createVisit(1L, 100L, request)
        ).isInstanceOf(FilmRollNotVisitableException.class);

        verify(placeRepository, never()).findById(any());
        verify(visitRepository, never())
                .saveAndFlush(any(Visit.class));
    }


    @Test
    @DisplayName("지역 이탈이 확정된 뒤에는 새 방문을 추가하지 않는다")
    void rejectsVisitAfterExitConfirmation() {
        when(filmRoll.isExitConfirmed()).thenReturn(true);
        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));

        assertThatThrownBy(() ->
                service.createVisit(1L, 100L, request)
        ).isInstanceOf(FilmRollNotVisitableException.class);

        verify(placeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("같은 FilmRoll의 같은 Place 중복 방문은 거부한다")
    void rejectsDuplicateVisit() {
        stubOwnedFilmRollAndPlace();
        when(visitRepository.existsByFilmRollIdAndPlaceId(
                100L,
                200L
        )).thenReturn(true);

        assertThatThrownBy(() ->
                service.createVisit(1L, 100L, request)
        ).isInstanceOf(VisitAlreadyExistsException.class);

        verify(visitRepository, never())
                .saveAndFlush(any(Visit.class));
    }

    @Test
    @DisplayName("동시 중복 요청이 DB UNIQUE에 걸려도 중복 방문 예외로 처리한다")
    void mapsDatabaseUniqueCollisionToDuplicateVisit() {
        stubOwnedFilmRollAndPlace();
        when(visitRepository.existsByFilmRollIdAndPlaceId(
                100L,
                200L
        )).thenReturn(false);
        when(visitRepository.saveAndFlush(any(Visit.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "uk_visits_film_roll_place"
                ));

        assertThatThrownBy(() ->
                service.createVisit(1L, 100L, request)
        ).isInstanceOf(VisitAlreadyExistsException.class);
    }

    @Test
    @DisplayName("중복 방문과 무관한 DB 오류는 중복으로 위장하지 않는다")
    void rethrowsUnrelatedDatabaseViolation() {
        stubOwnedFilmRollAndPlace();
        when(visitRepository.existsByFilmRollIdAndPlaceId(
                100L,
                200L
        )).thenReturn(false);
        when(visitRepository.saveAndFlush(any(Visit.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "some_other_constraint"
                ));

        assertThatThrownBy(() ->
                service.createVisit(1L, 100L, request)
        ).isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(VisitAlreadyExistsException.class);
    }

    private void stubOwnedFilmRollAndPlace() {
        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));
        when(placeRepository.findById(200L))
                .thenReturn(Optional.of(place));
    }
}
