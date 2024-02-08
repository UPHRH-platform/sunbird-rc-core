package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.StudentForeignVerification;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentForeignVerificationRowMapper implements RowMapper<StudentForeignVerification> {
    @Override
    public StudentForeignVerification mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new StudentForeignVerification(
                rs.getString("name"),
                rs.getString("registrationNumber"),
                rs.getString("refNo"),
                rs.getString("phoneNumber"),
                rs.getString("osid"),
                rs.getString("fathersName"),
                rs.getString("email"),
                rs.getString("date"),
                rs.getString("council"),
                rs.getString("candidatePic"),
                rs.getString("trainingCenter"),
                rs.getString("workPlace"),
                rs.getString("validityOfRegistration"),
                rs.getString("dob"),
                rs.getString("paymentStatus"),
                rs.getString("claimType"),
                rs.getString("feeReciptNo"),
                rs.getString("state"),
                rs.getString("district"),
                rs.getString("country"),
                rs.getString("pincode")
        );
    }
}
