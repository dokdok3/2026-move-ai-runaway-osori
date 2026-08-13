package com.hackathon.domain.cargo.service;

import com.hackathon.domain.cargo.dto.ParsedCargoResponse;
import com.hackathon.domain.cargo.dto.ParsedCargoResponse.ParsedAddress;
import com.hackathon.domain.region.repository.RegionCoordinateRepository;
import com.hackathon.domain.region.repository.RegionCoordinateRepository.RegionCoordinate;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CargoRegionNormalizer {

    private final RegionCoordinateRepository regionCoordinateRepository;

    public CargoRegionNormalizer(RegionCoordinateRepository regionCoordinateRepository) {
        this.regionCoordinateRepository = regionCoordinateRepository;
    }

    public ParsedCargoResponse normalize(ParsedCargoResponse parsed) {
        return parsed.withAddresses(
                normalizeAddress(parsed.origin()),
                normalizeAddress(parsed.destination())
        );
    }

    private ParsedAddress normalizeAddress(ParsedAddress address) {
        if (address == null || !StringUtils.hasText(address.sigungu())
                || "전체".equals(address.sigungu().trim())) {
            return address;
        }

        List<RegionCoordinate> candidates = regionCoordinateRepository.findBySigungu(address.sigungu().trim());
        RegionCoordinate resolved = resolveUnique(candidates, address.sido());

        if (resolved == null) {
            return address;
        }
        return new ParsedAddress(resolved.sido(), resolved.sigungu(), address.detail());
    }

    private RegionCoordinate resolveUnique(List<RegionCoordinate> candidates, String sido) {
        if (candidates.isEmpty()) {
            return null;
        }

        List<RegionCoordinate> matchingSido = candidates.stream()
                .filter(region -> sameText(region.sido(), sido))
                .toList();
        if (matchingSido.size() == 1) {
            return matchingSido.getFirst();
        }
        return candidates.size() == 1 ? candidates.getFirst() : null;
    }

    private static boolean sameText(String first, String second) {
        return StringUtils.hasText(first)
                && StringUtils.hasText(second)
                && compact(first).equals(compact(second));
    }

    private static String compact(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

}
