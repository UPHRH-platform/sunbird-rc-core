package dev.sunbirdrc.registry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Optional;
import dev.sunbirdrc.keycloak.OwnerCreationException;
import dev.sunbirdrc.pojos.AsyncRequest;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import dev.sunbirdrc.pojos.Response;
import dev.sunbirdrc.pojos.ResponseParams;
import dev.sunbirdrc.registry.dao.Credential;
import dev.sunbirdrc.registry.dao.Learner;
import dev.sunbirdrc.registry.digilocker.pulldoc.PullDocRequest;
import dev.sunbirdrc.registry.digilocker.pulldoc.PullDocResponse;
import dev.sunbirdrc.registry.digilocker.pulluriresponse.Person;
import dev.sunbirdrc.registry.digilocker.pulluriresponse.PullURIResponse;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.exception.AttestationNotFoundException;
import dev.sunbirdrc.registry.exception.ErrorMessages;
import dev.sunbirdrc.registry.exception.RecordNotFoundException;
import dev.sunbirdrc.registry.exception.UnAuthorizedException;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.middleware.util.OSSystemFields;
import dev.sunbirdrc.registry.model.Document;
import dev.sunbirdrc.registry.model.dto.CourseDetailDTO;
import dev.sunbirdrc.registry.model.dto.MailDto;
import dev.sunbirdrc.registry.model.dto.ManualPendingMailDTO;
import dev.sunbirdrc.registry.service.FileStorageService;
import dev.sunbirdrc.registry.service.impl.CertificateServiceImpl;
import dev.sunbirdrc.registry.transform.Configuration;
import dev.sunbirdrc.registry.transform.Data;
import dev.sunbirdrc.registry.transform.ITransformer;
import dev.sunbirdrc.registry.util.*;
import org.agrona.Strings;
import dev.sunbirdrc.validators.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static dev.sunbirdrc.registry.Constants.*;
import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_TYPE;

@RestController
public class RegistryEntityController extends AbstractController {

    private static final String TRANSACTION_ID = "transactionId";
    @Value("${claims.usrmanageurl:http://localhost:8001/api/v1/keycloak/persist/userCredential}")
    private String REGISTRY_ENDPOINT_SAVE_USERINFO;

    @Value("${keycloak-admin.token_endpoint:http://localhost:8080/auth/realms/sunbird-rc/protocol/openid-connect/token}")
    private String ADMIN_TOKEN_ENDPOINT;
    @Value("${keycloak-admin.user_name:admin}")
    private String ADMIN_USERNAME;
    @Value("${keycloak-admin.client-id:admin-api}")
    private String ADMIN_CLIENTID;
    @Value("${keycloak-admin.client-secret:}")
    private String ADMIN_TOKEN_SECRET;
    @Value("${keycloak-user.default-password:admin@admin}")
    private String DEFAULT_SECRET;
    private static Logger logger = LoggerFactory.getLogger(RegistryEntityController.class);
    @Value("${claims.domain-url}")
    private String claimRequestUrl;
    @Value("${claims.download-path}")
    private String claimDownloadPath;

    @Value("${examination.enabled:false}")
    private Boolean isExamResultEnabled;
    static String static_download_parameter = "?fileName=";
    private static ObjectMapper mapper = new ObjectMapper();
    @Value("${Logo.imgBaseUri:https://casa.upsmfac.org/UploadedFiles/Student/}")
    private String imgBaseUri;

    @Value("${Logo.imgFormat:.jpg}")
    private String imgFormat;
    @Value("${digilocker_hmackey:}")
    public static final String DIGILOCKER_KEY = "84600d73-e618-4c80-a347-6b51147103ee";
    @Autowired
    private CertificateServiceImpl certificateService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AsyncRequest asyncRequest;

    @Autowired
    private ViewTemplateManager viewTemplateManager;

    @Autowired
    private ClaimRequestClient claimRequestClient;

    @Value("${authentication.enabled:true}")
    boolean securityEnabled;

    @Value("${certificate.enableExternalTemplates:false}")
    boolean externalTemplatesEnabled;

    @RequestMapping(value = "/api/v1/{entityName}/invite", method = RequestMethod.POST)
    public ResponseEntity<Object> invite(
            @PathVariable String entityName,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode rootNode,
            HttpServletRequest request
    ) {
        final String TAG = "RegistryController:invite";
        logger.info("Inviting entity {}", rootNode);
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.INVITE, "OK", responseParams);
        Map<String, Object> result = new HashMap<>();
        ObjectNode newRootNode = objectMapper.createObjectNode();
        newRootNode.set(entityName, rootNode);
        try {
            checkEntityNameInDefinitionManager(entityName);
            registryHelper.authorizeInviteEntity(request, entityName);
            watch.start(TAG);

            String entityId = registryHelper.inviteEntity(newRootNode, "");
            // User Persistence in KC - Start
            String userName = rootNode.get("email").asText();
            logger.info("userName:"+userName);
            JsonNode password = rootNode.get("password");
            JsonNode token = rootNode.get("secretToken");
            String name = rootNode.get("name")!=null?rootNode.get("name").asText():"NA";

            String secretToken = null;
            if(password != null){
                logger.info("password:"+password.asText());
                secretToken = password.asText();
            }else if(token!=null){
                logger.info("token:"+token.asText());
                secretToken = token.asText();
            }else{
                secretToken = DEFAULT_SECRET;
                logger.info("DEFAULT_SECRET:"+secretToken);
            }

            logger.info("secretToken-last:"+secretToken);

            // User Persistence in KC - end

            registryHelper.autoRaiseClaim(entityName, entityId, name, null, newRootNode, dev.sunbirdrc.registry.Constants.USER_ANONYMOUS);
            Map resultMap = new HashMap();
            resultMap.put(dbConnectionInfoMgr.getUuidPropertyName(), entityId);
            result.put(entityName, resultMap);
            response.setResult(result);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.start(TAG);
            if(userName!=null && secretToken!=null){
                try {
                    String persistStatus  = persistUserInfo(userName,secretToken,"",entityName);
                }catch (Exception ex){
                    ex.printStackTrace();
                }

            }

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RecordNotFoundException e) {
            createSchemaNotFoundResponse(e.getMessage(), responseParams);
            response = new Response(Response.API_ID.INVITE, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (MiddlewareHaltException | ValidationException | OwnerCreationException e) {
            return badRequestException(responseParams, response, e.getMessage());
        } catch (UnAuthorizedException unAuthorizedException) {
            return createUnauthorizedExceptionResponse(unAuthorizedException);
        } catch (Exception e) {
            if (e.getCause() != null && e.getCause().getCause() != null &&
                    e.getCause().getCause() instanceof InvocationTargetException) {
                Throwable targetException = ((InvocationTargetException) (e.getCause().getCause())).getTargetException();
                if (targetException instanceof OwnerCreationException) {
                    return badRequestException(responseParams, response, targetException.getMessage());
                }
            }
            return internalErrorResponse(responseParams, response, e);
        }
    }

    public String persistUserInfo(final String userName, final String password,String name, final String role) throws IOException {
        logger.debug("saving user info to endpoint {}","UserManagement API");
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(REGISTRY_ENDPOINT_SAVE_USERINFO);
        JsonNode adminToken = getAdminToken();
        String authToken = adminToken.get("access_token").asText();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);

        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
        String requestBody = "{" +
                "\"username\": " + "\"" + userName + "\"" + "," +
                "\"password\": " + "\"" + password + "\"" +"," +
                "\"email\": " + "\"" + userName + "\"" +"," +
                "\"firstName\": " + "\"" + name + "\"" +"," +
                "\"lastName\": " + "\"" + name + "\"" +"," +
                "\"roleName\": " + "\"" + role + "\"" +
                "}";
        logger.debug("payload to save user info with body {} and header {}",requestBody,httpPost.getAllHeaders());
        StringEntity entity = new StringEntity(requestBody);
        httpPost.setEntity(entity);
        org.apache.http.HttpResponse response = httpClient.execute(httpPost);
        logger.debug("Response from server {}",response);
        String responseBody = EntityUtils.toString(response.getEntity());
        //String responseBody = claimRequestClient.persistUserinKeycloak(userDto,HttpMethod.POST,headers);

        return responseBody;
    }

    public JsonNode getAdminToken() throws IOException {
        String tokenEndpoint = ADMIN_TOKEN_ENDPOINT;
        logger.debug("Token endpoint: {}" ,tokenEndpoint);
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(tokenEndpoint);

        String requestBody = "username=" + ADMIN_USERNAME +
                "&grant_type=client_credentials" +
                "&client_id=" + ADMIN_CLIENTID +
                "&client_secret=" + ADMIN_TOKEN_SECRET;

        logger.debug("Request body: {}", requestBody);
        StringEntity entity = new StringEntity(requestBody);

        httpPost.setEntity(entity);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
        logger.debug("headers {}",httpPost);

        org.apache.http.HttpResponse response = httpClient.execute(httpPost);
        org.apache.http.HttpEntity httpEntity = response.getEntity();
        String responseBody = EntityUtils.toString(httpEntity, StandardCharsets.UTF_8);

        if (response.getStatusLine().getStatusCode() == 200) {
            logger.info("Access token obtained successfully.");
            logger.debug("Response: " + responseBody);
        } else {
            logger.info("Failed to obtain access token.");
            logger.debug("Response: " + responseBody);
        }

        logger.debug("Response body: {}", responseBody);
        JsonNode jsonNode = mapper.readTree(responseBody);
        return jsonNode;
    }


    @NotNull
    private void createSchemaNotFoundResponse(String errorMessage, ResponseParams responseParams) {
        responseParams.setStatus(Response.Status.UNSUCCESSFUL);
        responseParams.setErrmsg(errorMessage);
    }

    private void checkEntityNameInDefinitionManager(String entityName) throws RecordNotFoundException {
        if (definitionsManager.getDefinition(entityName) == null) {
            String errorMessage = String.format(ErrorMessages.NOT_PART_OF_THE_SYSTEM_EXCEPTION, entityName);
            throw new RecordNotFoundException(errorMessage);
        }
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.DELETE)
    public ResponseEntity<Object> deleteEntity(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestHeader HttpHeaders header,
            HttpServletRequest request
    ) {

        String userId = USER_ANONYMOUS;
        logger.info("Deleting entityType {} with Id {}", entityName, entityId);
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName)) {
            try {

                userId = registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                return createUnauthorizedExceptionResponse(e);
            }
        }
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.DELETE, "OK", responseParams);
        try {
            checkEntityNameInDefinitionManager(entityName);
            String tag = "RegistryController.delete " + entityName;
            watch.start(tag);
            Vertex deletedEntity = registryHelper.deleteEntity(entityName, entityId, userId);
            if (deletedEntity != null && deletedEntity.keys().contains(OSSystemFields._osSignedData.name())) {
                registryHelper.revokeExistingCredentials(entityName, entityId, userId, deletedEntity.value(OSSystemFields._osSignedData.name()));
            }
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop(tag);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (RecordNotFoundException e) {
            createSchemaNotFoundResponse(e.getMessage(), responseParams);
            response = new Response(Response.API_ID.DELETE, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("RegistryController: Exception while Deleting entity", e);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    @RequestMapping(value = "/api/v1/{entityName}/search", method = RequestMethod.POST)
    public ResponseEntity<Object> searchEntity(@PathVariable String entityName, @RequestHeader HttpHeaders header, @RequestBody ObjectNode searchNode) {

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEARCH, "OK", responseParams);

        try {
            watch.start("RegistryController.searchEntity");
            ArrayNode entity = JsonNodeFactory.instance.arrayNode();
            entity.add(entityName);
            searchNode.set(ENTITY_TYPE, entity);
            checkEntityNameInDefinitionManager(entityName);
            if (definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getEnableSearch()) {
                JsonNode result = registryHelper.searchEntity(searchNode);
                watch.stop("RegistryController.searchEntity");
                return new ResponseEntity<>(result.get(entityName), HttpStatus.OK);
            } else {
                watch.stop("RegistryController.searchEntity");
                logger.error("Searching on entity {} not allowed", entityName);
                response.setResult("");
                responseParams.setStatus(Response.Status.UNSUCCESSFUL);
                responseParams.setErrmsg(String.format("Searching on entity %s not allowed", entityName));
            }
        } catch (RecordNotFoundException e) {
            createSchemaNotFoundResponse(e.getMessage(), responseParams);
            response = new Response(Response.API_ID.SEARCH, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Exception in controller while searching entities !", e);
            response.setResult("");
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.PUT)
    public ResponseEntity<Object> putEntity(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestBody JsonNode rootNode,
            HttpServletRequest request) {

        logger.info("Updating entityType {} request body {}", entityName, rootNode);
        String userId = USER_ANONYMOUS;
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName)) {
            try {

                userId = registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                return createUnauthorizedExceptionResponse(e);
            }
        }
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        ((ObjectNode) rootNode).put(uuidPropertyName, entityId);
        ObjectNode objectNode = (ObjectNode) rootNode;
        String vCertNo = String.valueOf(claimRequestClient.getCertificateNumber());
        objectNode.put("certificateNumber", vCertNo);
        objectNode.put("certificateNo", vCertNo);
        objectNode.put("validityUpto", DigiLockerUtils.getValidityDate());
        objectNode.put("nurseRegDate", DigiLockerUtils.getCurrentDate());
        ObjectNode newRootNode = objectMapper.createObjectNode();
        newRootNode.set(entityName, rootNode);

        try {
            checkEntityNameInDefinitionManager(entityName);
            String tag = "RegistryController.update " + entityName;
            watch.start(tag);
            JsonNode existingNode = registryHelper.readEntity(newRootNode, userId);

            try {
                JsonNode university = existingNode.get(entityName).get("university");
                logger.info(">>>>>>>>>>>>>>>>>>>> university : " + university.asText());
            } catch (Exception e) {
                logger.error("Error while fetching university: ", e);
            }

//            JsonNode university = existingNode.get(entityName).get("university");
//            if(university == null || (university !=null && university.asText()==null)){
//                objectNode.put("university","NA");
//                logger.info("value for university is null");
//            } else {
//                logger.info("value for university is ::"+university.asText());
//            }

            String emailId = registryHelper.fetchEmailIdFromToken(request, entityName);
            registryHelper.updateEntityAndState(existingNode, newRootNode, userId);
            if (existingNode.get(entityName).has(OSSystemFields._osSignedData.name())) {
                registryHelper.revokeExistingCredentials(entityName, entityId, userId,
                        existingNode.get(entityName).get(OSSystemFields._osSignedData.name()).asText(""));
            }
            registryHelper.invalidateAttestation(entityName, entityId, userId, null);
            registryHelper.autoRaiseClaim(entityName, entityId, userId, existingNode, newRootNode, emailId);
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop(tag);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (RecordNotFoundException e) {
             createSchemaNotFoundResponse(e.getMessage(), responseParams);
            response = new Response(Response.API_ID.PUT, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("RegistryController: Exception while updating entity (without id)!", e);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @RequestMapping(value = "/api/v1/{entityName}", method = RequestMethod.POST)
    public ResponseEntity<Object> postEntity(
            @PathVariable String entityName,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode rootNode,
            @RequestParam(defaultValue = "sync") String mode,
            @RequestParam(defaultValue = "${webhook.url}") String callbackUrl,
            HttpServletRequest request
    ) {

        logger.info("MODE: {}", asyncRequest.isEnabled());
        logger.info("MODE: {}", asyncRequest.getWebhookUrl());
        logger.info("Adding entity {}", rootNode);
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.POST, "OK", responseParams);
        Map<String, Object> result = new HashMap<>();
        ObjectNode newRootNode = objectMapper.createObjectNode();
        ObjectNode objectNode = (ObjectNode) rootNode;
        objectNode.put("certificateNumber", String.valueOf(claimRequestClient.getCertificateNumber()));

        logger.info("certificate number: " + String.valueOf(claimRequestClient.getCertificateNumber()));
        logger.info("certificate number by object Node: " + objectNode.get("certificateNumber"));

//        JsonNode university = objectNode.get("university");
//        if (university == null || (university != null && (university.asText() == null || !university.asText().equalsIgnoreCase("NA")))) {
//            objectNode.put("university", "NA");
//            logger.info("value for university is null");
//        } else {
//            logger.info("value for university is ::" + university.asText());
//        }

        objectNode.put("validityUpto", DigiLockerUtils.getValidityDate());
        objectNode.put("nurseRegDate", DigiLockerUtils.getCurrentDate());
        newRootNode.set(entityName, rootNode);
        try {
            checkEntityNameInDefinitionManager(entityName);
            String userId = registryHelper.authorizeManageEntity(request, entityName);
            String label = registryHelper.addEntity(newRootNode, userId);
            String emailId = registryHelper.fetchEmailIdFromToken(request, entityName);
            Map<String, String> resultMap = new HashMap<>();
            if (asyncRequest.isEnabled()) {
                resultMap.put(TRANSACTION_ID, label);
            } else {
                registryHelper.autoRaiseClaim(entityName, label, userId, null, newRootNode, emailId);
                resultMap.put(dbConnectionInfoMgr.getUuidPropertyName(), label);
            }
            result.put(entityName, resultMap);
            response.setResult(result);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop("RegistryController.addToExistingEntity");

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RecordNotFoundException e) {
            createSchemaNotFoundResponse(e.getMessage(), responseParams);
            response = new Response(Response.API_ID.POST, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (MiddlewareHaltException e) {
            logger.info("Error in validating the request");
            return badRequestException(responseParams, response, e.getMessage());
        } catch (Exception e) {
            logger.error("Exception in controller while adding entity !", e);
            response.setResult(result);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/api/v1/diploma/{entityName}", method = RequestMethod.POST)
    public ResponseEntity<Object> postEntityDiploma(
            @PathVariable String entityName,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode rootNode,
            @RequestParam(defaultValue = "sync") String mode,
            @RequestParam(defaultValue = "${webhook.url}") String callbackUrl,
            HttpServletRequest request
    ) {

        logger.info("MODE: {}", asyncRequest.isEnabled());
        logger.info("MODE: {}", asyncRequest.getWebhookUrl());
        logger.info("Adding entity {}", rootNode);
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.POST, "OK", responseParams);
        Map<String, Object> result = new HashMap<>();
        ObjectNode newRootNode = objectMapper.createObjectNode();
        ObjectNode objectNode = (ObjectNode) rootNode;
        //if(entityName.equals("DiplomaFromUP")){
            objectNode.put("certificateNumber",String.valueOf(claimRequestClient.getCertificateNumber()));
            if(objectNode.get("university")==null)
                objectNode.put("university","NA");
            //if(objectNode.get("validityUpto")==null) {
                objectNode.put("validityUpto", DigiLockerUtils.getValidityDate());
            //}
        //}
        extractImgUrl(rootNode);
        newRootNode.set(entityName, rootNode);
        try {
            checkEntityNameInDefinitionManager(entityName);
            String userId = registryHelper.authorizeManageEntity(request, entityName);
            String label = registryHelper.addEntity(newRootNode, userId);
            String emailId = registryHelper.fetchEmailIdFromToken(request, entityName);
            Map<String, String> resultMap = new HashMap<>();
            if (asyncRequest.isEnabled()) {
                resultMap.put(TRANSACTION_ID, label);
            } else {
                registryHelper.autoRaiseClaim(entityName, label, userId, null, newRootNode, emailId);
                resultMap.put(dbConnectionInfoMgr.getUuidPropertyName(), label);
            }
            result.put(entityName, resultMap);
            response.setResult(result);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop("RegistryController.addToExistingEntity");

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RecordNotFoundException e) {
            createSchemaNotFoundResponse(e.getMessage(), responseParams);
            response = new Response(Response.API_ID.POST, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (MiddlewareHaltException e) {
            logger.info("Error in validating the request");
            return badRequestException(responseParams, response, e.getMessage());
        } catch (Exception e) {
            logger.error("Exception in controller while adding entity !", e);
            response.setResult(result);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @RequestMapping(value = "/api/v1/{entityName}/{entityId}/**", method = RequestMethod.PUT)
    public ResponseEntity<Object> updatePropertyOfTheEntity(
            HttpServletRequest request,
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestBody JsonNode requestBody

    ) {
        String userId = USER_ANONYMOUS;
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName)) {
            try {
                userId = registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                return createUnauthorizedExceptionResponse(e);
            }
        }
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);

        try {
            checkEntityNameInDefinitionManager(entityName);
            String tag = "RegistryController.update " + entityName;
            watch.start(tag);
            requestBody = registryHelper.removeFormatAttr(requestBody);
            JsonNode existingNode = registryHelper.readEntity(userId, entityName, entityId, false, null, false);
            registryHelper.updateEntityProperty(entityName, entityId, requestBody, request, existingNode);
            if (existingNode.get(entityName).has(OSSystemFields._osSignedData.name())) {
                registryHelper.revokeExistingCredentials(entityName, entityId, userId,
                        existingNode.get(entityName).get(OSSystemFields._osSignedData.name()).asText(""));
            }
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            registryHelper.invalidateAttestation(entityName, entityId, userId, registryHelper.getPropertyToUpdate(request, entityId));
            watch.stop(tag);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RecordNotFoundException e) {
             createSchemaNotFoundResponse(e.getMessage(), responseParams);
            response = new Response(Response.API_ID.PUT, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            responseParams.setErrmsg(e.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}/**", method = RequestMethod.POST)
    public ResponseEntity<Object> addNewPropertyToTheEntity(
            HttpServletRequest request,
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode requestBody
    ) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        try {
            checkEntityNameInDefinitionManager(entityName);
            registryHelper.authorize(entityName, entityId, request);
        } catch (RecordNotFoundException e) {
           createSchemaNotFoundResponse(e.getMessage(), responseParams);
           response = new Response(Response.API_ID.POST, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createUnauthorizedExceptionResponse(e);
        }

        try {

            String tag = "RegistryController.addNewPropertyToTheEntity " + entityName;
            watch.start(tag);
            String notes = getNotes(requestBody);
            requestBody = registryHelper.removeFormatAttr(requestBody);
            registryHelper.addEntityProperty(entityName, entityId, requestBody, request);
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop(tag);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            responseParams.setErrmsg(e.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    private String getNotes(JsonNode requestBody) {
        String notes = "";
        if (requestBody.has("notes")) {
            notes = requestBody.get("notes").asText();
            JSONUtil.removeNodes(requestBody, Collections.singletonList("notes"));
        }
        return notes;
    }

    private JsonNode getAttestationSignedData(String attestationId, JsonNode node) throws AttestationNotFoundException, JsonProcessingException {
        JsonNode attestationNode = getAttestationNode(attestationId, node);
        if (attestationNode.get(OSSystemFields._osAttestedData.name()) == null)
            throw new AttestationNotFoundException();
        attestationNode = objectMapper.readTree(attestationNode.get(OSSystemFields._osAttestedData.name()).asText());
        return attestationNode;
    }

    @Nullable
    private JsonNode getAttestationNode(String attestationId, JsonNode node) {
        Iterator<JsonNode> iterator = node.iterator();
        JsonNode attestationNode = null;
        while (iterator.hasNext()) {
            attestationNode = iterator.next();
            JsonNode jsonNode = attestationNode.get(uuidPropertyName);
            if (jsonNode!=null && jsonNode.toString().equals(attestationId)) {
                break;
            }
        }
        return attestationNode;
    }

    @RequestMapping(value = "/partner/api/v1/{entityName}", method = RequestMethod.GET)
    public ResponseEntity<Object> getEntityWithConsent(
            @PathVariable String entityName,
            HttpServletRequest request) {
        ResponseParams responseParams = new ResponseParams();
        try {
            checkEntityNameInDefinitionManager(entityName);
            ArrayList<String> fields = getConsentFields(request);
            JsonNode userInfoFromRegistry = registryHelper.getRequestedUserDetails(request, entityName);
            JsonNode jsonNode = userInfoFromRegistry.get(entityName);
            if (jsonNode instanceof ArrayNode) {
                ArrayNode values = (ArrayNode) jsonNode;
                if (values.size() > 0) {
                    JsonNode node = values.get(0);
                    if (node instanceof ObjectNode) {
                        ObjectNode entityNode = copyWhiteListedFields(fields, node);
                        return new ResponseEntity<>(entityNode, HttpStatus.OK);
                    }
                }
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RecordNotFoundException e) {
             createSchemaNotFoundResponse(e.getMessage(), responseParams);
           Response response = new Response(Response.API_ID.GET, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error in partner api access", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/api/v2/{entityName}", method = RequestMethod.GET)
    public ResponseEntity<Object> getUserDetails(
            @PathVariable String entityName,
            HttpServletRequest request) {
        ResponseParams responseParams = new ResponseParams();
        try {
            checkEntityNameInDefinitionManager(entityName);
            ArrayList<String> fields = getConsentFields(request);
            JsonNode userInfoFromRegistry = registryHelper.getRequestedUserDetails(request, entityName);
            JsonNode jsonNode = userInfoFromRegistry.get(entityName);
            if (jsonNode instanceof ArrayNode) {
                ArrayNode values = (ArrayNode) jsonNode;
                if (values.size() > 0) {
                    JsonNode node = values.get(0);
                    if (node instanceof ObjectNode) {
                        ObjectNode entityNode = copyWhiteListedFields(fields, node);
                        return new ResponseEntity<>(entityNode, HttpStatus.OK);
                    }
                }
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RecordNotFoundException e) {
            createSchemaNotFoundResponse(e.getMessage(), responseParams);
            Response response = new Response(Response.API_ID.GET, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error in partner api access", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ObjectNode copyWhiteListedFields(ArrayList<String> fields, JsonNode dataNode) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        for (String key : fields) {
            node.set(key, dataNode.get(key));
        }
        return node;
    }

    private ArrayList<String> getConsentFields(HttpServletRequest request) {
        ArrayList<String> fields = new ArrayList<>();
        KeycloakAuthenticationToken principal = (KeycloakAuthenticationToken) request.getUserPrincipal();
        try {
            Map<String, Object> otherClaims = ((KeycloakPrincipal) principal.getPrincipal()).getKeycloakSecurityContext().getToken().getOtherClaims();
            if (otherClaims.keySet().contains(dev.sunbirdrc.registry.Constants.KEY_CONSENT) && otherClaims.get(dev.sunbirdrc.registry.Constants.KEY_CONSENT) instanceof Map) {
                Map consentFields = (Map) otherClaims.get(dev.sunbirdrc.registry.Constants.KEY_CONSENT);
                for (Object key : consentFields.keySet()) {
                    fields.add(key.toString());
                }
            }
        } catch (Exception ex) {
            logger.error("Error while extracting other claims", ex);
        }
        return fields;
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.GET, produces =
            {MediaType.APPLICATION_PDF_VALUE, MediaType.TEXT_HTML_VALUE, Constants.SVG_MEDIA_TYPE})
    public ResponseEntity<Object> getEntityType(@PathVariable String entityName,
                                                @PathVariable String entityId,
                                                HttpServletRequest request,
                                                @RequestHeader(required = false) String viewTemplateId) {
        ResponseParams responseParams = new ResponseParams();
        Object certificateWebCopy = null;
        Object certificateOriginal = null;
        Response response;
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName) && securityEnabled) {
            try {

                registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                try {
                    checkEntityNameInDefinitionManager(entityName);
                    registryHelper.authorizeAttestor(entityName, request);
                } catch (RecordNotFoundException re) {
                    createSchemaNotFoundResponse(re.getMessage(), responseParams);
                    response = new Response(Response.API_ID.GET, "ERROR", responseParams);
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                } catch (Exception exceptionFromAuthorizeAttestor) {
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
            }
        }
        try {
            String checkIfAlreadyExists = "issuance/" + entityId + ".pdf";
            certificateOriginal = certificateService.getCred(checkIfAlreadyExists);

            String readerUserId = getUserId(entityName, request);
            JsonNode node = registryHelper.readEntity(readerUserId, entityName, entityId, false,
                            viewTemplateManager.getViewTemplateById(viewTemplateId), false)
                    .get(entityName);

            String courseName = node.get("courseName").asText();
            String templateUrlFromRequest = getTemplateUrlFromRequestFromRegType(request, entityName, courseName);
            String fileName = entityId;
            JsonNode signedNode = objectMapper.readTree(node.get(OSSystemFields._osSignedData.name()).asText());
            if ((certificateOriginal == null)) {
                String fileName1 = fileName + "webcopy";
                String url = "https://storage.googleapis.com/dev-public-upsmf/issuance/"+fileName1+".pdf";
                certificateWebCopy = certificateService.getCertificate(signedNode,
                        entityName,
                        entityId,
                        request.getHeader(HttpHeaders.ACCEPT),
                        templateUrlFromRequest.replace(".html", "-WC.html"),
                        JSONUtil.removeNodesByPath(node, definitionsManager.getExcludingFieldsForEntity(entityName)), url, false
                );

                String fileUrlForQR = url;
                //getFileUrl(fileName1);
                if (certificateWebCopy != null) {
                    url = certificateService.saveToGCS(certificateWebCopy, fileName1);
                    logger.debug("WebCopy of Certificate:" + url);
                }
                certificateOriginal = certificateService.getCertificate(signedNode,
                        entityName,
                        entityId,
                        request.getHeader(HttpHeaders.ACCEPT),
                        templateUrlFromRequest,
                        JSONUtil.removeNodesByPath(node, definitionsManager.getExcludingFieldsForEntity(entityName)), url, false
                );
                if (certificateOriginal != null) {
                    String originalUrl = getCredUrl(fileName, certificateOriginal);
                    if (originalUrl != null) {
                        shareCredentials(signedNode, originalUrl);
                    }
                }

            }
            return new ResponseEntity<>(certificateOriginal, HttpStatus.OK);
        } catch (Exception exception) {
            exception.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * @param entityName
     * @param entityId
     * @param request
     * @param viewTemplateId
     * @return
     */
    @RequestMapping(value = "/api/v3/{entityName}/{entityId}", method = RequestMethod.GET, produces =
            {MediaType.APPLICATION_PDF_VALUE, MediaType.TEXT_HTML_VALUE, Constants.SVG_MEDIA_TYPE})
    public ResponseEntity<Object> getEntityDownload(@PathVariable String entityName,
                                                @PathVariable String entityId,
                                                HttpServletRequest request,
                                                @RequestHeader(required = false) String viewTemplateId) {
        ResponseParams responseParams = new ResponseParams();
        Object certificateWebCopy = null;
        Object certificateOriginal = null;
        Response response;
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName) && securityEnabled) {
            try {

                registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                try {
                    checkEntityNameInDefinitionManager(entityName);
                    registryHelper.authorizeAttestor(entityName, request);
                } catch (RecordNotFoundException re) {
                    createSchemaNotFoundResponse(re.getMessage(), responseParams);
                    response = new Response(Response.API_ID.GET, "ERROR", responseParams);
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                } catch (Exception exceptionFromAuthorizeAttestor) {
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
            }
        }
        try {
            String checkIfAlreadyExists = "issuance/" + entityId + ".pdf";
            certificateOriginal = certificateService.getCred(checkIfAlreadyExists);

            String readerUserId = getUserId(entityName, request);
            JsonNode node = registryHelper.readEntity(readerUserId, entityName, entityId, false,
                            viewTemplateManager.getViewTemplateById(viewTemplateId), false)
                    .get(entityName);

            String courseType = "DIPLOMA";
            String councilName = node.get("council") != null ? node.get("council").asText() : null;
            String courseName = node.get("courseName") != null ? node.get("courseName").asText() : null;
            String activityName = node.get("activityName") != null ? node.get("activityName").asText() : null;

            CourseDetailDTO courseDetailDTO = CourseDetailDTO.builder()
                    .entityName(entityName)
                    .courseType(courseType)
                    .councilName(councilName)
                    .courseName(courseName)
                    .activityName(activityName)
                    .build();

            String templateUrlFromRequest = getTemplateUrlFromRequest(request, courseDetailDTO);
            String fileName = entityId;
            JsonNode signedNode = objectMapper.readTree(node.get(OSSystemFields._osSignedData.name()).asText());
            if ((certificateOriginal == null)) {
                String fileName1 = fileName + "webcopy";
                String url = "https://storage.googleapis.com/dev-public-upsmf/issuance/"+fileName1+".pdf";
                certificateWebCopy = certificateService.getCertificate(signedNode,
                        entityName,
                        entityId,
                        request.getHeader(HttpHeaders.ACCEPT),
                        templateUrlFromRequest.replace(".html", "-WC.html"),
                        JSONUtil.removeNodesByPath(node, definitionsManager.getExcludingFieldsForEntity(entityName)), url, false
                );

                String fileUrlForQR = url;
                //getFileUrl(fileName1);
                if (certificateWebCopy != null) {
                    url = certificateService.saveToGCS(certificateWebCopy, fileName1);
                    logger.debug("WebCopy of Certificate:" + url);
                }
                certificateOriginal = certificateService.getCertificate(signedNode,
                        entityName,
                        entityId,
                        request.getHeader(HttpHeaders.ACCEPT),
                        templateUrlFromRequest,
                        JSONUtil.removeNodesByPath(node, definitionsManager.getExcludingFieldsForEntity(entityName)), url, false
                );
                if (certificateOriginal != null) {
                    String originalUrl = getCredUrl(fileName, certificateOriginal);
                    if (originalUrl != null) {
                        shareCredentials(signedNode, originalUrl);
                    }
                }

            }
            return new ResponseEntity<>(certificateOriginal, HttpStatus.OK);
        } catch (Exception exception) {
            exception.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private String getTemplateUrlFromRequestFromRegType(HttpServletRequest request, String entityName, String courseName) {
        String template = Template;

        if(entityName.equalsIgnoreCase("StudentForeignVerification")){
            return "https://raw.githubusercontent.com/kumarpawantarento/templates/main/Foreign-certificate.html";
        } else if (entityName.equalsIgnoreCase("StudentGoodstanding")) {
            return "https://raw.githubusercontent.com/kumarpawantarento/templates/main/GoodStanding.html";
        }

        if (externalTemplatesEnabled && !StringUtils.isEmpty(request.getHeader(template))) {
            return request.getHeader(template);
        }

        String templateKey = claimRequestClient.getTemplateKey(courseName);

        logger.info("template key for courseName::"+courseName + ":" + templateKey);

        if (definitionsManager.getCertificateTemplates(entityName) != null && definitionsManager.getCertificateTemplates(entityName).size() > 0) {
            String templateUri = definitionsManager.getCertificateTemplates(entityName).getOrDefault(templateKey, null);
            if (!StringUtils.isEmpty(templateUri)) {
                try {
                    if (templateUri.startsWith(MINIO_URI_PREFIX)) {
                        return fileStorageService.getSignedUrl(templateUri.substring(MINIO_URI_PREFIX.length()));
                    } else if (templateUri.startsWith(HTTP_URI_PREFIX) || templateUri.startsWith(HTTPS_URI_PREFIX)) {
                        return templateUri;
                    }
                } catch (Exception e) {
                    logger.error("Exception while parsing certificate templates DID urls", e);
                    return null;
                }
            }

        }
        return null;
    }

    /**
     * @param request
     * @param entityName
     * @param courseName
     * @return
     */
    private String getTemplateUrlFromRequest(HttpServletRequest request, @NotNull CourseDetailDTO courseDetailDTO) {
        String template = Template;

        if ("StudentForeignVerification".equalsIgnoreCase(courseDetailDTO.getEntityName())) {
            return "https://raw.githubusercontent.com/kumarpawantarento/templates/main/Foreign-certificate-new.html";
        }

        if (externalTemplatesEnabled && !StringUtils.isEmpty(request.getHeader(template))) {
            return request.getHeader(template);
        }

        String templateKey = claimRequestClient.getCourseTemplateKey(courseDetailDTO);

        logger.info("template key for courseName::"+courseDetailDTO.getCourseName() + ":" + templateKey);

        if (definitionsManager.getCertificateTemplates(courseDetailDTO.getEntityName()) != null && definitionsManager.getCertificateTemplates(courseDetailDTO.getEntityName()).size() > 0) {
            String templateUri = definitionsManager.getCertificateTemplates(courseDetailDTO.getEntityName()).getOrDefault(templateKey, null);
            if (!StringUtils.isEmpty(templateUri)) {
                try {
                    if (templateUri.startsWith(MINIO_URI_PREFIX)) {
                        return fileStorageService.getSignedUrl(templateUri.substring(MINIO_URI_PREFIX.length()));
                    } else if (templateUri.startsWith(HTTP_URI_PREFIX) || templateUri.startsWith(HTTPS_URI_PREFIX)) {
                        return templateUri;
                    }
                } catch (Exception e) {
                    logger.error("Exception while parsing certificate templates DID urls", e);
                    return null;
                }
            }

        }
        return null;
    }

    private String getTemplateUrlFromRequest(HttpServletRequest request, String entityName) {
        if (externalTemplatesEnabled && !StringUtils.isEmpty(request.getHeader(Template))) {
            return request.getHeader(Template);
        }
        if (definitionsManager.getCertificateTemplates(entityName) != null && definitionsManager.getCertificateTemplates(entityName).size() > 0 && !StringUtils.isEmpty(request.getHeader(TemplateKey))) {
            String templateUri = definitionsManager.getCertificateTemplates(entityName).getOrDefault(request.getHeader(TemplateKey), null);
            if (!StringUtils.isEmpty(templateUri)) {
                try {
                    if (templateUri.startsWith(MINIO_URI_PREFIX)) {
                        return fileStorageService.getSignedUrl(templateUri.substring(MINIO_URI_PREFIX.length()));
                    } else if (templateUri.startsWith(HTTP_URI_PREFIX) || templateUri.startsWith(HTTPS_URI_PREFIX)) {
                        return templateUri;
                    }
                } catch (Exception e) {
                    logger.error("Exception while parsing certificate templates DID urls", e);
                    return null;
                }
            }

        }
        return null;
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.GET)
    public ResponseEntity<Object> getEntity(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestHeader HttpHeaders header, HttpServletRequest request,
            @RequestHeader(required = false) String viewTemplateId) {
        boolean requireLDResponse = false;
        boolean requireVCResponse = false;
        for (MediaType t : header.getAccept()) {
            if (t.toString().equals(Constants.LD_JSON_MEDIA_TYPE)) {
                requireLDResponse = true;
                break;
            } else if (t.toString().equals(Constants.VC_JSON_MEDIA_TYPE)) {
                requireVCResponse = true;
            }
        }
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName) && securityEnabled) {
            try {
                registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                try {
                    registryHelper.authorizeAttestor(entityName, request);
                } catch (Exception exceptionFromAuthorizeAttestor) {
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
            }
        }
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.READ, "OK", responseParams);
        try {
            checkEntityNameInDefinitionManager(entityName);
            String readerUserId = getUserId(entityName, request);
            JsonNode node = getEntityJsonNode(entityName, entityId, requireLDResponse, readerUserId, viewTemplateId);
            if (requireLDResponse) {
                addJsonLDSpec(node);
            } else if (requireVCResponse) {
                String vcString = node.get(OSSystemFields._osSignedData.name()).textValue();
                return new ResponseEntity<>(vcString, HttpStatus.OK);
            }
            return new ResponseEntity<>(node, HttpStatus.OK);

        } catch (RecordNotFoundException re) {
             createSchemaNotFoundResponse(re.getMessage(), responseParams);
            response = new Response(Response.API_ID.GET, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Read Api Exception occurred ", e);
            responseParams.setErrmsg(e.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getUserId(String entityName, HttpServletRequest request) throws Exception {
        return registryHelper.getUserId(request, entityName);
    }

    private void addJsonLDSpec(JsonNode node) {

    }

    private JsonNode getEntityJsonNode(@PathVariable String entityName, @PathVariable String entityId,
                                       boolean requireLDResponse, String userId, String viewTemplateId) throws Exception {
        JsonNode resultNode = registryHelper.readEntity(userId, entityName, entityId, false,
                viewTemplateManager.getViewTemplateById(viewTemplateId), false);
        Data<Object> data = new Data<>(resultNode);
        Configuration config = configurationHelper.getResponseConfiguration(requireLDResponse);
        ITransformer<Object> responseTransformer = transformer.getInstance(config);
        Data<Object> resultContent = responseTransformer.transform(data);
        logger.info("ReadEntity,{},{}", entityId, resultContent);
        if (!(resultContent.getData() instanceof JsonNode)) {
            throw new RuntimeException("Unknown response object " + resultContent);
        }
        JsonNode node = (JsonNode) resultContent.getData();
        JsonNode entityNode = node.get(entityName);
        return entityNode != null ? entityNode : node;
    }

    @RequestMapping(value = "/api/v1/{entityName}", method = RequestMethod.GET)
    public ResponseEntity<Object> getEntityByToken(@PathVariable String entityName, HttpServletRequest request,
                                                   @RequestHeader(required = false) String viewTemplateId) throws RecordNotFoundException {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.GET, "OK", responseParams);
        ResponseEntity<Object> objectResponseEntity = getObjectResponseEntity(entityName, request, viewTemplateId, responseParams, response);
        return objectResponseEntity;
    }


    @NotNull
    private ResponseEntity<Object> getObjectResponseEntity(String entityName, HttpServletRequest request, String viewTemplateId, ResponseParams responseParams, Response response) {
        try {
            checkEntityNameInDefinitionManager(entityName);
            String userId = registryHelper.getUserId(request, entityName);
            if (!Strings.isEmpty(userId)) {
                JsonNode responseFromDb = registryHelper.searchEntitiesByUserId(entityName, userId, viewTemplateId);
                JsonNode entities = responseFromDb.get(entityName);
                if (entities.size() > 0) {
                    return new ResponseEntity<>(entities, HttpStatus.OK);
                } else {
                    responseParams.setErrmsg("No record found");
                    responseParams.setStatus(Response.Status.UNSUCCESSFUL);
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }
            } else {
                responseParams.setErrmsg("User id is empty");
                responseParams.setStatus(Response.Status.UNSUCCESSFUL);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        } catch (RecordNotFoundException e) {
             createSchemaNotFoundResponse(e.getMessage(), responseParams);
            response = new Response(Response.API_ID.GET, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Exception in controller while searching entities !", e);
            response.setResult("");
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }

    //TODO: check the usage and deprecate the api if not used
    @GetMapping(value = "/api/v1/{entity}/{entityId}/attestationProperties")
    public ResponseEntity<Object> getEntityForAttestation(
            @PathVariable String entity,
            @PathVariable String entityId
    ) {
         ResponseParams responseParams = new ResponseParams();
        try {
            JsonNode resultNode = registryHelper.readEntity("", entity, entityId, false, null, false);
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.set("entity", resultNode.get(entity));
            checkEntityNameInDefinitionManager(entity);
            List<AttestationPolicy> attestationPolicies = definitionsManager.getDefinition(entity)
                    .getOsSchemaConfiguration()
                    .getAttestationPolicies();
            objectNode.set("attestationPolicies", objectMapper.convertValue(attestationPolicies, JsonNode.class));
            return new ResponseEntity<>(objectNode, HttpStatus.OK);

        } catch (RecordNotFoundException re) {
             createSchemaNotFoundResponse(re.getMessage(), responseParams);
            Response response = new Response(Response.API_ID.GET, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    //TODO: check the usage and deprecate the api if not used
    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.PATCH)
    public ResponseEntity<Object> attestEntity(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode rootNode
    ) throws Exception {
        ResponseParams responseParams = new ResponseParams();
        try {
            checkEntityNameInDefinitionManager(entityName);
        } catch (RecordNotFoundException re) {
             createSchemaNotFoundResponse(re.getMessage(),responseParams);
            Response response = new Response(Response.API_ID.PATCH, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        logger.info("Attestation request for {}", rootNode.get("fieldPaths"));
        JsonNode nodePath = rootNode.get("jsonPaths");
        if (nodePath instanceof ArrayNode) {
            Iterator<JsonNode> elements = ((ArrayNode) nodePath).elements();
            ArrayList<String> paths = new ArrayList<>();
            for (Iterator<JsonNode> it = elements; it.hasNext(); ) {
                JsonNode e = it.next();
                paths.add(e.textValue());
            }
            JsonNode node = registryHelper.readEntity("admin", entityName, entityId, false, null, false);
            registryHelper.attestEntity(entityName, node, paths.toArray(new String[]{}), "admin");
        }
        return null;
    }

    //TODO: check the usage and deprecate the api if not used
    @RequestMapping(value = "/api/v1/system/{property}/{propertyId}", method = RequestMethod.POST)
    public ResponseEntity<ResponseParams> updateProperty(
            @PathVariable String property,
            @PathVariable String propertyId,
            @RequestBody JsonNode requestBody) {
        logger.info("Got system request for the property {} {}", property, propertyId);
        ((ObjectNode) requestBody).put(uuidPropertyName, propertyId);
        ObjectNode newRootNode = objectMapper.createObjectNode();

        ResponseParams responseParams = new ResponseParams();
        newRootNode.set(property, requestBody);
        try {
            String response = registryHelper.updateProperty(newRootNode, "");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            responseParams.setResultList(Collections.singletonList(response));
            return new ResponseEntity<>(responseParams, HttpStatus.OK);
        } catch (Exception exception) {
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(exception.getMessage());
            exception.printStackTrace();
            return new ResponseEntity<>(responseParams, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //TODO: API called by claim-ms, need to be blocked from external access
    @RequestMapping(value = "/api/v1/{property}/{propertyId}/attestation/{attestationName}/{attestationId}", method = RequestMethod.PUT)
    public ResponseEntity<ResponseParams> updateAttestationProperty(
            @PathVariable String property,
            @PathVariable String propertyId,
            @PathVariable String attestationName,
            @PathVariable String attestationId,
            @RequestBody JsonNode requestBody) {
        logger.info("Got system request to update attestation property {} {} {} {}", property, propertyId, attestationName, attestationId);
        ((ObjectNode) requestBody).put(uuidPropertyName, propertyId);
        ObjectNode newRootNode = objectMapper.createObjectNode();

        ResponseParams responseParams = new ResponseParams();
        newRootNode.set(property, requestBody);
        try {
            logger.info("updateAttestationProperty: {}", requestBody);
            PluginResponseMessage pluginResponseMessage = objectMapper.convertValue(requestBody, PluginResponseMessage.class);
            registryHelper.updateState(pluginResponseMessage);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            responseParams.setResultList(Collections.singletonList("response"));
            return new ResponseEntity<>(responseParams, HttpStatus.OK);
        } catch (Exception exception) {
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(exception.getMessage());
            exception.printStackTrace();
            return new ResponseEntity<>(responseParams, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Deprecated
    @RequestMapping(value = "/api/v1/{entityName}/sign", method = RequestMethod.GET)
    public ResponseEntity<Object> getSignedEntityByToken(@PathVariable String entityName, HttpServletRequest request) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEARCH, "OK", responseParams);
        try {
            checkEntityNameInDefinitionManager(entityName);
            JsonNode result = registryHelper.getRequestedUserDetails(request, entityName);
            if (result.get(entityName).size() > 0) {
                Object credentialTemplate = definitionsManager.getCredentialTemplate(entityName);
                Object signedCredentials = registryHelper.getSignedDoc(result.get(entityName).get(0), credentialTemplate);
                return new ResponseEntity<>(signedCredentials, HttpStatus.OK);
            } else {
                responseParams.setErrmsg("Entity not found");
                responseParams.setStatus(Response.Status.UNSUCCESSFUL);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        } catch (RecordNotFoundException re) {
             createSchemaNotFoundResponse(re.getMessage(), responseParams);
            response = new Response(Response.API_ID.GET, "ERROR", responseParams);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Exception in controller while searching entities !", e);
            response.setResult("");
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/api/v1/{entityName}/{entityId}/attestation/{attestationName}/{attestationId}",
            produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.TEXT_HTML_VALUE, Constants.SVG_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> getAttestationCertificate(HttpServletRequest request, @PathVariable String entityName, @PathVariable String entityId,
                                                            @PathVariable String attestationName, @PathVariable String attestationId) {
        ResponseParams responseParams = new ResponseParams();
        Object certificate = null;
        boolean wc = false;
        try {
            checkEntityNameInDefinitionManager(entityName);
            String readerUserId = getUserId(entityName, request);
            JsonNode jsonNode = registryHelper.readEntity(readerUserId, entityName, entityId, false, null, false)
                    .get(entityName);
            JsonNode node = jsonNode.get(attestationName);
            String fileName = getFileNameOfCredentials(node);
            JsonNode credentialsType = node.get("credType");
            //generateImageURL(jsonNode);
            JsonNode attestationNode = getAttestationSignedData(attestationId, node);
            certificate = certificateService.getCertificate(attestationNode,
                    entityName,
                    entityId,
                    request.getHeader(HttpHeaders.ACCEPT),
                    getTemplateUrlFromRequest(request, entityName),
                    getAttestationNode(attestationId, node),
                    fileName, wc
            );
            return new ResponseEntity<>(certificate, HttpStatus.OK);
        } catch (RecordNotFoundException re) {
            createSchemaNotFoundResponse(re.getMessage(), responseParams);
            Response response = new Response(Response.API_ID.GET, "ERROR", responseParams);
            try {
                return new ResponseEntity<>(objectMapper.writeValueAsString(response), HttpStatus.NOT_FOUND);
            } catch (JsonProcessingException e) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (AttestationNotFoundException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/api/v2/{entityName}/{entityId}/attestation/{attestationName}/{attestationId}",
            produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.TEXT_HTML_VALUE, Constants.SVG_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> getAttestationCertificateGCS(HttpServletRequest request, @PathVariable String entityName, @PathVariable String entityId,
                                                            @PathVariable String attestationName, @PathVariable String attestationId) {
        ResponseParams responseParams = new ResponseParams();
        Object certificateWebCopy = null;
        Object certificateOriginal = null;
        try {
            String checkIfAlreadyExists = "issuance/"+entityId+".pdf";
            certificateOriginal = certificateService.getCred(checkIfAlreadyExists);
                checkEntityNameInDefinitionManager(entityName);
                String readerUserId = getUserId(entityName, request);
                JsonNode jsonNode = registryHelper.readEntity(readerUserId, entityName, entityId, false, null, false)
                        .get(entityName);
                JsonNode node = jsonNode.get(attestationName);
                String fileName = getFileNameOfCredentials(node);
            JsonNode courseName1 = jsonNode.get("courseName");
            String courseName = null;
            if(courseName1!=null){
                courseName = courseName1.asText();

            }
                String requestType = jsonNode.get("requestType") !=null ? jsonNode.get("requestType").asText():null;
            if((certificateOriginal == null) || (courseName!=null && (courseName.contains("Correction") || courseName.contains("Duplicate") || courseName.contains("Reissue"))) )
            {
                boolean wc = false;
                JsonNode attestationNode = getAttestationSignedData(attestationId, node);
                String templateUrlFromRequest = getTemplateUrlFromRequestFromRegType(request, entityName,courseName);
                String fileName1 = fileName + "webcopy";
                certificateWebCopy = certificateService.getCertificate(attestationNode,
                        entityName,
                        entityId,
                        request.getHeader(HttpHeaders.ACCEPT),
                        templateUrlFromRequest.replace(".html","-WC.html"),
                        getAttestationNode(attestationId, node),
                        fileName1, wc
                );
                String url = null;
                String fileUrlForQR = getFileUrl(fileName1);
                if(certificateWebCopy != null){
                    url = certificateService.saveToGCS(certificateWebCopy, fileName1);
                    logger.debug("WebCopy of Certificate:"+url);
                }
                certificateOriginal = certificateService.getCertificate(attestationNode,
                        entityName,
                        entityId,
                        request.getHeader(HttpHeaders.ACCEPT),
                        templateUrlFromRequest,
                        getAttestationNode(attestationId, node),
                        fileUrlForQR, false);
                if(certificateOriginal!=null){
                    String originalUrl = getCredUrl(fileName, certificateOriginal);
                    if(originalUrl != null){
                        shareCredentials(jsonNode, originalUrl);
                    }
                }

            }

            return new ResponseEntity<>(certificateOriginal, HttpStatus.OK);
        } catch (RecordNotFoundException re) {
            createSchemaNotFoundResponse(re.getMessage(), responseParams);
            Response response = new Response(Response.API_ID.GET, "ERROR", responseParams);
            try {
                return new ResponseEntity<>(objectMapper.writeValueAsString(response), HttpStatus.NOT_FOUND);
            } catch (JsonProcessingException e) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (AttestationNotFoundException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * @param request
     * @param entityName
     * @param entityId
     * @param attestationName
     * @param attestationId
     * @param activity
     * @return
     */
    @GetMapping(value = "/api/v3/{entityName}/{entityId}/attestation/{attestationName}/{attestationId}/{activity}",
            produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.TEXT_HTML_VALUE, Constants.SVG_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> getAttestationCertificateGCSByActivity(HttpServletRequest request, @PathVariable String entityName, @PathVariable String entityId,
                                                               @PathVariable String attestationName, @PathVariable String attestationId,
                                                               @PathVariable String activity) {
        ResponseParams responseParams = new ResponseParams();
        Object certificateWebCopy = null;
        Object certificateOriginal = null;
        try {
            String checkIfAlreadyExists = "issuance/"+entityId+".pdf";
            certificateOriginal = certificateService.getCred(checkIfAlreadyExists);
            checkEntityNameInDefinitionManager(entityName);
            String readerUserId = getUserId(entityName, request);
            JsonNode jsonNode = registryHelper.readEntity(readerUserId, entityName, entityId, false, null, false)
                    .get(entityName);
            JsonNode node = jsonNode.get(attestationName);
            String fileName = getFileNameOfCredentials(node);


            String courseType = jsonNode.get("courseType") != null ? jsonNode.get("courseType").asText() : null;
            String councilName = jsonNode.get("council") != null ? jsonNode.get("council").asText() : null;
            String courseName = jsonNode.get("courseName") != null ? jsonNode.get("courseName").asText() : null;


            String requestType = jsonNode.get("requestType") !=null ? jsonNode.get("requestType").asText():null;
            if((certificateOriginal == null) || (courseName!=null && (courseName.contains("Correction") || courseName.contains("Duplicate") || courseName.contains("Reissue"))) )
            {

                CourseDetailDTO courseDetailDTO = CourseDetailDTO.builder()
                        .entityName(entityName)
                        .courseType(courseType)
                        .councilName(councilName)
                        .courseName(courseName)
                        .activityName(activity)
                        .build();

                boolean wc = false;
                JsonNode attestationNode = getAttestationSignedData(attestationId, node);
                String templateUrlFromRequest = getTemplateUrlFromRequest(request, courseDetailDTO);
                String fileName1 = fileName + "webcopy";
                certificateWebCopy = certificateService.getCertificate(attestationNode,
                        entityName,
                        entityId,
                        request.getHeader(HttpHeaders.ACCEPT),
                        templateUrlFromRequest.replace(".html","-WC.html"),
                        getAttestationNode(attestationId, node),
                        fileName1, wc
                );
                String url = null;
                String fileUrlForQR = getFileUrl(fileName1);
                if(certificateWebCopy != null){
                    url = certificateService.saveToGCS(certificateWebCopy, fileName1);
                    logger.debug("WebCopy of Certificate:"+url);
                }
                certificateOriginal = certificateService.getCertificate(attestationNode,
                        entityName,
                        entityId,
                        request.getHeader(HttpHeaders.ACCEPT),
                        templateUrlFromRequest,
                        getAttestationNode(attestationId, node),
                        fileUrlForQR, false);
                if(certificateOriginal!=null){
                    String originalUrl = getCredUrl(fileName, certificateOriginal);
                    if(originalUrl != null){
                        shareCredentials(jsonNode, originalUrl);
                    }
                }

            }

            return new ResponseEntity<>(certificateOriginal, HttpStatus.OK);
        } catch (RecordNotFoundException re) {
            createSchemaNotFoundResponse(re.getMessage(), responseParams);
            Response response = new Response(Response.API_ID.GET, "ERROR", responseParams);
            try {
                return new ResponseEntity<>(objectMapper.writeValueAsString(response), HttpStatus.NOT_FOUND);
            } catch (JsonProcessingException e) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (AttestationNotFoundException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private String getFileUrl(String fileName) {
        String fileUrl = claimRequestUrl + claimDownloadPath + static_download_parameter + "issuance/"+fileName + ".pdf";
        return fileUrl;
    }


    @RequestMapping(value = "/api/v1/{entityName}/{entityId}/revoke", method = RequestMethod.POST)
    public ResponseEntity<Object> revokeACredential (
            HttpServletRequest request,
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestHeader HttpHeaders headers
    ){
        String userId = USER_ANONYMOUS;
        logger.info("Revoking the entityType {} with {} Id",entityName, entityId);
        // Check fot Authorisation
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName)) {
            try {
                userId = registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                return createUnauthorizedExceptionResponse(e);
            }
        }
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.REVOKE, "OK", responseParams);
        try {
            String tag = "RegistryController.revokeAnExistingCredential " + entityName;
            watch.start(tag);
            JsonNode existingEntityNode = getEntityJsonNode(entityName, entityId,false, userId, null);
            String signedData = existingEntityNode.get(OSSystemFields._osSignedData.name()).asText();
            if (signedData.equals(new String()) || signedData.equals(null)) {
                throw new RecordNotFoundException("Credential is already revoked");
            }
            JsonNode revokedEntity = registryHelper.revokeAnEntity( entityName ,entityId, userId, existingEntityNode);
            if (revokedEntity != null) {
                registryHelper.revokeExistingCredentials(entityName, entityId, userId, signedData);
            }
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop(tag);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Registry Controller: Exception while revoking an entity:", e);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Nullable
    private JsonNode getAttestationNode1(String attestationId, JsonNode node) {
        Iterator<JsonNode> iterator = node.iterator();
        JsonNode attestationNode = null;
        while (iterator.hasNext()) {
            attestationNode = iterator.next();
            if (attestationNode.get(uuidPropertyName).toString().equals(attestationId)) {
                break;
            }
        }
        return attestationNode;
    }
    /**
     * PULL-URI-Request API for DigiLocker
     * @param request
     * @param entityName
     * @return
     */
    @CrossOrigin (origins = "*")
    @RequestMapping(value = "/api/v1/pullUriRequest/{entityName}", method = RequestMethod.POST, produces =
            {MediaType.APPLICATION_XML_VALUE}, consumes = {MediaType.APPLICATION_XML_VALUE,MediaType.APPLICATION_PDF_VALUE, MediaType.TEXT_HTML_VALUE, Constants.SVG_MEDIA_TYPE})
    public ResponseEntity<Object> pullUriRequest(HttpServletRequest request, @PathVariable String entityName) {
        String statusCode = "1";
        Scanner scanner = null;
        try {
            scanner = new Scanner(request.getInputStream(), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String searchQuery = null;
        if(scanner!=null){
            Scanner scanner1 = scanner.useDelimiter("\\A");
            if(scanner1.hasNext())
                searchQuery =  scanner1.next();
        }
        DocDetails docDetails = null;
        ObjectNode searchNode = null;
        // Map to JAXB
        PullURIRequest req = null;
        try {
            req = DigiLockerUtils.processPullUriRequest(searchQuery);
            searchNode = DigiLockerUtils.getJsonQuery2(req.getDocDetails());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        entityName = updateEntityName(req.getDocDetails().getCandidateType());
        if(entityName.equalsIgnoreCase("StudentGoodstanding")){
            searchNode = DigiLockerUtils.queryForGoodStanding(req.getDocDetails());
        }
        JsonNode result = searchEntity(searchNode, entityName);
        JsonNode jsonNode = result.get(entityName);
        String osid = null;
        if(jsonNode!=null){
            osid =  jsonNode.size() > 0 ?jsonNode.get(0).get("osid").asText():null;
        }
        if(osid!=null) {
            try {
                // check if uri exists
                String uri = null;
                String fileName = null;
                byte[] certificate = null;
                String existingUri = claimRequestClient.getDocument(osid);
                HttpHeaders headers = new HttpHeaders();
                String responseString = null;

                fileName = "issuance/"+osid+".pdf";
                certificate = certificateService.getCred(fileName);
                if(certificate!=null) {
                    if(existingUri==null){
                        uri = DigiLockerUtils.getDocUri();
                        certificateService.saveforDGL((certificate), uri+".pdf");
                        Document docs = new Document(osid, uri);
                        claimRequestClient.saveDocument(docs);
                    }else{
                        uri = existingUri;
                    }
                    Person person = DigiLockerUtils.getPersonDetail(result, entityName);

                    byte[] encodedCertificate = Base64.getEncoder().encode(certificate);
                    PullURIResponse pullResponse = DigiLockerUtils.getPullUriResponse(uri, statusCode, osid, encodedCertificate, person);
                    responseString = DigiLockerUtils.convertJaxbToString(pullResponse);
                    responseString = responseString.replace(" xsi:type=\"xs:base64Binary\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"","");
                    String regexPattern = "xsi:type=\"xs:string\"\\s+xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\\s+xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
                    // Replace the matched pattern with an empty string to remove it
                    responseString = responseString.replaceAll(regexPattern, "");

                    headers.setContentType(MediaType.APPLICATION_XML);
                    return new ResponseEntity<>(responseString, headers, HttpStatus.OK);
                }else{
                    statusCode = "0";
                    return new ResponseEntity<>(statusCode, HttpStatus.NOT_FOUND);
                }
            } catch (Exception exception) {
                statusCode = "0";
                exception.printStackTrace();
                return new ResponseEntity<>(statusCode, HttpStatus.NOT_FOUND);
            }
        }
        else{
            statusCode = "0";
            return new ResponseEntity<>(statusCode, HttpStatus.NOT_FOUND);
        }
    }

    private String updateEntityName(String candidateType) {
        String entityName = "StudentFromUP";

        switch (candidateType) {
            case "From UP":
                // Code to handle "From UP" status
                entityName = "StudentFromUP";
                break;

            case "Outside UP":
                // Code to handle "Outside UP" status
                entityName = "StudentOutsideUP";
                break;

            case "Good Standing":
                // Code to handle "Good Standing" status
                entityName = "StudentGoodstanding";
                break;

            default:
                // Code to handle any other status
                entityName = "StudentFromUP";
                break;
        }

        return entityName;
    }

    /**
     * PULL-URI-Request API for DigiLocker
     * @param request
     * @param
     * @return
     */
    @CrossOrigin (origins = "*")
    @RequestMapping(value = "/api/v1/pullDocRequest/{entityName}", method = RequestMethod.POST, produces =
            {MediaType.APPLICATION_XML_VALUE}, consumes = {MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Object> pullDocReq(HttpServletRequest request) throws Exception {
        String statusCode = "1";
        Scanner scanner = null;
        String hmacFromRequest = request.getHeader("x-digilocker-hmac");
        logger.info(hmacFromRequest+":Hmac From server");
        try {
            scanner = new Scanner(request.getInputStream(), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String xmlString = null;
        if(scanner!=null){
            Scanner scanner1 = scanner.useDelimiter("\\A");
            if(scanner1.hasNext())
                xmlString =  scanner1.next();
        }
        PullDocRequest pullDocRequest = null;
       // boolean isValidReq = HmacValidator.validateHmac(xmlString,hmacFromRequest,DIGILOCKER_KEY);
        if(true) {
            try {
                pullDocRequest = DigiLockerUtils.processPullDocRequest(xmlString);
            } catch (Exception e) {
                statusCode = "0";
                return new ResponseEntity<>(statusCode, HttpStatus.NOT_FOUND);
            }

            String uri = pullDocRequest.getDocDetails().getUri();
            String osId = claimRequestClient.getOsIdWithURI(uri);

             JsonNode searchNode = DigiLockerUtils.getQuryNode(osId);

            JsonNode result = searchEntityForDGL((ObjectNode) searchNode);

            if (result == null || result.size() == 0) {
                statusCode = "0";
                logger.info("");
                return new ResponseEntity<>(statusCode, HttpStatus.NOT_FOUND);
            }

            boolean studentFromUP = result.get("StudentFromUP") != null;
            String fullName = studentFromUP ? result.get("StudentFromUP").get(0).get("name").asText():null;
            String dob = studentFromUP?result.get("StudentFromUP").get(0).get("dateOfBirth").asText():null;
            //String email = result.get("StudentFromUP").get(0).get("email").asText();
            String finalYearRollNo = studentFromUP ? result.get("StudentFromUP").get(0).get("finalYearRollNo").asText():null;

            String credName = pullDocRequest.getDocDetails().getUri();
            credName = "issuance/" + credName + ".pdf";
            byte[] cred = certificateService.getCred(credName);
            byte[] cert = Base64.getEncoder().encode(cred);
            if (cred != null) {
                try {
                    //DigiLockerUtils.getPersonDetail(result, "entityName");
                    Person person = new Person();
                    person.setDob(dob);
                    person.setName(fullName);
                    person.setFinalYearRollNo(finalYearRollNo);
                    PullDocResponse pullDocResponse = DigiLockerUtils.getDocPullDocResponse(pullDocRequest, statusCode, cert, person);
                    String responseString = DigiLockerUtils.convertJaxbToPullDoc(pullDocResponse);
                    responseString = responseString.replaceAll(" xsi:type=\"[^\"]*\" xmlns:xs=\"[^\"]*\" xmlns:xsi=\"[^\"]*\"", "");
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_XML);
                    return new ResponseEntity<>(responseString, headers, HttpStatus.OK);
                } catch (Exception e) {
                    statusCode = "0";
                    e.printStackTrace();
                    return new ResponseEntity<>(statusCode, HttpStatus.NOT_FOUND);
                }

            } else {
                statusCode = "0";
                return new ResponseEntity<>(statusCode, HttpStatus.NOT_FOUND);
            }
        }else {
            statusCode = "0";
            return new ResponseEntity<>(statusCode, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * @param entityName
     * @param entityId
     * @param files
     * @param request
     * @return
     */

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}/upload/multi-files", method = RequestMethod.PUT)
    public ResponseEntity<Object> putMultiEntityFiles(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestParam MultipartFile[] files,
            HttpServletRequest request) {

        if (registryHelper.doesEntityOperationRequireAuthorization(entityName)) {
            try {
                registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                return createUnauthorizedExceptionResponse(e);
            }
        }
        ResponseParams responseParams = new ResponseParams();

        try {
            List<String> fileUrlList = certificateService.uploadMultiEntityDocFiles(files, entityName, entityId);
            StringBuffer fileString = new StringBuffer();

                for (String str :fileUrlList  ) {
                    fileString.append(str+",");
                }

            Response response = new Response(Response.API_ID.CREATE, HttpStatus.OK.name(), responseParams);
            response.setResult(fileString.toString());
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("RegistryController: Exception while uploading files", e);

            Response response = new Response(Response.API_ID.CREATE, HttpStatus.INTERNAL_SERVER_ERROR.name(), responseParams);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());

            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @RequestMapping(value = "/api/v1/{entityName}/{entityId}/upload/file", method = RequestMethod.PUT)
    public ResponseEntity<Object> putSingleEntityFile(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestParam MultipartFile file,
            HttpServletRequest request) {

        if (registryHelper.doesEntityOperationRequireAuthorization(entityName)) {
            try {
                registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                return createUnauthorizedExceptionResponse(e);
            }
        }
        ResponseParams responseParams = new ResponseParams();

        try {
            String fileUrl = certificateService.uploadSingleEntityDocFiles(file, entityName, entityId);

            Response response = new Response(Response.API_ID.CREATE, HttpStatus.OK.name(), responseParams);
            response.setResult(fileUrl);
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("RegistryController: Exception while uploading single file", e);

            Response response = new Response(Response.API_ID.CREATE, HttpStatus.INTERNAL_SERVER_ERROR.name(), responseParams);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());

            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getFileNameOfCredentials(JsonNode node) {
        StringBuffer fileName = new StringBuffer();
        String osid = node.get("osid")!=null?node.get("osid").asText():null;
        if(osid!=null)
           fileName.append(osid);
        else{
            String name = node.get(0).get("entityId").toString().replace(" ","");
            name = name.replace("\"","");
            fileName.append(name);
        }

        return fileName!=null?fileName.toString():"";
    }

    private void trackCred(JsonNode node, String fileName) {
        try {
            // Track Credentials creation
            JsonNode name = node.get("name");
            JsonNode credentialsType = node.get("credType");
            JsonNode course = node.get("course");
            JsonNode rollNumber = node.get("rollNumber");
            JsonNode regNumber = node.get("regNumber");

            Learner learner = new Learner();
            learner.setName(name!=null?name.asText():null);
            learner.setEnrollmentNumber(regNumber!=null?regNumber.asText():"");
            learner.setRollNumber(rollNumber!=null?rollNumber.asText():"");

            Credential cred = new Credential();
            cred.setCourse(course!=null?course.asText():"");
            cred.setCredentialName(credentialsType!=null?credentialsType.asText():"");
            cred.setCredentialURL(fileName);
            List<Credential> list = new ArrayList<>();
            list.add(cred);
            learner.setCredentialsList(list);
            certificateService.trackCredentials(learner);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private String getCredUrl(final String entityId, Object certificate) throws Exception {
        String url = null;
        try {
            url = getCertificate(entityId, certificate);
        } catch (Exception e) {
            throw new Exception(e);
        }
        return url;
    }

    @Nullable
    private String getCertificate(String entityId, Object certificate) throws Exception {
        String url = null;
        if (certificate != null) {
            logger.info("Credentials generation for EntityId:"+entityId);
            url = certificateService.saveToGCS(certificate, entityId);
            logger.debug("Final url credentials:"+url);
        }
        return url;
    }
    private void shareCredentials(JsonNode node, String url) {
        try {
            // Mail Send Functionality -Start
            JsonNode email = node.get("email");
            JsonNode name = node.get("name");
            JsonNode credentialsType = node.get("claimType");
            MailDto mail = new MailDto();
            if (mail != null && name != null) {
                mail.setName(name.asText());
                mail.setEmailAddress(email.asText());
                mail.setCertificate(url);
                if (credentialsType != null) {
                    mail.setCredentialsType(credentialsType.asText());
                }
                certificateService.shareCertificateMail(mail);
                // Mail Send Functionality End
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JsonNode searchEntityForDGL(ObjectNode searchNode) {
        JsonNode result = null;
        String[] entityArr = {"StudentFromUP","StudentOutSideUP","StudentForeignVerification","StudentGoodstanding"};
        for (String entityName: entityArr) {
             try {
                ArrayNode entity = JsonNodeFactory.instance.arrayNode();
                entity.add(entityName);
                searchNode.set(ENTITY_TYPE, entity);
                checkEntityNameInDefinitionManager(entityName);
                if (definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getEnableSearch()) {
                    result = registryHelper.searchEntity(searchNode);
                    watch.stop("RegistryController.searchEntity");
                    if(result!=null || result.size() > 0)
                    return result;
                } else {
                    watch.stop("RegistryController.searchEntity");
                    logger.error("Searching on entity {} not allowed", entityName);
                }
            } catch (RecordNotFoundException e) {
                return null;
            } catch (Exception e) {
                logger.error("Exception in controller while searching entities !", e);
            }
        }

        return result;
    }

    public JsonNode searchEntity(ObjectNode searchNode, String entityName) {
       JsonNode result = null;
        try {
            ArrayNode entity = JsonNodeFactory.instance.arrayNode();
            entity.add(entityName);
            searchNode.set(ENTITY_TYPE, entity);
            checkEntityNameInDefinitionManager(entityName);
            if (definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getEnableSearch()) {
                result = registryHelper.searchEntity(searchNode);
                watch.stop("RegistryController.searchEntity");
                return result;
            } else {
                watch.stop("RegistryController.searchEntity");
                logger.error("Searching on entity {} not allowed", entityName);
            }
        } catch (RecordNotFoundException e) {
            return null;
        } catch (Exception e) {
            logger.error("Exception in controller while searching entities !", e);
        }
        return result;
    }

    @RequestMapping(value = "/api/v1/{entityName}/sendPendingForeignItemMail/{claimId}", method = RequestMethod.GET)
    public ResponseEntity<Object> sendPendingForeignItemMail(@PathVariable String entityName,
                                                             @PathVariable String claimId,
                                                             HttpServletRequest request) {

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEND, "OK", responseParams);

        try {
            registryHelper.authorizeInviteEntity(request, entityName);

            String mailStatus = claimRequestClient.sendPendingForeignItemMail(claimId);

            response.setResult(mailStatus);
            responseParams.setStatus(Response.Status.SUCCESSFUL);

        } catch (Exception exception) {
            logger.error("Exception : {}", exception.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(exception.getMessage());
            return new ResponseEntity<>(responseParams, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/{entityName}/sendEcPendingItemMail", method = RequestMethod.POST)
    public ResponseEntity<Object> sendEcPendingItemMail(@PathVariable String entityName,
                                                        @RequestBody ManualPendingMailDTO pendingMailDTO,
                                                             HttpServletRequest request) {

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEND, "OK", responseParams);

        try {
            registryHelper.authorizeInviteEntity(request, entityName);

            String mailStatus = claimRequestClient.sendEcPendingItemMail(pendingMailDTO);

            response.setResult(mailStatus);
            responseParams.setStatus(Response.Status.SUCCESSFUL);

        } catch (Exception exception) {
            logger.error("Exception : {}", exception.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(exception.getMessage());
            return new ResponseEntity<>(responseParams, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @RequestMapping(value = "/api/v1/category/{entityName}/{category}", method = RequestMethod.GET)
    public ResponseEntity<Object> getCourseName(@PathVariable String entityName, @Optional @PathVariable String category, HttpServletRequest request) {

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEND, "OK", responseParams);

        try {
            List list = null;
            if(category == null || category.equals("")){
                claimRequestClient.getAllCourses();
            }
            else{
                list = claimRequestClient.getCourseCategory(category);
            }

            //Long certNumber = claimRequestClient.getCertificateNumber();
            response.setResult(list);
            responseParams.setStatus(Response.Status.SUCCESSFUL);

        } catch (Exception exception) {
            logger.error("Exception : {}", exception.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(exception.getMessage());
            return new ResponseEntity<>(responseParams, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @RequestMapping(value = "/api/v1/allcourse/{entityName}", method = RequestMethod.GET)
    public ResponseEntity<Object> getAllCourseName(@PathVariable String entityName, HttpServletRequest request) {

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEND, "OK", responseParams);

        try {
            List list = claimRequestClient.getAllCourses();
            //Long certNumber = claimRequestClient.getCertificateNumber();
            response.setResult(list);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
        } catch (Exception exception) {
            logger.error("Exception : {}", exception.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(exception.getMessage());
            return new ResponseEntity<>(responseParams, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/course/{entityName}/certificate-number", method = RequestMethod.GET)
    public ResponseEntity<Object> getCourseCertificateNumber(@PathVariable String entityName,HttpServletRequest request) {

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEND, "OK", responseParams);

        try {
            Long certNumber = claimRequestClient.getCertificateNumber();
            response.setResult(certNumber);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
        } catch (Exception exception) {
            logger.error("Exception : {}", exception.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(exception.getMessage());
            return new ResponseEntity<>(responseParams, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private String generateImageURL(JsonNode rootNode) {
        JsonNode rollNumber = rootNode.get("finalYearRollNo");
        JsonNode enrollmentNumber = rootNode.get("nurseRegNo");
        JsonNode examYear = rootNode.get("examYear");
        String url = imgBaseUri;
        if (examYear != null)
            url = url.concat(examYear.asText());
        if (enrollmentNumber != null) {
            url = url.concat("/E" + enrollmentNumber.asText());
        } else if (rollNumber != null) {
            url = url.concat("/rp" + rollNumber.asText());
        }
        url = url.concat(imgFormat);
        logger.debug("Generated Img url::" + url);
        return url;
    }

    private void extractImgUrl(JsonNode rootNode) {
        String imgUrl = generateImageURL(rootNode);
        ObjectNode node = (ObjectNode) rootNode;
        node.put("candidatePic", imgUrl);
        node.put("candidateSignature", imgUrl.replace("rp","rs"));
        logger.info("orgLogo" + node.get("candidatePic"));
    }


}
