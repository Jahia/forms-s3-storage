package org.jahia.se.modules.forms.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.forms.actions.SaveToJcrAction;
import org.jahia.modules.forms.api.ApiBackendType;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONException;
import org.slf4j.Logger;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class SaveBinaryToS3Action extends Action implements ApiBackendType {
    private static final Logger logger = getLogger(SaveToJcrAction.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws JSONException {
        try {
            JCRSessionWrapper currentSession = session;
            // Get the node who contains the form structure
            JCRNodeWrapper formStructure = session.getNodeByIdentifier(parameters.get("formId").get(0));
            boolean hasFileUploadField = JCRContentUtils.getDescendantNodes(formStructure, "fcnt:fileUploadDefinition").hasNext();
            if(hasFileUploadField){
               storeFilesInResults(req,session,formStructure,parameters);
            }
            logger.info("hasFileUploadField : "+ hasFileUploadField);
        } catch (RepositoryException e) {
            logger.error("RepositoryException on node {} : " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String getBackendType() {
        return "S3";
    }



    private void storeFilesInResults(
            HttpServletRequest req,
            JCRSessionWrapper session,
            JCRNodeWrapper formStructure,
            Map<String, List<String>> parameters
        ) throws RepositoryException, JSONException {

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
        }
    }
}
