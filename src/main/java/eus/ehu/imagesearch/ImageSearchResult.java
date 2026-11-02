package eus.ehu.imagesearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One entry of SerpAPI's {@code images_results} array (port of models.py). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageSearchResult(
        int position,
        String thumbnail,
        String source,
        String title,
        String link,
        String original,
        @JsonProperty("is_product") boolean isProduct,
        String size,
        @JsonProperty("original_width") Integer originalWidth,
        @JsonProperty("original_height") Integer originalHeight) {
}
