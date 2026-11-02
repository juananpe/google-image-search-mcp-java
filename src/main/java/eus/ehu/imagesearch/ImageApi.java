package eus.ehu.imagesearch;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** SerpAPI search + image download (port of api.py). */
public class ImageApi {

    private static final String SERP_URL = "https://serpapi.com/search";

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final String apiKey;

    public ImageApi(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<ImageSearchResult> searchImages(String query, int limit) throws IOException, InterruptedException {
        System.err.println("[API] Searching for images with query: '" + query + "'");
        try {
            String url = SERP_URL
                    + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&engine=google_images"
                    + "&api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            raiseForStatus(response);

            JsonNode images = mapper.readTree(response.body()).path("images_results");
            if (!images.isArray() || images.isEmpty()) {
                throw new IllegalStateException("No image results found");
            }

            List<ImageSearchResult> all = mapper.readerForListOf(ImageSearchResult.class).readValue(images);
            return all.subList(0, Math.max(0, Math.min(limit, all.size())));
        } catch (IOException | InterruptedException | RuntimeException e) {
            System.err.println("[Error] Failed to search images: " + e.getMessage());
            throw e;
        }
    }

    public String toJson(Object value) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

    public String downloadImage(String imageUrl, String outputPath, String filename)
            throws IOException, InterruptedException {
        System.err.println("[API] Downloading image from: " + imageUrl);
        try {
            Path dir = Path.of(outputPath);
            Files.createDirectories(dir);
            Path fullPath = dir.resolve(filename);

            HttpResponse<byte[]> response = http.send(
                    HttpRequest.newBuilder(URI.create(imageUrl)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            raiseForStatus(response);

            Files.write(fullPath, response.body());
            System.err.println("[API] Image downloaded successfully to: " + fullPath);
            return fullPath.toString();
        } catch (IOException | InterruptedException | RuntimeException e) {
            System.err.println("[Error] Failed to download image: " + e.getMessage());
            throw e;
        }
    }

    private static void raiseForStatus(HttpResponse<?> response) throws IOException {
        int status = response.statusCode();
        if (status >= 400) {
            throw new IOException("HTTP " + status);
        }
    }
}
