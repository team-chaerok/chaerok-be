package com.chaerok.backend.odii.service;

import com.chaerok.backend.heritage.dto.HeritagePlaceResponse;
import com.chaerok.backend.heritage.service.HeritageService;
import com.chaerok.backend.odii.dto.OdiiGuideResponse;
import com.chaerok.backend.odii.external.OdiiApiClient;
import com.chaerok.backend.odii.external.OdiiStoryItem;
import com.chaerok.backend.odii.external.OdiiThemeItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OdiiService {

    private static final Map<String, String> ODII_SEARCH_KEYWORDS = Map.of(
            "공주 공산성", "공산성",
            "공주 무령왕릉과 왕릉원", "무령왕릉",
            "서동공원과 궁남지", "궁남지",
            "부여 정림사지 오층석탑", "정림사지",
            "관북리유적과 부소산성", "부소산성",
            "서산 해미읍성", "해미읍성",
            "해미국제성지", "해미순례성지",
            "향천사", "향천사"
    );

    private final HeritageService heritageService;
    private final OdiiApiClient odiiApiClient;

    public OdiiGuideResponse getAudioGuide(Long placeId) {
        HeritagePlaceResponse heritagePlace =
                heritageService.getHeritagePlace(placeId);

        if (!heritagePlace.heritage()) {
            return OdiiGuideResponse.withoutOdii(
                    heritagePlace.placeId(),
                    heritagePlace.title(),
                    false,
                    heritagePlace.overview()
            );
        }

        String keyword = resolveSearchKeyword(heritagePlace.title());

        List<OdiiThemeItem> themes =
                odiiApiClient.searchThemes(keyword);

        Optional<OdiiThemeItem> selectedTheme =
                selectTheme(themes, keyword);

        if (selectedTheme.isEmpty()) {
            return OdiiGuideResponse.withoutOdii(
                    heritagePlace.placeId(),
                    heritagePlace.title(),
                    true,
                    heritagePlace.overview()
            );
        }

        OdiiThemeItem theme = selectedTheme.get();

        List<OdiiStoryItem> stories = odiiApiClient.getStories(
                theme.tid(),
                theme.tlid()
        );

        Optional<OdiiStoryItem> representativeStory =
                selectRepresentativeStory(stories, keyword);

        if (representativeStory.isEmpty()) {
            return OdiiGuideResponse.withoutOdii(
                    heritagePlace.placeId(),
                    heritagePlace.title(),
                    true,
                    heritagePlace.overview()
            );
        }

        OdiiStoryItem story = representativeStory.get();

        if (story.hasPlayableAudio()) {
            return OdiiGuideResponse.withAudio(
                    heritagePlace.placeId(),
                    heritagePlace.title(),
                    story
            );
        }

        if (hasScript(story)) {
            return OdiiGuideResponse.withScript(
                    heritagePlace.placeId(),
                    heritagePlace.title(),
                    story
            );
        }

        return OdiiGuideResponse.withoutOdii(
                heritagePlace.placeId(),
                heritagePlace.title(),
                true,
                heritagePlace.overview()
        );
    }

    private String resolveSearchKeyword(String placeTitle) {
        if (placeTitle == null || placeTitle.isBlank()) {
            return placeTitle;
        }

        return ODII_SEARCH_KEYWORDS.entrySet().stream()
                .filter(entry -> placeTitle.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(placeTitle);
    }

    private Optional<OdiiThemeItem> selectTheme(
            List<OdiiThemeItem> themes,
            String keyword
    ) {
        if (themes == null || themes.isEmpty()) {
            return Optional.empty();
        }

        return themes.stream()
                .filter(theme -> containsKeyword(theme.title(), keyword))
                .findFirst();
    }

    private Optional<OdiiStoryItem> selectRepresentativeStory(
            List<OdiiStoryItem> stories,
            String keyword
    ) {
        if (stories == null || stories.isEmpty()) {
            return Optional.empty();
        }

        return stories.stream()
                .filter(story ->
                        isRepresentativeTitle(story.title(), keyword)
                )
                .findFirst();
    }

    private boolean isRepresentativeTitle(
            String title,
            String keyword
    ) {
        if (!containsKeyword(title, keyword)) {
            return false;
        }

        // "공산성 - 금서루" 같은 세부 지점보다
        // "공주 공산성" 같은 장소 전체 안내를 우선한다.
        return !title.contains("-");
    }

    private boolean containsKeyword(
            String title,
            String keyword
    ) {
        return title != null
                && keyword != null
                && title.contains(keyword);
    }

    private boolean hasScript(OdiiStoryItem story) {
        return story.script() != null
                && !story.script().isBlank();
    }
}