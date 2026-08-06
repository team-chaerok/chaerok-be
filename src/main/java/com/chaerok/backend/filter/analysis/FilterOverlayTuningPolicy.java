package com.chaerok.backend.filter.analysis;


public class FilterOverlayTuningPolicy {

    /**
     * 각 오버레이 이미지 자체의 색 밀도와 채도를 기준으로
     * 추가 배율 및 최대 허용 강도를 반환합니다.
     */
    public FilterOverlayTuning getTuning(String filterId) {
        if (filterId == null || filterId.isBlank()) {
            return defaultTuning();
        }

        return switch (filterId) {
            /*
             * 공주
             *
             * 짙은 초록과 주황빛이 함께 있어
             * 기본 강도를 유지하되 최대값은 조금 제한합니다.
             */
            case "gongju" ->
                    new FilterOverlayTuning(
                            1.00,
                            1.05
                    );

            /*
             * 부여
             *
             * 파스텔 색감과 낮은 대비의 오버레이이므로
             * 다른 필터보다 조금 강하게 적용합니다.
             */
            case "buyeo" ->
                    new FilterOverlayTuning(
                            1.08,
                            1.10
                    );

            /*
             * 서산
             *
             * 주황·적색 채도와 광량이 매우 강하므로
             * 배율과 최대값을 모두 낮춥니다.
             */
            case "seosan" ->
                    new FilterOverlayTuning(
                            0.82,
                            0.90
                    );

            /*
             * 예산
             *
             * 청록과 베이지 색면의 영향이 크므로
             * 기본보다 조금 약하게 적용합니다.
             */
            case "yesan" ->
                    new FilterOverlayTuning(
                            0.90,
                            0.98
                    );

            default -> defaultTuning();
        };
    }

    private FilterOverlayTuning defaultTuning() {
        return new FilterOverlayTuning(
                1.00,
                1.00
        );
    }
}