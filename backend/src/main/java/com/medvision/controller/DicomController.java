package com.medvision.controller;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.medvision.service.StorageService;
import com.medvision.service.AiQueueService;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/v1")
public class DicomController {

    private static final byte[] DICOM_MAGIC = {'D', 'I', 'C', 'M'};
    private static final int DICOM_PREAMBLE_SIZE = 128;

    @Autowired
    private StorageService storageService;

    @Autowired
    private AiQueueService aiQueueService;

    @PostMapping("/upload-dicom")
    public ResponseEntity<String> uploadDicom(@RequestParam("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".dcm")) {
            return ResponseEntity.badRequest().body("Invalid file: expected .dcm extension");
        }

        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            // Validate DICOM magic bytes (128-byte preamble + "DICM")
            if (!isDicomFile(is)) {
                return ResponseEntity.badRequest().body("Invalid file: not a valid DICOM file");
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error validating DICOM file.");
        }

        try (InputStream is = file.getInputStream()) {
            DicomInputStream dis = new DicomInputStream(is);

            Attributes dataset = dis.readDataset(-1, Tag.PixelData);

            String patientName = dataset.getString(Tag.PatientName);
            String studyInstanceUID = dataset.getString(Tag.StudyInstanceUID);
            String sopInstanceUID = dataset.getString(Tag.SOPInstanceUID);

            String filePath = storageService.store(file);

            aiQueueService.enqueueForAnalysis(studyInstanceUID, sopInstanceUID, filePath, patientName);

            return ResponseEntity.accepted().body("Upload accepted. Analysis queued.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Internal error processing DICOM file. Contact support.");
        }
    }

    private boolean isDicomFile(InputStream is) throws IOException {
        is.mark(DICOM_PREAMBLE_SIZE + DICOM_MAGIC.length);
        byte[] header = new byte[DICOM_PREAMBLE_SIZE + DICOM_MAGIC.length];
        int bytesRead = is.read(header);
        is.reset();

        if (bytesRead < header.length) {
            return false;
        }

        for (int i = 0; i < DICOM_MAGIC.length; i++) {
            if (header[DICOM_PREAMBLE_SIZE + i] != DICOM_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}
