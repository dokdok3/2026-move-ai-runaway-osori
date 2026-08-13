package com.hackathon.domain.region.service;

import com.hackathon.domain.region.dto.RegionResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RegionService {

    // 해커톤 범위: 데모에 필요한 시도만 정적으로 보관한다.
    private static final List<RegionResponse> REGIONS = List.of(
            new RegionResponse("서울특별시", List.of("강남구", "송파구", "서초구", "마포구", "성동구")),
            new RegionResponse("경기도", List.of("수원시", "용인시", "평택시", "성남시", "고양시")),
            new RegionResponse("인천광역시", List.of("서구", "남동구", "연수구")),
            new RegionResponse("대전광역시", List.of("유성구", "대덕구", "서구")),
            new RegionResponse("충청남도", List.of("천안시", "아산시", "서산시")),
            new RegionResponse("부산광역시", List.of("강서구", "해운대구", "사상구")),
            new RegionResponse("울산광역시", List.of("남구", "북구")),
            new RegionResponse("광주광역시", List.of("광산구", "북구")),
            new RegionResponse("전라북도", List.of("전주시", "익산시")),
            new RegionResponse("경상남도", List.of("창원시", "김해시"))
    );

    public List<RegionResponse> findAll() {
        return REGIONS;
    }
}
