-- 2026-07-01 현행 행정구역(16개 시도 그룹, 230개 시군구)으로 지역 기준을 동기화한다.
-- 기존 지역 좌표는 보존하고, 없는 지역에만 최신 행정경계 기반 대표 좌표를 넣는다.

-- 운영 참조 데이터에 남은 폐지 시도 명칭을 현행 명칭으로 바꾼다.
UPDATE cargo
SET origin_sido = '전남광주통합특별시'
WHERE origin_sido IN ('광주광역시', '전라남도');

UPDATE cargo
SET dest_sido = '전남광주통합특별시'
WHERE dest_sido IN ('광주광역시', '전라남도');

UPDATE cargo
SET origin_sido = '전북특별자치도'
WHERE origin_sido = '전라북도';

UPDATE cargo
SET dest_sido = '전북특별자치도'
WHERE dest_sido = '전라북도';

UPDATE cargo
SET origin_sigungu = CASE origin_sigungu
    WHEN '중구' THEN '제물포구'
    WHEN '동구' THEN '제물포구'
    WHEN '서구' THEN '서해구'
END
WHERE origin_sido = '인천광역시'
  AND origin_sigungu IN ('중구', '동구', '서구');

UPDATE cargo
SET dest_sigungu = CASE dest_sigungu
    WHEN '중구' THEN '제물포구'
    WHEN '동구' THEN '제물포구'
    WHEN '서구' THEN '서해구'
END
WHERE dest_sido = '인천광역시'
  AND dest_sigungu IN ('중구', '동구', '서구');

UPDATE driver_route_preference
SET sido = '전남광주통합특별시'
WHERE sido IN ('광주광역시', '전라남도');

UPDATE driver_route_preference
SET sido = '전북특별자치도'
WHERE sido = '전라북도';

UPDATE driver_route_preference
SET sigungu = CASE sigungu
    WHEN '중구' THEN '제물포구'
    WHEN '동구' THEN '제물포구'
    WHEN '서구' THEN '서해구'
END
WHERE sido = '인천광역시'
  AND sigungu IN ('중구', '동구', '서구');

-- 폐지 명칭의 좌표를 먼저 현행 키로 복사해 기존 좌표를 보존한다.
INSERT INTO region_coordinate (sido, sigungu, location)
SELECT '전남광주통합특별시', sigungu, location
FROM region_coordinate
WHERE sido IN ('광주광역시', '전라남도')
ON CONFLICT (sido, sigungu) DO NOTHING;

DELETE FROM region_coordinate
WHERE sido IN ('광주광역시', '전라남도');

INSERT INTO region_coordinate (sido, sigungu, location)
SELECT '전북특별자치도', sigungu, location
FROM region_coordinate
WHERE sido = '전라북도'
ON CONFLICT (sido, sigungu) DO NOTHING;

DELETE FROM region_coordinate
WHERE sido = '전라북도';

INSERT INTO region_coordinate (sido, sigungu, location)
SELECT '인천광역시', '제물포구', location
FROM region_coordinate
WHERE sido = '인천광역시' AND sigungu = '중구'
ON CONFLICT (sido, sigungu) DO NOTHING;

INSERT INTO region_coordinate (sido, sigungu, location)
SELECT '인천광역시', '제물포구', location
FROM region_coordinate
WHERE sido = '인천광역시' AND sigungu = '동구'
ON CONFLICT (sido, sigungu) DO NOTHING;

INSERT INTO region_coordinate (sido, sigungu, location)
SELECT '인천광역시', '서해구', location
FROM region_coordinate
WHERE sido = '인천광역시' AND sigungu = '서구'
ON CONFLICT (sido, sigungu) DO NOTHING;

DELETE FROM region_coordinate
WHERE sido = '인천광역시'
  AND sigungu IN ('중구', '동구', '서구');

-- 현재 목록 전체를 선언하되 이미 있는 좌표는 덮어쓰지 않는다.
INSERT INTO region_coordinate (sido, sigungu, location) VALUES
    ('강원특별자치도', '강릉시', ST_SetSRID(ST_MakePoint(128.8322511, 37.7094218), 4326)::geography),
    ('강원특별자치도', '고성군', ST_SetSRID(ST_MakePoint(128.4007937, 38.3764412), 4326)::geography),
    ('강원특별자치도', '동해시', ST_SetSRID(ST_MakePoint(129.0560278, 37.5071809), 4326)::geography),
    ('강원특별자치도', '삼척시', ST_SetSRID(ST_MakePoint(129.1221905, 37.277486), 4326)::geography),
    ('강원특별자치도', '속초시', ST_SetSRID(ST_MakePoint(128.5201296, 38.1763621), 4326)::geography),
    ('강원특별자치도', '양구군', ST_SetSRID(ST_MakePoint(127.9991566, 38.1806606), 4326)::geography),
    ('강원특별자치도', '양양군', ST_SetSRID(ST_MakePoint(128.5959359, 38.0047965), 4326)::geography),
    ('강원특별자치도', '영월군', ST_SetSRID(ST_MakePoint(128.5016755, 37.2036827), 4326)::geography),
    ('강원특별자치도', '원주시', ST_SetSRID(ST_MakePoint(127.9311026, 37.3083544), 4326)::geography),
    ('강원특별자치도', '인제군', ST_SetSRID(ST_MakePoint(128.2627702, 38.0678877), 4326)::geography),
    ('강원특별자치도', '정선군', ST_SetSRID(ST_MakePoint(128.7392177, 37.3785864), 4326)::geography),
    ('강원특별자치도', '철원군', ST_SetSRID(ST_MakePoint(127.3565304, 38.2341933), 4326)::geography),
    ('강원특별자치도', '춘천시', ST_SetSRID(ST_MakePoint(127.7400062, 37.8900877), 4326)::geography),
    ('강원특별자치도', '태백시', ST_SetSRID(ST_MakePoint(128.980206, 37.1727206), 4326)::geography),
    ('강원특별자치도', '평창군', ST_SetSRID(ST_MakePoint(128.4824286, 37.5564481), 4326)::geography),
    ('강원특별자치도', '홍천군', ST_SetSRID(ST_MakePoint(128.0738743, 37.7450721), 4326)::geography),
    ('강원특별자치도', '화천군', ST_SetSRID(ST_MakePoint(127.6875614, 38.160976), 4326)::geography),
    ('강원특별자치도', '횡성군', ST_SetSRID(ST_MakePoint(128.0773659, 37.50976), 4326)::geography),
    ('경기도', '가평군', ST_SetSRID(ST_MakePoint(127.4502677, 37.8185898), 4326)::geography),
    ('경기도', '고양시', ST_SetSRID(ST_MakePoint(126.8357775, 37.6650685), 4326)::geography),
    ('경기도', '과천시', ST_SetSRID(ST_MakePoint(127.0032938, 37.4335989), 4326)::geography),
    ('경기도', '광명시', ST_SetSRID(ST_MakePoint(126.8647699, 37.445423), 4326)::geography),
    ('경기도', '광주시', ST_SetSRID(ST_MakePoint(127.3011292, 37.4029647), 4326)::geography),
    ('경기도', '구리시', ST_SetSRID(ST_MakePoint(127.1313009, 37.5997282), 4326)::geography),
    ('경기도', '군포시', ST_SetSRID(ST_MakePoint(126.921344, 37.3431559), 4326)::geography),
    ('경기도', '김포시', ST_SetSRID(ST_MakePoint(126.624888, 37.6783827), 4326)::geography),
    ('경기도', '남양주시', ST_SetSRID(ST_MakePoint(127.2432364, 37.6630333), 4326)::geography),
    ('경기도', '동두천시', ST_SetSRID(ST_MakePoint(127.077953, 37.9165386), 4326)::geography),
    ('경기도', '부천시', ST_SetSRID(ST_MakePoint(126.7886834, 37.5042499), 4326)::geography),
    ('경기도', '성남시', ST_SetSRID(ST_MakePoint(127.1161409, 37.4073682), 4326)::geography),
    ('경기도', '수원시', ST_SetSRID(ST_MakePoint(127.0076542, 37.2803966), 4326)::geography),
    ('경기도', '시흥시', ST_SetSRID(ST_MakePoint(126.7837707, 37.386335), 4326)::geography),
    ('경기도', '안산시', ST_SetSRID(ST_MakePoint(126.8308604, 37.3217152), 4326)::geography),
    ('경기도', '안성시', ST_SetSRID(ST_MakePoint(127.303025, 37.0349049), 4326)::geography),
    ('경기도', '안양시', ST_SetSRID(ST_MakePoint(126.9277189, 37.4025769), 4326)::geography),
    ('경기도', '양주시', ST_SetSRID(ST_MakePoint(127.0011761, 37.8087241), 4326)::geography),
    ('경기도', '양평군', ST_SetSRID(ST_MakePoint(127.5787369, 37.518219), 4326)::geography),
    ('경기도', '여주시', ST_SetSRID(ST_MakePoint(127.6157494, 37.3024704), 4326)::geography),
    ('경기도', '연천군', ST_SetSRID(ST_MakePoint(127.0248562, 38.0982503), 4326)::geography),
    ('경기도', '오산시', ST_SetSRID(ST_MakePoint(127.0513407, 37.1632538), 4326)::geography),
    ('경기도', '용인시', ST_SetSRID(ST_MakePoint(127.2219906, 37.2215273), 4326)::geography),
    ('경기도', '의왕시', ST_SetSRID(ST_MakePoint(126.9896417, 37.3624797), 4326)::geography),
    ('경기도', '의정부시', ST_SetSRID(ST_MakePoint(127.0681568, 37.7362158), 4326)::geography),
    ('경기도', '이천시', ST_SetSRID(ST_MakePoint(127.4811547, 37.2098982), 4326)::geography),
    ('경기도', '파주시', ST_SetSRID(ST_MakePoint(126.8097795, 37.8557012), 4326)::geography),
    ('경기도', '평택시', ST_SetSRID(ST_MakePoint(126.9948984, 37.0157015), 4326)::geography),
    ('경기도', '포천시', ST_SetSRID(ST_MakePoint(127.2504168, 37.9700027), 4326)::geography),
    ('경기도', '하남시', ST_SetSRID(ST_MakePoint(127.2062383, 37.5226926), 4326)::geography),
    ('경기도', '화성시', ST_SetSRID(ST_MakePoint(126.8725372, 37.1660532), 4326)::geography),
    ('경상남도', '거제시', ST_SetSRID(ST_MakePoint(128.6236211, 34.8702783), 4326)::geography),
    ('경상남도', '거창군', ST_SetSRID(ST_MakePoint(127.9041994, 35.7325118), 4326)::geography),
    ('경상남도', '고성군', ST_SetSRID(ST_MakePoint(128.2910682, 35.0154782), 4326)::geography),
    ('경상남도', '김해시', ST_SetSRID(ST_MakePoint(128.8453462, 35.2719365), 4326)::geography),
    ('경상남도', '남해군', ST_SetSRID(ST_MakePoint(127.8925589, 34.8374437), 4326)::geography),
    ('경상남도', '밀양시', ST_SetSRID(ST_MakePoint(128.7893074, 35.4980723), 4326)::geography),
    ('경상남도', '사천시', ST_SetSRID(ST_MakePoint(128.064562, 35.003468), 4326)::geography),
    ('경상남도', '산청군', ST_SetSRID(ST_MakePoint(127.8843338, 35.3685307), 4326)::geography),
    ('경상남도', '양산시', ST_SetSRID(ST_MakePoint(129.041276, 35.4020145), 4326)::geography),
    ('경상남도', '의령군', ST_SetSRID(ST_MakePoint(128.2773331, 35.392272), 4326)::geography),
    ('경상남도', '진주시', ST_SetSRID(ST_MakePoint(128.1298457, 35.2051498), 4326)::geography),
    ('경상남도', '창녕군', ST_SetSRID(ST_MakePoint(128.4929063, 35.508504), 4326)::geography),
    ('경상남도', '창원시', ST_SetSRID(ST_MakePoint(128.5992918, 35.2001505), 4326)::geography),
    ('경상남도', '통영시', ST_SetSRID(ST_MakePoint(128.3747129, 34.8251109), 4326)::geography),
    ('경상남도', '하동군', ST_SetSRID(ST_MakePoint(127.7795928, 35.1373195), 4326)::geography),
    ('경상남도', '함안군', ST_SetSRID(ST_MakePoint(128.4311866, 35.2911282), 4326)::geography),
    ('경상남도', '함양군', ST_SetSRID(ST_MakePoint(127.7218794, 35.5514715), 4326)::geography),
    ('경상남도', '합천군', ST_SetSRID(ST_MakePoint(128.1416812, 35.57647), 4326)::geography),
    ('경상북도', '경산시', ST_SetSRID(ST_MakePoint(128.8092794, 35.8336609), 4326)::geography),
    ('경상북도', '경주시', ST_SetSRID(ST_MakePoint(129.235903, 35.826559), 4326)::geography),
    ('경상북도', '고령군', ST_SetSRID(ST_MakePoint(128.3057752, 35.7363291), 4326)::geography),
    ('경상북도', '구미시', ST_SetSRID(ST_MakePoint(128.3555673, 36.2076386), 4326)::geography),
    ('경상북도', '김천시', ST_SetSRID(ST_MakePoint(128.0779538, 36.0604268), 4326)::geography),
    ('경상북도', '문경시', ST_SetSRID(ST_MakePoint(128.1487184, 36.690535), 4326)::geography),
    ('경상북도', '봉화군', ST_SetSRID(ST_MakePoint(128.9129936, 36.9344585), 4326)::geography),
    ('경상북도', '상주시', ST_SetSRID(ST_MakePoint(128.0669416, 36.4294491), 4326)::geography),
    ('경상북도', '성주군', ST_SetSRID(ST_MakePoint(128.2342281, 35.9069687), 4326)::geography),
    ('경상북도', '안동시', ST_SetSRID(ST_MakePoint(128.779984, 36.5801793), 4326)::geography),
    ('경상북도', '영덕군', ST_SetSRID(ST_MakePoint(129.3179551, 36.4823436), 4326)::geography),
    ('경상북도', '영양군', ST_SetSRID(ST_MakePoint(129.1451104, 36.6970716), 4326)::geography),
    ('경상북도', '영주시', ST_SetSRID(ST_MakePoint(128.5978684, 36.8703734), 4326)::geography),
    ('경상북도', '영천시', ST_SetSRID(ST_MakePoint(128.9425391, 36.0156711), 4326)::geography),
    ('경상북도', '예천군', ST_SetSRID(ST_MakePoint(128.4225173, 36.6541189), 4326)::geography),
    ('경상북도', '울릉군', ST_SetSRID(ST_MakePoint(130.8620426, 37.506175), 4326)::geography),
    ('경상북도', '울진군', ST_SetSRID(ST_MakePoint(129.313088, 36.9040406), 4326)::geography),
    ('경상북도', '의성군', ST_SetSRID(ST_MakePoint(128.6153721, 36.3620012), 4326)::geography),
    ('경상북도', '청도군', ST_SetSRID(ST_MakePoint(128.7856577, 35.6725545), 4326)::geography),
    ('경상북도', '청송군', ST_SetSRID(ST_MakePoint(129.0573435, 36.3570771), 4326)::geography),
    ('경상북도', '칠곡군', ST_SetSRID(ST_MakePoint(128.4630179, 36.0156467), 4326)::geography),
    ('경상북도', '포항시', ST_SetSRID(ST_MakePoint(129.3057515, 36.0930415), 4326)::geography),
    ('대구광역시', '군위군', ST_SetSRID(ST_MakePoint(128.6485963, 36.1702256), 4326)::geography),
    ('대구광역시', '남구', ST_SetSRID(ST_MakePoint(128.5853922, 35.8353148), 4326)::geography),
    ('대구광역시', '달서구', ST_SetSRID(ST_MakePoint(128.5285293, 35.827522), 4326)::geography),
    ('대구광역시', '달성군', ST_SetSRID(ST_MakePoint(128.4986479, 35.7594597), 4326)::geography),
    ('대구광역시', '동구', ST_SetSRID(ST_MakePoint(128.6858038, 35.9345572), 4326)::geography),
    ('대구광역시', '북구', ST_SetSRID(ST_MakePoint(128.5773246, 35.9286404), 4326)::geography),
    ('대구광역시', '서구', ST_SetSRID(ST_MakePoint(128.5497866, 35.8750713), 4326)::geography),
    ('대구광역시', '수성구', ST_SetSRID(ST_MakePoint(128.6613258, 35.83421), 4326)::geography),
    ('대구광역시', '중구', ST_SetSRID(ST_MakePoint(128.5934672, 35.8664919), 4326)::geography),
    ('대전광역시', '대덕구', ST_SetSRID(ST_MakePoint(127.4405086, 36.4123042), 4326)::geography),
    ('대전광역시', '동구', ST_SetSRID(ST_MakePoint(127.4749847, 36.3237831), 4326)::geography),
    ('대전광역시', '서구', ST_SetSRID(ST_MakePoint(127.3452079, 36.2806238), 4326)::geography),
    ('대전광역시', '유성구', ST_SetSRID(ST_MakePoint(127.3329905, 36.3768385), 4326)::geography),
    ('대전광역시', '중구', ST_SetSRID(ST_MakePoint(127.4112879, 36.2807779), 4326)::geography),
    ('부산광역시', '강서구', ST_SetSRID(ST_MakePoint(128.891238, 35.135779), 4326)::geography),
    ('부산광역시', '금정구', ST_SetSRID(ST_MakePoint(129.0917848, 35.2590126), 4326)::geography),
    ('부산광역시', '기장군', ST_SetSRID(ST_MakePoint(129.2015758, 35.2969986), 4326)::geography),
    ('부산광역시', '남구', ST_SetSRID(ST_MakePoint(129.0940898, 35.1262341), 4326)::geography),
    ('부산광역시', '동구', ST_SetSRID(ST_MakePoint(129.0446848, 35.1292739), 4326)::geography),
    ('부산광역시', '동래구', ST_SetSRID(ST_MakePoint(129.0788096, 35.2063005), 4326)::geography),
    ('부산광역시', '부산진구', ST_SetSRID(ST_MakePoint(129.0431545, 35.1650327), 4326)::geography),
    ('부산광역시', '북구', ST_SetSRID(ST_MakePoint(129.0241585, 35.2303296), 4326)::geography),
    ('부산광역시', '사상구', ST_SetSRID(ST_MakePoint(128.9868276, 35.1579449), 4326)::geography),
    ('부산광역시', '사하구', ST_SetSRID(ST_MakePoint(128.9715802, 35.0882891), 4326)::geography),
    ('부산광역시', '서구', ST_SetSRID(ST_MakePoint(129.0148178, 35.1021968), 4326)::geography),
    ('부산광역시', '수영구', ST_SetSRID(ST_MakePoint(129.1117081, 35.1611158), 4326)::geography),
    ('부산광역시', '연제구', ST_SetSRID(ST_MakePoint(129.083146, 35.1823832), 4326)::geography),
    ('부산광역시', '영도구', ST_SetSRID(ST_MakePoint(129.0652212, 35.0785965), 4326)::geography),
    ('부산광역시', '중구', ST_SetSRID(ST_MakePoint(129.0318751, 35.1056388), 4326)::geography),
    ('부산광역시', '해운대구', ST_SetSRID(ST_MakePoint(129.1537507, 35.1936549), 4326)::geography),
    ('서울특별시', '강남구', ST_SetSRID(ST_MakePoint(127.0628902, 37.4970192), 4326)::geography),
    ('서울특별시', '강동구', ST_SetSRID(ST_MakePoint(127.1466757, 37.5505489), 4326)::geography),
    ('서울특별시', '강북구', ST_SetSRID(ST_MakePoint(127.0111057, 37.6435766), 4326)::geography),
    ('서울특별시', '강서구', ST_SetSRID(ST_MakePoint(126.822734, 37.5613154), 4326)::geography),
    ('서울특별시', '관악구', ST_SetSRID(ST_MakePoint(126.946003, 37.4669986), 4326)::geography),
    ('서울특별시', '광진구', ST_SetSRID(ST_MakePoint(127.0865462, 37.5465208), 4326)::geography),
    ('서울특별시', '구로구', ST_SetSRID(ST_MakePoint(126.8565824, 37.4943592), 4326)::geography),
    ('서울특별시', '금천구', ST_SetSRID(ST_MakePoint(126.9012387, 37.4604153), 4326)::geography),
    ('서울특별시', '노원구', ST_SetSRID(ST_MakePoint(127.0752445, 37.652074), 4326)::geography),
    ('서울특별시', '도봉구', ST_SetSRID(ST_MakePoint(127.0324041, 37.6691315), 4326)::geography),
    ('서울특별시', '동대문구', ST_SetSRID(ST_MakePoint(127.055272, 37.582045), 4326)::geography),
    ('서울특별시', '동작구', ST_SetSRID(ST_MakePoint(126.9514129, 37.4989282), 4326)::geography),
    ('서울특별시', '마포구', ST_SetSRID(ST_MakePoint(126.908194, 37.5593986), 4326)::geography),
    ('서울특별시', '서대문구', ST_SetSRID(ST_MakePoint(126.9390199, 37.5774604), 4326)::geography),
    ('서울특별시', '서초구', ST_SetSRID(ST_MakePoint(127.0315473, 37.4734149), 4326)::geography),
    ('서울특별시', '성동구', ST_SetSRID(ST_MakePoint(127.0410592, 37.551131), 4326)::geography),
    ('서울특별시', '성북구', ST_SetSRID(ST_MakePoint(127.0179081, 37.6058518), 4326)::geography),
    ('서울특별시', '송파구', ST_SetSRID(ST_MakePoint(127.1152889, 37.5051154), 4326)::geography),
    ('서울특별시', '양천구', ST_SetSRID(ST_MakePoint(126.8555354, 37.5246926), 4326)::geography),
    ('서울특별시', '영등포구', ST_SetSRID(ST_MakePoint(126.9103183, 37.5223224), 4326)::geography),
    ('서울특별시', '용산구', ST_SetSRID(ST_MakePoint(126.980317, 37.5312495), 4326)::geography),
    ('서울특별시', '은평구', ST_SetSRID(ST_MakePoint(126.9289165, 37.6199477), 4326)::geography),
    ('서울특별시', '종로구', ST_SetSRID(ST_MakePoint(126.9776706, 37.5943106), 4326)::geography),
    ('서울특별시', '중구', ST_SetSRID(ST_MakePoint(126.9958895, 37.5600482), 4326)::geography),
    ('서울특별시', '중랑구', ST_SetSRID(ST_MakePoint(127.0930464, 37.5979493), 4326)::geography),
    ('세종특별자치시', '세종시', ST_SetSRID(ST_MakePoint(127.2587884, 36.5607285), 4326)::geography),
    ('울산광역시', '남구', ST_SetSRID(ST_MakePoint(129.3285512, 35.5155798), 4326)::geography),
    ('울산광역시', '동구', ST_SetSRID(ST_MakePoint(129.4264457, 35.5251987), 4326)::geography),
    ('울산광역시', '북구', ST_SetSRID(ST_MakePoint(129.3797218, 35.6103747), 4326)::geography),
    ('울산광역시', '울주군', ST_SetSRID(ST_MakePoint(129.1876462, 35.5458802), 4326)::geography),
    ('울산광역시', '중구', ST_SetSRID(ST_MakePoint(129.3095455, 35.5709398), 4326)::geography),
    ('인천광역시', '강화군', ST_SetSRID(ST_MakePoint(126.4016256, 37.7099281), 4326)::geography),
    ('인천광역시', '검단구', ST_SetSRID(ST_MakePoint(126.6507673, 37.5948032), 4326)::geography),
    ('인천광역시', '계양구', ST_SetSRID(ST_MakePoint(126.7347578, 37.5572557), 4326)::geography),
    ('인천광역시', '남동구', ST_SetSRID(ST_MakePoint(126.7267456, 37.4308439), 4326)::geography),
    ('인천광역시', '미추홀구', ST_SetSRID(ST_MakePoint(126.6649116, 37.4528532), 4326)::geography),
    ('인천광역시', '부평구', ST_SetSRID(ST_MakePoint(126.7212301, 37.4966858), 4326)::geography),
    ('인천광역시', '서해구', ST_SetSRID(ST_MakePoint(126.6529203, 37.5304123), 4326)::geography),
    ('인천광역시', '연수구', ST_SetSRID(ST_MakePoint(126.6499175, 37.390562), 4326)::geography),
    ('인천광역시', '영종구', ST_SetSRID(ST_MakePoint(126.4649744, 37.4690481), 4326)::geography),
    ('인천광역시', '옹진군', ST_SetSRID(ST_MakePoint(126.4289021, 37.5330245), 4326)::geography),
    ('인천광역시', '제물포구', ST_SetSRID(ST_MakePoint(126.6233508, 37.4655917), 4326)::geography),
    ('전남광주통합특별시', '강진군', ST_SetSRID(ST_MakePoint(126.7722094, 34.6197089), 4326)::geography),
    ('전남광주통합특별시', '고흥군', ST_SetSRID(ST_MakePoint(127.3160924, 34.5971436), 4326)::geography),
    ('전남광주통합특별시', '곡성군', ST_SetSRID(ST_MakePoint(127.2637015, 35.2166374), 4326)::geography),
    ('전남광주통합특별시', '광산구', ST_SetSRID(ST_MakePoint(126.7528535, 35.1650459), 4326)::geography),
    ('전남광주통합특별시', '광양시', ST_SetSRID(ST_MakePoint(127.656315, 35.0192771), 4326)::geography),
    ('전남광주통합특별시', '구례군', ST_SetSRID(ST_MakePoint(127.5032393, 35.2369383), 4326)::geography),
    ('전남광주통합특별시', '나주시', ST_SetSRID(ST_MakePoint(126.7196775, 34.9881943), 4326)::geography),
    ('전남광주통합특별시', '남구', ST_SetSRID(ST_MakePoint(126.8569821, 35.0941038), 4326)::geography),
    ('전남광주통합특별시', '담양군', ST_SetSRID(ST_MakePoint(126.995415, 35.291477), 4326)::geography),
    ('전남광주통합특별시', '동구', ST_SetSRID(ST_MakePoint(126.9488222, 35.1174653), 4326)::geography),
    ('전남광주통합특별시', '목포시', ST_SetSRID(ST_MakePoint(126.3886862, 34.8030635), 4326)::geography),
    ('전남광주통합특별시', '무안군', ST_SetSRID(ST_MakePoint(126.4226984, 34.9567721), 4326)::geography),
    ('전남광주통합특별시', '보성군', ST_SetSRID(ST_MakePoint(127.1622863, 34.8145071), 4326)::geography),
    ('전남광주통합특별시', '북구', ST_SetSRID(ST_MakePoint(126.9260576, 35.1929089), 4326)::geography),
    ('전남광주통합특별시', '서구', ST_SetSRID(ST_MakePoint(126.8507087, 35.1356228), 4326)::geography),
    ('전남광주통합특별시', '순천시', ST_SetSRID(ST_MakePoint(127.3894445, 34.9946578), 4326)::geography),
    ('전남광주통합특별시', '신안군', ST_SetSRID(ST_MakePoint(126.3522531, 34.8331095), 4326)::geography),
    ('전남광주통합특별시', '여수시', ST_SetSRID(ST_MakePoint(127.6702291, 34.7643802), 4326)::geography),
    ('전남광주통합특별시', '영광군', ST_SetSRID(ST_MakePoint(126.4457961, 35.276644), 4326)::geography),
    ('전남광주통합특별시', '영암군', ST_SetSRID(ST_MakePoint(126.6281519, 34.7989077), 4326)::geography),
    ('전남광주통합특별시', '완도군', ST_SetSRID(ST_MakePoint(126.7553933, 34.3110373), 4326)::geography),
    ('전남광주통합특별시', '장성군', ST_SetSRID(ST_MakePoint(126.7686347, 35.3294724), 4326)::geography),
    ('전남광주통합특별시', '장흥군', ST_SetSRID(ST_MakePoint(126.9218972, 34.6761288), 4326)::geography),
    ('전남광주통합특별시', '진도군', ST_SetSRID(ST_MakePoint(126.2066688, 34.4328182), 4326)::geography),
    ('전남광주통합특별시', '함평군', ST_SetSRID(ST_MakePoint(126.5351171, 35.1126705), 4326)::geography),
    ('전남광주통합특별시', '해남군', ST_SetSRID(ST_MakePoint(126.5147067, 34.5530262), 4326)::geography),
    ('전남광주통합특별시', '화순군', ST_SetSRID(ST_MakePoint(127.0332828, 35.0082753), 4326)::geography),
    ('전북특별자치도', '고창군', ST_SetSRID(ST_MakePoint(126.6156875, 35.4484029), 4326)::geography),
    ('전북특별자치도', '군산시', ST_SetSRID(ST_MakePoint(126.7201098, 35.9539573), 4326)::geography),
    ('전북특별자치도', '김제시', ST_SetSRID(ST_MakePoint(126.9070807, 35.8037259), 4326)::geography),
    ('전북특별자치도', '남원시', ST_SetSRID(ST_MakePoint(127.4414803, 35.4223947), 4326)::geography),
    ('전북특별자치도', '무주군', ST_SetSRID(ST_MakePoint(127.7129013, 35.9392798), 4326)::geography),
    ('전북특별자치도', '부안군', ST_SetSRID(ST_MakePoint(126.639589, 35.6765807), 4326)::geography),
    ('전북특별자치도', '순창군', ST_SetSRID(ST_MakePoint(127.0898462, 35.4336204), 4326)::geography),
    ('전북특별자치도', '완주군', ST_SetSRID(ST_MakePoint(127.2151474, 35.9186818), 4326)::geography),
    ('전북특별자치도', '익산시', ST_SetSRID(ST_MakePoint(126.9893884, 36.0231842), 4326)::geography),
    ('전북특별자치도', '임실군', ST_SetSRID(ST_MakePoint(127.2361032, 35.5977603), 4326)::geography),
    ('전북특별자치도', '장수군', ST_SetSRID(ST_MakePoint(127.5440483, 35.6570406), 4326)::geography),
    ('전북특별자치도', '전주시', ST_SetSRID(ST_MakePoint(127.1159772, 35.828195), 4326)::geography),
    ('전북특별자치도', '정읍시', ST_SetSRID(ST_MakePoint(126.9049587, 35.6029592), 4326)::geography),
    ('전북특별자치도', '진안군', ST_SetSRID(ST_MakePoint(127.4300709, 35.8287098), 4326)::geography),
    ('제주특별자치도', '서귀포시', ST_SetSRID(ST_MakePoint(126.5808289, 33.3240264), 4326)::geography),
    ('제주특별자치도', '제주시', ST_SetSRID(ST_MakePoint(126.5298279, 33.4437284), 4326)::geography),
    ('충청남도', '계룡시', ST_SetSRID(ST_MakePoint(127.2343892, 36.2913205), 4326)::geography),
    ('충청남도', '공주시', ST_SetSRID(ST_MakePoint(127.0753532, 36.4797504), 4326)::geography),
    ('충청남도', '금산군', ST_SetSRID(ST_MakePoint(127.4783429, 36.1192579), 4326)::geography),
    ('충청남도', '논산시', ST_SetSRID(ST_MakePoint(127.1579816, 36.1908732), 4326)::geography),
    ('충청남도', '당진시', ST_SetSRID(ST_MakePoint(126.6515857, 36.9046193), 4326)::geography),
    ('충청남도', '보령시', ST_SetSRID(ST_MakePoint(126.5876737, 36.345747), 4326)::geography),
    ('충청남도', '부여군', ST_SetSRID(ST_MakePoint(126.8570423, 36.2463646), 4326)::geography),
    ('충청남도', '서산시', ST_SetSRID(ST_MakePoint(126.4617977, 36.7908962), 4326)::geography),
    ('충청남도', '서천군', ST_SetSRID(ST_MakePoint(126.703775, 36.1064559), 4326)::geography),
    ('충청남도', '아산시', ST_SetSRID(ST_MakePoint(126.9805008, 36.8070031), 4326)::geography),
    ('충청남도', '예산군', ST_SetSRID(ST_MakePoint(126.7841784, 36.6704432), 4326)::geography),
    ('충청남도', '천안시', ST_SetSRID(ST_MakePoint(127.2024708, 36.8041504), 4326)::geography),
    ('충청남도', '청양군', ST_SetSRID(ST_MakePoint(126.8531683, 36.4305635), 4326)::geography),
    ('충청남도', '태안군', ST_SetSRID(ST_MakePoint(126.2819377, 36.6973899), 4326)::geography),
    ('충청남도', '홍성군', ST_SetSRID(ST_MakePoint(126.6228038, 36.5695806), 4326)::geography),
    ('충청북도', '괴산군', ST_SetSRID(ST_MakePoint(127.8296099, 36.7694938), 4326)::geography),
    ('충청북도', '단양군', ST_SetSRID(ST_MakePoint(128.3878907, 36.994246), 4326)::geography),
    ('충청북도', '보은군', ST_SetSRID(ST_MakePoint(127.729486, 36.4900174), 4326)::geography),
    ('충청북도', '영동군', ST_SetSRID(ST_MakePoint(127.8143374, 36.1595166), 4326)::geography),
    ('충청북도', '옥천군', ST_SetSRID(ST_MakePoint(127.6566417, 36.3205516), 4326)::geography),
    ('충청북도', '음성군', ST_SetSRID(ST_MakePoint(127.6144456, 36.9759739), 4326)::geography),
    ('충청북도', '제천시', ST_SetSRID(ST_MakePoint(128.1410185, 37.0598682), 4326)::geography),
    ('충청북도', '증평군', ST_SetSRID(ST_MakePoint(127.604549, 36.7866068), 4326)::geography),
    ('충청북도', '진천군', ST_SetSRID(ST_MakePoint(127.4405228, 36.8708013), 4326)::geography),
    ('충청북도', '청주시', ST_SetSRID(ST_MakePoint(127.4988088, 36.6272994), 4326)::geography),
    ('충청북도', '충주시', ST_SetSRID(ST_MakePoint(127.8957719, 37.0152561), 4326)::geography)
ON CONFLICT (sido, sigungu) DO NOTHING;

-- 좌표가 비어 있던 기존 화물만 현행 지역 좌표로 역보정한다.
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

DO $migration$
BEGIN
    IF (SELECT count(*) FROM region_coordinate) <> 230 THEN
        RAISE EXCEPTION 'region_coordinate must contain exactly 230 current regions';
    END IF;

    IF (SELECT count(DISTINCT sido) FROM region_coordinate) <> 16 THEN
        RAISE EXCEPTION 'region_coordinate must contain exactly 16 current sido groups';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM region_coordinate
        WHERE sido IN ('광주광역시', '전라남도', '전라북도')
           OR (sido = '인천광역시' AND sigungu IN ('중구', '동구', '서구'))
    ) THEN
        RAISE EXCEPTION 'deprecated region names remain';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM region_coordinate
        WHERE location IS NULL
           OR ST_SRID(location::geometry) <> 4326
           OR ST_X(location::geometry) NOT BETWEEN 124 AND 132
           OR ST_Y(location::geometry) NOT BETWEEN 33 AND 39
    ) THEN
        RAISE EXCEPTION 'invalid region coordinates remain';
    END IF;
END
$migration$;
