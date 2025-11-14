package com.acme.apmdemo;

import com.acme.obs.apm.ApmClient;
import com.acme.obs.apm.TelemetryLayer;
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

  @TraceSpan(
    value="db_find_loan",
    layer=TelemetryLayer.REPOSITORY,
    attributes={"db.operation=select","db.table=loans"}
  )
  public Loan findById(Long id){
    apm.setAttribute("db.query.loanId", String.valueOf(id));
    return jdbc.queryForObject("SELECT * FROM loans WHERE id=?", mapper, id);
  }

  @TraceSpan(
    value="db_update_status",
    layer=TelemetryLayer.REPOSITORY,
    attributes={"db.operation=update","db.table=loans"}
  )
  public int updateStatus(Long id, String status){
    apm.setAttribute("db.query.loanId", String.valueOf(id));
    apm.setAttribute("db.new_status", status);
    int updated = jdbc.update("UPDATE loans SET status=? WHERE id=?", status, id);
    apm.setAttribute("db.rows_affected", updated);
    return updated;
  }
}
