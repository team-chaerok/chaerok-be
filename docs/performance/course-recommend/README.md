# 코스 추천 API 성능 Baseline

## 범위와 상태

- 대상: `GET /api/courses/recommend?regionId={regionId}`(지역 추천), `GET /api/courses/recommend?regionId={regionId}&anchorPlaceId={anchorPlaceId}`(Anchor 추천)
- 상태: **개선 전 Baseline**. 이 문서에는 개선 후 측정값이나 성능 개선 효과를 기록하지 않는다.
- 측정일: 2026-11-13 (KST)
- 측정 대상: `regionId=1`, `anchorPlaceId=1`. 두 요청 모두 측정 전에 HTTP 200과 완성된 코스 반환을 확인했다.

## 측정 환경과 방법

| 항목 | 내용 |
| --- | --- |
| 서버 | Windows의 IntelliJ에서 실행 중인 로컬 Spring Boot, `http://localhost:8080` |
| 프로젝트 설정 | Java 17 툴체인, Spring Boot 3.5.14, Spring Data JPA |
| JVM 확인 범위 | 프로젝트의 Java 17 설정은 확인했다. 당시 측정 셸의 `java -version`은 25.0.4였으며, **실행 중인 서버 JVM 버전은 독립적으로 확인하지 못했다.** |
| 데이터·외부 서비스 | 실제 DB와 Kakao Local API를 사용하는 로컬 서버. Supabase PostgreSQL 사용은 측정 환경 설명에 따르며 접속 대상을 별도로 검증하지는 않았다. 응답과 서버 로그에서 Kakao 후보 사용을 확인했다. |
| 요청 부하 | 시나리오별 단일 클라이언트, 순차 요청, 워밍업 5회 + 본 측정 20회. 동시 부하 시험이 아니다. |
| HTTP 시간 | `performance/scripts/measure-course.ps1`의 `Stopwatch`로 요청 시작부터 응답 본문 수신 완료까지 측정. 워밍업은 통계에서 제외. |
| 백분위수 | 응답 시간을 오름차순 정렬해 `ceil(p × N) - 1` 인덱스를 선택하는 nearest-rank 방식. |
| 성공·완성 | HTTP 200과 JSON 파싱 여부를 확인하고, 코스마다 장소 3개 및 `TOURISM`, `FOOD`, `CAFE_DESSERT` 포함 여부를 검사. 빈 코스는 별도로 기록. |

`GET /api/courses/recommend`는 [SecurityConfig](../../../src/main/java/com/chaerok/backend/global/config/SecurityConfig.java)에서 인증 없이 허용된다. 서버의 `/actuator/health`는 측정 전 `UP`이었다. 측정 중 서버와 DB 설정은 변경하지 않았다.

## 구현에서 확인한 호출 흐름

[CourseRecommendService](../../../src/main/java/com/chaerok/backend/course/service/CourseRecommendService.java)는 지역 존재 여부를 조회한 뒤 해당 지역의 대표 장소 목록을 조회한다. 지역 추천은 좌표가 있는 대표 `TOURISM` 장소를 우선순위와 ID로 정렬하여 최대 3개 Anchor를 순차 처리한다. Anchor 추천은 지정 장소를 추가 조회하고 지역·대표 여부·관광지 유형·좌표를 검증한다. `Place.region`은 지연 로딩 관계이므로 지역 정보 접근 시 SQL이 발생할 가능성이 있다. 초기 Baseline 실행에서는 SQL 횟수와 DB 실행 시간을 계측하지 않았다. 후속 요청별 계측 결과는 아래 DB 조회 절에 기록했다.

각 Anchor는 음식점(`FD6`) 다음 카페(`CE7`)를 검색한다. 각 유형은 먼저 반경 2km에서 후보를 찾고, **같은 지역 후보가 없을 때** 5km로 확장한다. Kakao 후보가 없으면 이미 조회한 DB 대표 장소 목록에서 같은 유형의 가까운 장소를 fallback으로 고른다. 장소 3개에 세 필수 유형이 모두 포함된 코스만 응답에 남긴다. 지역 추천은 최대 3개, Anchor 추천은 최대 1개 코스를 반환한다.

Kakao 호출은 Anchor별 누적 대기 예산 3초, 요청 전체 누적 대기 예산 기본 8초를 공유한다. 남은 예산이 250ms 미만이면 다음 호출을 생략한다. [KakaoLocalClient](../../../src/main/java/com/chaerok/backend/place/external/KakaoLocalClient.java)의 개별 호출 상한은 5초이며 전달된 잔여 예산이 더 짧으면 이를 적용한다. **이 예산은 HTTP 전체 처리 시간의 상한이 아니다.** 전체 예산 기본값은 [application.yml](../../../src/main/resources/application.yml)의 `COURSE_RECOMMEND_KAKAO_TOTAL_TIMEOUT`으로 변경될 수 있다. 측정 당시 이 환경변수의 실제 설정값은 확인하지 못했다.

## Baseline 결과

### Kakao 로그와 함께 수집한 실행

아래 HTTP와 Kakao 수치는 **같은 2026-11-13 재측정 실행**에서 나왔다. 시간 단위는 ms이다.

| 지표 | 지역 추천 | Anchor 추천 |
| --- | ---: | ---: |
| 본 측정 요청 | 20 | 20 |
| 평균 HTTP 응답 시간 | 450.800 | 212.628 |
| HTTP p50 | 446.646 | 201.127 |
| HTTP p95 | 490.944 | 244.217 |
| HTTP 최소 / 최대 | 386.910 / 493.759 | 163.876 / 246.662 |
| HTTP 200·정상 파싱 | 20/20 (100%) | 20/20 (100%) |
| 빈 코스 응답 | 0/20 | 0/20 |
| 반환된 완성 코스 / 전체 코스 | 60/60 (100%) | 20/20 (100%) |
| 요청당 Kakao 평균 호출 횟수 | 6.00회 | 2.00회 |
| 요청당 Kakao 평균 누적 대기 시간 | 358.300 | 119.550 |
| 요청별 Kakao 대기/HTTP 시간 비율의 평균 | 79.58% | 56.43% |
| 전체 Kakao 예산 소진 | 0/20 (0%) | 0/20 (0%) |
| DB fallback 사용 | 0/20 (0%) | 0/20 (0%) |

Kakao 비율은 **매칭된 요청마다** `kakaoElapsedMs / httpElapsedMs × 100`을 계산한 후 평균한 값이다. 평균 Kakao 시간과 평균 HTTP 시간의 단순 비율과는 계산 방식이 다르다. Kakao 시간은 서비스가 기록한 호출 대기 시간의 누적값이며, HTTP 시간은 클라이언트가 본 본문 수신 완료 시간이다.

| 시나리오 | Anchor ID | 본 측정 로그 수 | Anchor당 평균 Kakao 호출 | Anchor당 평균 Kakao 대기 | Anchor 예산 소진 | DB fallback |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 지역 추천 | 1 | 20 | 2.00회 | 119.40ms | 0/20 | 0/20 |
| 지역 추천 | 2 | 20 | 2.00회 | 118.00ms | 0/20 | 0/20 |
| 지역 추천 | 3 | 20 | 2.00회 | 119.75ms | 0/20 | 0/20 |
| Anchor 추천 | 1 | 20 | 2.00회 | 119.55ms | 0/20 | 0/20 |

본 측정의 Anchor 로그 80개 모두 `kakaoFoodUsed=true`, `kakaoCafeUsed=true`였다. Anchor별 대기 시간 합계와 요청 전체 대기 시간은 로그에서 각각 밀리초 단위로 절사되므로 소폭 다를 수 있다.

### 최초 HTTP 전용 실행

최초 실행은 서버 로그를 보존하지 못했다. 아래 수치를 Kakao 로그 기반 실행과 합치거나 Kakao 지표에 연결하지 않는다.

| 지표 | 지역 추천 | Anchor 추천 |
| --- | ---: | ---: |
| 평균 HTTP 응답 시간 | 498.649ms | 216.334ms |
| HTTP p50 | 434.125ms | 186.524ms |
| HTTP p95 | 915.245ms | 458.577ms |
| HTTP 최소 / 최대 | 395.882 / 923.463ms | 169.953 / 468.728ms |
| HTTP 성공·3유형 완성 | 각 20/20, 100% | 각 20/20, 100% |

## 로그 수집·매칭 근거와 한계

원본 `performance/logs/baseline-console.log`를 삭제하거나 덮어쓰지 않고, 측정 전후의 파일 바이트 오프셋을 `performance/scripts/save-course-log-slice.ps1`에 전달해 지역·Anchor 로그를 별도 보존했다. 지역 구간에는 요청 전체 로그 25개와 Anchor 로그 75개, Anchor 구간에는 각각 25개와 25개가 있었다. `performance/scripts/analyze-course-logs.ps1`는 각 HTTP 요청의 시작~종료 시간 창 안에서 전체 로그가 정확히 하나인지, Anchor 로그가 같은 스레드에서 예상 횟수만큼 선행하는지, 순서가 맞는지 검사했다. 두 시나리오 모두 워밍업 5개와 본 측정 20개가 이 검사를 통과했다.

로그에는 요청 ID가 없다. 측정 구간에서 추가 코스 추천 로그는 관찰되지 않았지만, 다른 클라이언트의 동시 요청이 전혀 없었다고 독립적으로 증명할 수는 없다. 따라서 요청별 비율과 로그 매칭은 **단일 클라이언트 실행 및 시간·스레드·개수 일치**라는 근거 범위에서 해석한다. 이 Baseline 로그에는 검색 반경이 없어 **2km→5km 확장 빈도는 미측정**이다. 이 실행에서 DB SQL 횟수·실행 시간과 Kakao 서버 내부 시간은 계측하지 않았다. DB는 후속 요청별 계측 결과를 아래에 따로 기록했다. 외부 API 상태와 운영 데이터에 따라 재측정값은 달라질 수 있다.

## Baseline 직후 병목 판단과 검토 계획

이 절은 Baseline 측정 직후의 판단과 당시 계획이다. 이후 병렬화 Mock, WebClient 재사용 및 DB 조회 검토 결과는 아래 의사결정 절에 기록했다.

- **실측 근거가 있는 후보:** Kakao 대기 시간은 지역 추천 HTTP 시간의 요청별 평균 79.58%, Anchor 추천의 56.43%를 차지했다. 지역 추천은 Anchor 3개를 순차 처리하면서 총 6회 호출했고, 각 Anchor는 평균 약 118~120ms를 Kakao 호출에서 기다렸다. 순차 외부 호출이 주요 지연 후보이다.
- **이번 표본에서 관찰되지 않은 문제:** 두 시나리오 모두 예산 소진과 DB fallback이 없었다. 각 Anchor의 호출 2회는 음식점·카페 검색 1회씩과 일치한다. 불필요한 반복 검색이나 timeout이 현재 지연의 원인이라는 근거는 없다.
- **추가 계측이 필요한 부분:** DB 조회 시간·SQL 횟수, 후보 처리 비용, 반경 확장 빈도는 이번 로그만으로 판단할 수 없다. 지역과 Anchor 응답 시간 차이를 개선 효과로 해석할 수 없다. 지역 추천은 처리하는 Anchor와 외부 호출 수가 다르다.
- **우선 검토할 개선 방향:** 코스 완성 정책과 지역 필터를 유지하면서 순차 호출 중 안전하게 독립 처리할 수 있는 부분이 있는지 검토한다. 호출 횟수 축소와 DB 중복 조회 제거는 추가 계측으로 중복·비용이 확인된 경우에 평가한다. 병렬 처리의 실제 효과와 오류·fallback 영향은 별도 Before/After 실측이 필요하다.

이 단계에서는 운영 코드, API 계약, 추천 정책을 변경하지 않는다.

## 근거 파일과 재현 명령

아래 `performance/` 경로는 프로젝트 루트 기준 **로컬 보관 위치**다. 현재 해당 폴더는 Git 추적에서 제외되어 있으므로 GitHub 공개 문서에서는 파일을 열 수 없다. 재현 명령은 로컬에 해당 스크립트와 결과 파일이 있는 환경에서만 실행할 수 있다.

| 시나리오 | HTTP 원본 | 서버 로그 구간 | 요청별 분석 | Anchor별 분석 |
| --- | --- | --- | --- | --- |
| 지역 추천 | `performance/results/baseline-region-20261113-184929.csv` | `performance/logs/baseline-region-20261113-184929.log` | `performance/results/baseline-region-20261113-184929-kakao-requests.csv` | `performance/results/baseline-region-20261113-184929-kakao-anchors.csv` |
| Anchor 추천 | `performance/results/baseline-anchor-20261113-185018.csv` | `performance/logs/baseline-anchor-20261113-185018.log` | `performance/results/baseline-anchor-20261113-185018-kakao-requests.csv` | `performance/results/baseline-anchor-20261113-185018-kakao-anchors.csv` |

최초 HTTP 전용 결과: `performance/results/baseline-region-20261113-183711.csv`, `performance/results/baseline-anchor-20261113-183728.csv`.

다음 명령은 프로젝트 루트의 PowerShell에서 실행한다. 새 측정 시에는 기존 CSV와 로그 파일을 덮어쓰지 않고, 같은 실행의 로그 구간을 별도 저장해야 한다. `-IsolatedRun`은 다른 코스 추천 요청이 없었고 로그 개수와 시간 창이 일치함을 확인한 경우에만 사용한다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\performance\scripts\measure-course.ps1 -Scenario REGION -RegionId 1 -WarmupCount 5 -MeasurementCount 20
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\performance\scripts\measure-course.ps1 -Scenario ANCHOR -RegionId 1 -AnchorPlaceId 1 -WarmupCount 5 -MeasurementCount 20
```

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\performance\scripts\analyze-course-logs.ps1 -LogPath .\performance\logs\baseline-region-20261113-184929.log -HttpCsvPath .\performance\results\baseline-region-20261113-184929.csv -Scenario REGION -IsolatedRun
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\performance\scripts\analyze-course-logs.ps1 -LogPath .\performance\logs\baseline-anchor-20261113-185018.log -HttpCsvPath .\performance\results\baseline-anchor-20261113-185018.csv -Scenario ANCHOR -IsolatedRun
```

## 의사결정 기록: 동일 Anchor 병렬화 B안 보류 (2026-11-13)

### 배경과 결정

위 Baseline에서 지역 추천은 Anchor 3개를 순서대로 처리하며 평균 Kakao 호출 6회, 누적 대기 358.300ms가 측정됐다. Anchor 추천은 평균 2회, 119.550ms였다. 요청별 HTTP 응답 시간에서 Kakao 대기가 차지한 비율은 각각 평균 79.58%, 56.43%였다. 현재 `CourseRecommendService`는 각 Anchor에서 음식점을 먼저 찾고 카페를 찾으므로 이 외부 호출은 순차 대기한다. 이에 동일 Anchor의 음식점·카페 검색만 병렬화하는 B안을 검토했다.

**결정: B안의 운영 코드 적용을 보류하고 기존 순차 처리 A안을 유지한다.** Mock에서는 벽시계 시간이 줄었지만, 현재의 누적 대기 예산을 유형별로 선예약하면 예산 경계에서 음식점 우선 검색 결과와 DB fallback 대상이 달라졌다. 코스의 완성 여부와 점수가 같아도 추천 장소가 바뀌므로 기존 추천 정책을 그대로 유지한다는 조건을 충족하지 못한다.

### A/B 실험 방법과 시간 결과

`performance/scripts/CourseParallelExperiment.java`는 Spring, DB 및 실제 Kakao API와 연결하지 않은 독립 Mock이다. Java 25.0.4의 Java 17 소스 실행 모드로 실행했다. A는 음식점→카페 순차 검색을 모사하고, B는 동일 Anchor에서 남은 누적 예산을 두 유형에 나누어 예약한 뒤 동시에 시작한다. B도 각 유형 안에서는 2km 검색 후 후보가 없을 때 5km를 검색하며 Anchor 간 처리는 순차로 유지한다. 지연, 빈 결과, HTTP 오류, timeout, DB fallback을 같은 입력으로 비교했다. 최종 `performance/results/parallel-experiment-20261113-threshold.csv`는 A/B 합계 80행이며 상세 조건은 `performance/results/parallel-experiment-20261113-summary.md`에 있다.

아래 `wallMs`는 실험 함수 시작부터 Mock 결과 조립까지의 경과 시간 평균이다. 실제 서버의 HTTP 응답 시간이나 실제 Kakao 지연이 아니며 위 Baseline과 직접 비교할 수 없다.

| Mock 조건 | 방식별 반복 | A 순차 평균 | B 예약 병렬 평균 | 추천 결과 |
| --- | ---: | ---: | ---: | --- |
| 음식점·카페 각 120ms | 12회 | 243.304ms | 123.236ms | 동일 |
| 음식점 700ms, 카페 100ms | 2회 | 801.976ms | 701.016ms | 동일 |
| 음식점·카페 각 700ms | 2회 | 1,404.169ms | 700.962ms | 동일 |
| 지역 Anchor 3개, 각 유형 120ms | 4회 | 726.369ms | 364.368ms | 코스 3개, 순서·점수 동일 |
| 동시 요청 5개, 각 유형 120ms | 5회 | 288.799ms | 217.191ms | 동일, 작업 큐 대기 발생 |

정상 지연 조건에서는 두 호출의 대기가 겹쳐 시간이 줄었다. 이 수치는 통제된 Mock에서의 관측값이며 운영 환경의 개선율을 예측하지 않는다. 2km 빈 결과→5km 성공, HTTP 오류→5km 성공, DB fallback, fallback 부재로 불완전 코스를 제외하는 조건에서도 A/B의 결과 서명이 일치했다. 예산 경계 조건에서는 아래와 같이 일치하지 않았다.

### 누적 시간 예산과 추천 결과의 차이

현재 서비스는 Anchor별 3초와 전체 8초의 **Kakao 누적 대기 시간** 예산을 사용하고, 잔여 예산이 250ms 미만이면 추가 호출을 생략한다. 순차 호출에서 사용하는 `KakaoBudget`의 가변 누적값을 병렬 작업이 그대로 공유하면 차감 경쟁이 생길 수 있다. 실험 B는 이를 피하려고 해당 Anchor의 잔여 예산을 음식점과 카페에 절반씩 선예약하고, 500ms 미만에서는 음식점만 예약했다. 이 방식은 예산 경쟁을 막는 대신 한 유형이 사용하지 않은 시간을 다른 유형에 즉시 넘기지 못한다.

| Mock 예산 경계 | A 순차 결과 | B 예약 병렬 결과 |
| --- | --- | --- |
| 음식점 2,800ms·카페 100ms | 음식점 Kakao 후보, 카페 DB fallback; Kakao 1회 | 음식점 1,500ms timeout·DB fallback, 카페 Kakao 후보; Kakao 2회 |
| 음식점 2,750ms·카페 100ms | 잔여 250ms에서 카페 호출 성공, 두 Kakao 후보 사용 | 음식점 1,500ms timeout·DB fallback, 카페 Kakao 후보 |
| 음식점 100ms·카페 2,800ms | 두 Kakao 후보 사용 | 카페 1,500ms timeout·DB fallback |
| Anchor 3개, 각 유형 1,800ms | 모델 누적 7,800ms·5회 호출; 각 Anchor의 음식점 Kakao, 카페 fallback | 모델 누적 8,000ms·6회 호출·6회 timeout; 각 Anchor의 두 유형 모두 fallback |

이 조건에서 세 유형으로 구성된 코스의 완성 여부와 점수·순서는 유지됐지만 **선택된 음식점 또는 카페의 출처가 달라졌다.** 최종 80행에서 결정적 모델의 Anchor·전체 예산 준수와 순서 유지 위반은 0건이었다. 그러나 모델 차감액 `modelKakaoMs`와 작업 제출·대기·취소를 포함한 `observedOperationMs`는 다르다. 전역 예산 조건의 B는 모델 차감액 8,000ms에 대해 관측 작업 시간 합계가 8,008.029ms였다. 따라서 실험은 실제 경과 시간 기준 예산을 동일한 의미로 보존한다고 입증하지 않는다.

동시 요청 5개 실험에서는 Mock 외부 작업 풀의 활성 스레드를 4개로 제한했으나 최대 대기 큐가 A 3개에서 B 6개로 늘었다. 큐는 실험에서 무제한이었으며, 큐 대기는 선예약 예산에 포함되지 않았다. 종료 시 활성·대기 작업이 남은 실행은 없었고 취소 발생 실행은 7건이었지만, 이는 `Future.cancel(true)`에 협조하는 `Thread.sleep` Mock의 결과다. `KakaoLocalClient`의 실제 `WebClient` 요청 취소, 연결 회수, 원격 Kakao 요청 종료는 검증하지 않았다. 현재 클라이언트는 호출마다 `WebClient.Builder`에 base URL을 설정해 `build()`하고 `block()`으로 응답을 기다리므로, 병렬화 전에는 빌더 공유와 호출 경계를 별도로 확인해야 한다.

### 병렬화 검토 당시 후속 계측 계획

- **DB 조회:** 대표 장소 조회, Anchor 조회, 지연 로딩 및 fallback 조회의 SQL 횟수·시간을 먼저 계측한다. 병렬화 검토 당시 자료에는 DB 실행 시간이 없어 병목이나 중복 비용을 확정할 수 없었다. 계측 후 실제 중복 조회가 있으면 제거 범위를 검토한다는 계획이었다. 후속 1회 계측 결과는 아래 DB 조회 절에 기록했다.
- **WebClient:** 호출마다 `WebClient`를 만드는 비용과 가변 `WebClient.Builder` 공유 위험을 검토하고, 설정이 고정된 클라이언트를 재사용하는 방안을 평가한다. Mock HTTP 서버로 timeout·취소 후 연결 및 자원 회수를 검증하고, 동시 요청 시 작업 수와 대기 큐의 상한도 확인한다는 계획이었다. 병렬화 검토 당시에는 성능 효과를 측정하지 않았으며, 후속 로컬 HTTP 실험 결과는 아래 WebClient 절에 기록했다.
- **병렬화 재검토 조건:** 음식점 우선권, 250ms 호출 생략, Anchor별 3초·전체 8초 누적 예산, fallback 결과를 어떤 방식으로 보존할지 먼저 정의한다. 결과 차이를 허용할 정책 결정과 실제 WebClient 자원 검증 없이는 B안을 적용하지 않는다.

이번 결정은 Mock 실험과 위 Baseline에 근거한다. 운영 코드의 Before/After 측정 결과는 없으며, 이 단계에서 운영 코드·설정·테스트는 변경하지 않았다.

## 후속 의사결정: WebClient 재사용 적용 보류 (2026-11-13)

`KakaoLocalClient`의 keyword·category 검색은 호출할 때마다 주입된 `WebClient.Builder`에 `baseUrl(BASE_URL).build()`를 적용한다. WebClient 객체 생성과 TCP 연결 생성은 별개이므로, `performance/scripts/WebClientReuseExperiment.java`에서 A(매번 생성)와 B(같은 설정의 WebClient 재사용)를 비교했다. Java 17.0.14와 프로젝트의 기존 Spring WebFlux·Reactor Netty 의존성을 사용했고, 실제 Kakao·DB 대신 연결 ID를 기록하는 로컬 HTTP/1.1 서버에 같은 GET·헤더·응답 DTO·timeout으로 요청했다. 준비 단계는 방식별 워밍업 1,000회·측정 5,000회, HTTP는 워밍업 20회·측정 100회였으며 독립 JVM 실행을 3회 반복했다. HTTP 타이머에는 A의 `build()`부터 응답 본문 역직렬화 완료까지 포함했다.

| 독립 실행 | A 로컬 HTTP 평균 | B 로컬 HTTP 평균 | A−B | 본 측정 신규 TCP 연결 |
| --- | ---: | ---: | ---: | --- |
| 1 | 1.887ms | 1.708ms | 0.178ms | 0개 |
| 2 | 2.010ms | 1.838ms | 0.172ms | 0개 |
| 3 | 1.595ms | 1.374ms | 0.221ms | 0개 |

A의 `baseUrl().build()` 단독 평균은 실행별 **0.0022~0.0024ms(약 0.0023ms)**였다. A와 B 모두 본 측정에서 워밍업 때의 TCP 연결을 다시 사용했다. 이는 **로컬 HTTP에서 현재 방식도 연결을 재사용했다**는 관측이며 실제 Kakao HTTPS의 TCP/TLS 상태는 확인하지 않았다. 두 방식의 본 측정 HTTP 오류는 각 실행에서 0/100이었다. B의 HTTP 평균은 로컬에서 0.17~0.22ms 낮았지만, 실행 간 시간 변동이 있고 `build()` 자체의 시간보다 차이가 커서 이를 객체 생성 비용만의 효과로 돌릴 수 없다. 실제 코스 추천 API의 Before/After 측정도 없으므로 **성능 목적의 WebClient 재사용 적용은 보류**한다.

재현은 프로젝트 루트 PowerShell에서 `performance/scripts/webclient-experiment.init.gradle`로 새 결과 파일명을 지정해 실행한다. 최종 실행의 원본은 `performance/results/webclient-experiment-20261113-timed1.csv`, `performance/results/webclient-experiment-20261113-timed2.csv`, `performance/results/webclient-experiment-20261113-timed3.csv`이고 조건·할당량·오류·동시 호출 결과는 `performance/results/webclient-experiment-20261113-analysis.md`에 있다. 앞서 생성된 탐색 CSV는 A의 `build()`가 HTTP 타이머 밖에 있었으므로 위 HTTP 비교에 사용하지 않았다.

```powershell
.\gradlew.bat --offline -q -I performance\scripts\webclient-experiment.init.gradle runWebClientExperiment '-PexperimentOutput=performance/results/webclient-experiment-new.csv' '-PexperimentSummary=performance/results/webclient-experiment-new-summary.md'
```

## 후속 의사결정: DB 조회 최적화 적용 보류 (2026-11-13)

앞선 Baseline 절의 DB `미측정` 표기는 그 실행에 한정된다. 이후 사용자가 Hibernate 임시 계측 옵션을 적용해 IntelliJ 서버를 재실행한 뒤, 실제 Supabase PostgreSQL·Kakao Local API를 사용하는 지역 추천(`regionId=1`)과 Anchor 추천(`regionId=1&anchorPlaceId=1`)을 **각 1회, 워밍업 없이** 순차 실행했다. `performance/scripts/capture-course-db-request.ps1`로 HTTP CSV와 서버 로그를 별도 저장했다. `performance/scripts/analyze-course-db-logs.ps1`는 SQL DEBUG 3행, 추천 완료 로그 1행, Session Metrics 1블록이 같은 HTTP 스레드에 있고 로그 시각이 요청 시간 창에 포함되며 Session의 JDBC 문장 수가 SQL 로그 수와 일치하는 것을 각 요청에서 확인했다. 요청 ID가 없으므로 이 시간·스레드·개수 일치를 연결 근거로 사용한다.

| 이번 1회 요청의 지표 | 지역 추천 | Anchor 추천 |
| --- | ---: | ---: |
| HTTP 전체 시간 | 884.612ms | 304.767ms |
| Kakao 호출·누적 대기 | 6회·580ms | 2회·119ms |
| SQL DEBUG / Session JDBC 문장 | 3건 / 3건 | 3건 / 3건 |
| Session JDBC 문장 실행 누적 | 38.343ms | 48.096ms |
| Session JDBC 연결 획득 | 1회·13.228ms | 1회·19.911ms |
| 추천 결과 | 완성 코스 3개 | 완성 코스 1개 |

실제 SQL은 두 요청 모두 **`regions`의 ID 존재 확인 → `places`의 지역 대표 장소 조회(`regions` 조인) → `regions`의 ID 조회** 순서였다. 마지막 조회는 `Place.region`의 `cityCountyName` 접근에 따른 지연 로딩으로 코드 경로와 로그 순서가 일치한다. Anchor 추천에서는 `PlaceRepository.findById(anchorPlaceId)`를 호출했지만 별도의 `places.id=?` SELECT는 관찰되지 않았다. 앞서 대표 장소 목록에서 로딩한 Anchor를 같은 영속성 컨텍스트에서 재사용한 것으로 해석한다. 지역 추천은 Anchor 3개를 처리해도 Region 조회가 1회여서 **이번 지역·데이터 조건에서는 Region N+1과 중복 Place SELECT가 관찰되지 않았다.**

별도 읽기 전용 `performance/results/course-db-explain-20261113-193325.txt`에서는 대표 장소 조회와 유사한 쿼리가 `idx_places_region_id`를 사용해 19행을 반환했고, `is_representative` 필터로 7행을 제외했으며 PostgreSQL 내부 Execution Time은 **0.109ms**였다. 이는 **별도 DB 세션의 단일 유사 쿼리**이고 위 Hibernate Session의 JDBC 실행 누적 38~48ms와 측정 범위가 다르다. JDBC 누적값은 DB 서버의 순수 실행 시간이나 전체 DB 왕복 시간으로 해석하지 않으며, 연결 획득값도 Hikari 대기만 분리한 수치가 아니다. SQL별 JDBC 시간은 미측정이다. 각 시나리오가 1회뿐이고 워밍업이 없어 이 HTTP 값들을 앞의 20회 Baseline 평균과 합치거나 성능 변화로 해석하지 않는다.

현재 표본에는 중복 SQL·N+1·인덱스 미사용 근거가 없어 **DB 조회 코드와 인덱스 변경을 보류**한다. 원본 `performance/logs/course-db-region-20261113-195022.log`·`performance/logs/course-db-anchor-20261113-195041.log`, 검증된 `performance/results/course-db-region-20261113-195022-sql-verified.csv`·`performance/results/course-db-anchor-20261113-195041-sql-verified.csv`, `performance/results/course-db-hibernate-20261113-analysis.md`을 보존했다. 임시 옵션과 요청별 재현 절차는 `performance/results/course-db-hibernate-runbook-20261113.md`에 있다.

## 최종 판단

| 검토 대상 | 관측 근거 | 결정 |
| --- | --- | --- |
| Kakao 순차 호출 | 기존 실제 API Baseline에서 Kakao 대기가 지역·Anchor HTTP 시간의 요청별 평균 79.58%·56.43% | 주요 지연 후보로 유지. 운영 코드 개선 효과는 미측정 |
| 동일 Anchor 병렬화 | 독립 Mock의 정상 지연에서는 시간 감소, 예산 경계에서는 Kakao 후보·DB fallback 결과 변경 | 추천 정책 보존 조건을 충족하지 못해 B안 보류 |
| WebClient 재사용 | 로컬 HTTP에서 기존 방식도 TCP 재사용, 객체 생성 약 0.0023ms, A/B HTTP 평균 차이 0.17~0.22ms | 실제 Kakao·코스 추천 개선 효과가 검증되지 않아 보류 |
| DB 조회·인덱스 | 실제 API 각 1회에서 SQL 3건, 중복 Place 조회·Region N+1 미관찰; 유사 쿼리의 별도 EXPLAIN은 인덱스 사용 | 병목·중복 근거가 없어 보류 |

**운영 코드의 Before/After 성능 개선 수치는 없다.** 검토한 방안 중 실제 서비스의 효과가 충분히 검증되지 않았거나 기존 추천 장소·fallback 정책을 바꿀 위험이 있는 것은 적용하지 않았다. 추가 변경을 판단하려면 정책 동등성 검증과 같은 조건의 실제 API 재측정이 필요하다.
