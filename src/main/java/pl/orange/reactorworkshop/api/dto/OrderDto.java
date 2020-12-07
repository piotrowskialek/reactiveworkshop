package pl.orange.reactorworkshop.api.dto;

import lombok.Value;

import java.time.Instant;

@Value
public class OrderDto {
    String id;
    Instant date;
}
