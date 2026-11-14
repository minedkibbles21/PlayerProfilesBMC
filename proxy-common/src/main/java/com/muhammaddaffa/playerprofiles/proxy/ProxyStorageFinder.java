package com.muhammaddaffa.playerprofiles.proxy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class ProxyStorageFinder {

    private static final String[] SUB_DIRECTORIES = new String[]{
            "",
            "playerdata",
            "players/data",
            "world/playerdata",
            "world/players/data"
    };

    public static File locatePlayerFile(File rootFolder, UUID playerUuid) {
        if (rootFolder == null || playerUuid == null) {
            return null;
        }

        String targetFileName = playerUuid + ".dat";
        for (String subDir : SUB_DIRECTORIES) {
            File searchDirectory = subDir.isEmpty() ? rootFolder : new File(rootFolder, subDir);
            File candidateFile = new File(searchDirectory, targetFileName);
            if (candidateFile.exists() && candidateFile.isFile()) {
                return candidateFile;
            }
        }
        return null;
    }

    public static byte[] readPlayerBytes(File rootFolder, UUID playerUuid) {
        File foundFile = locatePlayerFile(rootFolder, playerUuid);
        if (foundFile == null) {
            return null;
        }
        try {
            return Files.readAllBytes(foundFile.toPath());
        } catch (IOException readException) {
            return null;
        }
    }
}
