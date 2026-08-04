package com.chaerok.backend.filter.analysis;


public class AdaptiveFilterPolicy {

    /**
     * 이미지 분석 결과를 바탕으로
     * 각 필터 효과에 적용할 자동 보정 배율을 계산합니다.
     */
    public FilterAdjustment calculate(
            ImageAnalysis analysis
    ) {
        if (analysis == null) {
            throw new IllegalArgumentException(
                    "이미지 분석 결과가 비어 있습니다."
            );
        }

        SceneProfile profile =
                getSceneProfile(analysis.sceneType());

        /*
         * 사진이 어두울수록 노출·색감·오버레이를 강화하고,
         * 사진이 밝을수록 약하게 적용합니다.
         *
         * brightness 0.22 이하: 약 1.20배
         * brightness 0.85 이상: 약 0.75배
         */
        double brightnessIntensity = interpolate(
                1.20,
                0.75,
                normalize(
                        analysis.brightness(),
                        0.22,
                        0.85
                )
        );

        /*
         * 어두운 사진은 이미 노이즈와 암부가 많을 수 있으므로
         * 그레인과 비네팅을 별도로 약화합니다.
         *
         * brightness 0.15 이하: 약 0.60배
         * brightness 0.40 이상: 1.00배
         */
        double darkImageProtection = interpolate(
                0.60,
                1.00,
                normalize(
                        analysis.brightness(),
                        0.15,
                        0.40
                )
        );

        /*
         * 노출은 사진 밝기의 영향을 직접 받도록 합니다.
         */
        double exposureMultiplier =
                profile.exposure()
                        * brightnessIntensity;

        /*
         * 대비는 장면 프로필 값을 중심으로 적용합니다.
         *
         * 풍경에서는 조금 강하게,
         * 인물과 야간에서는 조금 부드럽게 적용됩니다.
         */
        double contrastMultiplier =
                profile.contrast();

        /*
         * 색온도는 밝기에 따라 소폭 조정합니다.
         *
         * 밝기 때문에 색감이 지나치게 변하지 않도록
         * 밝기 배율의 영향은 10%만 반영합니다.
         */
        double temperatureMultiplier =
                profile.temperature()
                        * (
                        0.90
                                + brightnessIntensity * 0.10
                );

        /*
         * 페이드는 장면 프로필을 기준으로 적용합니다.
         */
        double fadeMultiplier =
                profile.fade();

        /*
         * 어두운 사진에서는 노이즈 보호를 위해
         * 그레인을 자동으로 낮춥니다.
         */
        double grainMultiplier =
                profile.grain()
                        * darkImageProtection;

        /*
         * 어두운 사진에서는 모서리가 지나치게 검게 뭉치지 않도록
         * 비네팅을 자동으로 낮춥니다.
         */
        double vignetteMultiplier =
                profile.vignette()
                        * darkImageProtection;

        /*
         * 오버레이는 밝은 사진에서는 약하게,
         * 어두운 사진에서는 강하게 적용합니다.
         *
         * 최종 최대 강도는 FilmFilterEngine에서
         * 별도로 1.10으로 제한됩니다.
         */
        double overlayMultiplier =
                profile.overlay()
                        * brightnessIntensity;

        /*
         * 야간 인물 사진 보호
         *
         * NIGHT로 분류됐지만 얼굴도 검출된 경우,
         * 피부가 거칠어지거나 얼굴이 과하게 어두워지는 것을
         * 방지하기 위해 일부 효과를 추가로 완화합니다.
         *
         * 기존보다 완화 폭을 줄여 필터감이 유지되도록 했습니다.
         */
        if (
                analysis.sceneType() == SceneType.NIGHT
                        && analysis.hasFace()
        ) {
            contrastMultiplier *= 0.95;
            grainMultiplier *= 0.85;
            vignetteMultiplier *= 0.90;
            overlayMultiplier *= 0.92;
        }

        return new FilterAdjustment(
                exposureMultiplier,
                contrastMultiplier,
                temperatureMultiplier,
                fadeMultiplier,
                grainMultiplier,
                vignetteMultiplier,
                overlayMultiplier
        );
    }

    /**
     * 풍경·인물·야간 장면별 기본 프로필입니다.
     */
    private SceneProfile getSceneProfile(
            SceneType sceneType
    ) {
        if (sceneType == null) {
            return getLandscapeProfile();
        }

        return switch (sceneType) {
            /*
             * 풍경
             *
             * 색감과 대비, 오버레이를 조금 선명하게 적용합니다.
             */
            case LANDSCAPE -> new SceneProfile(
                    1.00, // exposure
                    1.05, // contrast
                    1.00, // temperature
                    1.00, // fade
                    1.00, // grain
                    1.00, // vignette
                    1.05  // overlay
            );

            /*
             * 인물
             *
             * 기존 인물 정책은 전체 필터를 너무 많이 약화해
             * 필터감이 거의 사라졌습니다.
             *
             * 피부 보호를 위해 그레인과 비네팅은 조금 낮추되,
             * 대비·색감·오버레이는 충분히 유지합니다.
             */
            case PORTRAIT -> new SceneProfile(
                    1.00, // exposure
                    0.94, // contrast
                    1.00, // temperature
                    1.00, // fade
                    0.82, // grain
                    0.88, // vignette
                    0.95  // overlay
            );

            /*
             * 야간
             *
             * 노출은 강화하지만,
             * 노이즈와 암부 손실을 막기 위해
             * 대비·그레인·비네팅을 낮춥니다.
             *
             * 오버레이는 기존 1.10에서 0.88로 낮춰
             * 어두운 사진을 질감이 덮어버리는 현상을 줄입니다.
             */
            case NIGHT -> new SceneProfile(
                    1.15, // exposure
                    0.82, // contrast
                    1.05, // temperature
                    0.85, // fade
                    0.50, // grain
                    0.55, // vignette
                    0.88  // overlay
            );
        };
    }

    private SceneProfile getLandscapeProfile() {
        return new SceneProfile(
                1.00,
                1.05,
                1.00,
                1.00,
                1.00,
                1.00,
                1.05
        );
    }

    /**
     * 값을 지정한 범위 안에서 0.0~1.0으로 변환합니다.
     */
    private double normalize(
            double value,
            double min,
            double max
    ) {
        if (max <= min) {
            return 0.0;
        }

        return clamp01(
                (value - min)
                        / (max - min)
        );
    }

    /**
     * 시작값과 종료값 사이를 부드럽게 보간합니다.
     */
    private double interpolate(
            double start,
            double end,
            double progress
    ) {
        double safeProgress =
                clamp01(progress);

        return start
                + (end - start)
                * safeProgress;
    }

    private double clamp01(double value) {
        return Math.max(
                0.0,
                Math.min(1.0, value)
        );
    }

    /**
     * 장면별 기본 배율을 보관합니다.
     */
    private record SceneProfile(
            double exposure,
            double contrast,
            double temperature,
            double fade,
            double grain,
            double vignette,
            double overlay
    ) {
    }
}