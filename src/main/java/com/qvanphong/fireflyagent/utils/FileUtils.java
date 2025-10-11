package com.qvanphong.fireflyagent.utils;

import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileUtils {

    public static String readFileContent(String path) throws IOException {
        File file = ResourceUtils.getFile(path);
        return new String(Files.readAllBytes(file.toPath()));
    }
}
