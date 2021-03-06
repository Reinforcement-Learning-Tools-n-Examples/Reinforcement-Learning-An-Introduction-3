@file:Suppress("NAME_SHADOWING")

package lab.mars.rl.model.impl.func

import lab.mars.rl.util.matrix.Matrix

class SimpleTileCoding(numOfTilings: Int,
                       _tilingSize: Int,
                       val tileWidth: Int,
                       val tilingOffset: Double, conv: (Array<out Any>) -> Double): Feature<Double>(conv) {
  val tilingSize = _tilingSize + 1
  override val numOfComponents = numOfTilings * tilingSize
  
  override fun _invoke(s: Double): Matrix {
    return Matrix.column(numOfComponents) {
      val tilingIdx = it / tilingSize
      val tileIdx = it % tilingSize
      val start = -tileWidth + tilingIdx * tilingOffset + tileIdx * tileWidth
      if (start <= s && s < start + tileWidth) 1.0 else 0.0
    }
  }
  
}