package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.StudentForeignVerification;
import dev.sunbirdrc.claim.entity.StudentGoodStanding;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentGoodStandingRowMapper implements RowMapper<StudentGoodStanding> {
    @Override
    public StudentGoodStanding mapRow(ResultSet rs, int i) throws SQLException {
        return new StudentGoodStanding(
                rs.getString("osid"),
                rs.getString("name"),
                rs.getString("fathersName"),
                rs.getString("presentAddress"),
                rs.getString("phoneNumber"),
                rs.getString("email"),
                rs.getString("trainingCenter"),
                rs.getString("council"),
                rs.getString("workPlace"),
                rs.getString("date"),
                rs.getString("refNo"),
                rs.getString("validityOfRegistration"),
                rs.getString("dob"),
                rs.getString("docproof"),
                rs.getString("candidatePic"),
                rs.getString("marriedName"),
                rs.getString("maidenName"),
                rs.getString("professionalQualification"),
                rs.getString("paymentStatus"),
                rs.getString("registrationNumber"),
                rs.getString("claimType"),
                rs.getString("state"),
                rs.getString("country"),
                rs.getString("pincode"),
                rs.getString("district")
        );
    }
}
