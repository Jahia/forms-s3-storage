<%@ page contentType="text/javascript" %>
<%@ taglib prefix="formfactory" uri="http://www.jahia.org/formfactory/functions" %>

    (function () {
        'use strict';

        //Define the action directive
        var sendBinaryToS3Action = function($log, $compile, contextualData, ffDataFactory, ffTemplateResolver) {
            var directive = {
                restrict: 'E',
                templateUrl: function(el, attrs) {
                    return ffTemplateResolver.resolveTemplatePath('${formfactory:addFormFactoryModulePath('/form-factory-actions/send-binary-to-s3/', renderContext)}', attrs.viewType);
                },
                link: linkFunc
            };
            return directive;

            function linkFunc(scope, el, attr) {
                /**
                 * Any initialization of action properties or any other variables
                 * can be done within this function.
                 */
            }
        };
        //Attach the directive to the module
        angular
            .module('formFactory')
            .directive('ffSendBinaryToS3', ['$log', '$compile', 'contextualData', 'ffDataFactory', 'ffTemplateResolver', sendBinaryToS3Action]);
    })();
