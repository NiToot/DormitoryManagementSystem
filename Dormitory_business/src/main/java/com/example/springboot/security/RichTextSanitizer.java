package com.example.springboot.security;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Shared policy for HTML persisted by the editor and returned from older records. */
public final class RichTextSanitizer {
    private static final int MAX_LENGTH = 20_000;
    private static final Safelist ALLOWED = new Safelist()
            .addTags("p", "br", "div", "span", "strong", "b", "em", "i", "u", "s", "strike",
                    "blockquote", "pre", "code", "ul", "ol", "li", "hr",
                    "h1", "h2", "h3", "h4", "h5", "h6", "a",
                    "table", "thead", "tbody", "tfoot", "tr", "th", "td")
            .addAttributes("a", "href", "title")
            .addProtocols("a", "href", "http", "https")
            .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer");

    private RichTextSanitizer() { }

    public static String forWrite(String html) {
        if (html != null && html.length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "内容过长，请控制在20000个字符以内（含HTML标签）");
        }
        return forDisplay(html);
    }

    public static String forDisplay(String html) {
        // Null means "leave unchanged" in MyBatis-Plus partial updates.
        if (html == null) return null;
        return Jsoup.clean(html, "", ALLOWED, new Document.OutputSettings().prettyPrint(false));
    }
}
