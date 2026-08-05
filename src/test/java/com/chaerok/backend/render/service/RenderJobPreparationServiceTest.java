package com.chaerok.backend.render.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollConflictException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.aws.AwsProperties;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.repository.RenderJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderJobPreparationServiceTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private RenderJobRepository renderJobRepository;

    @Mock
    private AwsProperties awsProperties;

    @Mock
    private FilmRoll filmRoll;

    @Mock
    private Photo firstPhoto;

    @Mock
    private Photo thirdPhoto;

    @Test
    @DisplayName("사진 순서가 1부터 연속되지 않으면 SQS 작업 생성 전에 거부한다")
    void rejectsNonContiguousPhotoSequences() {
        when(filmRollRepository.findByIdAndUserIdForUpdate(2L, 6L))
                .thenReturn(Optional.of(filmRoll));
        when(filmRoll.getStatus())
                .thenReturn(FilmRollStatus.READY);
        when(renderJobRepository.existsByFilmRollIdAndStatusIn(
                eq(2L),
                anyList()
        )).thenReturn(false);
        when(photoRepository
                .findAllByFilmRollIdOrderBySequenceAsc(2L))
                .thenReturn(List.of(firstPhoto, thirdPhoto));
        when(filmRoll.getTotalPhotoCount()).thenReturn(2);
        when(firstPhoto.getSequence()).thenReturn(1);
        when(thirdPhoto.getSequence()).thenReturn(3);

        RenderJobPreparationService service =
                new RenderJobPreparationService(
                        filmRollRepository,
                        photoRepository,
                        renderJobRepository,
                        awsProperties
                );

        assertThatThrownBy(() -> service.prepare(6L, 2L))
                .isInstanceOf(FilmRollConflictException.class)
                .hasMessageContaining("1부터")
                .hasMessageContaining("연속");

        verify(renderJobRepository, never())
                .saveAndFlush(any(RenderJob.class));
    }
}
