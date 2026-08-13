INSERT INTO region_coordinate (sido, sigungu, location) VALUES
    ('서울특별시', '성동구', ST_SetSRID(ST_MakePoint(127.0365, 37.5635), 4326)::geography),
    ('서울특별시', '구로구', ST_SetSRID(ST_MakePoint(126.8877, 37.4952), 4326)::geography),
    ('부산광역시', '수영구', ST_SetSRID(ST_MakePoint(129.1127, 35.1454), 4326)::geography)
ON CONFLICT (sido, sigungu) DO NOTHING;

UPDATE cargo c
SET origin_location = rc.location
FROM region_coordinate rc
WHERE c.origin_location IS NULL
  AND c.origin_sido = rc.sido
  AND c.origin_sigungu = rc.sigungu;

UPDATE cargo c
SET destination_location = rc.location
FROM region_coordinate rc
WHERE c.destination_location IS NULL
  AND c.dest_sido = rc.sido
  AND c.dest_sigungu = rc.sigungu;
