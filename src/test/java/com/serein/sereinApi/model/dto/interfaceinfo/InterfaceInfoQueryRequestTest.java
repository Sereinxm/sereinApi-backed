package com.serein.sereinApi.model.dto.interfaceinfo;

import static org.junit.jupiter.api.Assertions.*;

class InterfaceInfoQueryRequestTest {
    public static void main(String[] args) {
        InterfaceInfoQueryRequest interfaceInfoQueryRequest = new InterfaceInfoQueryRequest();
        String sortField = interfaceInfoQueryRequest.getSortField();
        System.out.println(sortField);

    }

}