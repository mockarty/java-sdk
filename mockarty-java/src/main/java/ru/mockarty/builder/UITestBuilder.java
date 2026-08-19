// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for UI tests (recorded browser/mobile flows). Reconstructs the
 * server's {@code RecordedAction[]} wire shape, so a flow authored here — or
 * generated from a Chrome-extension / companion recording via
 * {@code mockarty-cli ui export --lang java} — round-trips through
 * {@code POST /api/v1/ui-tests} and runs on the platform's browser-runner /
 * companion. No Playwright/Appium toolchain in the SDK; execution is
 * orchestrated on the platform and the result flows to TCM.
 *
 * <pre>
 * UITestBuilder ui = UITestBuilder.named("checkout")
 *     .navigate("https://shop.example.com")
 *     .click("[data-testid=cart]")
 *     .fill("#coupon", "SAVE10").press("#coupon", "Enter")
 *     .assertText(".total", "$90.00")
 *     .assertVisible(".confirmation")
 *     .screenshot();
 * </pre>
 */
public final class UITestBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private String platform = "web";
    private String startUrl = "";
    private final List<Map<String, Object>> actions = new ArrayList<>();

    private UITestBuilder(String name) {
        this.name = name;
    }

    public static UITestBuilder named(String name) {
        return new UITestBuilder(name);
    }

    public UITestBuilder platform(String p) {
        this.platform = p;
        return this;
    }

    public UITestBuilder startUrl(String url) {
        this.startUrl = url;
        return this;
    }

    private UITestBuilder add(Map<String, Object> action) {
        Map<String, Object> clean = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : action.entrySet()) {
            Object v = e.getValue();
            if (v == null) {
                continue;
            }
            if (v instanceof String && ((String) v).isEmpty()) {
                continue;
            }
            clean.put(e.getKey(), v);
        }
        actions.add(clean);
        return this;
    }

    private static Map<String, Object> a(String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        return m;
    }

    /** Override the last action's locator strategy (css|testid|role|text|xpath). */
    public UITestBuilder selectorKind(String kind) {
        if (!actions.isEmpty()) {
            actions.get(actions.size() - 1).put("selectorKind", kind);
        }
        return this;
    }

    // -- navigation ----------------------------------------------------------

    public UITestBuilder navigate(String url) {
        Map<String, Object> m = a("navigate");
        m.put("value", url);
        return add(m);
    }

    public UITestBuilder goBack() {
        return add(a("goBack"));
    }

    public UITestBuilder goForward() {
        return add(a("goForward"));
    }

    public UITestBuilder reload() {
        return add(a("reload"));
    }

    // -- interactions --------------------------------------------------------

    private UITestBuilder sel(String type, String selector) {
        Map<String, Object> m = a(type);
        m.put("selector", selector);
        return add(m);
    }

    private UITestBuilder selVal(String type, String selector, String value) {
        Map<String, Object> m = a(type);
        m.put("selector", selector);
        m.put("value", value);
        return add(m);
    }

    public UITestBuilder click(String selector) {
        return sel("click", selector);
    }

    public UITestBuilder doubleClick(String selector) {
        return sel("dblclick", selector);
    }

    public UITestBuilder rightClick(String selector) {
        return sel("rightclick", selector);
    }

    public UITestBuilder hover(String selector) {
        return sel("hover", selector);
    }

    public UITestBuilder focus(String selector) {
        return sel("focus", selector);
    }

    public UITestBuilder check(String selector) {
        return sel("check", selector);
    }

    public UITestBuilder uncheck(String selector) {
        return sel("uncheck", selector);
    }

    public UITestBuilder clear(String selector) {
        return sel("clear", selector);
    }

    public UITestBuilder scrollIntoView(String selector) {
        return sel("scrollIntoView", selector);
    }

    public UITestBuilder fill(String selector, String value) {
        return selVal("fill", selector, value);
    }

    public UITestBuilder type(String selector, String value) {
        return selVal("type", selector, value);
    }

    public UITestBuilder press(String selector, String key) {
        return selVal("press", selector, key);
    }

    public UITestBuilder select(String selector, String value) {
        return selVal("select", selector, value);
    }

    public UITestBuilder upload(String selector, String path) {
        return selVal("setInputFiles", selector, path);
    }

    public UITestBuilder dragAndDrop(String selector, String targetSelector) {
        Map<String, Object> m = a("dragAndDrop");
        m.put("selector", selector);
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("targetSelector", targetSelector);
        m.put("extras", extras);
        return add(m);
    }

    // -- assertions ----------------------------------------------------------

    public UITestBuilder assertVisible(String selector) {
        return sel("assertVisible", selector);
    }

    public UITestBuilder assertHidden(String selector) {
        return sel("assertHidden", selector);
    }

    public UITestBuilder assertEnabled(String selector) {
        return sel("assertEnabled", selector);
    }

    public UITestBuilder assertDisabled(String selector) {
        return sel("assertDisabled", selector);
    }

    public UITestBuilder assertChecked(String selector) {
        return sel("assertChecked", selector);
    }

    public UITestBuilder assertText(String selector, String text) {
        return selVal("assertText", selector, text);
    }

    public UITestBuilder assertValue(String selector, String value) {
        return selVal("assertValue", selector, value);
    }

    public UITestBuilder assertCount(String selector, int n) {
        return selVal("assertCount", selector, Integer.toString(n));
    }

    public UITestBuilder assertAttribute(String selector, String attr, String value) {
        Map<String, Object> m = a("assertAttribute");
        m.put("selector", selector);
        m.put("value", value);
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("attr", attr);
        m.put("extras", extras);
        return add(m);
    }

    public UITestBuilder assertUrl(String substr) {
        Map<String, Object> m = a("assertURL");
        m.put("value", substr);
        return add(m);
    }

    public UITestBuilder assertTitle(String substr) {
        Map<String, Object> m = a("assertTitle");
        m.put("value", substr);
        return add(m);
    }

    // -- misc ----------------------------------------------------------------

    public UITestBuilder waitFor(String selector) {
        return sel("waitFor", selector);
    }

    public UITestBuilder screenshot() {
        return add(a("screenshot"));
    }

    public UITestBuilder visualCheck(String selector) {
        return sel("visualCheck", selector);
    }

    public UITestBuilder a11yCheck() {
        return add(a("a11yCheck"));
    }

    /** Escape hatch for any action type not covered by a typed helper. */
    public UITestBuilder action(String type, String selector, String value) {
        return selVal(type, selector, value);
    }

    // -- output --------------------------------------------------------------

    public String getName() {
        return name;
    }

    public List<Map<String, Object>> getActions() {
        return new ArrayList<>(actions);
    }

    /** The create-request wire shape ({name, platform, startUrl, actions[]}). */
    public Map<String, Object> toMap() {
        String start = startUrl;
        if (start == null || start.isEmpty()) {
            for (Map<String, Object> act : actions) {
                if ("navigate".equals(act.get("type")) && act.get("value") != null) {
                    start = String.valueOf(act.get("value"));
                    break;
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", name);
        out.put("platform", platform);
        out.put("startUrl", start == null ? "" : start);
        out.put("actions", actions);
        return out;
    }

    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(toMap());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize UITest", e);
        }
    }
}
