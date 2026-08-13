-- sigungu가 NULL인 선호 지역은 해당 시·도 전체를 뜻한다.
-- 기존 데이터도 PostGIS 후보 검색에서 제외되지 않도록 첫 선호 지역의 대표 좌표를 다시 계산한다.
WITH first_preference AS (
    SELECT d.id,
           (
               SELECT p.sido
               FROM driver_route_preference p
               WHERE p.driver_id = d.id AND p.direction = 'ORIGIN'
               ORDER BY p.id
               LIMIT 1
           ) AS origin_sido,
           (
               SELECT p.sigungu
               FROM driver_route_preference p
               WHERE p.driver_id = d.id AND p.direction = 'ORIGIN'
               ORDER BY p.id
               LIMIT 1
           ) AS origin_sigungu,
           (
               SELECT p.sido
               FROM driver_route_preference p
               WHERE p.driver_id = d.id AND p.direction = 'DESTINATION'
               ORDER BY p.id
               LIMIT 1
           ) AS destination_sido,
           (
               SELECT p.sigungu
               FROM driver_route_preference p
               WHERE p.driver_id = d.id AND p.direction = 'DESTINATION'
               ORDER BY p.id
               LIMIT 1
           ) AS destination_sigungu
    FROM driver d
), representative_location AS (
    SELECT p.id,
           COALESCE(
               (
                   SELECT rc.location
                   FROM region_coordinate rc
                   WHERE rc.sido = p.origin_sido AND rc.sigungu = p.origin_sigungu
               ),
               (
                   SELECT ST_Centroid(ST_Collect(rc.location::geometry))::geography
                   FROM region_coordinate rc
                   WHERE rc.sido = p.origin_sido
               )
           ) AS current_location,
           COALESCE(
               (
                   SELECT rc.location
                   FROM region_coordinate rc
                   WHERE rc.sido = p.destination_sido AND rc.sigungu = p.destination_sigungu
               ),
               (
                   SELECT ST_Centroid(ST_Collect(rc.location::geometry))::geography
                   FROM region_coordinate rc
                   WHERE rc.sido = p.destination_sido
               )
           ) AS preferred_destination
    FROM first_preference p
)
UPDATE driver d
SET current_location = r.current_location,
    preferred_destination = r.preferred_destination
FROM representative_location r
WHERE d.id = r.id;
