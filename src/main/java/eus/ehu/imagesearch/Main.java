package eus.ehu.imagesearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Google Image Search MCP server (stdio). Java port of main.py.
 * NOTE: stdout carries the MCP protocol, so all logging goes to stderr.
 */
public class Main {

    public static void main(String[] args) {
        String apiKey = loadEnv("SERP_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[Error] Missing SERP_API_KEY in environment variables");
            System.exit(1);
        }

        ImageApi api = new ImageApi(apiKey);
        McpJsonMapper jsonMapper = McpJsonDefaults.getMapper();

        McpSyncServer server = McpServer.sync(new StdioServerTransportProvider(jsonMapper))
                .serverInfo("google-image-search", "1.0.0")
                .capabilities(ServerCapabilities.builder().tools(false).build())
                .tools(searchTool(api), downloadTool(api))
                .build();

        System.err.println("[Server] google-image-search running on stdio");
        // The stdio transport keeps the process alive until stdin closes.
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    }

    // ---- search_images_tool ------------------------------------------------

    private static SyncToolSpecification searchTool(ImageApi api) {
        Tool tool = Tool.builder("search_images_tool", schema(
                        Map.of(
                                "query", prop("string", "The search query for finding images"),
                                "limit", prop("integer", "Maximum number of results to return (default: 10)")),
                        List.of("query")))
                .description("Search for images using Google Image Search")
                .build();

        return SyncToolSpecification.builder().tool(tool).callHandler((exchange, request) -> {
            Map<String, Object> a = request.arguments();
            String query = (String) a.get("query");
            int limit = a.get("limit") instanceof Number n ? n.intValue() : 10;
            try {
                System.err.println("[Tool] Executing search_images with query: '" + query + "', limit: " + limit);
                List<ImageSearchResult> results = api.searchImages(query, limit);
                return CallToolResult.builder()
                        .addTextContent("Found " + results.size() + " images for query '" + query + "':")
                        .addTextContent(api.toJson(results))
                        .build();
            } catch (Exception e) {
                System.err.println("[Error] search_images failed: " + e.getMessage());
                return error("Failed to search for images: " + e.getMessage());
            }
        }).build();
    }

    // ---- download_image_tool -----------------------------------------------

    private static SyncToolSpecification downloadTool(ImageApi api) {
        Tool tool = Tool.builder("download_image_tool", schema(
                        Map.of(
                                "image_url", prop("string", "URL of the image to download"),
                                "output_path", prop("string", "Directory path where the image should be saved"),
                                "filename", prop("string",
                                        "Filename for the downloaded image (including extension)")),
                        List.of("image_url", "output_path", "filename")))
                .description("Download an image to a local directory")
                .build();

        return SyncToolSpecification.builder().tool(tool).callHandler((exchange, request) -> {
            Map<String, Object> a = request.arguments();
            String url = (String) a.get("image_url");
            try {
                System.err.println("[Tool] Executing download_image for URL: " + url);
                String saved = api.downloadImage(url, (String) a.get("output_path"), (String) a.get("filename"));
                return CallToolResult.builder()
                        .addTextContent("Image successfully downloaded to: " + saved)
                        .build();
            } catch (Exception e) {
                System.err.println("[Error] download_image failed: " + e.getMessage());
                return error("Failed to download image: " + e.getMessage());
            }
        }).build();
    }

    // ---- helpers -----------------------------------------------------------

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        return Map.of("type", "object", "properties", properties, "required", required,
                "additionalProperties", false);
    }

    private static Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private static CallToolResult error(String message) {
        return CallToolResult.builder().addTextContent(message).isError(true).build();
    }

    /** Environment variable first, then a .env file in the working directory (like python-dotenv). */
    private static String loadEnv(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        Path dotenv = Path.of(".env");
        if (!Files.isRegularFile(dotenv)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(dotenv)) {
                line = line.strip();
                if (line.startsWith("#") || !line.startsWith(key + "=")) {
                    continue;
                }
                String v = line.substring(key.length() + 1).strip();
                if (v.length() >= 2 && (v.startsWith("\"") && v.endsWith("\"") || v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length() - 1);
                }
                return v;
            }
        } catch (IOException e) {
            System.err.println("[Error] Cannot read .env: " + e.getMessage());
        }
        return null;
    }
}
