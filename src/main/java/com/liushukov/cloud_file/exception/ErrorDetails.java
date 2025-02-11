package com.liushukov.cloud_file.exception;

import java.time.Instant;

public record ErrorDetails(Instant timestamp, String message, String description) {
}
