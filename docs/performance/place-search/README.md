# 장소 검색 API 성능 Baseline과 최적화 결정

## 범위와 상태

- 대상: `GET /api/places/search?regionId={regionId}&keyword={keyword}`
- 측정일: **2026-11-14 (KST)**. `regionId=1`에 대해 서로 다른 검색어 세 개를 측정했다.
- 상태: **성능 개선 전 Baseline**. 이 문서에는 개선 후 응답 시간이나 확정된 개선 효과를 기록하지 않는다.
- 장소 검색 구간을 측정하려고 임시로 추가했던 `PLACE_SEARCH_TIMING` 코드는 분석 후 제거했다. 아래 구간별 수치는 **계측 코드가 있었던 당시** 수집한 값이다.

## 구현에서 확인한 검색 흐름

[PlaceController](../../../src/main/java/com/chaerok/backend/place/controller/PlaceController.java)의 검색 API는 `regionId`와 비어 있지 않은 `keyword`를 받고 `List<PlaceSearchResponse>`를 반환한다. [PlaceSearchService](../../../src/main/java/com/chaerok/backend/place/service/PlaceSearchService.java)는 [RegionRepository](../../../src/main/java/com/chaerok/backend/region/repository/RegionRepository.java)의 `findById()`로 지역을 조회한 뒤 [TourApiPlaceClient](../../../src/main/java/com/chaerok/backend/place/external/TourApiPlaceClient.java)의 `searchKeyword2`를 먼저 호출한다. TourAPI는 해당 지역·검색어의 **첫 페이지 20건**을 요청하며 클라이언트의 `Mono.timeout()` 상한은 5초다.

TourAPI 결과를 지원 관광 분류로 필터링한 뒤 **5건 이상이면 Kakao를 호출하지 않는다**. 5건 미만이면 [RegionCenterProvider](../../../src/main/java/com/chaerok/backend/place/service/RegionCenterProvider.java)의 중심 좌표로 [KakaoLocalClient](../../../src/main/java/com/chaerok/backend/place/external/KakaoLocalClient.java)의 키워드 검색을 호출한다. Kakao 호출도 5초 timeout을 사용한다. 응답 주소에 해당 시·군명이 있는 Kakao 후보만 남기고, 정규화한 제목·주소로 중복을 제거하면서 TourAPI 결과를 먼저 유지한다. 검색 결과는 DB에 저장하지 않는다. 이 요청 경로에서 지역 조회, TourAPI 검색, 조건부 Kakao 검색은 각각 최대 한 번이다.

## 측정 환경과 방법

| 항목 | 내용 |
| --- | --- |
| 서버 | Windows IntelliJ에서 실행한 로컬 Spring Boot, `http://localhost:8080`; 측정 전 `/actuator/health`는 `UP` |
| 프로젝트 설정 | [build.gradle](../../../build.gradle)의 Java 17 툴체인, Spring Boot 3.5.14. 측정 서버 JVM의 실제 버전은 이 자료에서 독립 확인하지 않았다. |
| 데이터·외부 서비스 | 실제 Supabase PostgreSQL, TourAPI, Kakao Local API를 사용하는 로컬 환경. DB 접속 대상과 외부 서비스 내부 처리 시간은 별도 검증하지 않았다. |
| 부하 | 시나리오마다 단일 클라이언트의 순차 요청, 워밍업 5회와 본 측정 20회. 동시 부하 시험이 아니다. |
| HTTP 시간 | 로컬 `measure-place-search.ps1`의 `Stopwatch`로 요청 시작부터 응답 본문 수신 완료까지 측정. |
| 서비스 시간 | 당시 `PLACE_SEARCH_TIMING` 로그의 `System.nanoTime()` 구간 측정. 외부 클라이언트 호출 시간에는 연결·응답 수신·역직렬화·오류 처리가 포함될 수 있다. |
| 통계 | 워밍업 제외. p50·p95는 오름차순 nearest-rank의 `ceil(p × N) - 1` 인덱스. |

| 시나리오 | 검색어 | 본 측정 결과 | Kakao 클라이언트 호출 |
| --- | --- | --- | --- |
| TOUR_ONLY | 공주 | 평균 17건, 모두 TourAPI | 0/20 |
| KAKAO_SUPPLEMENT | 공주역 | 평균 5건: TourAPI 1건, Kakao 4건 | 20/20 |
| SPARSE | 석장리박물관 | 평균 3건: TourAPI 1건, Kakao 2건 | 20/20 |

## Baseline 결과

시간 단위는 ms이다. Kakao 평균은 **실제 호출된 요청만** 포함한다. `필터링·병합`은 TourAPI 필터, Kakao 필터, 병합의 **각각 소수 셋째 자리로 반올림한 평균을 합산**한 값이다.

| 지표 | TOUR_ONLY | KAKAO_SUPPLEMENT | SPARSE |
| --- | ---: | ---: | ---: |
| HTTP 평균 | 175.790 | 198.011 | 201.426 |
| HTTP p50 | 169.741 | 195.175 | 203.031 |
| HTTP p95 | 198.511 | 213.594 | 216.802 |
| HTTP 최소 / 최대 | 153.663 / 219.384 | 175.760 / 220.417 | 181.297 / 228.052 |
| 서비스 평균 | 164.266 | 188.329 | 193.575 |
| TourAPI 클라이언트 평균 | 137.546 | 99.132 | 98.830 |
| Kakao 클라이언트 평균 | 미호출 | 60.364 | 67.993 |
| 지역 조회 평균 | 26.458 | 28.259 | 26.310 |
| 필터링·병합 평균의 합 | 0.207 | 0.500 | 0.376 |
| 요청별 `(TourAPI + Kakao) / HTTP` 비율의 평균 | 78.26% | 80.55% | 82.88% |
| HTTP 200·JSON 파싱 성공 | 20/20 | 20/20 | 20/20 |
| 빈 결과 / 응답 내 중복 / 지역 외 Kakao 결과 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |

비율은 각 요청에서 클라이언트 호출 구간 합계를 해당 HTTP 경과 시간으로 나눈 뒤 평균했다. HTTP 시간과 서비스 시간의 시작·종료 지점은 다르다. `tourApiOutcome`과 호출된 경우의 `kakaoOutcome`은 당시 요약 로그에서 `unknown`이었다. 클라이언트가 오류를 내부에서 빈 목록으로 처리할 수 있으므로 **HTTP 200과 결과 수만으로 외부 API 호출 성공을 확정하지 않는다**. 로그의 호출 여부는 클라이언트 메서드 호출 여부이며 실제 내부 HTTP 요청 수는 별도 계측하지 않았다. 수집한 구간에 WARN·ERROR·timeout 로그는 관찰되지 않았다.

### HTTP 요청과 로그의 조건부 매칭

원본 서버 로그의 시작·종료 바이트 오프셋으로 시나리오별 구간을 보존했다. 각 구간에는 HTTP CSV 25행(워밍업 5, 본 측정 20)과 `PLACE_SEARCH_TIMING` 로그 25건이 있었다. 요청 순서, 지역 ID, 검색어 길이, HTTP 시간 창, 결과 수, 성공 여부를 검사한 요청별 CSV는 **총 75/75행 `matched`**였고 로그의 requestId도 시나리오별로 중복이 없었다. 로그에는 서버 작업 스레드 표기가 있지만 HTTP CSV에는 스레드나 requestId가 없어 **스레드 또는 requestId로 HTTP 행과 직접 연결한 것은 아니다**. 다른 클라이언트의 요청이 전혀 없었다는 독립 증명도 없으므로, 이 매칭은 단일 클라이언트·로그 순서·시간 창·개수 일치라는 조건 아래에서만 유효하다.

## 병목 판단

세 시나리오에서 외부 클라이언트 호출 구간은 HTTP 전체의 요청별 평균 **78.26~82.88%**였다. TOUR_ONLY의 HTTP p95 요청 198.511ms에서는 TourAPI 클라이언트 구간이 161.717ms였고, KAKAO_SUPPLEMENT의 HTTP p95 요청 213.594ms에서는 TourAPI 114.557ms와 Kakao 60.962ms였다. **순차적인 외부 클라이언트 호출 구간이 이 표본의 주된 지연 후보**다. 이는 순수 네트워크 시간이나 외부 서버 내부 처리 시간의 측정값은 아니다.

지역 조회의 26.310~28.259ms는 `findById()`를 포함한 서비스 구간이지 SQL 실행 시간은 아니다. 필터링·병합 평균 합계는 0.207~0.500ms로 작았다. 세 시나리오는 **서로 다른 검색어와 호출 구성**을 사용하므로 HTTP 평균 차이를 최적화 효과로 해석하지 않는다. 외부 서비스 응답의 장기 변동성과 동시 부하도 이 측정으로 판단할 수 없다.

## 후속 DB 조회 검증: 최적화 보류

Baseline과 별도로, 2026-11-14의 Hibernate SQL DEBUG·Session Metrics 임시 실행에서 `regionId=1&keyword=공주`를 **1회** 요청했다. HTTP 200과 결과 17건을 확인했고, 요청의 시간 창·서버 스레드·로그 개수를 대조해 SQL 로그 1건, Session Metrics 1건, 당시 장소 검색 요약 로그 1건을 같은 요청에 조건부 연결했다. 실제 SQL은 `regions`에서 `id=?`로 지역 기본 필드를 조회하는 SELECT 1건으로 `RegionRepository.findById()`에 대응했다.

| 항목 | 해당 1회 요청 |
| --- | ---: |
| Hibernate JDBC 문장 실행 | 1건·13.5555ms 누적 |
| Hibernate JDBC 연결 획득 | 1회·14.7547ms 누적 |
| 서비스 지역 조회 구간 | 208.3062ms |
| 추가 SELECT·지연 로딩·N+1 | 관찰되지 않음 |

`regions.id`는 [지역 테이블 마이그레이션](../../../src/main/resources/db/migration/V3__create_regions_table.sql)의 기본 키이며, 기존 별도 실행 계획 자료에서 `regions_pkey` 인덱스가 확인됐다. 그 실행 계획은 이번 장소 검색 요청의 계획은 아니다. JDBC 문장 실행 누적은 PostgreSQL 서버의 순수 실행 시간이 아니며, 연결 획득 시간에서 Hikari 풀 대기만 분리할 수 없다. SQL별 실행 시간도 미측정이다. 이 **재시작 후 단일 요청**의 지역 조회 208.3062ms를 20회 Baseline 평균과 직접 비교하지 않는다. 중복 SQL이나 N+1이 관찰되지 않아 인덱스·캐싱·Repository 변경은 보류하고 구조 검증을 종료했다.

## TourAPI 정상 빈 응답 오류 수정

성능 탐색 중 `제민천 카페`의 TourAPI `searchKeyword2` 직접 조회 1회에서 HTTP 200, `resultCode="0000"`, `totalCount=0`, `response.body.items=""`를 확인했다. 당시 `Items` 객체만 받던 DTO는 이 **정상 빈 문자열**을 역직렬화하지 못해 서버 로그에 `DecodingException`과 원인 `InvalidFormatException`이 남았다. [TourApiPlaceClient](../../../src/main/java/com/chaerok/backend/place/external/TourApiPlaceClient.java)는 예외를 빈 목록으로 처리하므로 검색 API가 Kakao 보완으로 진행할 수 있었지만, 정상 0건 응답이 오류로 기록되는 문제였다.

[TourApiPlaceResponse](../../../src/main/java/com/chaerok/backend/place/external/TourApiPlaceResponse.java)의 `items` 역직렬화는 **객체와 빈 문자열만** 정상 처리하도록 수정됐다. 빈 문자열은 장소 0건이 되고, 예상하지 못한 문자열·배열 등은 예외로 남는다. [DTO 테스트](../../../src/test/java/com/chaerok/backend/place/external/TourApiPlaceResponseTest.java)는 정상 객체·빈 문자열·잘못된 타입을 검증하고, [장소 검색 서비스 테스트](../../../src/test/java/com/chaerok/backend/place/service/PlaceSearchServiceTest.java)는 0건의 Kakao 보완 및 5건 이상에서 Kakao 미호출을 검증한다. 계측 제거 후에도 관련 테스트와 전체 `gradlew.bat test`가 통과했다. **이 버그 수정은 성능 최적화가 아니며, 수정 전후 HTTP 개선 수치는 측정하지 않았다.**

## 최적화 검토와 최종 결정

| 후보 | 실제 근거와 위험 | 결정 |
| --- | --- | --- |
| TourAPI 호출 구조·연결 단계 | 첫 페이지 20건을 한 번 호출하며 현재 요청 내 중복 호출은 확인되지 않았다. 호출 구간이 크지만 연결·외부 서버 시간을 분리하지 못했다. | 호출 단계의 추가 계측 전 변경 보류 |
| Kakao 조건부 보완 | TourAPI 필터 결과 5건 이상이면 미호출이며, TOUR_ONLY에서 0/20회였다. | 현 정책 유지 |
| TourAPI·Kakao 선제 병렬 호출 | Kakao 필요 여부는 TourAPI 필터 후에 결정된다. 선제 호출은 불필요한 Kakao 트래픽과 결과 정책 변경을 낳는다. | 적용하지 않음 |
| Kakao WebClient 재사용 | 같은 클라이언트를 대상으로 한 **별도 코스 추천 로컬 HTTP 실험**에서 객체 생성 평균 약 0.0023ms와 기존 방식의 TCP 재사용을 확인했다. 장소 검색 실서비스 효과는 검증하지 않았다. | 성능 개선으로 적용하지 않음 |
| 지역 DB 조회·캐싱 | 단일 API 요청의 SQL 1건, 중복·N+1 미관찰. 지역 조회 전체 시간은 SQL 서버 시간과 다르다. | 인덱스·캐싱·조회 코드 변경 보류 |
| 필터링·중복 제거 | 본 측정의 필터링·병합 평균 합계 0.207~0.500ms. | 최적화 불필요 |

분석 당시에는 `PLACE_SEARCH_TIMING` 구간 측정과 요청 요약 INFO 로그를 임시로 운영 코드에 추가했다. 분석을 마친 뒤 타이머·상태 변수·요약 로그를 제거하고 기존 검색 흐름을 복원했다. 로그 형식에 종속된 테스트 단언만 제거하고 **빈 결과, 호출 순서, 지역 필터, 중복 제거** 등의 기능 검증은 유지했다. 장소 검색 서비스 테스트, TourAPI DTO 테스트와 전체 Gradle 테스트가 통과했다. 현재 코드에서는 요청별 구간 로그가 생성되지 않으므로 과거 로그 분석 스크립트는 그대로 재실행할 수 없다. **이번 작업에서 운영 응답 시간이 개선됐다는 주장은 하지 않는다.**

## 로컬 근거 파일과 재현 범위

아래 경로는 프로젝트 루트 기준 **로컬 보관 경로**다. `performance/`는 Git 추적에서 제외되어 GitHub 공개 README에서는 열 수 없으므로 클릭 링크로 만들지 않았다. CSV와 로그에는 실제 서비스 데이터가 포함될 수 있어 공개 근거로 사용하려면 별도 선별이 필요하다.

| 시나리오 | HTTP 원본 | 서버 로그 구간 | 요청별 매칭 |
| --- | --- | --- | --- |
| TOUR_ONLY | `performance/results/place-search-baseline-tour_only-20261114-234558-492.csv` | `performance/logs/place-search-baseline-tour_only-20261114-234558-492.log` | `performance/results/place-search-baseline-tour_only-20261114-234558-492-matched.csv` |
| KAKAO_SUPPLEMENT | `performance/results/place-search-baseline-kakao_supplement-20261114-234613-376.csv` | `performance/logs/place-search-baseline-kakao_supplement-20261114-234613-376.log` | `performance/results/place-search-baseline-kakao_supplement-20261114-234613-376-matched.csv` |
| SPARSE | `performance/results/place-search-baseline-sparse-20261114-234625-940.csv` | `performance/logs/place-search-baseline-sparse-20261114-234625-940.log` | `performance/results/place-search-baseline-sparse-20261114-234625-940-matched.csv` |

- Baseline 집계: `performance/results/place-search-baseline-summary-20261114-234558.md`
- DB 1회 계측: `performance/results/place-db-final-20261114-235920-156.csv`, `performance/logs/place-db-final-20261114-235920-156.log`, `performance/results/place-db-final-20261114-235920-156-analysis.md`
- TourAPI 빈 응답 원형 확인: `performance/results/place-search-tourapi-empty-shape-20261114.md`
- 당시 측정 스크립트: `performance/scripts/measure-place-search.ps1`, `performance/scripts/capture-place-search-baseline.ps1`, `performance/scripts/analyze-place-search-timing.ps1`

위 Baseline 구간별 재현에는 **당시의 계측 코드와 로그 수집 설정이 필요**하다. 현재 구현에서 HTTP 응답 시간만 재측정할 수는 있지만, 저장된 Baseline의 서비스 구간과 바로 비교해서는 안 된다. 이 문서에는 API 키, 인증 토큰, DB 접속 문자열을 기록하지 않았다.
