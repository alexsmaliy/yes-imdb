package com.alexsmaliy.yesimdb.persistence;

import com.alexsmaliy.yesimdb.app.YesImdbConfiguration;
import io.dropwizard.lifecycle.Managed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class LuceneIndexDirManager implements Managed {
    private final YesImdbConfiguration configuration;

    public LuceneIndexDirManager(YesImdbConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() {
        makeLuceneIndexDir(configuration);
    }
    @Override
    public void stop() { /* nothing */ }

    private static void makeLuceneIndexDir(YesImdbConfiguration configuration) {
        Set<PosixFilePermission> perms =
            PosixFilePermissions.fromString("rwxrwxrwx");
        FileAttribute<Set<PosixFilePermission>> fileAttributes =
            PosixFilePermissions.asFileAttribute(perms);
        Path indexDir = configuration.getApplicationConfiguration().lucene().indexesRootDir();
        if (!Files.exists(indexDir, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.createDirectories(indexDir, fileAttributes);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create directory for Lucene indexes!", e);
            }
        }
        if (!Files.isDirectory(indexDir, LinkOption.NOFOLLOW_LINKS) || !Files.isWritable(indexDir)) {
            throw new RuntimeException("Something already exists at the path configured for Lucene indexes, but is not  writable directory!");
        }
    }
}
