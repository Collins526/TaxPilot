package com.taxpilot.storage;

public interface DocumentStorageService {

    StoredObject store(byte[] content, String fileName, String contentType, String folder);

    boolean isConfigured();
}
