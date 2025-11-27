package com.glvov.springaimcpserver.model;

import java.time.LocalDateTime;

public record OpenMeteoResponse(Current current) {
    public record Current(LocalDateTime time, int interval, double temperature_2m) {
    }
}
