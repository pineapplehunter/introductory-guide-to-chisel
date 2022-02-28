// See README.md for license details.

package chapter4

import chiseltest.{ChiselScalatestTester, VerilatorBackendAnnotation}
import chiseltest.iotesters.PeekPokeTester
import org.scalatest.flatspec.AnyFlatSpec

import math.pow

/**
  * HelloChiselのテストモジュール
  */
class HelloChiselTester extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "HelloChisel"

  it should "io.outは指定の周期で変化する" in {

    // テストのために周期を短く設定
    val freq = 50 * pow(10, 3).toInt // 50kHz
    val interval = 500 // 500msec

    test(new HelloChisel(freq, interval))
      .withAnnotations(Seq(VerilatorBackendAnnotation))
      .runPeekPoke(c => new PeekPokeTester(c) {
        reset()

        // リセット直後はLow
        expect(c.io.out, false)

        // その後は25000サイクル周期で変化する
        val expData = Range(0, 16).map(_ % 2)
        for (exp <- expData) {
          var i = 0
          while (i != (25 * pow(10, 3)).toInt) {
            expect(c.io.out, exp)
            step(1)
            i += 1
          }
        }
      })
  }
}