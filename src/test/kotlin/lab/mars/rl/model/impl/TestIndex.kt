package lab.mars.rl.model.impl

import lab.mars.rl.util.DefaultIntBuf
import lab.mars.rl.util.MultiIndex
import org.junit.Assert
import org.junit.Test

/**
 * <p>
 * Created on 2017-09-18.
 * </p>
 *
 * @author wumo
 */
class TestIndex {
    @Test
    fun `range forEach`() {
        val indices = arrayOf(DefaultIntBuf.of(0),
                              DefaultIntBuf.of(1, 2, 3),
                              DefaultIntBuf.of(4, 5, 6, 7))
        val idx = MultiIndex(indices)
        val expected = IntArray(8) { it }
        idx.forEach(0,0) { idx, value ->
            Assert.assertEquals(expected[idx], value)
        }
        idx.forEach(4, 7) { idx, value ->
            Assert.assertEquals(expected[idx], value)
        }
        idx.forEach { idx, value ->
            Assert.assertEquals(expected[idx], value)
        }
        idx.forEach(2, 5) { idx, value ->
            Assert.assertEquals(expected[idx], value)
        }
        idx.forEach(0, 5) { idx, value ->
            Assert.assertEquals(expected[idx], value)
        }
    }
}