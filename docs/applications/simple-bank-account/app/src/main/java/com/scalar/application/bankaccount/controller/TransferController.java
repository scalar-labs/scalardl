package com.scalar.application.bankaccount.controller;

import com.scalar.application.bankaccount.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/transfers")
public class TransferController {
  @Autowired private AccountService accountService;

  @PostMapping
  public ResponseEntity<String> transfer(
      @RequestParam(value = "from") String from,
      @RequestParam(value = "to") String to,
      @RequestParam(value = "amount") long amount) {
    return accountService.transfer(from, to, amount);
  }
}
