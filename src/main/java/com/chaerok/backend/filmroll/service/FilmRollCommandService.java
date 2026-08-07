package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollCreateRequest;
import com.chaerok.backend.filmroll.dto.FilmRollResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.ActiveFilmRollExistsException;
import com.chaerok.backend.filmroll.exception.FilmRollConflictException;
import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.filter.preset.FilmFilterPresetProvider;
import com.chaerok.backend.global.exception.RegionNotFoundException;
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.repository.UserRepository;
import com.chaerok.backend.visit.service.VisitRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FilmRollCommandService {

    private static final int CURRENT_FILTER_VERSION = 1;

    private final FilmRollRepository filmRollRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final FilmFilterPresetProvider filterPresetProvider;
    private final VisitRequirementService visitRequirementService;

    @Transactional
    public FilmRollResponse createFilmRoll(
            Long userId,
            FilmRollCreateRequest request
    ) {
        if (filmRollRepository.existsByUserIdAndStatusIn(
                userId,
                FilmRollStatus.incompleteStatuses()
        )) {
            throw new ActiveFilmRollExistsException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(RegionNotFoundException::new);

        if (!region.isServiceEnabled()) {
            throw new RegionNotFoundException();
        }

        filterPresetProvider.getByFilterId(
                request.filterId().trim()
        );

        FilmRoll filmRoll = FilmRoll.create(
                user,
                region,
                request.filterId(),
                request.filterStrength(),
                CURRENT_FILTER_VERSION
        );

        try {
            FilmRoll savedFilmRoll =
                    filmRollRepository.saveAndFlush(filmRoll);

            return FilmRollResponse.from(savedFilmRoll);
        } catch (DataIntegrityViolationException exception) {
            throw new ActiveFilmRollExistsException();
        }
    }

    @Transactional
    public FilmRollResponse markReady(
            Long userId,
            Long filmRollId
    ) {
        FilmRoll filmRoll =
                findOwnedFilmRollForUpdate(
                        userId,
                        filmRollId
                );

        if (filmRoll.getStatus() != FilmRollStatus.CAPTURING) {
            throw new FilmRollConflictException(
                    "촬영 중인 필름 롤만 현상 준비 상태로 전환할 수 있습니다."
            );
        }

        validateUploadedPhotos(filmRoll);
        visitRequirementService.requireSatisfied(filmRollId);
        filmRoll.markReady();

        return FilmRollResponse.from(filmRoll);
    }

    @Transactional
    public PreparedFilmRollDevelopment prepareDevelopment(
            Long userId,
            Long filmRollId
    ) {
        FilmRoll filmRoll =
                findOwnedFilmRollForUpdate(
                        userId,
                        filmRollId
                );

        return switch (filmRoll.getStatus()) {
            case CAPTURING -> {
                validateUploadedPhotos(filmRoll);
                visitRequirementService.requireSatisfied(filmRollId);
                filmRoll.markReady();
                yield PreparedFilmRollDevelopment.requestRequired(
                        filmRoll
                );
            }
            case READY -> {
                validateUploadedPhotos(filmRoll);
                visitRequirementService.requireSatisfied(filmRollId);
                yield PreparedFilmRollDevelopment.requestRequired(
                        filmRoll
                );
            }
            case FAILED -> {
                validateUploadedPhotos(filmRoll);
                visitRequirementService.requireSatisfied(filmRollId);
                filmRoll.prepareRetry();
                yield PreparedFilmRollDevelopment.requestRequired(
                        filmRoll
                );
            }
            case QUEUED, PROCESSING ->
                    PreparedFilmRollDevelopment.alreadyRequested(
                            filmRoll
                    );
            case COMPLETED -> throw new FilmRollConflictException(
                    "이미 현상이 완료된 필름 롤입니다."
            );
            case EXPIRED -> throw new FilmRollConflictException(
                    "현상 결과가 만료된 필름 롤은 다시 현상할 수 없습니다."
            );
        };
    }

    private void validateUploadedPhotos(FilmRoll filmRoll) {
        Long filmRollId = filmRoll.getId();

        long savedPhotoCount =
                photoRepository.countByFilmRollId(filmRollId);

        if (savedPhotoCount == 0) {
            throw new FilmRollConflictException(
                    "사진이 없는 필름 롤은 현상할 수 없습니다."
            );
        }

        if (savedPhotoCount != filmRoll.getTotalPhotoCount()) {
            throw new FilmRollConflictException(
                    "아직 업로드가 완료되지 않은 사진이 있습니다."
            );
        }

        boolean hasIncompletePhoto =
                photoRepository.existsByFilmRollIdAndStatusNot(
                        filmRollId,
                        PhotoStatus.UPLOADED
                );

        if (hasIncompletePhoto) {
            throw new FilmRollConflictException(
                    "모든 사진의 업로드가 완료되어야 현상을 시작할 수 있습니다."
            );
        }
    }

    private FilmRoll findOwnedFilmRollForUpdate(
            Long userId,
            Long filmRollId
    ) {
        return filmRollRepository
                .findByIdAndUserIdForUpdate(
                        filmRollId,
                        userId
                )
                .orElseThrow(FilmRollNotFoundException::new);
    }
}
