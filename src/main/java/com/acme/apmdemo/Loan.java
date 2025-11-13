package com.acme.apmdemo;

import java.math.BigDecimal;

public class Loan {
  private Long id;
  private BigDecimal amount;
  private String currency;
  private String status;

  public Loan() {}
  public Loan(Long id, BigDecimal amount, String currency, String status){
    this.id=id; this.amount=amount; this.currency=currency; this.status=status;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
