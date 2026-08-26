package com.chaerok.backend.odii.service;

import com.chaerok.backend.heritage.dto.HeritagePlaceResponse;
import com.chaerok.backend.heritage.service.HeritageService;
import com.chaerok.backend.odii.dto.OdiiGuideResponse;
import com.chaerok.backend.odii.external.OdiiApiClient;
import com.chaerok.backend.odii.external.OdiiStoryItem;
import com.chaerok.backend.odii.external.OdiiThemeItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OdiiServiceTest {

    @Mock
    private HeritageService heritageService;

    @Mock
    private OdiiApiClient odiiApiClient;

    private OdiiService odiiService;

    @BeforeEach
    void setUp() {
        odiiService = new OdiiService(
                heritageService,
                odiiApiClient
        );
    }

    @Test
    @DisplayName("키워드가 포함된 Odii 테마와 스토리에 오디오가 있으면 오디오 가이드를 반환한다")
    void getAudioGuideWithPlayableAudio() {
        // given
        Long placeId = 1L;

        when(heritageService.getHeritagePlace(placeId))
                .thenReturn(createHeritagePlace(
                        placeId,
                        "공주 공산성",
                        "공산성 소개"
                ));

        OdiiThemeItem theme =
                createTheme("theme-1", "theme-list-1", "공주 공산성");

        when(odiiApiClient.searchThemes("공산성"))
                .thenReturn(List.of(theme));

        OdiiStoryItem story = createStory(
                "theme-1",
                "theme-list-1",
                "story-1",
                "공주 공산성",
                "공산성 오디오 스크립트",
                "https://example.com/gongsanseong.mp3"
        );

        when(odiiApiClient.getStories(
                "theme-1",
                "theme-list-1"
        )).thenReturn(List.of(story));

        // when
        OdiiGuideResponse response =
                odiiService.getAudioGuide(placeId);

        // then
        assertThat(response.heritage()).isTrue();
        assertThat(response.audioAvailable()).isTrue();
        assertThat(response.audioGuide()).isNotNull();
        assertThat(response.audioGuide().storyId()).isEqualTo("story-1");
        assertThat(response.audioGuide().title()).isEqualTo("공주 공산성");
        assertThat(response.audioGuide().audioUrl())
                .isEqualTo("https://example.com/gongsanseong.mp3");
        assertThat(response.guideScript())
                .isEqualTo("공산성 오디오 스크립트");
        assertThat(response.fallbackOverview()).isNull();
    }

    @Test
    @DisplayName("Odii 테마 검색 결과가 있어도 키워드와 관련 없으면 TourAPI 소개 문구를 반환한다")
    void getAudioGuideFallsBackWhenThemeDoesNotMatch() {
        // given
        Long placeId = 1L;

        when(heritageService.getHeritagePlace(placeId))
                .thenReturn(createHeritagePlace(
                        placeId,
                        "공주 공산성",
                        "공산성 TourAPI 소개"
                ));

        OdiiThemeItem unrelatedTheme =
                createTheme(
                        "theme-2",
                        "theme-list-2",
                        "공주 국립박물관"
                );

        when(odiiApiClient.searchThemes("공산성"))
                .thenReturn(List.of(unrelatedTheme));

        // when
        OdiiGuideResponse response =
                odiiService.getAudioGuide(placeId);

        // then
        assertThat(response.audioAvailable()).isFalse();
        assertThat(response.audioGuide()).isNull();
        assertThat(response.guideScript()).isNull();
        assertThat(response.fallbackOverview())
                .isEqualTo("공산성 TourAPI 소개");

        verify(odiiApiClient, never())
                .getStories("theme-2", "theme-list-2");
    }

    @Test
    @DisplayName("테마는 일치하지만 관련된 대표 스토리가 없으면 TourAPI 소개 문구를 반환한다")
    void getAudioGuideFallsBackWhenStoryDoesNotMatch() {
        // given
        Long placeId = 1L;

        when(heritageService.getHeritagePlace(placeId))
                .thenReturn(createHeritagePlace(
                        placeId,
                        "공주 공산성",
                        "공산성 TourAPI 소개"
                ));

        OdiiThemeItem theme =
                createTheme("theme-1", "theme-list-1", "공주 공산성");

        when(odiiApiClient.searchThemes("공산성"))
                .thenReturn(List.of(theme));

        OdiiStoryItem unrelatedStory = createStory(
                "theme-1",
                "theme-list-1",
                "story-2",
                "무령왕릉 이야기",
                "무령왕릉 스크립트",
                "https://example.com/muryeong.mp3"
        );

        when(odiiApiClient.getStories(
                "theme-1",
                "theme-list-1"
        )).thenReturn(List.of(unrelatedStory));

        // when
        OdiiGuideResponse response =
                odiiService.getAudioGuide(placeId);

        // then
        assertThat(response.audioAvailable()).isFalse();
        assertThat(response.audioGuide()).isNull();
        assertThat(response.guideScript()).isNull();
        assertThat(response.fallbackOverview())
                .isEqualTo("공산성 TourAPI 소개");
    }

    @Test
    @DisplayName("오디오 URL이 없고 스크립트가 있으면 Odii 스크립트를 반환한다")
    void getAudioGuideWithScriptOnly() {
        // given
        Long placeId = 2L;

        when(heritageService.getHeritagePlace(placeId))
                .thenReturn(createHeritagePlace(
                        placeId,
                        "서산 해미읍성",
                        "해미읍성 TourAPI 소개"
                ));

        OdiiThemeItem theme =
                createTheme("theme-3", "theme-list-3", "서산 해미읍성");

        when(odiiApiClient.searchThemes("해미읍성"))
                .thenReturn(List.of(theme));

        OdiiStoryItem story = createStory(
                "theme-3",
                "theme-list-3",
                "story-3",
                "서산 해미읍성",
                "해미읍성 Odii 설명입니다.",
                null
        );

        when(odiiApiClient.getStories(
                "theme-3",
                "theme-list-3"
        )).thenReturn(List.of(story));

        // when
        OdiiGuideResponse response =
                odiiService.getAudioGuide(placeId);

        // then
        assertThat(response.audioAvailable()).isFalse();
        assertThat(response.audioGuide()).isNull();
        assertThat(response.guideScript())
                .isEqualTo("해미읍성 Odii 설명입니다.");
        assertThat(response.fallbackOverview()).isNull();
    }

    @Test
    @DisplayName("Odii 데이터가 없으면 TourAPI 소개 문구를 반환한다")
    void getAudioGuideFallsBackWhenOdiiDataIsEmpty() {
        // given
        Long placeId = 3L;

        when(heritageService.getHeritagePlace(placeId))
                .thenReturn(createHeritagePlace(
                        placeId,
                        "향천사",
                        "향천사 TourAPI 소개"
                ));

        when(odiiApiClient.searchThemes("향천사"))
                .thenReturn(List.of());

        // when
        OdiiGuideResponse response =
                odiiService.getAudioGuide(placeId);

        // then
        assertThat(response.heritage()).isTrue();
        assertThat(response.audioAvailable()).isFalse();
        assertThat(response.audioGuide()).isNull();
        assertThat(response.guideScript()).isNull();
        assertThat(response.fallbackOverview())
                .isEqualTo("향천사 TourAPI 소개");
    }

    @Test
    @DisplayName("정림사지 키워드가 포함된 관련 테마와 스토리는 정상 매칭한다")
    void getAudioGuideMatchesJeongnimsaWithContains() {
        // given
        Long placeId = 4L;

        when(heritageService.getHeritagePlace(placeId))
                .thenReturn(createHeritagePlace(
                        placeId,
                        "부여 정림사지 오층석탑",
                        "정림사지 TourAPI 소개"
                ));

        OdiiThemeItem theme = createTheme(
                "theme-4",
                "theme-list-4",
                "정림사지 박물관"
        );

        when(odiiApiClient.searchThemes("정림사지"))
                .thenReturn(List.of(theme));

        OdiiStoryItem story = createStory(
                "theme-4",
                "theme-list-4",
                "story-4",
                "정림사지 박물관",
                "정림사지 관련 Odii 설명입니다.",
                "https://example.com/jeongnimsa.mp3"
        );

        when(odiiApiClient.getStories(
                "theme-4",
                "theme-list-4"
        )).thenReturn(List.of(story));

        // when
        OdiiGuideResponse response =
                odiiService.getAudioGuide(placeId);

        // then
        assertThat(response.audioAvailable()).isTrue();
        assertThat(response.audioGuide()).isNotNull();
        assertThat(response.audioGuide().title())
                .isEqualTo("정림사지 박물관");
        assertThat(response.fallbackOverview()).isNull();
    }

    @Test
    @DisplayName("해미국제성지는 해미순례성지 키워드로 검색하여 관련 스크립트를 반환한다")
    void getAudioGuideUsesMappedKeywordForHaemiInternationalShrine() {
        // given
        Long placeId = 5L;

        when(heritageService.getHeritagePlace(placeId))
                .thenReturn(createHeritagePlace(
                        placeId,
                        "해미국제성지",
                        "해미국제성지 TourAPI 소개"
                ));

        OdiiThemeItem theme = createTheme(
                "theme-5",
                "theme-list-5",
                "서산 해미순례성지"
        );

        when(odiiApiClient.searchThemes("해미순례성지"))
                .thenReturn(List.of(theme));

        OdiiStoryItem story = createStory(
                "theme-5",
                "theme-list-5",
                "story-5",
                "서산 해미순례성지",
                "해미순례성지 Odii 설명입니다.",
                null
        );

        when(odiiApiClient.getStories(
                "theme-5",
                "theme-list-5"
        )).thenReturn(List.of(story));

        // when
        OdiiGuideResponse response =
                odiiService.getAudioGuide(placeId);

        // then
        assertThat(response.audioAvailable()).isFalse();
        assertThat(response.audioGuide()).isNull();
        assertThat(response.guideScript())
                .isEqualTo("해미순례성지 Odii 설명입니다.");
        assertThat(response.fallbackOverview()).isNull();
    }

    private HeritagePlaceResponse createHeritagePlace(
            Long placeId,
            String title,
            String overview
    ) {
        return new HeritagePlaceResponse(
                placeId,
                "1001",
                title,
                "충청남도",
                "충청남도",
                true,
                "HS",
                "HS01",
                "HS010100",
                overview,
                null
        );
    }

    private OdiiThemeItem createTheme(
            String tid,
            String tlid,
            String title
    ) {
        return new OdiiThemeItem(
                tid,
                tlid,
                title,
                "127.0000",
                "36.0000"
        );
    }

    private OdiiStoryItem createStory(
            String tid,
            String tlid,
            String stid,
            String title,
            String script,
            String audioUrl
    ) {
        return new OdiiStoryItem(
                tid,
                tlid,
                stid,
                "story-list-1",
                title,
                "127.0000",
                "36.0000",
                title + " 오디오",
                script,
                120,
                audioUrl,
                "ko",
                null
        );
    }
}