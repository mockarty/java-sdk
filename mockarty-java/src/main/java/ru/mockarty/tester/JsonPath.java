// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSONPath walker mirroring jsonpath.go / jsonpath.py.
 * Supports {@code $}, {@code $.a.b.c}, {@code $.arr[N]}, {@code $.arr[-1]},
 * {@code $.arr[*]}.
 */
final class JsonPath {

    private JsonPath() {}

    @SuppressWarnings("unchecked")
    static Object resolve(Object root, String path) {
        if (path == null || path.isEmpty() || "$".equals(path)) {
            return root;
        }
        if (!path.startsWith("$")) {
            throw new JsonPathException("jsonpath must start with $: " + path);
        }
        List<String> segments = splitPath(path.substring(1));
        Object cur = root;
        for (int i = 0; i < segments.size(); i++) {
            String seg = segments.get(i);
            try {
                cur = step(cur, seg);
            } catch (JsonPathException e) {
                StringBuilder so = new StringBuilder();
                for (int j = 0; j <= i; j++) {
                    so.append(segments.get(j));
                }
                throw new JsonPathException("at " + so + ": " + e.getMessage(), e);
            }
        }
        return cur;
    }

    private static List<String> splitPath(String s) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '.') {
                i++;
                int j = i;
                while (j < s.length() && s.charAt(j) != '.' && s.charAt(j) != '[') {
                    j++;
                }
                if (j == i) {
                    throw new JsonPathException("empty segment at offset " + i);
                }
                out.add(s.substring(i, j));
                i = j;
            } else if (c == '[') {
                int end = s.indexOf(']', i);
                if (end < 0) {
                    throw new JsonPathException("unterminated [ at offset " + i);
                }
                out.add(s.substring(i, end + 1));
                i = end + 1;
            } else {
                throw new JsonPathException("unexpected char " + c + " at offset " + i);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object step(Object cur, String seg) {
        if (seg.startsWith("[")) {
            String inner = seg.substring(1, seg.length() - 1);
            if (!(cur instanceof List)) {
                throw new JsonPathException("indexer on non-array (" + classOf(cur) + ")");
            }
            List<Object> arr = (List<Object>) cur;
            if ("*".equals(inner)) {
                return new ArrayList<>(arr);
            }
            int idx;
            try {
                idx = Integer.parseInt(inner);
            } catch (NumberFormatException e) {
                throw new JsonPathException("invalid index " + inner);
            }
            if (idx < 0) {
                idx += arr.size();
            }
            if (idx < 0 || idx >= arr.size()) {
                throw new JsonPathException("index " + inner + " out of range (len=" + arr.size() + ")");
            }
            return arr.get(idx);
        }
        if (!(cur instanceof Map)) {
            throw new JsonPathException("key access on non-object (" + classOf(cur) + ")");
        }
        Map<String, Object> obj = (Map<String, Object>) cur;
        if (!obj.containsKey(seg)) {
            throw new JsonPathException("key " + seg + " not found");
        }
        return obj.get(seg);
    }

    static boolean equalsLoose(Object got, Object want) {
        if (got == null && want == null) {
            return true;
        }
        if (got == null || want == null) {
            return false;
        }
        if (want instanceof Number && got instanceof Number) {
            return ((Number) got).doubleValue() == ((Number) want).doubleValue();
        }
        return got.equals(want);
    }

    private static String classOf(Object o) {
        return o == null ? "null" : o.getClass().getSimpleName();
    }
}

class JsonPathException extends RuntimeException {
    JsonPathException(String msg) {
        super(msg);
    }

    JsonPathException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
