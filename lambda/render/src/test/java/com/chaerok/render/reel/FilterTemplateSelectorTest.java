package com.chaerok.render.reel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterTemplateSelectorTest {

    private final FilterTemplateSelector selector =
            new FilterTemplateSelector(new ReelTemplateRegistry());

    @Test
    @DisplayName("공주 필터는 공주 V1 필름 셀 템플릿을 선택한다")
    void selectsGongjuTemplate() {
        ReelTemplate template = selector.select("gongju");

        assertThat(template.templateId()).isEqualTo("gongju-v1");
    }

    @Test
    @DisplayName("아직 템플릿이 없는 필터는 명시적으로 거부한다")
    void rejectsUnsupportedFilter() {
        assertThatThrownBy(() -> selector.select("buyeo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported reel filter template");
    }

    @Test
    @DisplayName("빈 필터 ID는 거부한다")
    void rejectsBlankFilterId() {
        assertThatThrownBy(() -> selector.select(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("filterId is required.");
    }
}
