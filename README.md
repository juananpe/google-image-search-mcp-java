# Google Image Search MCP (Java)

Java port of [google-image-search-mcp-python](https://github.com/juananpe/google-image-search-mcp-python).
Same two tools and behavior, built on the official [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) (stdio transport).

## Requirements

- Java 21+ and Maven 3.9+
- A SerpAPI key (https://serpapi.com/dashboard)

## Build

```bash
mvn package
```

Produces a runnable fat jar: `target/google-image-search-mcp-java-1.0.0.jar`.

## Configuration

Set `SERP_API_KEY` as an environment variable, or put it in a `.env` file in the working directory:

```
SERP_API_KEY=your_api_key_here
```

## Run

```bash
SERP_API_KEY=xxx java -jar target/google-image-search-mcp-java-1.0.0.jar
```

Test with the inspector:

```bash
npx @modelcontextprotocol/inspector java -jar target/google-image-search-mcp-java-1.0.0.jar
```

VSCode `mcp.json`:

```json
{
  "servers": {
    "search-images-java": {
      "command": "java",
      "args": ["-jar", "/path/to/google-image-search-mcp-java-1.0.0.jar"],
      "env": { "SERP_API_KEY": "your_api_key_here" }
    }
  }
}
```

## Tools

1. `search_images_tool(query, limit=10)`: search Google Images via SerpAPI.
2. `download_image_tool(image_url, output_path, filename)`: download an image, creating the directory if needed.

Errors are returned with `isError: true`. Logs go to stderr (stdout is reserved for the MCP protocol).

## Python to Java mapping

| Python | Java |
|---|---|
| `main.py` (FastMCP) | `Main.java` (MCP Java SDK, sync server) |
| `api.py` (aiohttp) | `ImageApi.java` (`java.net.http.HttpClient`) |
| `models.py` (TypedDict) | `ImageSearchResult.java` (record) |
| python-dotenv | small `.env` loader in `Main` |
