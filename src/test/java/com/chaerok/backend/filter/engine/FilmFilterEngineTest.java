package com.chaerok.backend.filter.engine;

import com.chaerok.backend.filter.analysis.AdaptiveFilterPolicy;
import com.chaerok.backend.filter.analysis.FilterAdjustment;
import com.chaerok.backend.filter.analysis.FilterOverlayTuning;
import com.chaerok.backend.filter.analysis.FilterOverlayTuningPolicy;
import com.chaerok.backend.filter.analysis.ImageAnalysis;
import com.chaerok.backend.filter.analysis.ImageSceneAnalyzer;
import com.chaerok.backend.filter.analysis.SceneType;
import com.chaerok.backend.filter.preset.FilmFilterPreset;
import com.chaerok.backend.filter.preset.FilmFilterPresetProvider;
import com.chaerok.backend.filter.processor.OverlayImageCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmFilterEngineTest {

    @Mock
    private FilmFilterPresetProvider presetProvider;

    @Mock
    private OverlayImageCache overlayImageCache;

    @Mock
    private ImageSceneAnalyzer imageSceneAnalyzer;

    @Mock
    private AdaptiveFilterPolicy adaptiveFilterPolicy;

    @Mock
    private FilterOverlayTuningPolicy filterOverlayTuningPolicy;

    private FilmFilterEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FilmFilterEngine(
                presetProvider,
                overlayImageCache,
                imageSceneAnalyzer,
                adaptiveFilterPolicy,
                filterOverlayTuningPolicy
        );
    }

    @Test
    @DisplayName("필터 ID의 공백을 제거하고 이미지 분석 및 보정 정책을 적용한다")
    void appliesFilterWithTrimmedFilterId() {
        // given
        BufferedImage original = createImage(
                100,
                100,
                100
        );

        FilmFilterPreset preset =
                createPreset(10.0);

        ImageAnalysis analysis =
                createAnalysis();

        FilterAdjustment adjustment =
                createAdjustment(1.0);

        FilterOverlayTuning overlayTuning =
                new FilterOverlayTuning(
                        1.0,
                        1.0
                );

        when(presetProvider.getByFilterId("gongju"))
                .thenReturn(preset);
        when(imageSceneAnalyzer.analyze(original))
                .thenReturn(analysis);
        when(adaptiveFilterPolicy.calculate(analysis))
                .thenReturn(adjustment);
        when(filterOverlayTuningPolicy.getTuning("gongju"))
                .thenReturn(overlayTuning);

        // when
        BufferedImage result = engine.apply(
                original,
                "  gongju  ",
                0.5
        );

        // then
        assertThat(result).isNotSameAs(original);
        assertThat(result.getWidth())
                .isEqualTo(original.getWidth());
        assertThat(result.getHeight())
                .isEqualTo(original.getHeight());

        Color resultColor = new Color(
                result.getRGB(0, 0)
        );

        assertThat(resultColor.getRed()).isEqualTo(105);
        assertThat(resultColor.getGreen()).isEqualTo(105);
        assertThat(resultColor.getBlue()).isEqualTo(105);

        InOrder inOrder = inOrder(
                presetProvider,
                imageSceneAnalyzer,
                adaptiveFilterPolicy,
                filterOverlayTuningPolicy
        );

        inOrder.verify(presetProvider)
                .getByFilterId("gongju");
        inOrder.verify(imageSceneAnalyzer)
                .analyze(original);
        inOrder.verify(adaptiveFilterPolicy)
                .calculate(analysis);
        inOrder.verify(filterOverlayTuningPolicy)
                .getTuning("gongju");

        verifyNoInteractions(overlayImageCache);
    }

    @Test
    @DisplayName("사용자 필터 강도가 1보다 크면 1로 제한한다")
    void clampsUserStrengthToOne() {
        // given
        BufferedImage original = createImage(
                100,
                100,
                100
        );

        FilmFilterPreset preset =
                createPreset(10.0);

        ImageAnalysis analysis =
                createAnalysis();

        FilterAdjustment adjustment =
                createAdjustment(1.0);

        givenFilterDependencies(
                original,
                "gongju",
                preset,
                analysis,
                adjustment
        );

        // when
        BufferedImage result = engine.apply(
                original,
                "gongju",
                2.0
        );

        // then
        Color resultColor = new Color(
                result.getRGB(0, 0)
        );

        assertThat(resultColor.getRed()).isEqualTo(110);
        assertThat(resultColor.getGreen()).isEqualTo(110);
        assertThat(resultColor.getBlue()).isEqualTo(110);

        verifyNoInteractions(overlayImageCache);
    }

    @Test
    @DisplayName("사용자 필터 강도가 음수이면 0으로 제한한다")
    void clampsNegativeUserStrengthToZero() {
        // given
        BufferedImage original = createImage(
                100,
                100,
                100
        );

        FilmFilterPreset preset =
                createPreset(20.0);

        ImageAnalysis analysis =
                createAnalysis();

        FilterAdjustment adjustment =
                createAdjustment(1.0);

        givenFilterDependencies(
                original,
                "gongju",
                preset,
                analysis,
                adjustment
        );

        // when
        BufferedImage result = engine.apply(
                original,
                "gongju",
                -0.5
        );

        // then
        Color resultColor = new Color(
                result.getRGB(0, 0)
        );

        assertThat(resultColor.getRed()).isEqualTo(100);
        assertThat(resultColor.getGreen()).isEqualTo(100);
        assertThat(resultColor.getBlue()).isEqualTo(100);

        verifyNoInteractions(overlayImageCache);
    }

    @Test
    @DisplayName("자동 보정 강도가 최대값을 초과하면 1.25로 제한한다")
    void clampsAdaptiveStrengthToMaximum() {
        // given
        BufferedImage original = createImage(
                100,
                100,
                100
        );

        FilmFilterPreset preset =
                createPreset(20.0);

        ImageAnalysis analysis =
                createAnalysis();

        FilterAdjustment adjustment =
                createAdjustment(2.0);

        givenFilterDependencies(
                original,
                "gongju",
                preset,
                analysis,
                adjustment
        );

        // when
        BufferedImage result = engine.apply(
                original,
                "gongju",
                1.0
        );

        // then
        Color resultColor = new Color(
                result.getRGB(0, 0)
        );

        /*
         * exposure 20 * 최대 자동 보정 강도 1.25 = 25
         */
        assertThat(resultColor.getRed()).isEqualTo(125);
        assertThat(resultColor.getGreen()).isEqualTo(125);
        assertThat(resultColor.getBlue()).isEqualTo(125);

        verifyNoInteractions(overlayImageCache);
    }

    @Test
    @DisplayName("원본 이미지가 null이면 예외가 발생한다")
    void rejectsNullOriginalImage() {
        // when & then
        assertThatThrownBy(
                () -> engine.apply(
                        null,
                        "gongju",
                        1.0
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("원본 이미지가 비어 있습니다.");

        verifyNoInteractions(
                presetProvider,
                overlayImageCache,
                imageSceneAnalyzer,
                adaptiveFilterPolicy,
                filterOverlayTuningPolicy
        );
    }

    @Test
    @DisplayName("원본 이미지 너비가 올바르지 않으면 예외가 발생한다")
    void rejectsInvalidOriginalImageWidth() {
        // given
        BufferedImage invalidImage =
                org.mockito.Mockito.mock(
                        BufferedImage.class
                );

        when(invalidImage.getWidth()).thenReturn(0);

        // when & then
        assertThatThrownBy(
                () -> engine.apply(
                        invalidImage,
                        "gongju",
                        1.0
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("원본 이미지 크기가 올바르지 않습니다.");

        verifyNoInteractions(
                presetProvider,
                overlayImageCache,
                imageSceneAnalyzer,
                adaptiveFilterPolicy,
                filterOverlayTuningPolicy
        );
    }

    @Test
    @DisplayName("원본 이미지 높이가 올바르지 않으면 예외가 발생한다")
    void rejectsInvalidOriginalImageHeight() {
        // given
        BufferedImage invalidImage =
                org.mockito.Mockito.mock(
                        BufferedImage.class
                );

        when(invalidImage.getWidth()).thenReturn(10);
        when(invalidImage.getHeight()).thenReturn(0);

        // when & then
        assertThatThrownBy(
                () -> engine.apply(
                        invalidImage,
                        "gongju",
                        1.0
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("원본 이미지 크기가 올바르지 않습니다.");

        verifyNoInteractions(
                presetProvider,
                overlayImageCache,
                imageSceneAnalyzer,
                adaptiveFilterPolicy,
                filterOverlayTuningPolicy
        );
    }

    @Test
    @DisplayName("필터 ID가 null이면 예외가 발생한다")
    void rejectsNullFilterId() {
        // given
        BufferedImage original = createImage(
                100,
                100,
                100
        );

        // when & then
        assertThatThrownBy(
                () -> engine.apply(
                        original,
                        null,
                        1.0
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("filterId는 필수입니다.");

        verifyNoInteractions(
                presetProvider,
                overlayImageCache,
                imageSceneAnalyzer,
                adaptiveFilterPolicy,
                filterOverlayTuningPolicy
        );
    }

    @Test
    @DisplayName("필터 ID가 공백이면 예외가 발생한다")
    void rejectsBlankFilterId() {
        // given
        BufferedImage original = createImage(
                100,
                100,
                100
        );

        // when & then
        assertThatThrownBy(
                () -> engine.apply(
                        original,
                        "   ",
                        1.0
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("filterId는 필수입니다.");

        verifyNoInteractions(
                presetProvider,
                overlayImageCache,
                imageSceneAnalyzer,
                adaptiveFilterPolicy,
                filterOverlayTuningPolicy
        );
    }

    private void givenFilterDependencies(
            BufferedImage original,
            String filterId,
            FilmFilterPreset preset,
            ImageAnalysis analysis,
            FilterAdjustment adjustment
    ) {
        when(presetProvider.getByFilterId(filterId))
                .thenReturn(preset);
        when(imageSceneAnalyzer.analyze(original))
                .thenReturn(analysis);
        when(adaptiveFilterPolicy.calculate(analysis))
                .thenReturn(adjustment);
        when(filterOverlayTuningPolicy.getTuning(filterId))
                .thenReturn(
                        new FilterOverlayTuning(
                                1.0,
                                1.0
                        )
                );
    }

    private BufferedImage createImage(
            int red,
            int green,
            int blue
    ) {
        BufferedImage image = new BufferedImage(
                1,
                1,
                BufferedImage.TYPE_INT_RGB
        );

        image.setRGB(
                0,
                0,
                new Color(red, green, blue).getRGB()
        );

        return image;
    }

    private ImageAnalysis createAnalysis() {
        return new ImageAnalysis(
                0.5,
                0.1,
                0.1,
                0.2,
                SceneType.LANDSCAPE
        );
    }

    private FilterAdjustment createAdjustment(
            double exposureMultiplier
    ) {
        return new FilterAdjustment(
                exposureMultiplier,
                1.0,
                1.0,
                1.0,
                1.0,
                1.0,
                1.0
        );
    }

    private FilmFilterPreset createPreset(
            double exposure
    ) {
        return new FilmFilterPreset(
                "gongju",
                "공주",
                "테스트 필터",
                exposure,
                1.0,
                0.0,
                0.0,
                0.0,
                0.0,
                "",
                0.0,
                "overlay"
        );
    }
}