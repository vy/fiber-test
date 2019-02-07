package com.vlkan.fibertest.ring;

import kilim.tools.Kilim;
import org.junit.Test;

public class KilimCellRingBenchmarkTest {

    @SuppressWarnings("unused")     // entrance for Kilim.run()
    public static void kilimEntrance(String[] ignored) {
        RingBenchmarkTestUtil.test(KilimCellRingBenchmark::new);
    }

    @Test
    public void testRingBenchmark() throws Exception {
        Kilim.run("com.vlkan.fibertest.ring.KilimCellRingBenchmarkTest", "kilimEntrance");
    }

}
