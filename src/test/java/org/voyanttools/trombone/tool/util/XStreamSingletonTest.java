package org.voyanttools.trombone.tool.util;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class XStreamSingletonTest {

    @Test
    public void testConcurrentMarshalling() throws InterruptedException {
        final int threadCount = 100;
        final XStream xstream = ToolSerializer.getXMLXStream();
        final String expectedXml = "<string>test</string>";
        
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            service.submit(() -> {
                try {
                    startLatch.await();
                    String xml = xstream.toXML("test");
                    if (expectedXml.equals(xml)) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);

        Assert.assertTrue("Test timed out", completed);
        Assert.assertEquals("Errors detected during concurrent access!", 0, errorCount.get());
        Assert.assertEquals("Not all threads finished correctly", threadCount, successCount.get());
    }

    @Test
    public void testInstanceIntegrity() {
        XStream instance1 = ToolSerializer.getXMLXStream();
        XStream instance2 = ToolSerializer.getXMLXStream();
        Assert.assertSame("XStream should be a singleton", instance1, instance2);
    }
}
