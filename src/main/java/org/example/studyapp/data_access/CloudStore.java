package org.example.studyapp.data_access;

import org.example.studyapp.use_case.storage.StudySetDataAccessInterface;
import org.example.studyapp.use_case.account.AccountDataAccessInterface;
import com.google.gson.*;
import org.example.studyapp.entity.StudySet;
import java.util.*;

/** Supabase Auth + PostgREST. Tokens stay in memory and RLS isolates each account. */
public final class CloudStore implements StudySetDataAccessInterface, AccountDataAccessInterface {
    private final Config config;
    private final ApiClient api = new ApiClient();
    private final Gson gson = new Gson();
    private String token;
    private String userId;
    public CloudStore(Config config) { this.config = config; }
    private String url() {
        String url = config.require("SUPABASE_URL").replaceAll("/+$", "");
        if (!url.matches("https://[a-zA-Z0-9.-]+")) throw new IllegalArgumentException("SUPABASE_URL must be an HTTPS project URL.");
        return url;
    }
    private Map<String, String> headers() {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", config.require("SUPABASE_ANON_KEY"));
        if (token != null) headers.put("Authorization", "Bearer " + token);
        return headers;
    }
    public void authenticate(String email, String password, boolean signUp) throws Exception {
        if (email.isBlank() || password.isBlank()) throw new IllegalArgumentException("Enter an email and password.");
        JsonObject result = api.request("POST", url() + (signUp ? "/auth/v1/signup" : "/auth/v1/token?grant_type=password"),
                headers(), gson.toJsonTree(Map.of("email", email, "password", password))).getAsJsonObject();
        if (!result.has("access_token") || result.get("access_token").isJsonNull()) {
            throw new IllegalStateException("Account created. Confirm your email, then sign in.");
        }
        String nextToken = result.get("access_token").getAsString();
        String nextUser = result.getAsJsonObject("user").get("id").getAsString();
        UUID.fromString(nextUser);
        token = nextToken; userId = nextUser;
    }
    private void requireLogin() {
        if (token == null) throw new IllegalStateException("Sign in first. If the session expired, sign in again.");
    }
    public void save(List<StudySet> sets) throws Exception {
        requireLogin();
        JsonArray rows = new JsonArray();
        for (StudySet set : sets) {
            set.validate();
            JsonObject row = new JsonObject();
            row.addProperty("id", set.id); row.addProperty("user_id", userId); row.add("data", gson.toJsonTree(set));
            rows.add(row);
        }
        if (sets.isEmpty()) return;
        Map<String, String> headers = headers();
        headers.put("Prefer", "resolution=merge-duplicates,return=minimal");
        api.request("POST", url() + "/rest/v1/study_sets?on_conflict=user_id,id", headers, rows);
    }
    public List<StudySet> load() throws Exception {
        requireLogin();
        JsonArray rows = api.request("GET", url() + "/rest/v1/study_sets?select=data&user_id=eq." + userId,
                headers(), null).getAsJsonArray();
        List<StudySet> sets = new ArrayList<>();
        for (JsonElement row : rows) {
            StudySet set = gson.fromJson(row.getAsJsonObject().get("data"), StudySet.class);
            set.validate(); sets.add(set);
        }
        return sets;
    }
    public void signOut() { token = null; userId = null; }
}
