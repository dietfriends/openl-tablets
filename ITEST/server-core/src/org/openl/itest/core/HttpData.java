package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpData {
    static final ObjectMapper OBJECT_MAPPER;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.*?)}");

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final Pattern NO_CONTENT_STATUS_PATTERN = Pattern.compile("HTTP/\\S+\\s+204(\\s.*)?");
    private static final Set<String> BLOB_TYPES = Stream.of("application/zip").collect(Collectors.toSet());

    private final String firstLine;
    private final TreeMap<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, String> settings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final byte[] body;
    private String cookie;

    private HttpData(String firstLine, Map<String, String> headers, byte[] body) {
        this.firstLine = firstLine;
        this.headers.putAll(headers);
        var settings = this.headers.subMap("X-OpenL-Test-", "X-OpenL-Test.");
        this.settings.putAll(settings);
        settings.clear();
        this.body = body;
    }

    String getSetting(String key) {
        return settings.get("X-OpenL-Test-" + key);
    }

    private int getResponseCode() {
        if (firstLine == null) {
            return 200; // OK
        }
        String[] status = firstLine.split(" ", 3);
        return Integer.parseInt(status[1]);
    }

    private String getHttpMethod() {
        String[] status = firstLine.split(" ", 3);
        return status[0];
    }

    private String getUrl() {
        String[] status = firstLine.split(" ", 3);
        return status[1];
    }

    static HttpData readFile(String resource) throws IOException {
        try (InputStream input = getStream(resource)) {
            return readData(input, resource);
        }
    }

    static HttpData ok() {
        return new HttpData("HTTP/1.1 200 OK", Collections.emptyMap(), null);
    }

    static HttpData send(URI baseURL, HttpData httpData, String cookie, Map<String, String> localEnv) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL.toString() + httpData.getUrl()))
                .method(httpData.getHttpMethod(), HttpRequest.BodyPublishers.ofByteArray(httpData.body))
                .timeout(Duration.ofMillis(Integer.parseInt(System.getProperty("http.timeout.read"))))
                .header("Host", "example.com");
        httpData.headers.forEach((key, value) -> request.header(key, replacePlaceholders(value, localEnv)));

        if (cookie != null && !cookie.isEmpty() && !httpData.headers.containsKey("Cookie")) {
            request.header("Cookie", cookie);
        }

        var response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Integer.parseInt(System.getProperty("http.timeout.connect"))))
                .build()
                .sendAsync(request.build(), HttpResponse.BodyHandlers.ofByteArray()).join();
        return readData(response);
    }

    private static String replacePlaceholders(String text, Map<String, String> env) {
        var matcher = PLACEHOLDER_PATTERN.matcher(text);

        var result = new StringBuilder();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = env.get(placeholder);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    void writeBodyTo(String responseFile) throws IOException {
        try (var rf = new RandomAccessFile(responseFile, "rw")) {
            while (!rf.readLine().isEmpty()) ; // find the first empty string
            rf.setLength(rf.getFilePointer()); // truncate
            rf.write(body); // append new body
        }
    }

    void assertTo(HttpData expected) throws Exception, AssertionError {
        try {
            assertEquals(expected.getResponseCode(), this.getResponseCode(), "Status code: ");
            for (Map.Entry<String, String> r : expected.headers.entrySet()) {
                String headerName = r.getKey();
                String value = r.getValue();
                Comparators.txt(headerName, value, this.headers.get(headerName));
            }

            if (expected.body == null) {
                return; // No body expected
            }
            String contentEncoding = headers.get("Content-Encoding");
            Function<byte[], byte[]> decoder = Function.identity(); // empty
            if (contentEncoding != null) {
                // Binary encoding
                for (String encoding : contentEncoding.split(",")) {
                    if ("gzip".equals(encoding) || "x-gzip".equals(encoding)) {
                        // decode gzip bytes
                        decoder = decoder.andThen(HttpData::decodeGzipBytes);
                    }
                }
            }
            String contentType = headers.get("Content-Type");
            contentType = contentType == null ? "null" : contentType;
            int sep = contentType.indexOf(';');
            if (sep > 0) {
                contentType = contentType.substring(0, sep);
            }
            switch (contentType) {
                case "text/css":
                case "text/javascript":
                case "text/html":
                case "text/plain":
                case "image/svg+xml":
                    Comparators.txt("Difference", decoder.apply(expected.body), decoder.apply(this.body));
                    break;
                case "application/xml":
                case "text/xml":
                    Comparators.xml("Difference", decoder.apply(expected.body), decoder.apply(this.body));
                    break;
                case "application/json":
                    JsonNode actualNode;
                    actualNode = OBJECT_MAPPER.readTree(decoder.apply(this.body));
                    JsonNode expectedNode = OBJECT_MAPPER.readTree(decoder.apply(expected.body));
                    Comparators.compareJsonObjects(expectedNode, actualNode, "");
                    break;
                case "application/zip":
                    Comparators.zip(decoder.apply(expected.body), decoder.apply(this.body));
                    break;
                default:
                    if (!new String(expected.body, StandardCharsets.ISO_8859_1).trim().equals("***")) {
                        assertArrayEquals(decoder.apply(expected.body), decoder.apply(this.body), "Body: ");
                    }
            }
        } catch (Exception | AssertionError ex) {
            throw ex;
        }
    }

    private static byte[] decodeGzipBytes(byte[] bytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[64 * 1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode GZIP input", e); // wrapper
        }
        return out.toByteArray();
    }

    void log(String resourceName) {
        try {
            System.err.println("--------------------");
            System.err.println(firstLine);
            for (Map.Entry<String, String> r : headers.entrySet()) {
                String headerName = r.getKey();
                String value = r.getValue();
                System.err.println(headerName + ": " + value);
            }
            System.err.println();

            System.err.write(body);
            System.err.println("\n--------------------");

            String path = System.getProperty("server.responses") + resourceName + ".body";
            Path responsePath = Paths.get(path);
            Files.createDirectories(responsePath.getParent());
            Files.write(responsePath, body);
        } catch (IOException ignored) {
            // Ignored
        }
    }

    private static HttpData readData(HttpResponse<byte[]> connection) {
        String firstLine = "HTTP/1.1 " + connection.statusCode();
        String cookie = null;
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, List<String>> entries : connection.headers().map().entrySet()) {
            if (entries.getKey() != null) {
                if (entries.getKey().equalsIgnoreCase("Set-Cookie")) {
                    cookie = String.join("; ", entries.getValue());
                }
                headers.put(entries.getKey(), String.join(", ", entries.getValue()));
            }
        }

        HttpData httpData = new HttpData(firstLine, headers, connection.body());
        httpData.setCookie(cookie);
        return httpData;
    }

    private static HttpData readData(InputStream input, String resource) throws IOException {
        if (input == null) {
            return null;
        }
        String firstLine = readLine(input);
        Map<String, String> headers = readHeaders(input);

        byte[] body;
        String cl = headers.get("Content-Length");
        String te = headers.get("Transfer-Encoding");
        String ct = headers.get("Content-Type");
        String ce = headers.get("Content-Encoding");

        if (ct != null && ct.startsWith("multipart/form-data") && ct.contains("boundary=")) {
            String boundary = ct.substring(ct.indexOf("boundary=") + "boundary=".length());
            String boundaryEnd = "--" + boundary + "--";
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (PrintWriter writer = new PrintWriter(os)) {
                while (true) {
                    String line = readLine(input);
                    if (isFileRef(line)) {
                        writer.flush();
                        String fileRes = resolveFileRef(Paths.get(resource).getParent(), line);
                        try (InputStream fileStream = getStream(fileRes)) {
                            if (fileStream == null) {
                                throw new FileNotFoundException(fileRes);
                            }
                            fileStream.transferTo(os);
                        }
                        os.flush();
                    } else {
                        writer.append(line);
                    }
                    writer.print("\r\n");
                    if (boundaryEnd.equals(line)) {
                        writer.flush();
                        break;
                    }
                }
                writer.print("\r\n");
            }
            body = os.toByteArray();
        } else if (BLOB_TYPES.contains(ct) || ce != null) {
            String line = readLine(input);
            if (isFileRef(line)) {
                String fileRes = resolveFileRef(Paths.get(resource).getParent(), line);
                try (InputStream fileStream = getStream(fileRes)) {
                    if (fileStream == null) {
                        throw new FileNotFoundException(fileRes);
                    }
                    body = fileStream.readAllBytes();
                }
            } else {
                body = line.getBytes(StandardCharsets.UTF_8);
            }
            if (input.available() != 0) {
                throw new IllegalStateException("Unexpected content");
            }
        } else if (cl != null) {
            body = readBody(input, cl);
        } else if (te != null && te.equalsIgnoreCase("chunked")) {
            body = readChunckedBody(input);
        } else if (NO_CONTENT_STATUS_PATTERN.matcher(firstLine).matches()) {
            // Depending on the implementation of InputStream, reading it can hang if no data is available.
            // So for 204 status we just don't read body because it doesn't needed for this status.
            body = new byte[0];
        } else {
            body = input.readAllBytes();
        }

        return new HttpData(firstLine, headers, body);
    }

    private static InputStream getStream(String fileRes) {
        try {
            return Files.newInputStream(Paths.get(fileRes));
        } catch (IOException e) {
            return HttpData.class.getResourceAsStream(fileRes);
        }
    }

    private static boolean isFileRef(String s) {
        return !s.isEmpty() && s.charAt(0) == '&';
    }

    private static String resolveFileRef(Path parent, String fileRef) {
        return parent.resolve(fileRef.substring(1)).toString().replace('\\', '/');
    }

    private static Map<String, String> readHeaders(InputStream input) throws IOException {
        TreeMap<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        String header = readLine(input);
        while (!header.isEmpty()) {
            int separator = header.indexOf(":");
            String name = header.substring(0, separator);
            String value = header.substring(separator + 1).trim();
            value = value.isEmpty() ? null : value;
            headers.put(name, value);
            header = readLine(input);
        }
        return headers;
    }

    private static byte[] readBody(InputStream input, String length) throws IOException {
        byte[] body;
        int size = Integer.parseInt(length);
        body = new byte[size];
        int actual = input.read(body);
        if (actual != size) {
            throw new IOException("Unexpected size of the body.");
        }
        return body;
    }

    private static byte[] readChunckedBody(InputStream input) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream(1024);
        byte[] chunk = readChunck(input);

        while (chunk.length > 0) {
            body.write(chunk);
            chunk = readChunck(input);
        }
        return body.toByteArray();
    }

    private static byte[] readChunck(InputStream input) throws IOException {
        String hexSize = readLine(input);
        int size = Integer.parseInt(hexSize, 16);
        byte[] body = new byte[size];
        int actual = input.read(body);
        if (actual != size) {
            throw new IOException("Unexpected size of the chunk.");
        }
        String eol = readLine(input);
        if (eol.isEmpty()) {
            return body;
        }
        throw new IOException("Unexpected format of the chunk.");
    }

    private static String readLine(InputStream input) throws IOException {
        StringBuilder line = new StringBuilder(120);
        boolean eol = false;
        int n;
        while (!eol && (n = input.read()) > 0) {

            if (n != 10 && n != 13) {
                line.append((char) n);
            } else {
                eol = n == 10;
            }
        }
        if (!eol) {
            throw new IOException("Unexpected end of the stream. Expected CRLF in the end of the line.");
        }
        return line.toString();
    }

    public String getCookie() {
        return cookie;
    }

    private void setCookie(String cookie) {
        this.cookie = cookie;
    }
}
