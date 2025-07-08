package com.aws.app1.services.Gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GatewayService {

    public String sendMessage() {
        return "OK";
    }

}
