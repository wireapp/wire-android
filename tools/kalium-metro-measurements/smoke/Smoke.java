package smoke;
import com.wire.metro.measure.Probe;
import java.util.concurrent.atomic.AtomicInteger;
public class Smoke {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void rejected(Runnable action) {
        try { action.run(); throw new AssertionError("expected window rejection"); }
        catch (IllegalStateException expected) { }
    }
    private static void expectedFailure(Runnable action) {
        try { action.run(); throw new AssertionError("expected constructor/action failure"); }
        catch (ExpectedFailure expected) { }
    }
    public static void main(String[] args) throws Exception {
        Probe.provenance();
        String test = System.getProperty("smoke.case", "normal");
        if (test.equals("control")) {
            Probe.verifyCallbacks(CallbackSubject.class, () -> new Object[] {new CallbackSubject(), new CallbackSubject()});
            check(Probe.retainedIdentityReferences() == 0, "callback control retained identities");
            Probe.finish(); return;
        }
        if (test.equals("limit")) {
            boolean rejected = false;
            try { Probe.measure("standalone_identity_limit", 0, false, () -> { new Subject(); new Subject(); new Subject(); return null; }); }
            catch (AssertionError expected) { rejected = true; }
            check(rejected, "identity limit must invalidate end");
            check(Probe.retainedIdentityReferences() == 0, "limit failure must clear references");
            try { Probe.finish(); throw new IllegalStateException("finish accepted invalid counts"); } catch (AssertionError expected) { }
            return;
        }
        if (test.equals("abandon")) {
            Probe.begin("standalone_abandoned_window", 0, false); new Subject();
            check(Probe.retainedIdentityReferences() > 0, "expected active strong roots");
            try { Probe.finish(); throw new IllegalStateException("finish accepted open window"); } catch (AssertionError expected) { }
            check(Probe.retainedIdentityReferences() == 0, "finish must release abandoned window"); return;
        }
        new Subject(); // Outside all windows: ENTRY cumulative only; RETURN identities must not be retained.
        check(Probe.retainedIdentityReferences() == 0, "outside-window reference retention");
        var span = Probe.begin("standalone_identity_semantics", 0, false);
        rejected(() -> Probe.begin("overlap", 0, false));
        AtomicInteger rejections = new AtomicInteger();
        Thread intruder = new Thread(() -> {
            rejected(() -> Probe.begin("foreign_overlap", 0, false)); rejections.incrementAndGet();
            rejected(() -> Probe.end(span, true)); rejections.incrementAndGet();
        }); intruder.start(); intruder.join(); check(rejections.get() == 2, "foreign begin/end guards");
        new Subject(); new Subject(3); new Base();
        expectedFailure(PlainFail::new); // Enters, no selected constructor reaches RETURN.
        expectedFailure(OuterFail::new); // Selected superclass returns, outer constructor fails.
        expectedFailure(DelegatingOuterFail::new); // Selected inner overload returns, outer fails.
        expectedFailure(ChildOfThrowingBase::new); // Neither selected superclass nor subclass returns.
        new UnselectedChild(); // Selected superclass callback observes an unselected runtime class.
        Thread worker = new Thread(() -> new Subject(4)); worker.start(); worker.join();
        byte[] bytes = Smoke.class.getResourceAsStream("Smoke$Subject.class").readAllBytes();
        ClassLoader isolated = new ClassLoader(Smoke.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals("smoke.Smoke$Subject")) return super.loadClass(name, resolve);
                Class<?> c = findLoadedClass(name);
                if (c == null) c = defineClass(name, bytes, 0, bytes.length);
                if (resolve) resolveClass(c); return c;
            }
        };
        isolated.loadClass("smoke.Smoke$Subject").getConstructor().newInstance();
        Probe.end(span, true);
        check(Probe.retainedIdentityReferences() == 0, "closed span keeps strong references");
        rejected(() -> Probe.end(span, true)); // Double close is rejected without damaging the next window.
        Thread late = new Thread(Subject::new); late.start(); late.join();
        check(Probe.retainedIdentityReferences() == 0, "late worker retained an identity outside any window");
        Probe.measure("standalone_second_window", 1, false, () -> { new Subject(); return null; });
        check(Probe.retainedIdentityReferences() == 0, "second window did not clear");
        Probe.measure("standalone_empty_window", 2, false, () -> null);
        expectedFailure(() -> Probe.measure("standalone_action_failure", 3, false, () -> { new Subject(); throw new ExpectedFailure(); }));
        check(Probe.retainedIdentityReferences() == 0, "exceptional action did not clear");
        Probe.finish();
        rejected(() -> Probe.begin("after_finish", 4, false));
        check(Probe.retainedIdentityReferences() == 0, "finished probe retained references");
    }
    public static class CallbackSubject {
        private CallbackSubject(int marker) { }
        public CallbackSubject() { this(1); }
    }
    public static class Base {
        public Base() { }
        // Equal-valued objects must remain distinct; even calling these would make the smoke fail.
        @Override public final int hashCode() { throw new AssertionError("identity counter called hashCode"); }
        @Override public final boolean equals(Object other) { throw new AssertionError("identity counter called equals"); }
    }
    public static class Subject extends Base {
        public Subject() { this(2); }
        public Subject(int ignored) { super(); }
    }
    public static class PlainFail { public PlainFail() { throw new ExpectedFailure(); } }
    public static class OuterFail extends Base { public OuterFail() { super(); throw new ExpectedFailure(); } }
    public static class DelegatingOuterFail {
        public DelegatingOuterFail() { this(1); throw new ExpectedFailure(); }
        public DelegatingOuterFail(int ignored) { }
    }
    public static class ThrowingBase { public ThrowingBase() { throw new ExpectedFailure(); } }
    public static class ChildOfThrowingBase extends ThrowingBase { public ChildOfThrowingBase() { super(); } }
    public static class UnselectedChild extends Base { public UnselectedChild() { super(); } }
    public static class ExpectedFailure extends RuntimeException { }
}
