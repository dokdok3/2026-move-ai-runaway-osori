package com.hackathon.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AbstractCustomExceptionTest {

    @Test
    void domainExceptionCanExtendAbstractCustomException() {
        TestDomainException exception = new TestDomainException();

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CARGO_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.CARGO_NOT_FOUND.getMessage());
    }

    private static class TestDomainException extends AbstractCustomException {

        private TestDomainException() {
            super(ErrorCode.CARGO_NOT_FOUND);
        }
    }
}
