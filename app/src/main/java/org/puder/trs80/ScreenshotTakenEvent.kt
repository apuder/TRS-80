package org.puder.trs80

/** Posted on the event bus after a screenshot for a configuration has been saved. */
data class ScreenshotTakenEvent(
    // @JvmField: MainActivity (Java) reads this as a plain field.
    @JvmField val configurationId: Int
)
