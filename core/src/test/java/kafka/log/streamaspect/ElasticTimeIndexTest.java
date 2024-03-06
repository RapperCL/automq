/*
 * Copyright 2024, AutoMQ CO.,LTD.
 *
 * Use of this software is governed by the Business Source License
 * included in the file BSL.md
 *
 * As of the Change Date specified in that file, in accordance with
 * the Business Source License, use of this software will be governed
 * by the Apache License, Version 2.0
 */
package kafka.log.streamaspect;

import java.io.IOException;
import java.util.List;
import kafka.log.streamaspect.cache.FileCache;
import kafka.utils.TestUtils;
import org.apache.kafka.storage.internals.log.TimestampOffset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO: replace S3Unit to AutoMQ
@Tag("S3Unit")
public class ElasticTimeIndexTest {
    int maxEntries = 30;
    long baseOffset = 45L;

    @Test
    public void testLookUp() throws IOException {
        for (boolean withCache : List.of(false, true)) {
            FileCache cache = withCache ? new FileCache(TestUtils.tempFile().getPath(), 10 * 1024) : new FileCache(TestUtils.tempFile().getPath(), 0);
            ElasticStreamSlice slice = new DefaultElasticStreamSlice(new MemoryClient.StreamImpl(1), SliceRange.of(0, Offsets.NOOP_OFFSET));
            ElasticTimeIndex idx = new ElasticTimeIndex(TestUtils.tempFile(), new IStreamSliceSupplier(slice),
                baseOffset, maxEntries * 12, TimestampOffset.UNKNOWN, cache);

            assertEquals(new TimestampOffset(-1, baseOffset), idx.lookup(100L));

            appendEntries(idx, maxEntries);

            // look for timestamp smaller than the earliest entry
            assertEquals(new TimestampOffset(-1L, baseOffset), idx.lookup(9));
            // look for timestamp in the middle of two entries.
            assertEquals(new TimestampOffset(20L, 65L), idx.lookup(25));
            // look for timestamp same as the one in the entry
            assertEquals(new TimestampOffset(30L, 75L), idx.lookup(30));
        }

    }

    void appendEntries(ElasticTimeIndex idx, int numEntries) {
        for (int i = 1; i < numEntries; i++) {
            idx.maybeAppend(i * 10L, i * 10L + baseOffset, false);
        }
    }
}
