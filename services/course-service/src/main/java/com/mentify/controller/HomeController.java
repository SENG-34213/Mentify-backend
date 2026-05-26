package com.mentify.controller;

import com.mentify.payload.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course")
public class HomeController {

    @GetMapping
    public ResponseEntity<ApiResponse> getWelcomeMessage(){
        return ResponseEntity.ok(new ApiResponse("Welcome"));
    }
}
