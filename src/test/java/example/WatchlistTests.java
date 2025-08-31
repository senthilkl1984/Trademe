package com.example;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class WatchlistTests {

    private static Playwright playwright;
    private static APIRequestContext request;

    // Replace with your Sandbox OAuth credentials
    private static final String OAUTH_CONSUMER_KEY = "YOUR_KEY";
    private static final String OAUTH_TOKEN = "YOUR_TOKEN";
    private static final String OAUTH_SIGNATURE = "YOUR_SIGNATURE";

    private static final String LISTING_ID = "1234567"; // Replace with valid Sandbox listing

    @BeforeAll
    static void setup() {
        playwright = Playwright.create();

        request = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL("https://api.tmsandbox.co.nz/v1")
                        .setExtraHTTPHeaders(Map.of(
                                "Authorization",
                                "OAuth oauth_consumer_key=" + OAUTH_CONSUMER_KEY +
                                        ", oauth_token=" + OAUTH_TOKEN +
                                        ", oauth_signature_method=PLAINTEXT" +
                                        ", oauth_signature=" + OAUTH_SIGNATURE
                        ))
        );
    }

    @AfterAll
    static void teardown() {
        if (request != null) request.dispose();
        if (playwright != null) playwright.close();
    }

    @Test
    void testRetrieveListing() {
        APIResponse response = request.get("/Listings/" + LISTING_ID + ".json");
        assertEquals(200, response.status(), "Listing not found. Check ID or credentials.");
        String body = response.text();
        assertTrue(body.contains("\"ListingId\":" + LISTING_ID));
    }

    @Test
    void testAddRemoveAndFilterWatchlist() {
        // Create form data for POST
        FormData formData = new FormData();
        formData.set("ListingId", LISTING_ID);

        // Add listing to watchlist
        APIResponse addResponse = request.post("/MyTradeMe/Watchlist.json",
                RequestOptions.create().setForm(formData)
        );
        assertEquals(201, addResponse.status(), "Failed to add listing to watchlist");

        // Retrieve watchlist
        APIResponse watchlistResponse = request.get("/MyTradeMe/Watchlist.json");
        assertEquals(200, watchlistResponse.status());

        // Filter watchlist for our listing
        List<String> filteredIds = watchlistResponse.json().asList().stream()
                .map(item -> ((Map<String, Object>) item).get("ListingId").toString())
                .filter(id -> id.equals(LISTING_ID))
                .collect(Collectors.toList());

        assertFalse(filteredIds.isEmpty(), "Filtered watchlist does not contain the added listing");

        // Remove listing from watchlist
        APIResponse removeResponse = request.delete("/MyTradeMe/Watchlist/" + LISTING_ID + ".json");
        assertEquals(204, removeResponse.status(), "Failed to remove listing from watchlist");

        // Verify removal
        APIResponse finalWatchlist = request.get("/MyTradeMe/Watchlist.json");
        List<String> finalFiltered = finalWatchlist.json().asList().stream()
                .map(item -> ((Map<String, Object>) item).get("ListingId").toString())
                .filter(id -> id.equals(LISTING_ID))
                .collect(Collectors.toList());

        assertTrue(finalFiltered.isEmpty(), "Listing still exists in filtered watchlist after removal");
    }
}
