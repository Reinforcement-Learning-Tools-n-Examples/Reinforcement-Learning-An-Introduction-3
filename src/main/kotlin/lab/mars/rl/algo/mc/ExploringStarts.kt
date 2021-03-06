@file:Suppress("NAME_SHADOWING")

package lab.mars.rl.algo.mc

import lab.mars.rl.algo.V_from_Q
import lab.mars.rl.model.impl.mdp.*
import lab.mars.rl.model.isNotTerminal
import lab.mars.rl.model.log
import lab.mars.rl.util.buf.newBuf
import lab.mars.rl.util.collection.fork
import lab.mars.rl.util.log.debug
import lab.mars.rl.util.math.argmax
import lab.mars.rl.util.tuples.tuple3

fun IndexedMDP.`Monte Carlo Exploring Starts`(π: IndexedPolicy = null_policy, episodes: Int): OptimalSolution {
  val π = if (π == null_policy) IndexedPolicy(QFunc { 1.0 }) else π
  val Q = QFunc { 0.0 }
  val tmpQ = QFunc { Double.NaN }
  val count = QFunc { 0 }
  val tmpS = newBuf<IndexedState>(states.size)
  
  for (episode in 1..episodes) {
    log.debug { "$episode/$episodes" }
    var s = started()
    var a = s.actions.rand()//Exploring Starts
    
    var accumulate = 0.0
    do {
      val (s_next, reward) = a.sample()
      if (tmpQ[s, a].isNaN())
        tmpQ[s, a] = accumulate
      accumulate += reward
      s = s_next
    } while (s.isNotTerminal.apply { if (this) a = π(s) })
    
    tmpS.clear()
    for ((s, a) in states.fork { it.actions }) {
      val value = tmpQ[s, a]
      if (!value.isNaN()) {
        Q[s, a] += accumulate - value
        count[s, a] += 1
        tmpS.append(s)
        tmpQ[s, a] = Double.NaN
      }
    }
    for (s in tmpS) {
      val a_greedy = argmax(s.actions) {
        val n = count[s, it]
        if (n > 0)
          Q[s, it] / n
        else
          Q[s, it]
      }
      for (a in s.actions)
        π[s, a] = if (a === a_greedy) 1.0 else 0.0
    }
  }
  
  Q.set { idx, value ->
    val n = count[idx]
    if (n > 0)
      value / n
    else
      value
  }
  val V = VFunc { 0.0 }
  val result = tuple3(π, V, Q)
  V_from_Q(states, result)
  return result
}