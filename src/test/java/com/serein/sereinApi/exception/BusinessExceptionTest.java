package com.serein.sereinApi.exception;

import com.serein.sereinApi.common.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void getCode() {
        BusinessException businessException = new BusinessException(ErrorCode.SYSTEM_ERROR);
        System.out.println(businessException);
    }
}