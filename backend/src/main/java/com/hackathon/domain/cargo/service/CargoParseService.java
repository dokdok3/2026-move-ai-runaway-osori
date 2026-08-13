package com.hackathon.domain.cargo.service;

import com.hackathon.domain.cargo.dto.ParseRequest;
import com.hackathon.domain.cargo.dto.ParsedCargoResponse;
import org.springframework.stereotype.Service;

@Service
public class CargoParseService {

    /**
     * TODO(AI): Claude API(structured outputs)로 rawText를 파싱해 ParsedCargoResponse를 채운다.
     * - 서버의 오늘 날짜를 프롬프트에 함께 넣어 "내일" 같은 상대 날짜를 절대 시각(ISO-8601 문자열)으로
     *   변환한다.
     * - 시/도는 정식 명칭으로 정규화한다("서울"→"서울특별시") — 매칭 스코어가 시도 문자열 일치로
     *   동작하므로 어긋나면 화물 목록이 비어 보인다.
     * - 읽지 못한 항목은 null로 채워 프론트가 "고치기"로 채우게 한다. 출발지·도착지를 못 읽으면
     *   ErrorCode.AI_PARSING_FAILED(422)로 BusinessException을 던진다.
     * - global/ai/ClaudeClient(아직 없음)를 통해 호출할 것.
     */
    public ParsedCargoResponse parse(ParseRequest request) {
        throw new UnsupportedOperationException(
                "TODO(AI): 자연어 파싱이 아직 연동되지 않았습니다 (rawText=" + request.rawText() + ")");
    }
}
