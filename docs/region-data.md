# 시도·시군구 좌표 데이터

## 기준

`region_coordinate`는 화물 출발·도착 좌표와 기사 활동 지역의 거리 계산에 사용하는 정적 기준
데이터다. 현재 데이터의 기준일은 **2026년 7월 1일**이며, 애플리케이션 선택 단위로 16개 시도
그룹과 230개 시군구를 관리한다.

- 광주광역시와 전라남도는 `전남광주통합특별시`로 통합한다.
- 전라북도는 현행 명칭인 `전북특별자치도`를 사용한다.
- 인천광역시는 `중구·동구·서구` 대신 `제물포구·영종구·서해구·검단구`를 사용한다.
- 수원시·창원시처럼 일반구를 둔 도시는 일반구가 아니라 시 단위로 합친다.
- 세종특별자치시는 애플리케이션의 시군구 선택값으로 `세종시`를 사용한다.

행정구역 변경은 다음 자료를 기준으로 확인했다.

- [행정안전부 2026년 7월 1일 행정구역 변경 내역](https://www.mois.go.kr/frt/bbs/type001/commonSelectBoardArticle.do?bbsId=BBSMSTR_000000000052&nttId=127039)
- [인천광역시 행정체제 개편 개요](https://www.incheon.go.kr/IC01070101)
- [2026년 7월 1일 행정동 경계 데이터](https://github.com/vuski/admdongkor/tree/master/ver20260701)

## 좌표 산출

새 지역의 기본 좌표는 `HangJeongDong_ver20260701.geojson`의 행정동 경계를 시군구 단위로 합친
뒤 산출했다.

1. WGS84 경계를 EPSG:5179로 투영한다.
2. 일반구를 상위 시로 합쳐 면적 중심점을 계산한다.
3. 중심점을 EPSG:4326 경도·위도 순서로 되돌린다.
4. 중심점이 행정구역 밖으로 나가는 다중 영역은 내부 대표점으로 보정한다.

안산시·남해군·사천시·옹진군·신안군·완도군은 OpenStreetMap Nominatim의 행정구역 대표점을
사용했고, 여수시는 경계 내부 대표점을 사용했다. Flyway는 `ON CONFLICT DO NOTHING`으로 기존
53개 지역 좌표를 보존하고 없는 지역에만 산출 좌표를 넣는다.

좌표 컬럼은 PostGIS `geography(Point, 4326)`이며 `ST_MakePoint(경도, 위도)` 순서를 지켜야 한다.

## Flyway 반영

[`V14__synchronize_current_regions.sql`](../backend/src/main/resources/db/migration/V14__synchronize_current_regions.sql)은
다음 작업을 한 트랜잭션에서 수행한다.

- 화물과 기사 선호 지역의 폐지 시도·시군구 명칭을 현행 명칭으로 변경
- 기존 좌표를 현행 키로 옮긴 뒤 누락 지역 좌표 삽입
- 좌표가 비어 있던 기존 화물의 출발·도착 좌표 역보정
- 최종 16개 시도 그룹·230개 시군구, SRID 4326, 국내 좌표 범위 검증

이미 배포된 마이그레이션의 체크섬을 바꾸지 않기 위해 `B13`과 `V13`은 수정하지 않는다. 신규
DB는 `B13` 이후 `V14`가 적용되고, 기존 DB는 현재 버전 다음에 `V14`가 적용되어 동일한 상태가
된다. 운영 DB에 데이터가 먼저 수동 반영된 경우에도 기존 행을 덮어쓰지 않고 검증만 통과한다.

## 운영 반영 이력

2026년 8월 13일 운영 DB를 기준 자료와 대조해 다음과 같이 정리했다.

- `region_coordinate`: 53개에서 230개로 확장(177개 신규 추가)
- 운영 지역 API: 16개 시도 그룹과 230개 시군구 반환 확인
- 폐지 명칭을 사용하던 화물·기사 선호 데이터 현행화
- 지역과 조인되지 않던 비정상 화물 19건과 연결된 배차 1건 삭제
- 삭제 후 잘못된 지역 화물, 고아 배차, 고아 숨김 데이터가 모두 0건임을 확인

운영 확인 쿼리는 다음과 같다.

```sql
SELECT count(*) AS region_count,
       count(DISTINCT sido) AS sido_count
FROM region_coordinate;

SELECT sido, count(*) AS sigungu_count
FROM region_coordinate
GROUP BY sido
ORDER BY sido;

SELECT count(*) AS invalid_coordinate_count
FROM region_coordinate
WHERE location IS NULL
   OR ST_SRID(location::geometry) <> 4326
   OR ST_X(location::geometry) NOT BETWEEN 124 AND 132
   OR ST_Y(location::geometry) NOT BETWEEN 33 AND 39;
```
