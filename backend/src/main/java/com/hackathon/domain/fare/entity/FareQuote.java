package com.hackathon.domain.fare.entity;

import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FareQuote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** originSido|destSido|cargoType|weightTon 조합. 같은 조건이면 항상 같은 캐시 값을 돌려주기 위한 키. */
    @Column(unique = true, length = 150)
    private String quoteKey;

    private Integer averageFare;
    private Integer sameDayThreshold;
    private Integer distanceKm;

    public static FareQuote create(String quoteKey, Integer averageFare,
                                   Integer sameDayThreshold, Integer distanceKm) {
        FareQuote quote = new FareQuote();
        quote.quoteKey = quoteKey;
        quote.averageFare = averageFare;
        quote.sameDayThreshold = sameDayThreshold;
        quote.distanceKm = distanceKm;
        return quote;
    }
}
