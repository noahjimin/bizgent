package com.bizgent.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** 업로드 파일(매장·상품 사진, 매출 파일, 영수증)을 uploads/ 폴더에 저장한다. uploads/는 Git에 올리지 않는다. */
@Component
public class FileStorage {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final Path root;

    public FileStorage(@Value("${bizgent.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path getRoot() {
        return root;
    }

    /**
     * 이미지 파일을 저장하고 화면에서 쓸 URL(/uploads/...)을 돌려준다.
     *
     * @return 파일이 없거나 이미지가 아니면 null
     */
    public String saveImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String ext = extension(file.getOriginalFilename());
        if (!IMAGE_EXTENSIONS.contains(ext)) {
            return null;
        }
        String name = UUID.randomUUID() + "." + ext;
        try {
            Path dir = root.resolve(folder);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException("파일을 저장하지 못했습니다", e);
        }
        return "/uploads/" + folder + "/" + name;
    }

    private static String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
