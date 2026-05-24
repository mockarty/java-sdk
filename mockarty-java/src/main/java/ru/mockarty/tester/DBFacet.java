// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DB (SQL) facet. Mirrors {@code sdk/go-sdk/tester/db.go} and the
 * Python port. Driver-agnostic — the user adapts JDBC / Hikari /
 * whatever to {@link SQLConn}.
 */
public final class DBFacet {

    private final Tester t;
    private final SQLConn conn;

    DBFacet(Tester t, SQLConn conn) {
        this.t = t;
        this.conn = conn;
    }

    public DBStep query(String sql, Object... args) {
        t.flushPending();
        Map<String, String> v = t.snapshotVars();
        DBStep s = new DBStep(t, conn, "query",
                Interpolate.apply(sql, v),
                interpArgs(args, v));
        t.setPending(s);
        return s;
    }

    public DBStep exec(String sql, Object... args) {
        t.flushPending();
        Map<String, String> v = t.snapshotVars();
        DBStep s = new DBStep(t, conn, "exec",
                Interpolate.apply(sql, v),
                interpArgs(args, v));
        t.setPending(s);
        return s;
    }

    private static Object[] interpArgs(Object[] args, Map<String, String> v) {
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            out[i] = (args[i] instanceof String) ? Interpolate.apply((String) args[i], v) : args[i];
        }
        return out;
    }

    public interface SQLConn {
        List<Map<String, Object>> query(String sql, Object... args) throws Exception;
        ExecResult exec(String sql, Object... args) throws Exception;
    }

    public static final class ExecResult {
        public long rowsAffected;
        public long lastInsertId;
        public ExecResult() {}
        public ExecResult(long ra, long lid) { this.rowsAffected = ra; this.lastInsertId = lid; }
    }

    public static final class DBStep implements Committable {
        private final Tester t;
        private final SQLConn conn;
        private final String kind;
        private final String sql;
        private final Object[] args;
        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private List<Map<String, Object>> rows = Collections.emptyList();
        private ExecResult result = new ExecResult();
        private Throwable err;
        private final List<String> failures = new ArrayList<>();

        DBStep(Tester t, SQLConn conn, String kind, String sql, Object[] args) {
            this.t = t; this.conn = conn; this.kind = kind; this.sql = sql; this.args = args;
        }

        public DBStep expectOK() {
            if (!ensureSent()) { return this; }
            if (err != null) { fail("expectOK: " + err.getMessage()); }
            return this;
        }
        public DBStep expectError() {
            ensureSent();
            if (err == null) { fail("expectError: query succeeded"); }
            return this;
        }
        public DBStep expectRowCount(int n) {
            if (!ensureSent()) { return this; }
            if (!"query".equals(kind)) {
                return fail("expectRowCount only valid after query()");
            }
            if (rows.size() != n) {
                fail("expectRowCount: want " + n + ", got " + rows.size());
            }
            return this;
        }
        public DBStep expectAtLeastRows(int n) {
            if (!ensureSent()) { return this; }
            if (!"query".equals(kind)) {
                return fail("expectAtLeastRows only valid after query()");
            }
            if (rows.size() < n) {
                fail("expectAtLeastRows: want >=" + n + ", got " + rows.size());
            }
            return this;
        }
        public DBStep expectField(int row, String col, Object want) {
            if (!ensureSent()) { return this; }
            if (row < 0 || row >= rows.size()) {
                return fail("expectField[" + row + "." + col + "]: row out of range (len=" + rows.size() + ")");
            }
            Map<String, Object> r = rows.get(row);
            if (!r.containsKey(col)) {
                return fail("expectField[" + row + "." + col + "]: column not in result");
            }
            Object got = r.get(col);
            if (!JsonPath.equalsLoose(got, want)) {
                fail("expectField[" + row + "." + col + "]: want " + want + ", got " + got);
            }
            return this;
        }
        public DBStep expectColumn(String col, Object want) {
            return expectField(0, col, want);
        }
        public DBStep expectAffected(long n) {
            if (!ensureSent()) { return this; }
            if (!"exec".equals(kind)) {
                return fail("expectAffected only valid after exec()");
            }
            if (result.rowsAffected != n) {
                fail("expectAffected: want " + n + ", got " + result.rowsAffected);
            }
            return this;
        }
        public DBStep extract(int row, String col, String name) {
            if (!ensureSent()) { return this; }
            if (row < 0 || row >= rows.size()) {
                return fail("extract[" + row + "." + col + "]: row out of range (len=" + rows.size() + ")");
            }
            Map<String, Object> r = rows.get(row);
            if (!r.containsKey(col)) {
                return fail("extract[" + row + "." + col + "]: column not in result");
            }
            t.setVar(name, TesterScalar.stringify(r.get(col)));
            return this;
        }
        public List<Map<String, Object>> rows() {
            ensureSent();
            return new ArrayList<>(rows);
        }
        public ExecResult result() {
            ensureSent();
            return result;
        }
        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        private DBStep fail(String msg) { failures.add(msg); return this; }

        private boolean ensureSent() {
            if (sent) { return !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }
            startedAt = Instant.now();
            try {
                if ("exec".equals(kind)) {
                    result = conn.exec(sql, args);
                    if (result == null) { result = new ExecResult(); }
                } else {
                    rows = conn.query(sql, args);
                    if (rows == null) { rows = Collections.emptyList(); }
                }
            } catch (Exception e) {
                err = e;
                endedAt = Instant.now();
                return true;
            }
            endedAt = Instant.now();
            return true;
        }

        @Override
        public void commit() {
            if (committed) { return; }
            committed = true;
            if (!sent) { ensureSent(); }
            StepRecord rec = new StepRecord();
            rec.protocol = "sql";
            rec.method = kind;
            rec.name = "sql " + kind + " " + sqlPreview(sql);
            rec.url = sqlPreview(sql);
            rec.statusOrCode = "query".equals(kind) ? rows.size() : (int) result.rowsAffected;
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }

        private static String sqlPreview(String q) {
            String s = q.replaceAll("\\s+", " ").trim();
            if (s.length() > 80) { return s.substring(0, 77) + "..."; }
            return s;
        }
    }
}
