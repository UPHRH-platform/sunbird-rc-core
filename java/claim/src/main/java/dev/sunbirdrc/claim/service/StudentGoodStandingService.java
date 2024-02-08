package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.config.PropertyMapper;
import dev.sunbirdrc.claim.entity.StudentGoodStanding;
import dev.sunbirdrc.claim.exception.ClaimMailException;
import dev.sunbirdrc.claim.repository.StudentGoodStandingRowMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentGoodStandingService {
    private static final Logger logger = LoggerFactory.getLogger(StudentGoodStandingService.class);
    @Autowired
    private PropertyMapper propertyMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Configuration freeMarkerConfiguration;

    /**
     * @param osid
     * @return
     */
    public List<StudentGoodStanding> findByOsid(String osid) {
        try {
            return jdbcTemplate.query("SELECT * FROM \"" + propertyMapper.getStudentGoodStandingTableName()
                    + "\" where osid=?", new StudentGoodStandingRowMapper(), osid);

        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    /**
     * @return
     */
    public List<StudentGoodStanding> findAll() {
        return jdbcTemplate.query("SELECT * from \"" + propertyMapper.getStudentGoodStandingTableName() + "\"",
                new StudentGoodStandingRowMapper());
    }

    /**
     * @return
     */
    public boolean isSudentGoodStandingTableExist() {
        String sqlQuery = "SELECT count(*) FROM information_schema.tables WHERE table_name = '"
                + propertyMapper.getStudentGoodStandingTableName() + "'";

        Integer tableCount = jdbcTemplate.queryForObject(sqlQuery, Integer.class);

        return tableCount > 0;
    }

    /**
     * @param id
     * @return
     */
    public String generateVerificationLinkContent(String id) {
        String processedTemplateString = null;
        List<StudentGoodStanding> StudentGoodStandingList = findByOsid(id);

        if (StudentGoodStandingList != null && !StudentGoodStandingList.isEmpty()) {
            StudentGoodStanding studentGoodStanding = StudentGoodStandingList.get(0);

            Map<String, Object> mailMap = new HashMap<>();
            mailMap.put("candidate", studentGoodStanding);
            mailMap.put("entityId", propertyMapper.getRegistryShardId() + "-" + studentGoodStanding.getOsid());
            mailMap.put("claimUrl", propertyMapper.getClaimUrl());

            try {
                freeMarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/templates/");
                Template template = freeMarkerConfiguration.getTemplate("student-good-standing-candidate-details.ftl");
                processedTemplateString = FreeMarkerTemplateUtils.processTemplateIntoString(template, mailMap);

            } catch (TemplateException e) {
                logger.error("TemplateException while creating mail template for certificate ", e);
                throw new ClaimMailException("Error while creating mail template for certificate");
            } catch (IOException e) {
                logger.error("IOException while creating mail template for certificate ", e);
                throw new ClaimMailException("Error while creating mail template for certificate");
            }
        }

        return processedTemplateString;
    }

}
