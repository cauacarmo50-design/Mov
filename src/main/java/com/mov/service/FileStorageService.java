package com.mov.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path uploadDir;

    // Magic Bytes signatures (Hex)
    private static final byte[] MAGIC_JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] MAGIC_PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] MAGIC_PDF = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    private static final byte[] MAGIC_ZIP_OOXML = new byte[]{0x50, 0x4B, 0x03, 0x04}; // DOCX / XLSX (ZIP container)
    private static final byte[] MAGIC_OLE_DOC = new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}; // DOC / XLS

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of(
            ".pdf", ".docx", ".doc", ".xlsx", ".xls", ".csv", ".txt", ".md", ".png", ".jpg", ".jpeg"
    );

    public FileStorageService(@Value("${app.upload.dir:./uploads}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (Exception ex) {
            throw new RuntimeException("Não foi possível criar o diretório de uploads isolado.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo foi selecionado para envio.");
        }

        // 1. Sanitize the filename to prevent Path Traversal
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento.pdf");
        if (originalFilename.contains("..")) {
            throw new SecurityException("Tentativa de Path Traversal detectada: " + originalFilename);
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!EXTENSOES_PERMITIDAS.contains(extension)) {
            throw new SecurityException("Formato de arquivo não permitido (" + extension + "). Formatos aceitos: PDF, DOCX, XLSX, CSV, TXT, PNG, JPG.");
        }

        // 2. Validate Magic Bytes / Safe Content
        if (!isValidFileSignature(file, extension)) {
            throw new SecurityException("Conteúdo do arquivo inválido ou incompatível com o formato " + extension);
        }

        // 3. Rename to UUID (preventing execution via exact name)
        String targetFilename = UUID.randomUUID().toString() + extension;

        try {
            Path targetLocation = this.uploadDir.resolve(targetFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Arquivo salvo com sucesso na sandbox: {}", targetFilename);
            return targetFilename;
        } catch (IOException ex) {
            throw new RuntimeException("Falha ao armazenar arquivo " + targetFilename, ex);
        }
    }

    public String storeBytes(byte[] content, String originalFilename) {
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (extension.isEmpty()) extension = ".pdf";
        String targetFilename = UUID.randomUUID().toString() + extension;
        try {
            Path targetLocation = this.uploadDir.resolve(targetFilename);
            Files.write(targetLocation, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return targetFilename;
        } catch (IOException ex) {
            throw new RuntimeException("Falha ao gravar arquivo inicial " + targetFilename, ex);
        }
    }

    public Resource loadFileAsResource(String storedFilename) {
        try {
            if (storedFilename == null || storedFilename.contains("..")) {
                throw new SecurityException("Nome de arquivo inválido.");
            }
            Path filePath = this.uploadDir.resolve(storedFilename).normalize();
            if (!filePath.startsWith(this.uploadDir)) {
                throw new SecurityException("Acesso fora do diretório de uploads negado.");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Arquivo não encontrado ou indisponível: " + storedFilename);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Erro ao localizar arquivo: " + storedFilename, ex);
        }
    }

    private boolean isValidFileSignature(MultipartFile file, String extension) {
        if (".txt".equals(extension) || ".md".equals(extension) || ".csv".equals(extension)) {
            return file.getSize() > 0;
        }
        try (InputStream is = file.getInputStream()) {
            byte[] headerBytes = new byte[8];
            int read = is.read(headerBytes);
            if (read < 3) return false;

            if (".pdf".equals(extension)) return startsWith(headerBytes, MAGIC_PDF);
            if (".png".equals(extension)) return startsWith(headerBytes, MAGIC_PNG);
            if (".jpg".equals(extension) || ".jpeg".equals(extension)) return startsWith(headerBytes, MAGIC_JPEG);
            if (".docx".equals(extension) || ".xlsx".equals(extension)) return startsWith(headerBytes, MAGIC_ZIP_OOXML);
            if (".doc".equals(extension) || ".xls".equals(extension)) return startsWith(headerBytes, MAGIC_OLE_DOC) || startsWith(headerBytes, MAGIC_ZIP_OOXML);

            return startsWith(headerBytes, MAGIC_JPEG) ||
                   startsWith(headerBytes, MAGIC_PNG) ||
                   startsWith(headerBytes, MAGIC_PDF) ||
                   startsWith(headerBytes, MAGIC_ZIP_OOXML);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean startsWith(byte[] source, byte[] match) {
        if (match.length > source.length) {
            return false;
        }
        for (int i = 0; i < match.length; i++) {
            if (source[i] != match[i]) {
                return false;
            }
        }
        return true;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex);
        }
        return "";
    }
}
