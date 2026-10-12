/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Plain JUnit; no Robolectric. A scripted connection stands in for the network. */
public class YrrpAdBlockFetcherTest {
    private static final long CAP = YrrpAdBlockFetcher.MAX_BYTES;

    /** A scripted response for one URL. */
    static final class Reply {
        int code = 200;
        final Map<String, String> headers = new HashMap<>();
        byte[] body = new byte[0];
        long bodyLength = -1;
        long contentLength = -1;
        boolean failOnResponse;
    }

    /** Records what the fetcher did to it. */
    static final class FakeConnection extends HttpURLConnection {
        private final Reply mReply;
        boolean disconnected;

        FakeConnection(URL url, Reply reply) {
            super(url);
            mReply = reply;
        }

        @Override public int getResponseCode() throws IOException {
            if (mReply.failOnResponse) {
                throw new IOException("reset");
            }
            return mReply.code;
        }

        @Override public String getHeaderField(String name) {
            return mReply.headers.get(name);
        }

        @Override public long getContentLengthLong() {
            return mReply.contentLength;
        }

        @Override public InputStream getInputStream() {
            if (mReply.bodyLength >= 0) {
                return new InputStream() {
                    private long mLeft = mReply.bodyLength;

                    @Override public int read() {
                        return mLeft-- > 0 ? 'x' : -1;
                    }

                    @Override public int read(byte[] b, int off, int len) {
                        if (mLeft <= 0) {
                            return -1;
                        }
                        final int n = (int) Math.min(len, mLeft);
                        mLeft -= n;
                        return n;
                    }
                };
            }
            return new ByteArrayInputStream(mReply.body);
        }

        @Override public void disconnect() {
            disconnected = true;
        }

        @Override public boolean usingProxy() {
            return false;
        }

        @Override public void connect() {}
    }

    private final Map<String, Reply> mReplies = new HashMap<>();
    private final List<FakeConnection> mOpened = new ArrayList<>();
    private final YrrpAdBlockFetcher mFetcher = new YrrpAdBlockFetcher(url -> {
        final Reply reply = mReplies.get(url.toString());
        if (reply == null) {
            throw new IOException("unscripted " + url);
        }
        final FakeConnection c = new FakeConnection(url, reply);
        mOpened.add(c);
        return c;
    });
    private File mDir;
    private File mDest;

    @Before
    public void setUp() throws IOException {
        mDir = YrrpAdBlockTestFiles.newDir();
        mDest = new File(mDir, "dest.hosts");
    }

    @After
    public void tearDown() {
        YrrpAdBlockTestFiles.delete(mDir);
    }

    private Reply reply(String url) {
        final Reply reply = new Reply();
        mReplies.put(url, reply);
        return reply;
    }

    @Test
    public void okWritesBodyAndReturnsValidators() throws IOException {
        final Reply r = reply("https://lists.example/h");
        r.body = "0.0.0.0 a.example\n".getBytes(StandardCharsets.UTF_8);
        r.headers.put("ETag", "\"v2\"");
        r.headers.put("Last-Modified", "Mon, 12 Oct 2026 10:00:00 GMT");

        final YrrpAdBlockFetcher.Result result =
                mFetcher.fetch("https://lists.example/h", "", "", mDest);

        assertEquals(YrrpAdBlockFetcher.Status.OK, result.status);
        assertEquals("\"v2\"", result.etag);
        assertEquals("Mon, 12 Oct 2026 10:00:00 GMT", result.lastModified);
        assertEquals("0.0.0.0 a.example\n", YrrpAdBlockTestFiles.read(mDest));
        assertFalse(mOpened.get(0).getInstanceFollowRedirects());
        assertTrue(mOpened.get(0).disconnected);
        assertNull(mOpened.get(0).getRequestProperty("If-None-Match"));
    }

    @Test
    public void sendsConditionalHeadersAndKeepsCacheOn304() throws IOException {
        YrrpAdBlockTestFiles.write(mDest, "old\n");
        reply("https://lists.example/h").code = 304;

        final YrrpAdBlockFetcher.Result result = mFetcher.fetch(
                "https://lists.example/h", "\"v1\"", "Sun, 11 Oct 2026 10:00:00 GMT", mDest);

        assertEquals(YrrpAdBlockFetcher.Status.NOT_MODIFIED, result.status);
        assertEquals("\"v1\"", mOpened.get(0).getRequestProperty("If-None-Match"));
        assertEquals("Sun, 11 Oct 2026 10:00:00 GMT",
                mOpened.get(0).getRequestProperty("If-Modified-Since"));
        assertEquals("old\n", YrrpAdBlockTestFiles.read(mDest));
    }

    @Test
    public void refusesPlainHttpWithoutConnecting() {
        assertEquals(YrrpAdBlockFetcher.Status.NOT_HTTPS,
                mFetcher.fetch("http://lists.example/h", "", "", mDest).status);
        assertTrue(mOpened.isEmpty());
        assertFalse(mDest.exists());
    }

    @Test
    public void refusesRedirectToPlainHttp() {
        final Reply r = reply("https://lists.example/h");
        r.code = 302;
        r.headers.put("Location", "http://lists.example/h");
        assertEquals(YrrpAdBlockFetcher.Status.NOT_HTTPS,
                mFetcher.fetch("https://lists.example/h", "", "", mDest).status);
        assertEquals(1, mOpened.size());
    }

    @Test
    public void followsHttpsRedirects() throws IOException {
        final Reply first = reply("https://lists.example/h");
        first.code = 301;
        first.headers.put("Location", "https://cdn.example/h");
        reply("https://cdn.example/h").body =
                "0.0.0.0 b.example\n".getBytes(StandardCharsets.UTF_8);

        assertEquals(YrrpAdBlockFetcher.Status.OK,
                mFetcher.fetch("https://lists.example/h", "", "", mDest).status);
        assertEquals("0.0.0.0 b.example\n", YrrpAdBlockTestFiles.read(mDest));
    }

    @Test
    public void resolvesRelativeRedirects() {
        final Reply first = reply("https://lists.example/a/h");
        first.code = 307;
        first.headers.put("Location", "/b/h");
        reply("https://lists.example/b/h");
        assertEquals(YrrpAdBlockFetcher.Status.OK,
                mFetcher.fetch("https://lists.example/a/h", "", "", mDest).status);
    }

    @Test
    public void stopsAfterTooManyRedirects() {
        for (int i = 0; i <= YrrpAdBlockFetcher.MAX_REDIRECTS; i++) {
            final Reply r = reply("https://lists.example/" + i);
            r.code = 302;
            r.headers.put("Location", "https://lists.example/" + (i + 1));
        }
        assertEquals(YrrpAdBlockFetcher.Status.TOO_MANY_REDIRECTS,
                mFetcher.fetch("https://lists.example/0", "", "", mDest).status);
    }

    @Test
    public void refusesDeclaredOversizeBody() {
        reply("https://lists.example/h").contentLength = CAP + 1;
        assertEquals(YrrpAdBlockFetcher.Status.TOO_LARGE,
                mFetcher.fetch("https://lists.example/h", "", "", mDest).status);
        assertFalse(mDest.exists());
    }

    @Test
    public void refusesUndeclaredOversizeBodyAndKeepsCache() throws IOException {
        YrrpAdBlockTestFiles.write(mDest, "old\n");
        reply("https://lists.example/h").bodyLength = CAP + 1;
        assertEquals(YrrpAdBlockFetcher.Status.TOO_LARGE,
                mFetcher.fetch("https://lists.example/h", "", "", mDest).status);
        assertEquals("old\n", YrrpAdBlockTestFiles.read(mDest));
        assertFalse(new File(mDest.getPath() + ".tmp").exists());
    }

    @Test
    public void acceptsBodyExactlyAtTheCap() {
        reply("https://lists.example/h").bodyLength = CAP;
        assertEquals(YrrpAdBlockFetcher.Status.OK,
                mFetcher.fetch("https://lists.example/h", "", "", mDest).status);
        assertEquals(CAP, mDest.length());
    }

    @Test
    public void reportsServerErrors() {
        reply("https://lists.example/h").code = 500;
        final YrrpAdBlockFetcher.Result result =
                mFetcher.fetch("https://lists.example/h", "", "", mDest);
        assertEquals(YrrpAdBlockFetcher.Status.HTTP_ERROR, result.status);
        assertEquals(500, result.httpCode);
    }

    @Test
    public void reportsIoErrorsAndKeepsCache() throws IOException {
        YrrpAdBlockTestFiles.write(mDest, "old\n");
        reply("https://lists.example/h").failOnResponse = true;
        assertEquals(YrrpAdBlockFetcher.Status.IO_ERROR,
                mFetcher.fetch("https://lists.example/h", "", "", mDest).status);
        assertEquals("old\n", YrrpAdBlockTestFiles.read(mDest));
    }

    @Test
    public void reportsMalformedUrlsAsIoErrors() {
        assertEquals(YrrpAdBlockFetcher.Status.IO_ERROR,
                mFetcher.fetch("not a url", "", "", mDest).status);
    }
}
