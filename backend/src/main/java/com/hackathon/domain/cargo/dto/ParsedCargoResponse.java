package com.hackathon.domain.cargo.dto;

import java.time.LocalDate;
import java.util.List;

/** 자연어 화물 요청의 구조화 결과. 원문에서 확인할 수 없는 값은 null이다. */
public record ParsedCargoResponse(
        ParsedAddress origin,
        ParsedAddress destination,
        String cargoType,
        String cargoDescription,
        Double weightTon,
        Integer offeredFareKrw,
        LocalDate loadingDate,
        String loadingTimeText,
        LocalDate unloadingDate,
        String unloadingTimeText,
        List<String> missingFields,
        String confidence,
        List<String> warnings
) {
    public record ParsedAddress(String sido, String sigungu, String detail) {
    }
}
