package com.pipeline.crm.avatar;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** A tiny stand-in for t.me and its CDN: each handle is told what to answer. Unknown handles have no photo. */
final class TelegramStub {

    enum Mode { PHOTO, NO_PHOTO, ERROR, FOREIGN_HOST }

    private record Behaviour(Mode mode, Color color) {
    }

    private final HttpServer server;
    private final Map<String, Behaviour> handles = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> pageHits = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> photoDrops = new ConcurrentHashMap<>();

    TelegramStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/", this::handle);
        server.start();
    }

    void stop() {
        server.stop(0);
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void photo(String handle, Color color) {
        handles.put(handle, new Behaviour(Mode.PHOTO, color));
    }

    void set(String handle, Mode mode) {
        handles.put(handle, new Behaviour(mode, Color.GRAY));
    }

    /** Like the real t.me now and then: the next {@code count} pages omit the photo although the profile has one. */
    void dropPhotoFromNextPages(String handle, int count) {
        photoDrops.put(handle, new AtomicInteger(count));
    }

    int pageHits(String handle) {
        AtomicInteger hits = pageHits.get(handle);
        return hits == null ? 0 : hits.get();
    }

    private void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.startsWith("/img/")) {
            String handle = path.substring("/img/".length(), path.length() - ".jpg".length());
            Behaviour b = handles.get(handle);
            respond(ex, 200, "image/jpeg", jpeg(640, 480, b != null ? b.color() : Color.GRAY));
            return;
        }
        String handle = path.substring(1);
        pageHits.computeIfAbsent(handle, h -> new AtomicInteger()).incrementAndGet();
        Behaviour b = handles.getOrDefault(handle, new Behaviour(Mode.NO_PHOTO, Color.GRAY));
        AtomicInteger drops = photoDrops.get(handle);
        if (b.mode() == Mode.PHOTO && drops != null && drops.getAndDecrement() > 0) {
            respond(ex, 200, "text/html", page("https://telegram.org/img/t_logo.png", false));
            return;
        }
        switch (b.mode()) {
            case PHOTO -> respond(ex, 200, "text/html", page(baseUrl() + "/img/" + handle + ".jpg", true));
            case FOREIGN_HOST -> respond(ex, 200, "text/html", page("https://example.com/" + handle + ".jpg", true));
            case NO_PHOTO -> respond(ex, 200, "text/html", page("https://telegram.org/img/t_logo.png", false));
            case ERROR -> respond(ex, 500, "text/plain", "oops".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static byte[] page(String ogImage, boolean withPhoto) {
        String html = "<html><head><meta property=\"og:image\" content=\"" + ogImage + "\"></head><body>"
                + (withPhoto ? "<img class=\"tgme_page_photo_image\" src=\"" + ogImage + "\">" : "")
                + "</body></html>";
        return html.getBytes(StandardCharsets.UTF_8);
    }

    static byte[] jpeg(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void respond(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }
}
