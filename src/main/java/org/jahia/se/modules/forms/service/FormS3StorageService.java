package org.jahia.se.modules.forms.service;

public interface FormS3StorageService {
    String upload(byte[] attachment, String fileName, String mimeType);
    byte[] getObjectFile(String filePath);
}
