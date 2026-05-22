// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import java.util.Map;

/** {{var}} interpolation — mirrors interpolate.go / interpolate.py. */
final class Interpolate {

    private Interpolate() {}

    static String apply(String s, Map<String, String> vars) {
        if (s == null || !s.contains("{{")) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        int n = s.length();
        while (i < n) {
            if (i + 1 < n && s.charAt(i) == '{' && s.charAt(i + 1) == '{') {
                int end = s.indexOf("}}", i + 2);
                if (end < 0) {
                    out.append(s.substring(i));
                    break;
                }
                String name = s.substring(i + 2, end).trim();
                String v = vars.get(name);
                if (v != null) {
                    out.append(v);
                } else {
                    out.append(s, i, end + 2);
                }
                i = end + 2;
                continue;
            }
            out.append(s.charAt(i));
            i++;
        }
        return out.toString();
    }
}
