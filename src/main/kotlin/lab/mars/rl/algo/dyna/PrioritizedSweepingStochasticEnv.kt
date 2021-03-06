package lab.mars.rl.algo.dyna

import lab.mars.rl.algo.V_from_Q
import lab.mars.rl.algo.`ε-greedy (tie broken randomly)`
import lab.mars.rl.model.impl.mdp.*
import lab.mars.rl.model.isNotTerminal
import lab.mars.rl.model.log
import lab.mars.rl.util.log.debug
import lab.mars.rl.util.math.Rand
import lab.mars.rl.util.math.max
import lab.mars.rl.util.math.repeat
import lab.mars.rl.util.tuples.tuple2
import lab.mars.rl.util.tuples.tuple3
import org.apache.commons.math3.util.FastMath.abs
import java.util.*

@Suppress("NAME_SHADOWING")
fun IndexedMDP.PrioritizedSweepingStochasticEnv(
    n: Int,
    θ: Double,
    ε: Double,
    α: (IndexedState, IndexedAction) -> Double,
    episodes: Int,
    stepListener: (StateValueFunction, IndexedState) -> Unit = { _, _ -> },
    episodeListener: (StateValueFunction) -> Unit = {}): OptimalSolution {
  val π = IndexedPolicy(QFunc { 0.0 })
  val Q = QFunc { 0.0 }
  val PQueue = PriorityQueue(Q.size, Comparator<tuple3<Double, IndexedState, IndexedAction>> { o1, o2 ->
    o2._1.compareTo(o1._1)
  })
  val Model = QFunc { hashMapOf<tuple2<IndexedState, Double>, Int>() }
  val N = QFunc { 0 }
  val predecessor = VFunc { hashSetOf<tuple2<IndexedState, IndexedAction>>() }
  val V = VFunc { 0.0 }
  val result = tuple3(π, V, Q)
  for (episode in 1..episodes) {
    log.debug { "$episode/$episodes" }
    var step = 0
    var s = started()
    while (s.isNotTerminal) {
      V_from_Q(states, result)
      stepListener(V, s)
      step++
      `ε-greedy (tie broken randomly)`(s, Q, π, ε)
      val a = π(s)
      val (s_next, reward) = a.sample()
      Model[s, a].compute(tuple2(s_next, reward)) { _, v -> (v ?: 0) + 1 }
      N[s, a]++
      predecessor[s_next] += tuple2(s, a)
      val P = abs(reward + γ * max(s_next.actions, 0.0) { Q[s_next, it] } - Q[s, a])
      if (P > θ) PQueue.add(tuple3(P, s, a))
      repeat(n, { PQueue.isNotEmpty() }) {
        val (_, s, a) = PQueue.poll()
        val (s_next, reward) = Model[s, a].rand(N[s, a])
        Q[s, a] += α(s, a) * (reward + γ * max(s_next.actions, 0.0) { Q[s_next, it] } - Q[s, a])
        for ((s_pre, a_pre) in predecessor[s]) {
          val reward = Model[s_pre, a_pre].expectedReward(s)
          val P = abs(reward + γ * max(s.actions, 0.0) { Q[s, it] } - Q[s_pre, a_pre])
          if (P > θ) PQueue.add(tuple3(P, s_pre, a_pre))
        }
      }
      s = s_next
    }
    episodeListener(V)
    log.debug { "steps=$step" }
  }
  return result
}

private fun HashMap<tuple2<IndexedState, Double>, Int>.rand(N: Int): tuple2<IndexedState, Double> {
  if (isEmpty()) throw NoSuchElementException()
  val p = Rand().nextDouble()
  var acc = 0.0
  for ((k, v) in this) {
    acc += v.toDouble() / N
    if (p <= acc)
      return k
  }
  throw IllegalArgumentException("random=$p, but accumulation=$acc")
}

private fun HashMap<tuple2<IndexedState, Double>, Int>.expectedReward(s: IndexedState): Double {
  if (isEmpty()) throw NoSuchElementException()
  var sum = 0.0
  var N = 0
  for ((k, v) in this)
    if (k._1 == s) {
      sum += k._2 * v
      N += v
    }
  return sum / N
}