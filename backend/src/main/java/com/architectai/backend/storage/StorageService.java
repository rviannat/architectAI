package com.architectai.backend.storage;

/**
 * Abstração de armazenamento de arquivos (PDFs, artefatos).
 * Implementações: MinIO (produção) e Local (desenvolvimento).
 */
public interface StorageService {

    /**
     * Faz upload de um arquivo e retorna a URL de acesso.
     *
     * @param objectKey  caminho/nome do objeto no storage (ex: "reports/analysis_123/report.pdf")
     * @param content    bytes do arquivo
     * @param contentType MIME type (ex: "application/pdf")
     * @return URL de acesso ao arquivo
     */
    String upload(String objectKey, byte[] content, String contentType);

    /**
     * Baixa um arquivo pelo seu objectKey.
     */
    byte[] download(String objectKey);

    /**
     * Retorna a URL de download para um objectKey já armazenado.
     */
    String getDownloadUrl(String objectKey);

    /**
     * Tipo de storage configurado ("minio" ou "local").
     */
    String getStorageType();
}

