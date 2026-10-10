/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import kotlin.test.Test
import kotlin.test.assertSame

class StorePublishStabilityTest {
    @Test
    fun publishIsStableInstance() {
        val store = Store<Int, Unit>(
            initial = 0,
            update = { _, count -> count },
        )
        assertSame(store.publish, store.publish)
    }
}
