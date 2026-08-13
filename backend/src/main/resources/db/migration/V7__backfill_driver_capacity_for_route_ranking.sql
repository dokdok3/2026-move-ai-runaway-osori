-- 기존 CSV 기반 기사 데이터에는 적재량이 비어 있어 PostGIS 필수조건에서 모두 제외된다.
-- 프런트 목 데이터와 같은 결정적 id 순환값으로 누락 값만 보완한다.
UPDATE driver
SET capacity_ton = CASE MOD(id, 5)
    WHEN 0 THEN 1.0
    WHEN 1 THEN 2.5
    WHEN 2 THEN 5.0
    WHEN 3 THEN 11.0
    WHEN 4 THEN 18.0
END
WHERE capacity_ton IS NULL;

ALTER TABLE driver
    ADD CONSTRAINT driver_capacity_ton_check CHECK (capacity_ton IS NULL OR capacity_ton > 0);
