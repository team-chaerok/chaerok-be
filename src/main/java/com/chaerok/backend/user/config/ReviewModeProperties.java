package com.chaerok.backend.user.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "chaerok.review-mode")
public class ReviewModeProperties {

    @NotBlank(message = "심사용 지역 시도명은 필수입니다.")
    private String provinceName = "충청남도";

    @NotBlank(message = "심사용 지역 시군명은 필수입니다.")
    private String cityCountyName = "공주시";

    @NotNull(message = "심사용 장소 목록은 필수입니다.")
    @Size(
            min = 3,
            max = 3,
            message = "심사용 장소는 정확히 3개여야 합니다."
    )
    private List<@NotBlank(
            message = "심사용 장소 TourAPI contentId는 비어 있을 수 없습니다."
    ) String> testPlaceTourContentIds = new ArrayList<>(List.of(
            "125949",
            "2738735",
            "2876760"
    ));

    @AssertTrue(message = "심사용 장소는 서로 다른 3개여야 합니다.")
    public boolean isTestPlaceConfigurationDistinct() {
        if (testPlaceTourContentIds == null) {
            return true;
        }

        return new HashSet<>(testPlaceTourContentIds).size()
                == testPlaceTourContentIds.size();
    }
}
