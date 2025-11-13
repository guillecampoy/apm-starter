package com.acme.apmdemo;

import com.acme.obs.apm.TraceSpan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/loans")
public class LoanController {
  private final LoanService service;
  private final LoanRepository repo;

  public LoanController(LoanService s, LoanRepository r){ this.service=s; this.repo=r; }

  @PostMapping("/{loanId}/approve")
  @TraceSpan(value="loan_approve", attributes={"component=controller"}, recordArgs=true)
  public ResponseEntity<Void> approve(@PathVariable Long loanId){
    service.approveLoan(loanId, "demo-user");
    return ResponseEntity.accepted().build();
  }

  @GetMapping("/{loanId}")
  @TraceSpan(value="loan_get", attributes={"component=controller"})
  public ResponseEntity<Loan> get(@PathVariable Long loanId){
    return ResponseEntity.ok(repo.findById(loanId));
  }
}
