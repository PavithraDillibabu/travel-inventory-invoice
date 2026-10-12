package com.navan.travel_inventory_invoice.dto.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {
    FLIGHT("flight"),
    HOTEL("hotel"),
    RAIL("rail"),
    NAVAN_FEE("navan_fee");
    private final String value; TransactionType(String v){value=v;}

    @JsonValue
    public String value(){return value;}
    @JsonCreator
    public static TransactionType from(String v){
        for(var t:values())
            if(t.value.equals(v))
                return t;
        throw new IllegalArgumentException("Unknown transaction type: "+v);
    }
}