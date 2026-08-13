CREATE TABLE region_coordinate (
    sido      VARCHAR(30) NOT NULL,
    sigungu   VARCHAR(30) NOT NULL,
    location  geography(Point, 4326) NOT NULL,
    PRIMARY KEY (sido, sigungu)
);

INSERT INTO region_coordinate (sido, sigungu, location) VALUES
    ('강원특별자치도', '원주시', ST_SetSRID(ST_MakePoint(127.8997649, 37.3250589), 4326)::geography),
    ('강원특별자치도', '춘천시', ST_SetSRID(ST_MakePoint(127.7870808, 37.8799993), 4326)::geography),
    ('경기도', '고양시', ST_SetSRID(ST_MakePoint(126.8448357, 37.6600020), 4326)::geography),
    ('경기도', '남양주시', ST_SetSRID(ST_MakePoint(127.2580036, 37.6429699), 4326)::geography),
    ('경기도', '성남시', ST_SetSRID(ST_MakePoint(127.1082564, 37.4040263), 4326)::geography),
    ('경기도', '수원시', ST_SetSRID(ST_MakePoint(127.0101491, 37.2877574), 4326)::geography),
    ('경기도', '용인시', ST_SetSRID(ST_MakePoint(127.2040548, 37.2282934), 4326)::geography),
    ('경기도', '평택시', ST_SetSRID(ST_MakePoint(127.0037134, 37.0230320), 4326)::geography),
    ('경기도', '화성시', ST_SetSRID(ST_MakePoint(126.8912592, 37.1540860), 4326)::geography),
    ('경상남도', '김해시', ST_SetSRID(ST_MakePoint(128.8613894, 35.2730789), 4326)::geography),
    ('경상남도', '진주시', ST_SetSRID(ST_MakePoint(128.1279759, 35.2009655), 4326)::geography),
    ('경상남도', '창원시', ST_SetSRID(ST_MakePoint(128.5983607, 35.2233694), 4326)::geography),
    ('경상북도', '구미시', ST_SetSRID(ST_MakePoint(128.3641669, 36.2112924), 4326)::geography),
    ('경상북도', '포항시', ST_SetSRID(ST_MakePoint(129.2592159, 36.0880090), 4326)::geography),
    ('광주광역시', '광산구', ST_SetSRID(ST_MakePoint(126.7392898, 35.1643067), 4326)::geography),
    ('광주광역시', '북구', ST_SetSRID(ST_MakePoint(126.9097147, 35.1895670), 4326)::geography),
    ('대구광역시', '달서구', ST_SetSRID(ST_MakePoint(128.5270504, 35.8213560), 4326)::geography),
    ('대구광역시', '동구', ST_SetSRID(ST_MakePoint(128.6820704, 35.9341351), 4326)::geography),
    ('대구광역시', '북구', ST_SetSRID(ST_MakePoint(128.5805759, 35.9309960), 4326)::geography),
    ('대전광역시', '동구', ST_SetSRID(ST_MakePoint(127.4679233, 36.3172908), 4326)::geography),
    ('대전광역시', '서구', ST_SetSRID(ST_MakePoint(127.3405265, 36.2775521), 4326)::geography),
    ('대전광역시', '유성구', ST_SetSRID(ST_MakePoint(127.3453350, 36.3822463), 4326)::geography),
    ('부산광역시', '강서구', ST_SetSRID(ST_MakePoint(128.9178976, 35.1578311), 4326)::geography),
    ('부산광역시', '기장군', ST_SetSRID(ST_MakePoint(129.1931583, 35.2852320), 4326)::geography),
    ('부산광역시', '사상구', ST_SetSRID(ST_MakePoint(128.9862960, 35.1596565), 4326)::geography),
    ('부산광역시', '해운대구', ST_SetSRID(ST_MakePoint(129.1443890, 35.2011962), 4326)::geography),
    ('서울특별시', '강남구', ST_SetSRID(ST_MakePoint(127.0624661, 37.4960058), 4326)::geography),
    ('서울특별시', '강동구', ST_SetSRID(ST_MakePoint(127.1472445, 37.5489056), 4326)::geography),
    ('서울특별시', '강서구', ST_SetSRID(ST_MakePoint(126.8184992, 37.5657558), 4326)::geography),
    ('서울특별시', '마포구', ST_SetSRID(ST_MakePoint(126.8983078, 37.5622169), 4326)::geography),
    ('서울특별시', '서초구', ST_SetSRID(ST_MakePoint(127.0136546, 37.4768786), 4326)::geography),
    ('서울특별시', '송파구', ST_SetSRID(ST_MakePoint(127.1058583, 37.5048645), 4326)::geography),
    ('세종특별자치시', '세종시', ST_SetSRID(ST_MakePoint(127.2697257, 36.5702933), 4326)::geography),
    ('울산광역시', '남구', ST_SetSRID(ST_MakePoint(129.3287666, 35.5038361), 4326)::geography),
    ('울산광역시', '동구', ST_SetSRID(ST_MakePoint(129.4243625, 35.5215351), 4326)::geography),
    ('인천광역시', '부평구', ST_SetSRID(ST_MakePoint(126.7167452, 37.4941478), 4326)::geography),
    ('인천광역시', '서구', ST_SetSRID(ST_MakePoint(126.6750362, 37.5454627), 4326)::geography),
    ('인천광역시', '연수구', ST_SetSRID(ST_MakePoint(126.6424876, 37.3883088), 4326)::geography),
    ('인천광역시', '중구', ST_SetSRID(ST_MakePoint(126.6226039, 37.4738464), 4326)::geography),
    ('전라남도', '광양시', ST_SetSRID(ST_MakePoint(127.6645020, 35.0330742), 4326)::geography),
    ('전라남도', '순천시', ST_SetSRID(ST_MakePoint(127.3708901, 35.0105175), 4326)::geography),
    ('전라남도', '여수시', ST_SetSRID(ST_MakePoint(127.6710019, 34.7622503), 4326)::geography),
    ('전라북도', '익산시', ST_SetSRID(ST_MakePoint(127.0048130, 36.0201307), 4326)::geography),
    ('전라북도', '전주시', ST_SetSRID(ST_MakePoint(127.1494635, 35.8159657), 4326)::geography),
    ('제주특별자치도', '서귀포시', ST_SetSRID(ST_MakePoint(126.5957228, 33.3390580), 4326)::geography),
    ('제주특별자치도', '제주시', ST_SetSRID(ST_MakePoint(126.4844745, 33.4196028), 4326)::geography),
    ('충청남도', '당진시', ST_SetSRID(ST_MakePoint(126.6463735, 36.9072472), 4326)::geography),
    ('충청남도', '아산시', ST_SetSRID(ST_MakePoint(126.9767762, 36.7951925), 4326)::geography),
    ('충청남도', '천안시', ST_SetSRID(ST_MakePoint(127.2560508, 36.7967147), 4326)::geography),
    ('충청북도', '청주시', ST_SetSRID(ST_MakePoint(127.4677012, 36.5942094), 4326)::geography);

ALTER TABLE driver
    ADD COLUMN current_location geography(Point, 4326),
    ADD COLUMN preferred_destination geography(Point, 4326),
    ADD COLUMN available_from TIMESTAMP NOT NULL DEFAULT '2026-08-01 00:00:00',
    ADD COLUMN available_until TIMESTAMP NOT NULL DEFAULT '2026-12-31 23:59:59',
    ADD COLUMN pickup_radius_m INTEGER NOT NULL DEFAULT 50000,
    ADD COLUMN destination_radius_m INTEGER NOT NULL DEFAULT 100000,
    ADD COLUMN preference_text TEXT;

ALTER TABLE cargo
    ADD COLUMN origin_location geography(Point, 4326),
    ADD COLUMN destination_location geography(Point, 4326);

UPDATE cargo c
SET origin_location = origin.location,
    destination_location = destination.location
FROM region_coordinate origin, region_coordinate destination
WHERE origin.sido = c.origin_sido AND origin.sigungu = c.origin_sigungu
  AND destination.sido = c.dest_sido AND destination.sigungu = c.dest_sigungu;

UPDATE driver d
SET current_location = origin.location,
    preferred_destination = destination.location
FROM driver_route_preference origin_pref
JOIN region_coordinate origin
  ON origin.sido = origin_pref.sido AND origin.sigungu = origin_pref.sigungu
JOIN driver_route_preference destination_pref
  ON destination_pref.driver_id = origin_pref.driver_id AND destination_pref.direction = 'DESTINATION'
JOIN region_coordinate destination
  ON destination.sido = destination_pref.sido AND destination.sigungu = destination_pref.sigungu
WHERE origin_pref.driver_id = d.id AND origin_pref.direction = 'ORIGIN';

ALTER TABLE driver
    ADD CONSTRAINT driver_pickup_radius_m_check CHECK (pickup_radius_m BETWEEN 1000 AND 200000),
    ADD CONSTRAINT driver_destination_radius_m_check CHECK (destination_radius_m BETWEEN 1000 AND 300000),
    ADD CONSTRAINT driver_available_window_check CHECK (available_from <= available_until);

CREATE TABLE driver_hidden_cargo (
    driver_id BIGINT NOT NULL REFERENCES driver(id),
    cargo_id BIGINT NOT NULL REFERENCES cargo(id),
    hidden_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (driver_id, cargo_id)
);

CREATE INDEX cargo_origin_location_gix ON cargo USING GIST (origin_location);
CREATE INDEX cargo_destination_location_gix ON cargo USING GIST (destination_location);
CREATE INDEX cargo_candidate_idx ON cargo (status, cargo_type, loading_at, desired_fare);
