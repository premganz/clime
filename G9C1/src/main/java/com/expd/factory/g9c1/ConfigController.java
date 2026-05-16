package com.expd.factory.g9c1;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Tiny persistence shim. Writes the front-end config (a JSON blob) to a
 * single file on disk so it survives a page refresh / restart.
 *
 * Storage location: ./data/config.json relative to the working directory.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final Path CONFIG_PATH = Paths.get("data", "config.json");

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> load() throws Exception {
        if (!Files.exists(CONFIG_PATH)) {
            return ResponseEntity.ok("{}");
        }
        String body = new String(Files.readAllBytes(CONFIG_PATH));
        return ResponseEntity.ok(body);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> save(@RequestBody String body) throws Exception {
        Files.createDirectories(CONFIG_PATH.getParent());
        Files.write(CONFIG_PATH, body.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        return ResponseEntity.ok("{\"saved\":true,\"path\":\"" + CONFIG_PATH.toString().replace("\\", "/") + "\"}");
    }
}
