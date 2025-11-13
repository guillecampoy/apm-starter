package com.acme.apmdemo;

import com.acme.obs.apm.ApmClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LoanService {
  private final ApmClient apm;
  private final LoanRepository repo;
  private final PaymentGateway gateway;

  public LoanService(ApmClient apm, LoanRepository repo, PaymentGateway gateway){
    this.apm = apm; this.repo=repo; this.gateway=gateway;
  }

  public void approveLoan(Long loanId, String user){
    apm.setUser(user, Map.of("role","auditor"));
    try (var span = apm.startSpan("approve_loan", Map.of("loan.id", String.valueOf(loanId)))){
      long t0 = System.nanoTime();
      Loan loan = repo.findById(loanId);
      Map<String,Object> instr = Map.of("loanId", loan.getId(), "amount", loan.getAmount(), "currency", loan.getCurrency());
      gateway.enqueueTransfer(instr);
      apm.incrementCounter("loan.approve.count", 1, Map.of("status","enqueued"));
      apm.recordHistogram("loan.approve.amount", loan.getAmount().doubleValue(), Map.of("currency", loan.getCurrency()));
      span.setAttribute("result","enqueued");
      span.setAttribute("elapsed.ms", (System.nanoTime()-t0)/1_000_000);
      repo.updateStatus(loanId, "APPROVED");
    } catch (Exception e){
      apm.incrementCounter("loan.approve.count", 1, Map.of("status","error"));
      apm.recordException(e);
      throw e;
    }
  }
}
