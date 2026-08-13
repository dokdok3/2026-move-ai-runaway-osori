package com.hackathon.domain.region.service;

import com.hackathon.domain.region.dto.RegionResponse;
import com.hackathon.domain.region.repository.RegionCoordinateRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionCoordinateRepository regionCoordinateRepository;

    @Transactional(readOnly = true)
    public List<RegionResponse> findAll() {
        Map<String, List<String>> sigungusBySido = new LinkedHashMap<>();
        regionCoordinateRepository.findAll().forEach(region ->
                sigungusBySido.computeIfAbsent(region.sido(), key -> new ArrayList<>())
                        .add(region.sigungu())
        );

        return sigungusBySido.entrySet().stream()
                .map(entry -> new RegionResponse(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }
}
