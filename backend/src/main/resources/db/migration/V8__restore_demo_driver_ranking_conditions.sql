-- 공개 데모의 1번 기사는 DriverPage와 DataInitializer에서 정의한 대형 차량 조건을 사용한다.
-- V7의 일괄 누락값 보정으로 2.5t/42만원이 들어가 후보가 1건으로 축소된 값을 복구한다.
UPDATE driver
SET capacity_ton = 25.0,
    min_accept_fare = 150000
WHERE id = 1;
