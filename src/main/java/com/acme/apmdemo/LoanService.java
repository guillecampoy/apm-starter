package com.acme.apmdemo;

import com.acme.obs.apm.ApmClient;
import com.acme.obs.apm.TelemetryLayer;
import com.acme.obs.apm.TraceSpan;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LoanService {
  private final ApmClient apm;
  private final LoanRepository repo;
  private final PaymentGateway gateway;

  public LoanService(ApmClient apm, LoanRepository repo, PaymentGateway gateway){
    this.apm = apm; this.repo=repo; this.gateway=gateway;
  }

  @TraceSpan(value="loan_service_approve", recordArgs=true, layer=TelemetryLayer.SERVICE)
  public void approveLoan(Long loanId, String user){
    apm.setUser(user, Map.of("role","auditor"));
    long t0 = System.nanoTime();
    try {
      Loan loan = repo.findById(loanId);
      Map<String,Object> instr = Map.of(
        "loanId", loan.getId(),
        "amount", loan.getAmount(),
        "currency", loan.getCurrency()
      );
      apm.setAttribute("loan.amount", loan.getAmount().doubleValue());
      apm.setAttribute("loan.currency", loan.getCurrency());
      gateway.enqueueTransfer(instr);
      apm.incrementCounter("loan.approve.count", 1, Map.of("status","enqueued"));
      apm.recordHistogram("loan.approve.amount", loan.getAmount().doubleValue(), Map.of("currency", loan.getCurrency()));
      apm.setAttribute("loan.status", "enqueued");
      apm.setAttribute("loan.elapsed.ms", (System.nanoTime()-t0)/1_000_000);
      repo.updateStatus(loanId, "APPROVED");
    } catch (Exception e){
      apm.incrementCounter("loan.approve.count", 1, Map.of("status","error"));
      throw e;
    }
  }
}
