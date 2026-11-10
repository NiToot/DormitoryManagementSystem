package com.example.springboot;

import com.example.springboot.security.RichTextSanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class RichTextSanitizerTests {
    @ParameterizedTest
    @ValueSource(strings = {
            "<p onclick='alert(1)' style='background:url(https://example.com/track)'>正文</p>",
            "<a href='java&#x73;cript:alert(1)'>正文</a>",
            "<a href='data:text/html,unsafe'>正文</a>",
            "<a href='//example.com/path'>正文</a>",
            "<img src='https://example.com/track' onerror='alert(1)'>正文",
            "<iframe srcdoc='<script>alert(1)</script>'></iframe>正文",
            "<svg onload='alert(1)'><script>alert(2)</script></svg>正文",
            "<math><mtext><img src=x onerror='alert(1)'></mtext></math>正文"
    })
    void unsafeMarkupAndProtocolsAreRemoved(String html) {
        String cleaned = RichTextSanitizer.forWrite(html);
        assertFalse(cleaned.contains("alert"), cleaned);
        assertFalse(cleaned.contains("href="), cleaned);
        assertFalse(cleaned.contains("src="), cleaned);
        assertFalse(cleaned.contains("style="), cleaned);
        assertFalse(cleaned.contains("<svg"), cleaned);
        assertFalse(cleaned.contains("<math"), cleaned);
        assertFalse(cleaned.contains("<iframe"), cleaned);
        assertTrue(cleaned.contains("正文"), cleaned);
        assertEquals(cleaned, RichTextSanitizer.forDisplay(cleaned));
    }

    @Test void editorFormattingAndHttpsLinksSurviveRoundTrip() {
        String html = "<h2>标题</h2><p><strong>重点</strong><br><em>说明</em></p>"
                + "<ul><li>项目</li></ul><table><tbody><tr><td>房间</td></tr></tbody></table>"
                + "<a href=\"https://example.com/help\" title=\"说明\">帮助</a>";
        String cleaned = RichTextSanitizer.forWrite(html);
        assertTrue(cleaned.contains("<h2>标题</h2>"));
        assertTrue(cleaned.contains("<strong>重点</strong>"));
        assertTrue(cleaned.contains("<li>项目</li>"));
        assertTrue(cleaned.contains("<td>房间</td>"));
        assertTrue(cleaned.contains("href=\"https://example.com/help\""));
        assertTrue(cleaned.contains("rel=\"nofollow noopener noreferrer\""));
        assertEquals(cleaned, RichTextSanitizer.forDisplay(cleaned));
    }

    @Test void plainTextAndEmptyValuesArePreserved() {
        assertNull(RichTextSanitizer.forWrite(null));
        assertEquals("", RichTextSanitizer.forWrite(""));
        assertEquals("温度 &lt; 30 &amp; 门窗完好", RichTextSanitizer.forWrite("温度 < 30 & 门窗完好"));
    }

    @Test void lengthLimitAppliesToNewWritesWithoutTruncatingLegacyReads() {
        assertEquals(20000, RichTextSanitizer.forWrite("x".repeat(20000)).length());
        assertThrows(ResponseStatusException.class, () -> RichTextSanitizer.forWrite("x".repeat(20001)));
        assertEquals(20001, RichTextSanitizer.forDisplay("x".repeat(20001)).length());
    }
}
