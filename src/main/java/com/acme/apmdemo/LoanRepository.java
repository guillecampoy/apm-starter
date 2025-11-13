package com.acme.apmdemo;

import com.acme.obs.apm.ApmClient;
import com.acme.obs.apm.TraceSpan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class LoanRepository {
  private final JdbcTemplate jdbc;
  private final ApmClient apm;

  public LoanRepository(JdbcTemplate jdbc, ApmClient apm){ this.jdbc=jdbc; this.apm=apm; }

  private final RowMapper<Loan> mapper = new RowMapper<>(){
    @Override public Loan mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new Loan(
        rs.getLong("id"),
        rs.getBigDecimal("amount"),
        rs.getString("currency"),
        rs.getString("status")
      );
    }
  };

  @TraceSpan(value="db_find_loan", attributes={"component=repository"})
  public Loan findById(Long id){
    return jdbc.queryForObject("SELECT * FROM loans WHERE id=?", mapper, id);
  }

  public int updateStatus(Long id, String status){
    try (var s = apm.startSpan("db_update_status")){
      int updated = jdbc.update("UPDATE loans SET status=? WHERE id=?", status, id);
      s.setAttribute("db.rows_affected", updated);
      return updated;
    }
  }
}
