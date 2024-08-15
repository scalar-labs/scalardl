package com.scalar.application.bankaccount.controller;

import com.scalar.application.bankaccount.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts")
public class AccountController {
  @Autowired private AccountService accountService;

  // FIXME: use an ordered set to keep track of all the created accounts then call get on each of
  // them and return the result
  // @GetMapping
  // public ResponseEntity<List<String>> read() {
  //  List<String> response = accountService.accounts();
  //  return ResponseEntity.ok(response);
  // }

  @PutMapping(value = "/{id}")
  public ResponseEntity<String> create(@PathVariable("id") String id) {
    return accountService.create(id);
  }

  @GetMapping(value = "/{id}")
  public ResponseEntity<String> history(
      @PathVariable("id") String id,
      @RequestParam(value = "start", defaultValue = "0") int start,
      @RequestParam(value = "end", defaultValue = "0") int end,
      @RequestParam(value = "limit", defaultValue = "0") int limit,
      @RequestParam(value = "order", defaultValue = "desc") String order) {
    return accountService.history(id, start, end, limit, order);
  }

  @PostMapping(value = "/{id}/deposit")
  public ResponseEntity<String> deposit(
      @PathVariable("id") String id, @RequestParam(value = "amount") long amount) {
    return accountService.deposit(id, amount);
  }

  @PostMapping(value = "/{id}/withdraw")
  public ResponseEntity<String> withdraw(
      @PathVariable("id") String id, @RequestParam(value = "amount") long amount) {
    return accountService.withdraw(id, amount);
  }
}
