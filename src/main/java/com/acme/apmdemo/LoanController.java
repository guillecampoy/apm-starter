package com.acme.apmdemo;

import com.acme.obs.apm.TelemetryLayer;
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
  @TraceSpan(value="loan_approve", layer=TelemetryLayer.CONTROLLER, recordArgs=true)
  public ResponseEntity<Void> approve(@PathVariable("loanId") Long loanId){
    service.approveLoan(loanId, "demo-user");
    return ResponseEntity.accepted().build();
  }

  @GetMapping("/{loanId}")
  @TraceSpan(value="loan_get", layer=TelemetryLayer.CONTROLLER, recordArgs=true)
  public ResponseEntity<Loan> get(@PathVariable("loanId") Long loanId){
    return ResponseEntity.ok(repo.findById(loanId));
  }
}
