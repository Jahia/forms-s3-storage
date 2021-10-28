package org.jahia.se.modules.forms.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.forms.actions.SaveToJcrAction;
import org.jahia.modules.forms.api.ApiBackendType;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class saveBinaryToS3Action extends Action implements ApiBackendType {
    private static final Logger logger = getLogger(SaveToJcrAction.class);

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws JSONException {
        return null;
    }

    @Override
    public String getBackendType() {
        return "S3";
    }
}
