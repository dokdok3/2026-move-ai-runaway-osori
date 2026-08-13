CREATE TABLE shipper (
    id                  BIGINT PRIMARY KEY,
    company_name        VARCHAR(100),
    contact_name        VARCHAR(50),
    phone_number        VARCHAR(20),
    business_number     VARCHAR(20),
    address             VARCHAR(200),
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

-- 기존 화물의 shipper_id를 그대로 프로필 PK로 백필한다. 기존 스키마에는 화주 상세 정보가 없으므로 값은 비워 둔다.
INSERT INTO shipper (id)
SELECT DISTINCT shipper_id
FROM cargo
ON CONFLICT (id) DO NOTHING;
