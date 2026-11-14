package org.fossify.gallery

import org.fossify.gallery.helpers.OperationProgressFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class OperationProgressFormatterTest {
    @Test
    fun computePercentHandlesZeroNegativeAndOutOfRangeValues() {
        assertEquals(0, OperationProgressFormatter.computePercent(0, 0))
        assertEquals(0, OperationProgressFormatter.computePercent(5, 0))
        assertEquals(0, OperationProgressFormatter.computePercent(-3, 10))
        assertEquals(50, OperationProgressFormatter.computePercent(5, 10))
        assertEquals(100, OperationProgressFormatter.computePercent(15, 10))
    }

    @Test
    fun formatCountSummarySupportsByteScaledProgressBar() {
        assertEquals(
            "0 / 4 (0%)",
            OperationProgressFormatter.formatCountSummary(current = 0, total = 4)
        )
        assertEquals(
            "1 / 4 (75%)",
            OperationProgressFormatter.formatCountSummary(
                current = 1,
                total = 4,
                barCurrent = 750,
                barMax = 1000
            )
        )
        assertEquals(
            "2 / 4 (50%)",
            OperationProgressFormatter.formatCountSummary(
                current = 2,
                total = 4,
                barCurrent = 0,
                barMax = 0
            )
        )
    }

    @Test
    fun resolveNotificationContentTextUsesExplicitTextOrSummaryUnlessIndeterminate() {
        assertEquals(
            "photo.jpg",
            OperationProgressFormatter.resolveNotificationContentText(
                current = 3,
                max = 10,
                indeterminate = false,
                explicitText = "photo.jpg"
            )
        )
        assertEquals(
            "3 / 10 (30%)",
            OperationProgressFormatter.resolveNotificationContentText(
                current = 3,
                max = 10,
                indeterminate = false,
                explicitText = ""
            )
        )
        assertEquals(
            "",
            OperationProgressFormatter.resolveNotificationContentText(
                current = 0,
                max = 1,
                indeterminate = true,
                explicitText = ""
            )
        )
    }
}
