package com.example.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.TradeRequest;
import com.example.demo.service.TradeService;

@RestController
@RequestMapping("/trade")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @PostMapping("/buy")
    public String buyStock(@RequestBody TradeRequest request) {
        return tradeService.buyStock(request);
    }
}