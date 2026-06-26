package com.aitutor.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String provider;
    private String model;
    private String apiKey;
}