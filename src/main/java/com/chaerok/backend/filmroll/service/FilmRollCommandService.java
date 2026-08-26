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
import com.chaerok.backend.photo.entity.Photo;
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

import java.util.List;

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
    private final RegionFilterPolicy regionFilterPolicy;
    private final VisitRequirementService visitRequirementService;
    private final FilmRollDevelopmentTimingService developmentTimingService;

    @Transactional
    public FilmRollResponse createFilmRoll(
            Long userId,
            FilmRollCreateRequest request
    ) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        FilmRoll existingFilmRoll = filmRollRepository
                .findByUserIdAndClientFilmRollId(
                        userId,
                        request.clientFilmRollId()
                )
                .orElse(null);

        if (existingFilmRoll != null) {
            return FilmRollResponse.from(existingFilmRoll);
        }

        if (filmRollRepository
                .existsByUserIdAndStatusAndExitedAtIsNull(
                        userId,
                        FilmRollStatus.CAPTURING
                )) {
            throw new ActiveFilmRollExistsException();
        }

        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(RegionNotFoundException::new);

        if (!region.isServiceEnabled()) {
            throw new RegionNotFoundException();
        }

        String filterId = request.filterId().trim();

        filterPresetProvider.getByFilterId(filterId);
        regionFilterPolicy.validate(region, filterId);

        FilmRoll filmRoll = FilmRoll.create(
                user,
                region,
                request.clientFilmRollId(),
                filterId,
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

        visitRequirementService.requireSatisfied(filmRollId);
        developmentTimingService.requireAvailable(filmRoll);
        finalizePhotosForDevelopment(filmRoll);
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
                visitRequirementService.requireSatisfied(filmRollId);
                developmentTimingService.requireAvailable(filmRoll);
                finalizePhotosForDevelopment(filmRoll);
                filmRoll.markReady();
                yield PreparedFilmRollDevelopment.requestRequired(
                        filmRoll
                );
            }
            case READY -> {
                visitRequirementService.requireSatisfied(filmRollId);
                developmentTimingService.requireAvailable(filmRoll);
                finalizePhotosForDevelopment(filmRoll);
                yield PreparedFilmRollDevelopment.requestRequired(
                        filmRoll
                );
            }
            case FAILED -> {
                visitRequirementService.requireSatisfied(filmRollId);
                developmentTimingService.requireAvailable(filmRoll);
                finalizePhotosForDevelopment(filmRoll);
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

    private void finalizePhotosForDevelopment(FilmRoll filmRoll) {
        Long filmRollId = filmRoll.getId();

        List<Photo> photos = photoRepository
                .findAllByFilmRollIdOrderBySequenceAscForUpdate(
                        filmRollId
                );

        if (photos.isEmpty()) {
            throw new FilmRollConflictException(
                    "사진이 없는 필름 롤은 현상할 수 없습니다."
            );
        }

        boolean hasUnexpectedStatus = photos.stream()
                .anyMatch(photo ->
                        photo.getStatus() != PhotoStatus.UPLOADED
                                && photo.getStatus() != PhotoStatus.UPLOADING
                );

        if (hasUnexpectedStatus) {
            throw new FilmRollConflictException(
                    "현재 사진 상태에는 현상을 시작할 수 없습니다."
            );
        }

        List<Photo> uploadedPhotos = photos.stream()
                .filter(photo ->
                        photo.getStatus() == PhotoStatus.UPLOADED
                )
                .toList();

        if (uploadedPhotos.isEmpty()) {
            throw new FilmRollConflictException(
                    "업로드가 완료된 사진이 없어 현상할 수 없습니다."
            );
        }

        if (uploadedPhotos.size() != filmRoll.getTotalPhotoCount()) {
            throw new FilmRollConflictException(
                    "필름 롤 사진 수와 업로드 완료 사진 수가 일치하지 않습니다."
            );
        }

        List<Photo> abandonedUploads = photos.stream()
                .filter(photo ->
                        photo.getStatus() == PhotoStatus.UPLOADING
                )
                .toList();

        if (!abandonedUploads.isEmpty()) {
            photoRepository.deleteAll(abandonedUploads);
            photoRepository.flush();
        }

        compactUploadedPhotoSequences(uploadedPhotos);
    }

    private void compactUploadedPhotoSequences(
            List<Photo> uploadedPhotos
    ) {
        for (int index = 0; index < uploadedPhotos.size(); index++) {
            Photo photo = uploadedPhotos.get(index);
            int expectedSequence = index + 1;

            if (photo.getSequence() == expectedSequence) {
                continue;
            }

            photo.resequenceForDevelopment(expectedSequence);

            // UNIQUE(film_roll_id, sequence) 충돌을 피하려고
            // 낮은 sequence부터 한 장씩 반영한다.
            photoRepository.flush();
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
