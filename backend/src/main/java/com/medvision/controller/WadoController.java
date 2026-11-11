package com.medvision.controller;

import org.dcm4che3.imageio.plugins.dcm.DicomImageReader;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReaderSpi;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/dicom")
public class WadoController {

    private final Path rootLocation = Paths.get("/upload-dir");

    private static final Pattern UID_PATTERN = Pattern.compile("^[0-9.]+$");

    @GetMapping(value = "/studies/{studyUID}/instances/{instanceUID}/frames/{frame}",
                produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getDicomFrame(
            @PathVariable String studyUID,
            @PathVariable String instanceUID,
            @PathVariable int frame) {

        if (!UID_PATTERN.matcher(studyUID).matches() || !UID_PATTERN.matcher(instanceUID).matches()) {
            return ResponseEntity.badRequest().build();
        }

        if (frame < 1) {
            return ResponseEntity.badRequest().build();
        }

        try {
            File dicomFile = findDicomFile(instanceUID);
            if (dicomFile == null || !dicomFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Validate the file is within the allowed directory
            if (!dicomFile.getCanonicalPath().startsWith(rootLocation.toFile().getCanonicalPath())) {
                return ResponseEntity.badRequest().build();
            }

            DicomImageReaderSpi spi = new DicomImageReaderSpi();
            DicomImageReader reader = (DicomImageReader) spi.createReaderInstance();

            try (ImageInputStream iis = ImageIO.createImageInputStream(dicomFile)) {
                reader.setInput(iis);
                BufferedImage bi = reader.read(frame - 1); // frames are 1-indexed in WADO-RS

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bi, "jpg", baos);
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(baos.toByteArray());
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private File findDicomFile(String instanceUID) {
        // In production, StorageService would return the exact path by instanceUID
        // For POC: search for matching file in upload directory
        File folder = rootLocation.toFile();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".dcm"));
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }
}
