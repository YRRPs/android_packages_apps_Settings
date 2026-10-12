/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ad-block sources, allowlist, and update status, stored as tab-separated records in
 * {@code state.tsv}, plus one cache file per source. Pure Java; the caller picks the directory.
 * A record that does not parse, or that fails the rules {@link State#addSource} and
 * {@link State#addAllowed} apply, is skipped on load.
 */
final class YrrpAdBlockStore {
    static final List<String> DEFAULT_URLS = List.of(
            "https://adaway.org/hosts.txt",
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0"
                    + "&mimetype=plaintext");
    static final int MAX_SOURCES = 20;
    static final int MAX_ALLOWED = 1000;
    private static final String STATE_FILE = "state.tsv";
    private static final String CACHE_DIR = "cache";

    enum AddResult { OK, NOT_HTTPS, MALFORMED, DUPLICATE, LIMIT, INVALID_HOST }

    /** Everything the page, the tile, and the updater share. */
    static final class State {
        final List<YrrpAdBlockSource> sources = new ArrayList<>();
        final Set<String> allowlist = new TreeSet<>();
        boolean autoUpdate = true;
        long lastUpdateMillis;
        String lastResult = "";
        /** Block lines in the last hosts.blocked built. */
        int lastEntries;

        /** Adds an enabled source; only an https URL with a host and no user info. */
        AddResult addSource(String rawUrl) {
            final String url = rawUrl.trim();
            final AddResult valid = checkUrl(url);
            if (valid != AddResult.OK) {
                return valid;
            }
            if (findSource(url) != null) {
                return AddResult.DUPLICATE;
            }
            if (sources.size() >= MAX_SOURCES) {
                return AddResult.LIMIT;
            }
            sources.add(new YrrpAdBlockSource(url));
            return AddResult.OK;
        }

        YrrpAdBlockSource findSource(String url) {
            for (YrrpAdBlockSource source : sources) {
                if (source.url.equals(url)) {
                    return source;
                }
            }
            return null;
        }

        /** Adds a normalized name the hosts file must never block. */
        AddResult addAllowed(String rawHost) {
            final String host = YrrpAdBlockHostnames.normalize(rawHost.trim());
            if (host == null) {
                return AddResult.INVALID_HOST;
            }
            if (allowlist.contains(host)) {
                return AddResult.DUPLICATE;
            }
            if (allowlist.size() >= MAX_ALLOWED) {
                return AddResult.LIMIT;
            }
            allowlist.add(host);
            return AddResult.OK;
        }

        boolean removeAllowed(String host) {
            return allowlist.remove(host);
        }
    }

    private final File mDir;

    YrrpAdBlockStore(File dir) {
        mDir = dir;
    }

    /** OK only for an https URL with a host and no user info. */
    static AddResult checkUrl(String url) {
        final URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return AddResult.MALFORMED;
        }
        if (uri.getScheme() == null) {
            return AddResult.MALFORMED;
        }
        if (!"https".equals(uri.getScheme().toLowerCase(Locale.ROOT))) {
            return AddResult.NOT_HTTPS;
        }
        if (uri.getHost() == null || uri.getRawUserInfo() != null) {
            return AddResult.MALFORMED;
        }
        return AddResult.OK;
    }

    /**
     * The saved state, or the defaults when nothing is saved yet. Throws when a saved file
     * cannot be read, so a caller never saves defaults over the user's lists.
     */
    State load() throws IOException {
        final File file = new File(mDir, STATE_FILE);
        if (!file.exists()) {
            return defaults();
        }
        final State state = new State();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                readRecord(line.split("\t", -1), state);
            }
        }
        return state;
    }

    /** For display only: the saved state, or the defaults when it cannot be read. */
    State loadOrDefaults() {
        try {
            return load();
        } catch (IOException e) {
            return defaults();
        }
    }

    void save(State state) throws IOException {
        mDir.mkdirs();
        YrrpAdBlockFiles.writeAtomically(new File(mDir, STATE_FILE), out -> writeState(out, state));
    }

    /** Removes the source and its cache. Returns false if no source has that URL. */
    boolean removeSource(State state, String url) {
        final YrrpAdBlockSource source = state.findSource(url);
        if (source == null) {
            return false;
        }
        state.sources.remove(source);
        cacheFile(source).delete();
        return true;
    }

    File cacheFile(YrrpAdBlockSource source) {
        return new File(new File(mDir, CACHE_DIR), "src-" + shortHash(source.url) + ".hosts");
    }

    private static State defaults() {
        final State state = new State();
        for (String url : DEFAULT_URLS) {
            state.sources.add(new YrrpAdBlockSource(url));
        }
        return state;
    }

    private static void readRecord(String[] f, State state) {
        try {
            switch (f[0]) {
                case "A" -> {
                    state.autoUpdate = "1".equals(f[1]);
                    state.lastUpdateMillis = Long.parseLong(f[2]);
                    state.lastResult = f[3];
                    state.lastEntries = f.length > 4 ? Integer.parseInt(f[4]) : 0;
                }
                case "S" -> readSource(f, state);
                case "H" -> state.addAllowed(f[1]);
                default -> { }
            }
        } catch (RuntimeException e) {
            // A short or non-numeric record is skipped; the rest of the file still loads.
        }
    }

    private static void readSource(String[] f, State state) {
        final YrrpAdBlockSource source = new YrrpAdBlockSource(f[1]);
        source.enabled = "1".equals(f[2]);
        source.etag = f[3];
        source.lastModified = f[4];
        source.status = f[5];
        source.entries = Integer.parseInt(f[6]);
        source.redirectsDropped = Integer.parseInt(f[7]);
        source.lastSuccessMillis = Long.parseLong(f[8]);
        if (checkUrl(source.url) == AddResult.OK && state.findSource(source.url) == null
                && state.sources.size() < MAX_SOURCES) {
            state.sources.add(source);
        }
    }

    private static void writeState(Writer out, State state) throws IOException {
        out.write("V\t1\n");
        out.write(String.join("\t", "A", state.autoUpdate ? "1" : "0",
                Long.toString(state.lastUpdateMillis), clean(state.lastResult),
                Integer.toString(state.lastEntries)) + "\n");
        for (YrrpAdBlockSource s : state.sources) {
            out.write(String.join("\t", "S", s.url, s.enabled ? "1" : "0", clean(s.etag),
                    clean(s.lastModified), clean(s.status), Integer.toString(s.entries),
                    Integer.toString(s.redirectsDropped), Long.toString(s.lastSuccessMillis))
                    + "\n");
        }
        for (String host : state.allowlist) {
            out.write("H\t" + host + "\n");
        }
    }

    /** A value holding a control character is stored empty rather than breaking the record. */
    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < 0x20 || value.charAt(i) == 0x7f) {
                return "";
            }
        }
        return value;
    }

    private static String shortHash(String text) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format(Locale.ROOT, "%02x", digest[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
