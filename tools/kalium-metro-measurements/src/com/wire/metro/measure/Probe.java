package com.wire.metro.measure;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLongArray;

/** Test-JVM utility, bootstrap-loaded across Robolectric loaders. No SDK hooks. */
public final class Probe {
    private static final int LIMIT = 8192;
    private static final int IDENTITY_LIMIT = Integer.getInteger("metro.measure.identityLimit", 250_000);
    private static final Object LOCK = new Object(), OUTPUT_LOCK = new Object();
    private static final AtomicLongArray TOTAL = new AtomicLongArray(LIMIT), CALLER = new AtomicLongArray(LIMIT);
    private static final List<String> KEYS = new ArrayList<>();
    private static final Set<String> COVERAGE = new TreeSet<>();
    private static final List<String> TRANSFORM_ERRORS = new ArrayList<>(), COUNTER_ERRORS = new ArrayList<>();
    private static volatile long activeThread = -1;
    private static volatile boolean installed;
    private static boolean finished, callbackControlPassed;
    private static String callbackControlLoader;
    private static Span active; // Guarded by LOCK; the ONLY root of retained measured identities.
    private static final com.sun.management.ThreadMXBean MEMORY = memoryBean();
    private static final Path OUTPUT = Path.of(System.getProperty("metro.measure.output", "."),
            "metrics-" + ProcessHandle.current().pid() + ".jsonl");
    static { if (IDENTITY_LIMIT < 1) throw new IllegalArgumentException("identityLimit must be positive"); }

    private Probe() {}
    private static com.sun.management.ThreadMXBean memoryBean() {
        var raw = ManagementFactory.getThreadMXBean();
        if (!(raw instanceof com.sun.management.ThreadMXBean bean) || !bean.isThreadAllocatedMemorySupported()) return null;
        try {
            if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
            return bean;
        } catch (RuntimeException unsupported) { return null; }
    }
    public static int register(String key, String loader) {
        synchronized (LOCK) {
            COVERAGE.add(key + " @ " + loader);
            int id = KEYS.indexOf(key);
            if (id >= 0) return id;
            if (KEYS.size() == LIMIT) throw new IllegalStateException("constructor counter capacity exceeded");
            KEYS.add(key); return KEYS.size() - 1;
        }
    }
    /** Exact selected constructor ENTRY callback. Never receives uninitialized this. */
    public static void hit(int id) {
        TOTAL.incrementAndGet(id);
        if (Thread.currentThread().getId() == activeThread) CALLER.incrementAndGet(id);
    }
    /** Before a selected constructor's normal RETURN; window membership linearizes under LOCK. */
    public static void returned(Object value) {
        if (!installed) return;
        synchronized (LOCK) {
            Span s = active;
            if (s == null || s.identityFailed) return; // No references retained outside an active window.
            try {
                if (value == null) throw new IllegalArgumentException("null constructor RETURN identity");
                boolean first = !s.allSeen.containsKey(value);
                if (first && s.allSeen.size() == IDENTITY_LIMIT) {
                    invalidateIdentities(s, "identity limit exceeded (" + IDENTITY_LIMIT + ")");
                    return; // Do not manufacture a constructor failure; end()/finish() invalidate the run.
                }
                Class<?> type = value.getClass();
                RuntimeCounts counts = s.byClass.get(type);
                if (counts == null) {
                    ClassLoader loader = type.getClassLoader();
                    Integer loaderId = loader == null ? Integer.valueOf(0) : s.loaderIds.get(loader);
                    if (loaderId == null) { loaderId = s.loaderIds.size() + 1; s.loaderIds.put(loader, loaderId); }
                    counts = new RuntimeCounts(type.getName(), loaderId,
                            loader == null ? "bootstrap" : loader.getClass().getName());
                    s.byClass.put(type, counts);
                }
                if (first) { s.allSeen.put(value, Boolean.TRUE); counts.all++; }
                if (Thread.currentThread().getId() == s.tid && !s.callerSeen.containsKey(value)) {
                    s.callerSeen.put(value, Boolean.TRUE); counts.caller++;
                }
            } catch (Throwable failure) {
                // Clear strong roots first. A failed callback invalidates the run, never silently truncates.
                invalidateIdentities(s, "identity callback failed: " + failure.getClass().getName());
                if (failure instanceof ThreadDeath death) throw death;
            }
        }
    }
    private static void invalidateIdentities(Span s, String message) {
        s.identityFailed = true;
        releaseIdentities(s);
        if (COUNTER_ERRORS.size() < 64) COUNTER_ERRORS.add(message);
    }
    private static void releaseIdentities(Span s) {
        if (s.allSeen != null) { s.allSeen.clear(); s.allSeen = null; }
        if (s.callerSeen != null) { s.callerSeen.clear(); s.callerSeen = null; }
        if (s.byClass != null) { s.byClass.clear(); s.byClass = null; }
        if (s.loaderIds != null) { s.loaderIds.clear(); s.loaderIds = null; }
    }
    public static void error(String error) { synchronized (LOCK) { if (TRANSFORM_ERRORS.size() < 64) TRANSFORM_ERRORS.add(error); } }
    public static void installed() {
        synchronized (LOCK) {
            if (installed || finished) throw new IllegalStateException("agent already installed or probe finished");
            installed = true;
        }
    }
    public static boolean isInstalled() { return installed; }
    /** Numeric diagnostic for the standalone verifier; no identity/resource handles escape. */
    public static int retainedIdentityReferences() {
        synchronized (LOCK) {
            return active == null || active.allSeen == null ? 0 : active.allSeen.size() + active.callerSeen.size();
        }
    }
    private static long bytes() {
        return MEMORY == null ? -1 : MEMORY.getThreadAllocatedBytes(Thread.currentThread().getId());
    }
    private static final class RuntimeCounts {
        final String name, loaderType;
        final int loaderId;
        long all, caller;
        RuntimeCounts(String name, int loaderId, String loaderType) { this.name = name; this.loaderId = loaderId; this.loaderType = loaderType; }
        Map<String,Object> snapshot() {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("runtime_class", name); row.put("defining_loader_window_id", loaderId); row.put("loader_type", loaderType);
            row.put("all_threads", all); row.put("caller_thread", caller); return row;
        }
    }
    public static final class Span {
        String phase;
        int iteration;
        boolean warmup, identityFailed;
        long tid, start, allocated, windowStart;
        long[] total = new long[LIMIT], caller = new long[LIMIT];
        IdentityHashMap<Object,Boolean> allSeen, callerSeen;
        IdentityHashMap<Class<?>,RuntimeCounts> byClass;
        IdentityHashMap<ClassLoader,Integer> loaderIds;
        private Span() {}
    }
    /** One active window globally. No coroutine suspension/thread switching within a timed action. */
    public static Span begin(String phase, int iteration, boolean warmup) {
        synchronized (LOCK) {
            if (finished) throw new IllegalStateException("probe already finished");
            if (active != null) throw new IllegalStateException("overlapping measurement windows");
            if (!COUNTER_ERRORS.isEmpty()) throw new AssertionError("earlier counter failure: " + COUNTER_ERRORS);
            Span s = new Span(); s.phase = phase; s.iteration = iteration; s.warmup = warmup;
            s.tid = Thread.currentThread().getId();
            for (int i = 0; i < LIMIT; i++) { s.total[i] = TOTAL.get(i); s.caller[i] = CALLER.get(i); }
            if (installed) {
                s.allSeen = new IdentityHashMap<>(); s.callerSeen = new IdentityHashMap<>();
                s.byClass = new IdentityHashMap<>(); s.loaderIds = new IdentityHashMap<>();
            }
            activeThread = s.tid; active = s;
            s.windowStart = System.nanoTime();
            try {
                s.allocated = installed ? -1 : bytes(); s.start = System.nanoTime(); return s;
            } catch (RuntimeException | Error failure) {
                active = null; activeThread = -1; releaseIdentities(s); throw failure;
            }
        }
    }
    /** Strong identity/Class/ClassLoader references are cleared before row I/O, even on failure. */
    public static void end(Span s, boolean success) {
        end(s, success, "sample");
    }
    private static void end(Span s, boolean success, String rowType) {
        long end = System.nanoTime(), allocated = installed ? -1 : bytes();
        Map<String,Object> row;
        boolean failed;
        synchronized (LOCK) {
            if (s == null || s != active) throw new IllegalStateException("window is not active or already ended");
            if (Thread.currentThread().getId() != s.tid) throw new IllegalStateException("measurement changed threads");
            try {
                row = new LinkedHashMap<>();
                row.put("metric_schema", 2); row.put("type", rowType); row.put("phase", s.phase); row.put("iteration", s.iteration);
                row.put("warmup", s.warmup); row.put("success", success && !s.identityFailed);
                row.put("pass", installed ? "constructors" : "timing"); row.put("caller_thread", s.tid);
                row.put("start_monotonic_ns", s.start); row.put("end_monotonic_ns", end); row.put("elapsed_ns", end - s.start);
                row.put("caller_allocated_bytes", installed || allocated < 0 || s.allocated < 0 || allocated < s.allocated ? null : allocated - s.allocated);
                row.put("allocated_bytes_scope", "uninstrumented timing pass only; ThreadMXBean approximation for timed caller platform thread; excludes workers");
                Map<String,Long> all = new TreeMap<>(), caller = new TreeMap<>();
                for (int i = 0; i < KEYS.size(); i++) { all.put(KEYS.get(i), TOTAL.get(i) - s.total[i]); caller.put(KEYS.get(i), CALLER.get(i) - s.caller[i]); }
                row.put("constructor_entries_all_threads", installed ? all : null);
                row.put("constructor_entries_caller_thread", installed ? caller : null);
                row.put("constructor_return_identities_valid", installed && !s.identityFailed);
                row.put("constructor_return_unique_identities_all_threads", installed && !s.identityFailed ? s.allSeen.size() : null);
                row.put("constructor_return_unique_identities_caller_thread", installed && !s.identityFailed ? s.callerSeen.size() : null);
                List<Map<String,Object>> classes = null;
                if (installed && !s.identityFailed) {
                    classes = new ArrayList<>();
                    for (RuntimeCounts counts : s.byClass.values()) classes.add(counts.snapshot());
                    classes.sort(Comparator.comparing(x -> x.get("runtime_class") + ":" + x.get("defining_loader_window_id")));
                }
                row.put("constructor_return_unique_identities_by_runtime_class", classes);
                row.put("identity_limit", IDENTITY_LIMIT);
                row.put("identity_window_start_monotonic_ns", installed ? s.windowStart : null);
                row.put("identity_window_end_monotonic_ns", installed ? System.nanoTime() : null);
                row.put("transform_errors", new ArrayList<>(TRANSFORM_ERRORS)); row.put("counter_errors", new ArrayList<>(COUNTER_ERRORS));
                failed = !COUNTER_ERRORS.isEmpty();
            } finally {
                active = null; activeThread = -1; releaseIdentities(s);
            }
        }
        row.put("retained_identity_references_after_end", 0); // The closed span no longer holds any measured object/class/loader.
        write(row);
        if (failed) throw new AssertionError("constructor identity counter failed; see counter_errors");
    }
    /** Before SDK setup: prove symbolic callbacks and the fixture reach this same bootstrap state. */
    public static void verifyCallbacks(Class<?> subject, java.util.function.Supplier<Object[]> action) throws ClassNotFoundException {
        if (Probe.class.getClassLoader() != null || Class.forName(Probe.class.getName(), false, subject.getClassLoader()) != Probe.class)
            throw new AssertionError("constructor callbacks must resolve the fixture's bootstrap Probe");
        Span s = begin("robolectric_callback_control", 0, false);
        boolean success = false;
        try {
            Object[] values = action.get();
            synchronized (LOCK) {
                if (values == null || values.length != 2 || values[0] == values[1]
                        || values[0] == null || values[1] == null
                        || values[0].getClass() != subject || values[1].getClass() != subject)
                    throw new AssertionError("callback control must construct two distinct instances of its selected class");
                long entries = 0, caller = 0;
                for (int i = 0; i < KEYS.size(); i++) {
                    entries += TOTAL.get(i) - s.total[i]; caller += CALLER.get(i) - s.caller[i];
                }
                if (entries != (installed ? 4 : 0) || caller != entries)
                    throw new AssertionError("callback control expected four constructor ENTRY calls (zero in timing mode): " + entries + "/" + caller);
                if (installed && (s.identityFailed || s.allSeen.size() != 2 || s.callerSeen.size() != 2
                        || !s.allSeen.containsKey(values[0]) || !s.allSeen.containsKey(values[1])
                        || !s.callerSeen.containsKey(values[0]) || !s.callerSeen.containsKey(values[1])
                        || s.byClass.size() != 1 || !s.byClass.containsKey(subject)))
                    throw new AssertionError("callback control did not observe both exact normal-RETURN identities");
                if (!TRANSFORM_ERRORS.isEmpty() || !COUNTER_ERRORS.isEmpty())
                    throw new AssertionError("callback control transform/counter failure");
                callbackControlPassed = true;
                callbackControlLoader = subject.getClassLoader() == null ? "bootstrap" : subject.getClassLoader().getClass().getName();
                success = true;
            }
        } finally {
            end(s, success, "callback_control");
        }
    }
    public static <T> T measure(String phase, int iteration, boolean warmup, java.util.function.Supplier<T> action) {
        Span span = begin(phase, iteration, warmup);
        boolean success = false; Throwable original = null;
        try { T result = action.get(); success = true; return result; }
        catch (RuntimeException | Error failure) { original = failure; throw failure; }
        finally {
            try { end(span, success); }
            catch (RuntimeException | Error cleanup) { if (original == null) throw cleanup; if (cleanup != original) original.addSuppressed(cleanup); }
        }
    }
    public static void provenance() {
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-javaagent:") && !arg.contains("/constructors-agent.jar="))
                throw new IllegalStateException("Uncontrolled startup instrumentation agent: " + arg);
        }
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("metric_schema", 2); row.put("type", "jvm"); row.put("pass", installed ? "constructors" : "timing");
        row.put("pid", ProcessHandle.current().pid()); row.put("java_version", System.getProperty("java.runtime.version"));
        row.put("java_vm", System.getProperty("java.vm.name")); row.put("jvm_args", ManagementFactory.getRuntimeMXBean().getInputArguments());
        row.put("allocation_supported", MEMORY != null); row.put("bootstrap_probe", Probe.class.getClassLoader() == null);
        row.put("identity_limit", IDENTITY_LIMIT); row.put("retained_identity_references", retainedIdentityReferences());
        row.put("identity_unit", "per-window unique references reaching selected constructor RETURN callbacks; not complete allocations or successful outermost constructions");
        synchronized (LOCK) {
            row.put("callback_control_passed", callbackControlPassed); row.put("callback_control_loader", callbackControlLoader);
            row.put("coverage", new ArrayList<>(COVERAGE)); row.put("transform_errors", new ArrayList<>(TRANSFORM_ERRORS));
            row.put("counter_errors", new ArrayList<>(COUNTER_ERRORS));
            Map<String,Long> cumulative = new TreeMap<>();
            for (int i = 0; i < KEYS.size(); i++) cumulative.put(KEYS.get(i), TOTAL.get(i));
            row.put("cumulative_constructor_entries_all_threads", installed ? cumulative : null);
        }
        write(row);
    }
    public static void finish() {
        synchronized (LOCK) {
            finished = true;
            if (active != null) {
                invalidateIdentities(active, "finish called with an open window"); active = null; activeThread = -1;
            }
        }
        provenance();
        synchronized (LOCK) {
            if (!TRANSFORM_ERRORS.isEmpty() || !COUNTER_ERRORS.isEmpty()) throw new AssertionError("constructor transform/counter failure: " + TRANSFORM_ERRORS + COUNTER_ERRORS);
        }
    }
    private static void write(Map<String,Object> row) {
        synchronized (OUTPUT_LOCK) {
            try {
                Files.createDirectories(OUTPUT.getParent());
                Files.writeString(OUTPUT, json(row) + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
    }
    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?,?> map) {
            var out = new StringJoiner(",", "{", "}");
            for (var e : map.entrySet()) out.add(json(e.getKey().toString()) + ":" + json(e.getValue())); return out.toString();
        }
        if (value instanceof Iterable<?> list) { var out = new StringJoiner(",", "[", "]"); for (var e : list) out.add(json(e)); return out.toString(); }
        String s = value.toString(); var out = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\"); case '"' -> out.append("\\\""); case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
                default -> { if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }
}
