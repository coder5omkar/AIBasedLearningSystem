package com.aitutor.controller;

import com.aitutor.service.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelService modelService;

    @Autowired
    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping
    public List<Map<String, String>> getModels(@RequestParam(required = false) String apiKey) {
        return modelService.getModels(apiKey);
    }
}
