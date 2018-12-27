package core

import groovy.transform.SourceURI

import java.nio.file.Path
import java.nio.file.Paths


class FileUtils {
    public static final String INTERNAL_FILE_ROOT_DIR = "misc";
    public static final Path INSTALLATION_ROOT;
    static {
        @SourceURI
        URI src;
        INSTALLATION_ROOT = Paths.get(src).getParent().getParent().resolve(INTERNAL_FILE_ROOT_DIR)
    }

    static File getInternalFile(String name) {
        def path = INSTALLATION_ROOT.resolve(name);
        def file = path.toFile();
        if (!file.exists()) throw new RuntimeException("File not found $path")
        return file;
    }


}
