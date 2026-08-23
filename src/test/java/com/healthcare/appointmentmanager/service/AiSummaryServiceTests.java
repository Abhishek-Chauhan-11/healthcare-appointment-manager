package com.healthcare.appointmentmanager.service;

import com.healthcare.appointmentmanager.model.UrgencyLevel;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiSummaryServiceTests {

    @Test
    void generatesSummaryWithGemini() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiSummaryService service = new AiSummaryService(
                builder,
                "test-gemini-key",
                "gemini-3.5-flash-lite",
                "https://gemini.test/v1beta/",
                "",
                "gpt-4.1-mini",
                "https://api.openai.com/v1"
        );

        server.expect(requestTo("https://gemini.test/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-gemini-key"))
                .andRespond(withSuccess(
                        """
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "Urgency: Medium; Chief complaint: Headache; Important details: Two days; Suggested questions: When did it begin? Is it worsening? Any medicines? Safety note: This is not a diagnosis."
                                  }
                                ]
                              }
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        AiSummaryResult result = service.generatePreVisitSummary("Headache for two days");

        assertTrue(result.generatedByLlm());
        assertEquals(UrgencyLevel.MEDIUM, result.urgencyLevel());
        assertTrue(result.summary().contains("Chief complaint:"));
        server.verify();
    }

    @Test
    void usesSafetyFallbackWhenNoProviderKeyIsConfigured() {
        AiSummaryService service = new AiSummaryService(
                RestClient.builder(),
                "",
                "gemini-3.5-flash-lite",
                "https://generativelanguage.googleapis.com/v1beta",
                "",
                "gpt-4.1-mini",
                "https://api.openai.com/v1"
        );

        AiSummaryResult result = service.generatePreVisitSummary(
                "Mild headache for two days and occasional tiredness. No chest pain or difficulty breathing."
        );

        assertFalse(result.generatedByLlm());
        assertEquals(UrgencyLevel.LOW, result.urgencyLevel());
        assertTrue(result.summary().contains("This summary is not a diagnosis"));
    }

    @Test
    void keepsNonNegatedEmergencySymptomsHighUrgency() {
        AiSummaryService service = new AiSummaryService(
                RestClient.builder(),
                "",
                "gemini-3.5-flash-lite",
                "https://generativelanguage.googleapis.com/v1beta",
                "",
                "gpt-4.1-mini",
                "https://api.openai.com/v1"
        );

        AiSummaryResult result = service.generatePreVisitSummary("Severe chest pain since this morning");

        assertEquals(UrgencyLevel.HIGH, result.urgencyLevel());
    }
}
