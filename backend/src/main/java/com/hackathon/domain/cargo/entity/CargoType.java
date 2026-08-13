package com.hackathon.domain.cargo.entity;

import java.util.List;

/** mock-data.md 화물 목록의 cargoType 통제 어휘를 그대로 따른다. */
public enum CargoType {
    REFRIGERATED("냉장 화물", List.of("냉장", "신선식품", "채소", "과일", "유제품")),
    GENERAL("일반 화물", List.of("일반", "박스", "공산품", "잡화", "가구")),
    FROZEN("냉동 화물", List.of("냉동", "빙과", "냉동식품")),
    CONSTRUCTION("건설 화물", List.of("건설자재", "철근", "시멘트", "목재", "중장비")),
    HAZARDOUS("위험물", List.of("위험물", "유류", "가스", "화학물질"));

    private final String koreanName;
    private final List<String> examples;

    CargoType(String koreanName, List<String> examples) {
        this.koreanName = koreanName;
        this.examples = examples;
    }

    public String koreanName() {
        return koreanName;
    }

    public List<String> examples() {
        return examples;
    }
}
