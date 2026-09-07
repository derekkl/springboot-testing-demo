package com.example.orderapi.functional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Two modes, both genuinely end-to-end (no in-process shortcuts either way):
 *
 * <p>ORDERAPI_BASE_URL - point at an already-running instance over the
 * network (e.g. the real Service inside an OpenShift cluster:
 * http://order-api-springboot:8080). No process is spawned; this is what
 * runs as an OpenShift Job hitting the actual deployed pod.
 *
 * <p>ORDERAPI_JAR_PATH - spawn the published app as a local OS process on a
 * real port and hit that instead. This is what CI/local dev uses when
 * there's nothing already deployed.
 *
 * <p>Exactly one of the two should be set.
 */
public final class ApiClient {

    private static final String LOCAL_BASE_URL = "http://localhost:8181";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String baseUrl;
    private Process process;

    private ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static ApiClient start() throws IOException, InterruptedException {
        String remoteBaseUrl = System.getenv("ORDERAPI_BASE_URL");
        String jarPath = System.getenv("ORDERAPI_JAR_PATH");

        ApiClient client;
        if (remoteBaseUrl != null && !remoteBaseUrl.isBlank()) {
            client = new ApiClient(remoteBaseUrl);
        } else if (jarPath != null && !jarPath.isBlank()) {
            client = new ApiClient(LOCAL_BASE_URL);
            ProcessBuilder builder = new ProcessBuilder(
                    "java", "-jar", jarPath, "--server.port=8181");
            builder.redirectErrorStream(true);
            builder.inheritIO();
            client.process = builder.start();
        } else {
            throw new IllegalStateException(
                    "Set either ORDERAPI_BASE_URL (to test an already-running instance, "
                            + "e.g. in OpenShift) or ORDERAPI_JAR_PATH (to spawn a local "
                            + "published build) before running functional tests.");
        }

        client.waitUntilHealthy();
        return client;
    }

    private void waitUntilHealthy() throws IOException, InterruptedException {
        Instant deadline = Instant.now().plusSeconds(30);
        Exception lastError = null;

        while (Instant.now().isBefore(deadline)) {
            if (process != null && !process.isAlive()) {
                throw new IllegalStateException(
                        "OrderApi process exited early with code " + process.exitValue());
            }
            try {
                HttpResponse<String> response = httpClient.send(
                        HttpRequest.newBuilder(URI.create(baseUrl + "/health")).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (Exception ex) {
                lastError = ex;
            }
            Thread.sleep(500);
        }

        throw new IllegalStateException("OrderApi did not become healthy within 30 seconds.", lastError);
    }

    public HttpResponse<String> post(String path, String jsonBody) throws IOException, InterruptedException {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }
}
