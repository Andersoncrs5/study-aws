package com.aws.app1.controller.gatewayController;

import com.aws.app1.services.Gateway.GatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("gateway")
public class GatewayController {

    private GatewayService service;

    @GetMapping("/send")
    public ResponseEntity<?> sendMessage() {
        String message = this.service.sendMessage();

        return ResponseEntity.ok(message);
    }

}
