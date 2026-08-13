package com.hackathon.domain.fare.repository;

import com.hackathon.domain.fare.entity.FareQuote;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FareQuoteRepository extends JpaRepository<FareQuote, Long> {

    Optional<FareQuote> findByQuoteKey(String quoteKey);
}
