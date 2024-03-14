package uk.gov.companieshouse.exemptions.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.springframework.util.FileCopyUtils;

public class FileReaderUtil {
    public static String readFile(String path) {
        String data;
        try {
            data = FileCopyUtils.copyToString(new InputStreamReader(new FileInputStream(path)));
        } catch (IOException e) {
            data = null;
        }
        return data;
    }
}
