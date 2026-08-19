package org.tabletest.reporter;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies how a report run's timestamp reaches the footer: an exact machine-readable
 * instant beside a label a reader can scan.
 */
@DisplayName("Report timestamp")
public class GeneratedAtTest {

    @Description("""
        Report timestamps are always stated in UTC, whatever zone the run happened in, so a
        spec generated in CI reads the same for every reader. The label drops sub-second
        precision the reader has no use for; the attribute keeps whole seconds.
        """)
    @TableTest("""
        Scenario             | Run instant                   | Timestamp attribute?   | Footer label?
        Afternoon run        | "2026-07-20T14:32:09Z"        | "2026-07-20T14:32:09Z" | 20 Jul 2026 14:32 UTC
        Sub-second precision | "2026-07-20T14:32:09.123456Z" | "2026-07-20T14:32:09Z" | 20 Jul 2026 14:32 UTC
        Turn of the year     | "2027-01-01T00:00:00Z"        | "2027-01-01T00:00:00Z" | 1 Jan 2027 00:00 UTC
        """)
    void states_the_run_timestamp_in_utc(Instant runInstant, String timestampAttribute, String footerLabel) {
        assertThat(new GeneratedAt(runInstant).toMap())
                .containsEntry("datetime", timestampAttribute)
                .containsEntry("label", footerLabel);
    }
}
