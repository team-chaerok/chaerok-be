package com.chaerok.backend.filter.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FilmFilterPipelineTest {

    @Test
    @DisplayName("프로세서를 등록된 순서대로 적용한다")
    void appliesProcessorsInOrder() {
        // given
        ImageProcessor firstProcessor =
                mock(ImageProcessor.class);
        ImageProcessor secondProcessor =
                mock(ImageProcessor.class);
        ImageProcessor thirdProcessor =
                mock(ImageProcessor.class);

        BufferedImage original =
                mock(BufferedImage.class);
        BufferedImage firstResult =
                mock(BufferedImage.class);
        BufferedImage secondResult =
                mock(BufferedImage.class);
        BufferedImage finalResult =
                mock(BufferedImage.class);

        double strength = 0.75;

        when(firstProcessor.process(
                original,
                strength
        )).thenReturn(firstResult);

        when(secondProcessor.process(
                firstResult,
                strength
        )).thenReturn(secondResult);

        when(thirdProcessor.process(
                secondResult,
                strength
        )).thenReturn(finalResult);

        FilmFilterPipeline pipeline =
                new FilmFilterPipeline(
                        List.of(
                                firstProcessor,
                                secondProcessor,
                                thirdProcessor
                        )
                );

        // when
        BufferedImage result = pipeline.apply(
                original,
                strength
        );

        // then
        assertThat(result).isSameAs(finalResult);

        InOrder inOrder = inOrder(
                firstProcessor,
                secondProcessor,
                thirdProcessor
        );

        inOrder.verify(firstProcessor)
                .process(original, strength);
        inOrder.verify(secondProcessor)
                .process(firstResult, strength);
        inOrder.verify(thirdProcessor)
                .process(secondResult, strength);
    }

    @Test
    @DisplayName("각 프로세서의 결과를 다음 프로세서의 입력으로 전달한다")
    void passesProcessorResultToNextProcessor() {
        // given
        ImageProcessor firstProcessor =
                mock(ImageProcessor.class);
        ImageProcessor secondProcessor =
                mock(ImageProcessor.class);

        BufferedImage original =
                mock(BufferedImage.class);
        BufferedImage intermediateResult =
                mock(BufferedImage.class);
        BufferedImage finalResult =
                mock(BufferedImage.class);

        double strength = 0.5;

        when(firstProcessor.process(
                original,
                strength
        )).thenReturn(intermediateResult);

        when(secondProcessor.process(
                intermediateResult,
                strength
        )).thenReturn(finalResult);

        FilmFilterPipeline pipeline =
                new FilmFilterPipeline(
                        List.of(
                                firstProcessor,
                                secondProcessor
                        )
                );

        // when
        BufferedImage result = pipeline.apply(
                original,
                strength
        );

        // then
        assertThat(result).isSameAs(finalResult);

        verify(firstProcessor)
                .process(original, strength);
        verify(secondProcessor)
                .process(intermediateResult, strength);
    }

    @Test
    @DisplayName("모든 프로세서에 동일한 필터 강도를 전달한다")
    void passesSameStrengthToEveryProcessor() {
        // given
        ImageProcessor firstProcessor =
                mock(ImageProcessor.class);
        ImageProcessor secondProcessor =
                mock(ImageProcessor.class);

        BufferedImage original =
                mock(BufferedImage.class);
        BufferedImage intermediateResult =
                mock(BufferedImage.class);
        BufferedImage finalResult =
                mock(BufferedImage.class);

        double strength = 1.25;

        when(firstProcessor.process(
                original,
                strength
        )).thenReturn(intermediateResult);

        when(secondProcessor.process(
                intermediateResult,
                strength
        )).thenReturn(finalResult);

        FilmFilterPipeline pipeline =
                new FilmFilterPipeline(
                        List.of(
                                firstProcessor,
                                secondProcessor
                        )
                );

        // when
        pipeline.apply(original, strength);

        // then
        verify(firstProcessor)
                .process(original, strength);
        verify(secondProcessor)
                .process(intermediateResult, strength);
    }

    @Test
    @DisplayName("프로세서가 없으면 원본 이미지를 그대로 반환한다")
    void returnsOriginalImageWhenProcessorListIsEmpty() {
        // given
        BufferedImage original =
                mock(BufferedImage.class);

        FilmFilterPipeline pipeline =
                new FilmFilterPipeline(List.of());

        // when
        BufferedImage result = pipeline.apply(
                original,
                0.8
        );

        // then
        assertThat(result).isSameAs(original);
        verifyNoInteractions(original);
    }

    @Test
    @DisplayName("프로세서에서 예외가 발생하면 이후 프로세서를 실행하지 않는다")
    void stopsProcessingWhenProcessorThrowsException() {
        // given
        ImageProcessor firstProcessor =
                mock(ImageProcessor.class);
        ImageProcessor secondProcessor =
                mock(ImageProcessor.class);

        BufferedImage original =
                mock(BufferedImage.class);

        double strength = 0.7;

        when(firstProcessor.process(
                original,
                strength
        )).thenThrow(
                new IllegalStateException(
                        "이미지 처리 실패"
                )
        );

        FilmFilterPipeline pipeline =
                new FilmFilterPipeline(
                        List.of(
                                firstProcessor,
                                secondProcessor
                        )
                );

        // when & then
        assertThatThrownBy(
                () -> pipeline.apply(
                        original,
                        strength
                )
        ).isInstanceOf(IllegalStateException.class)
                .hasMessage("이미지 처리 실패");

        verify(firstProcessor)
                .process(original, strength);
        verify(secondProcessor, never())
                .process(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyDouble()
                );
    }
}