package io.mosip.registration.controller;

import com.sun.javafx.application.LauncherImpl;
import io.mosip.registration.preloader.ClientPreLoader;

import java.nio.file.Path;
import java.util.Properties;


public class Initialization {

    public static void main(String[] args) throws Exception {
        // CEIN: force French as the only UI / registration language
        System.setProperty("mosip.registration.language.primary", "fra");
        System.setProperty("mosip.registration.ui.language", "fra");
        System.setProperty("mosip.registration.default.language", "fra");
        System.setProperty("mosip.registration.multilang.enabled", "false");
        System.setProperty("mosip.registration.language.secondary", "");
        System.setProperty("mosip.registration.language.fallback.enabled", "false");
        System.out.println("*** INTERFACE FRANÇAISE ACTIVÉE ***");
        System.out.println("Langue système: " + System.getProperty("user.language"));
        System.out.println("Langue MOSIP: " + System.getProperty("mosip.registration.language.primary"));
        System.out.println("=== DEBUG PROPERTIES MOSIP ===");
        Properties props = System.getProperties();
        props.stringPropertyNames().stream()
                .filter(key -> key.contains("mosip") || key.contains("language"))
                .sorted()
                .forEach(key -> System.out.println(key + " = " + props.getProperty(key)));
        System.out.println("=== FIN DEBUG PROPERTIES ===");
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("logback.configurationFile", Path.of("lib", "logback.xml").toFile().getCanonicalPath());   //NOSONAR Setting logger configuration file path here.
        LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);
    }
}
