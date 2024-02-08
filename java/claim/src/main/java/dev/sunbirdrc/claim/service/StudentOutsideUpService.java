package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.config.PropertyMapper;
import dev.sunbirdrc.claim.entity.StudentOutsideUP;
import dev.sunbirdrc.claim.exception.ClaimMailException;
import dev.sunbirdrc.claim.exception.InvalidInputException;
import dev.sunbirdrc.claim.model.EntityType;
import dev.sunbirdrc.claim.repository.StudentOutsideUpRowMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang.StringUtils;
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
public class StudentOutsideUpService {
    private static final Logger logger = LoggerFactory.getLogger(StudentForeignVerificationService.class);
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
    public List<StudentOutsideUP> findByOsid(String osid) {
        try {
            return jdbcTemplate.query("SELECT * FROM \"" + propertyMapper.getStudentOutsideVerificationTableName()
                    + "\" where osid=?", new StudentOutsideUpRowMapper(), osid);

        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    /**
     * @return
     */
    public boolean isStudentFromOutsideVerificationTableExist() {
        String sqlQuery = "SELECT count(*) FROM information_schema.tables WHERE table_name = '"
                + propertyMapper.getStudentOutsideVerificationTableName() + "'";

        Integer tableCount = jdbcTemplate.queryForObject(sqlQuery, Integer.class);

        return tableCount > 0;
    }

    /**
     * @param id
     * @return
     */
    public String generateVerificationLinkContent(String id) {
        String processedTemplateString = null;
        List<StudentOutsideUP> studentOutsideUpList = findByOsid(id);

        if (studentOutsideUpList != null && !studentOutsideUpList.isEmpty()) {
            StudentOutsideUP studentOutsideUP = studentOutsideUpList.get(0);

            Map<String, Object> mailMap = new HashMap<>();
            mailMap.put("candidate", studentOutsideUP);
            mailMap.put("entityId", propertyMapper.getRegistryShardId() + "-" + studentOutsideUP.getOsid());
            mailMap.put("claimUrl", propertyMapper.getClaimUrl());

            try {
                freeMarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/templates/");
                Template template = freeMarkerConfiguration.getTemplate("outside-up-student-candidate-details.ftl");
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

    /**
     * @param entityType
     * @param entityId
     * @return
     */
    public String generateVerifyLinkForForeignOutsideStudent(String entityType, String entityId) {
        if (StringUtils.isEmpty(entityType) || StringUtils.isEmpty(entityId)) {
            throw new InvalidInputException("Parameters are invalid");
        }

        if (StringUtils.isEmpty(entityId)) {
            throw new ClaimMailException("Entity id is missing");
        }

        if (entityId.length() > 2 && '-' == entityId.charAt(1)) {
            entityId = entityId.substring(2, entityId.length());
        }

        if (EntityType.StudentForeignVerification.name().equalsIgnoreCase(entityType)) {
            return propertyMapper.getClaimUrl() + "/api/v1/generate/foreignStudentDetails/" + entityId;
        } else if (EntityType.StudentOutsideUP.name().equalsIgnoreCase(entityType)) {
            return propertyMapper.getClaimUrl() + "/api/v1/generate/outsideStudentDetails/" + entityId;
        } else if (EntityType.StudentGoodstanding.name().equalsIgnoreCase(entityType)) {
            return propertyMapper.getClaimUrl() + "/api/v1/generate/studentGoodStandingDetails/" + entityId;
        } else {
            throw new InvalidInputException("Invalid entity type");
        }
    }

}
