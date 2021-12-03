package org.jahia.se.modules.forms.actions;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.forms.actions.SaveToJCRBackgroundJob;
import org.jahia.modules.forms.actions.SendEmailAction;
import org.jahia.modules.forms.api.ApiBackendType;
import org.jahia.se.modules.forms.storage.FormS3StorageServiceImpl;
import org.jahia.services.content.*;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.slf4j.Logger;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class SaveBinaryToS3Action extends Action implements ApiBackendType {
    private static final Logger logger = getLogger(SaveBinaryToS3Action.class);

    private enum FileInfo {
        TEMP_PATH(0), ORIGINAL_NAME(1), FILE_TYPE(2);
        private final int index;
        FileInfo(int index) {
            this.index = index;
        }
    }

    private SchedulerService schedulerService;
    private static volatile SaveBinaryToS3Action instance;
    private FormS3StorageServiceImpl s3StorageService;

    private JCRTemplate jcrTemplate;

    private SaveBinaryToS3Action() {
        this.s3StorageService = new FormS3StorageServiceImpl();
    }

    /**
     * Obtain the JCRTemplate singleton
     *
     * @return the JCRTemplate singleton instance
     */
    public static SaveBinaryToS3Action getInstance() {
        if (instance == null) {
            synchronized (SaveBinaryToS3Action.class) {
                if (instance == null) {
                    instance = new SaveBinaryToS3Action();
                }
            }
        }
        return instance;
    }

    public void setSchedulerService(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }
    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    public String getBackendType() {
        return "JCR";
    }

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws JSONException {

        try {
            JCRSessionWrapper currentSession = session;
            // Get the node who contains the form structure
            JCRNodeWrapper formStructure = session.getNodeByIdentifier(parameters.get("formId").get(0));
            boolean hasFileUploadField = JCRContentUtils.getDescendantNodes(formStructure, "fcnt:fileUploadDefinition").hasNext();
            boolean shouldTrackUser = false;
            if (formStructure.isNodeType("fcmix:trackUser") && formStructure.getProperty("trackUser").getBoolean()) {
                shouldTrackUser = true;
            }
            String origin;
            if (StringUtils.isNotEmpty(req.getHeader("referer"))) {
                origin = req.getHeader("referer");
            } else {
                origin = req.getRequestURI();
            }
            String ipAddress = req.getRemoteAddr();
            if (hasFileUploadField) {
                if (shouldTrackUser) {
                    // Get the node who contains the results of the form
                    saveResult(origin, ipAddress, session, parameters, formStructure, true, req);
                    return populateAndReturnResult();
                } else {
                    JahiaUser jahiaUser = JahiaUserManagerService.getInstance().lookup(JahiaUserManagerService.GUEST_USERNAME).getJahiaUser();
                    JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(jahiaUser, Constants.LIVE_WORKSPACE, resource.getLocale(), new JCRCallback<Object>() {
                        @Override
                        public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                            try {
                                saveResult(origin, ipAddress,
                                        session, parameters,
                                        formStructure, true, req);
                            } catch (JSONException e) {
                                throw new RepositoryException(e);
                            }
                            return null;
                        }
                    });
                    return populateAndReturnResult();
                }
            } else {
                JobDetail jahiaJob = BackgroundJob.createJahiaJob("Saving Forms Result in the JCR", SaveToJCRBackgroundJob.class);
                jahiaJob.setName("SaveToJCRJob" + org.apache.commons.id.uuid.UUID.randomUUID().toString());
                jahiaJob.setGroup("FFActions");
                JobDataMap jobDataMap = jahiaJob.getJobDataMap();
                jobDataMap.put("username", !shouldTrackUser ? JahiaUserManagerService.GUEST_USERNAME : session.getUser().getUserKey());
                jobDataMap.put("parameters", parameters);
                jobDataMap.put("origin", origin);
                jobDataMap.put("ip_address", ipAddress);
                jobDataMap.put("formIdentifier", formStructure.getIdentifier());
                jobDataMap.put("locale", resource.getLocale());
                schedulerService.scheduleJobAtEndOfRequest(jahiaJob);
                return populateAndReturnResult();
            }
        } catch (RepositoryException e) {
            logger.error("RepositoryException on node {} : " + e.getMessage(), e);

            JSONObject jsonAnswer = new JSONObject();
            jsonAnswer.append("actionName", "saveToJCR");
            jsonAnswer.append("status", "error");
            jsonAnswer.append("code", HttpServletResponse.SC_NOT_MODIFIED);
            jsonAnswer.append("message", "The results were not saved in the JCR because of RepositoryException:" + e.getMessage());

            return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, jsonAnswer);
        } catch (SchedulerException e) {
            logger.error("RepositoryException on node {} : " + e.getMessage(), e);

            JSONObject jsonAnswer = new JSONObject();
            jsonAnswer.append("actionName", "saveToJCR");
            jsonAnswer.append("status", "error");
            jsonAnswer.append("code", HttpServletResponse.SC_NOT_MODIFIED);
            jsonAnswer.append("message", "The results were not saved in the JCR because of SchedulerException:" + e.getMessage());

            return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, jsonAnswer);
        }
    }

    private ActionResult populateAndReturnResult() throws JSONException {
        JSONObject jsonAnswer = new JSONObject();
        jsonAnswer.append("actionName", "saveToJCR");
        jsonAnswer.append("status", "success");
        jsonAnswer.append("code", HttpServletResponse.SC_CREATED);
        jsonAnswer.append("message", "The results were correctly saved");
        return new ActionResult(HttpServletResponse.SC_OK, null, jsonAnswer);
    }

    void saveResult(String origin, String ip_address, JCRSessionWrapper session, Map<String, List<String>> parameters, JCRNodeWrapper formStructure, boolean hasFileUploadField, HttpServletRequest req) throws RepositoryException, JSONException {
        QueryManager qm = session.getWorkspace().getQueryManager();
        Query q = qm.createQuery("SELECT * FROM [fcnt:formResults] AS child WHERE child.[parentForm] = '" + parameters.get("formId").get(0) + "'", Query.JCR_SQL2);

        // Check if formResults has the form node, if yes get the node, if not create
        NodeIterator ni = q.execute().getNodes();

        //Those nodes are created by the flowHandler which call this action
        JCRNodeWrapper currentForm = (JCRNodeWrapper) ni.nextNode();
        JCRNodeWrapper labelsNode = currentForm.getNode("labels");
        Map<String, String> labelIds = new LinkedHashMap<String, String>();
        //Get labels
        JCRNodeIteratorWrapper labels = labelsNode.getNodes();
        while (labels.hasNext()) {
            JCRNodeWrapper label = (JCRNodeWrapper) labels.next();
            String[] path = label.getPath().split("/");
            String labelName = path[path.length - 1];
            labelIds.put(labelName, label.getIdentifier());
        }
        JCRNodeWrapper formResultsNode = currentForm.getNode("submissions");

        // Create the node for the new result
        JCRNodeWrapper currentFormResult = formResultsNode.addNode(org.apache.commons.id.uuid.UUID.randomUUID().toString(), "fcnt:result");

        // Register the url of origin of the result

        currentFormResult.setProperty("origin", origin);

        if (formStructure.isNodeType("fcmix:trackUser") && formStructure.getProperty("trackUser").getBoolean()) {
            currentFormResult.setProperty("ip_address", ip_address);
        }
        currentFormResult.setProperty("parentReference", currentForm);
        NodeIterator childrenOfType = JCRContentUtils.getDescendantNodes(formStructure, "fcnt:passwordDefinition");
        final List<String> passwordInputName = new ArrayList<>(4);
        while (childrenOfType.hasNext()) {
            JCRNodeWrapper next = (JCRNodeWrapper) childrenOfType.next();
            passwordInputName.add(next.getName());
        }
        List<JCRNodeWrapper> stepsList = JCRContentUtils.getChildrenOfType(formStructure, "fcnt:step");
        for (JCRNodeWrapper stepNode : stepsList) {
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                String inputName = entry.getKey();
                if (stepNode.hasNode(inputName)) {
                    JCRNodeWrapper field = stepNode.getNode(inputName);

                    if (!field.isNodeType("fcnt:fileUploadDefinition")) {
                        // Set the result of the field
                        List<String> values = entry.getValue();
                        boolean emptyResult = values.size() == 1 && StringUtils.isEmpty(values.get(0));

                        //Write result only if not empty
                        if (!emptyResult) {
                            String[] array = values.toArray(new String[values.size()]);
                            JCRNodeWrapper currentResult = currentFormResult.addNode(inputName, "fcnt:resultField");
                            if (passwordInputName.contains(inputName)) {
                                final String[] encrypted = {SendEmailAction.DEFAULT_PASSWORD_VALUE};
                                currentResult.setProperty("result", encrypted);
                                parameters.put(entry.getKey(), Arrays.asList(encrypted[0]));
                            } else {
                                currentResult.setProperty("result", array);
                            }
                            // Set if the field was mandatory or not
                            if ((!field.hasNode("validations")) || (field.hasNode("validations") && !field.getNode("validations").hasNode("required"))) {
                                currentResult.setProperty("optional", true);
                            }
                            //Set field label from the cloned labels map
                            currentResult.setProperty("label", labelIds.get(field.getName()));
                        }
                    }
                }
            }
        }
        session.save();

        // For files we need the node to be saved and split already
        // That is why we do it in two phases
        if (hasFileUploadField) {
            storeFilesInResults(req, session, formStructure, currentFormResult, parameters, labelIds);
        }
    }

    /**
     * Stores uploaded files in JCR
     *
     * @param req
     * @param session
     * @param formStructure
     * @param currentFormResult
     * @param parameters
     * @throws RepositoryException
     */
    private void storeFilesInResults(HttpServletRequest req, JCRSessionWrapper session, JCRNodeWrapper formStructure,
                                     JCRNodeWrapper currentFormResult, Map<String, List<String>> parameters,
                                     Map<String, String> labelIds) throws RepositoryException, JSONException {
        NodeIterator childrenOfType = JCRContentUtils.getDescendantNodes(formStructure, "fcnt:fileUploadDefinition");
        List<String> fileInputName = new ArrayList<>(4);
        while (childrenOfType.hasNext()) {
            JCRNodeWrapper children = (JCRNodeWrapper) childrenOfType.next();
            fileInputName.add(children.getName());
        }

        for (String fileName : fileInputName) {
            List<String> tempPaths = new ArrayList<>();
            List<String> absoluteUrls = new ArrayList<>();
            List<String> origNames = new ArrayList<>();
            List<String> fileTypes = new ArrayList<>();

            JSONArray urlArray = new JSONArray();
            JSONArray nameArray = new JSONArray();
            JSONArray typeArray = new JSONArray();
            JSONArray sizeArray = new JSONArray();
            JSONArray imageArray = new JSONArray();


            JSONObject fileJSON = new JSONObject();
            if (parameters.containsKey(fileName)) {
                List<String> fileInfo = parameters.get(fileName);
                List<List<String>> filesData = Lists.partition(fileInfo, 3);
                JCRNodeWrapper currentResult;
                if (currentFormResult.hasNode(fileName)) {
                    currentResult = currentFormResult.getNode(fileName);
                } else {
                    currentResult = currentFormResult.addNode(fileName, "fcnt:resultField");
                }

                currentResult.setProperty("label", labelIds.get(fileName));
                for (List<String> filesInformation : filesData) {
                    String fileTempPath = filesInformation.get(SaveBinaryToS3Action.FileInfo.TEMP_PATH.index);
                    String fileOrigName = filesInformation.get(SaveBinaryToS3Action.FileInfo.ORIGINAL_NAME.index);
                    String fileType = filesInformation.get(SaveBinaryToS3Action.FileInfo.FILE_TYPE.index);

                    if (StringUtils.isNotEmpty(fileTempPath)) {
                        tempPaths.add(fileTempPath);
                        origNames.add(fileOrigName);
                        fileTypes.add(fileType);

                        try {
//                            uploadFileAndAppendFileDataToResult(req, absoluteUrls, urlArray,
//                                    nameArray, typeArray, sizeArray, imageArray,
//                                    currentResult, fileTempPath, fileOrigName, fileType);
                            String formResultUUID = currentFormResult.getIdentifier();
                            String s3FileName = formResultUUID+"/"+fileOrigName;

                            uploadFileAndAppendFileDataToResult(
                                    urlArray, nameArray, typeArray, sizeArray, imageArray,
                                    fileTempPath, s3FileName, fileType);
                        } catch (IOException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                populateJsonAndResultNode(urlArray, nameArray, typeArray, sizeArray, imageArray, fileJSON, currentResult);
                parameters.remove(fileName);
                List<String> params = Arrays.asList(String.join(",", absoluteUrls), String.join("", tempPaths),
                        String.join(",", origNames), String.join(",", fileTypes));
                parameters.put(fileName, params);
                session.save();
            }
        }
    }


    private void uploadFileAndAppendFileDataToResult(
        JSONArray urlArray,
        JSONArray nameArray,
        JSONArray typeArray,
        JSONArray sizeArray,
        JSONArray imageArray,
        String fileTempPath,
        String fileOrigName,
        String fileType
    ) throws FileNotFoundException {
        byte[] attachment = s3StorageService.getObjectFile(fileTempPath);
        String fileName = cleanFilename(fileOrigName);
        String url = s3StorageService.upload(attachment,fileName,fileType);
        File tempFile = new File(fileTempPath);
        if(tempFile.delete()){
            logger.info("temp file deleted :"+fileTempPath);
        }else{
            logger.warn("temp file NOT deleted :"+fileTempPath);
        }

        urlArray.put(url);
        nameArray.put(fileName);
        sizeArray.put((long) attachment.length);
        typeArray.put(fileType);
//        imageArray.put(false);
        imageArray.put(fileType.startsWith("image"));
    }

    private String cleanFilename (String filename){
        return filename.replace(" ","_")
                .replace("è","e")
                .replace("é","e")
                .replace("à","a")
                .replace("@","_")
                .replace("'","_")
                .replace("`","_")
                .replace("!","_")
                .replace(":","_")
                .replace(";","_")
                .replace("?","_");
    }

//    private void uploadFileAndAppendFileDataToResult(HttpServletRequest req,
//                                                     List<String> absoluteUrls, JSONArray urlArray,
//                                                     JSONArray nameArray, JSONArray typeArray, JSONArray
//                                                             sizeArray, JSONArray imageArray, JCRNodeWrapper currentResult,
//                                                     String fileTempPath, String fileOrigName, String fileType) throws RepositoryException, FileNotFoundException {
//        JCRNodeWrapper file = currentResult.uploadFile(fileOrigName,
//                new FileInputStream(fileTempPath), JCRContentUtils.getMimeType(fileOrigName, fileType));
//        absoluteUrls.add(file.getAbsoluteUrl(req));
//        file.denyRoles("u:guest", Collections.singleton("reader"));
//        urlArray.put(file.getUrl());
//        nameArray.put(file.getDisplayableName());
//        sizeArray.put(file.getFileContent().getContentLength());
//        typeArray.put(file.getFileContent().getContentType());
//        imageArray.put(file.getFileContent().getContentType().startsWith("image"));
//    }

    private void populateJsonAndResultNode(JSONArray urlArray, JSONArray nameArray,
                                           JSONArray typeArray, JSONArray sizeArray,
                                           JSONArray imageArray, JSONObject fileJSON, JCRNodeWrapper currentResult) throws JSONException, RepositoryException {
        fileJSON.put("url", urlArray);
        fileJSON.put("name", nameArray);
        fileJSON.put("type", typeArray);
        fileJSON.put("size", sizeArray);
        fileJSON.put("image", imageArray);
        fileJSON.put("rendererName", "fileUpload");
        currentResult.setProperty("result", new String[]{fileJSON.toString()});
    }

}
