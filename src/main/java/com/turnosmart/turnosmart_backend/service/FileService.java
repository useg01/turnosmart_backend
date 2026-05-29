package com.turnosmart.turnosmart_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import com.turnosmart.turnosmart_backend.exception.BusinessException;

@Service
public class FileService {

    private final Path root = Paths.get("uploads/tramites");

    public String save(MultipartFile file, String ticket) {
        try {
            // Validación de formato estricto (Regla 5.4)
            if (file.getContentType() == null || !file.getContentType().equals("application/pdf")) {
                throw new BusinessException("Error: Solo se permiten archivos en formato .pdf");
            }

            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            String filename = ticket + "_" + System.currentTimeMillis() + ".pdf";
            Files.copy(file.getInputStream(), this.root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

            return filename;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo. Error: " + e.getMessage());
        }
    }
}