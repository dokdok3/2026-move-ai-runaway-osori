-- 경로 선호를 변경한 뒤에도 V6에서 만든 대표 좌표가 이전 구간을 가리킬 수 있다.
-- 각 방향의 첫 번째 시군구 선호를 PostGIS 후보 검색의 대표 좌표로 다시 맞춘다.
WITH representative_location AS (
    SELECT d.id,
           (
               SELECT rc.location
               FROM driver_route_preference p
               JOIN region_coordinate rc ON rc.sido = p.sido AND rc.sigungu = p.sigungu
               WHERE p.driver_id = d.id
                 AND p.direction = 'ORIGIN'
               ORDER BY p.id
               LIMIT 1
           ) AS current_location,
           (
               SELECT rc.location
               FROM driver_route_preference p
               JOIN region_coordinate rc ON rc.sido = p.sido AND rc.sigungu = p.sigungu
               WHERE p.driver_id = d.id
                 AND p.direction = 'DESTINATION'
               ORDER BY p.id
               LIMIT 1
           ) AS preferred_destination
    FROM driver d
)
UPDATE driver d
SET current_location = r.current_location,
    preferred_destination = r.preferred_destination
FROM representative_location r
WHERE d.id = r.id;
