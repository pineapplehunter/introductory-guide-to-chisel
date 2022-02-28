// See README.md for license details.

package chapter5

import chiseltest.ChiselScalatestTester
import chiseltest.iotesters.PeekPokeTester
import org.scalatest.flatspec.AnyFlatSpec


/**
  * SampleAndの単体テスト用クラス
  * こちらにはテスト時に使用する制御用のメソッドを定義
  *
  * @param c
  */
class SampleAndUnitTester(c: SampleAnd) extends PeekPokeTester(c) {

  val io = c.io

  /**
    * 入力端子にデータをセット
    */
  def set(a: Int, b: Int): Unit = {
    poke(io.a, a)
    poke(io.b, b)
  }

  /**
    * 期待値と出力を比較
    */
  def compare(exp: Int): Unit = expect(io.c, exp, msg = f"(raw, exp) = (${peek(io.c)}, $exp)")
}

/**
  * SampleAndのテストクラス
  * テストの管理を行う
  */
class SampleAndTester extends AnyFlatSpec with ChiselScalatestTester {
  val dutName = "SampleAnd"

  behavior of dutName

  val args = Array(
    s"-tn=$dutName",
    s"-td=test_run_dir/$dutName"
  )

  // 最初のサンプルのテストを実装
  it should "(a, b) = (0, 0)の時、出力cは0になる" in {
    test(new SampleAnd).runPeekPoke(c => new SampleAndUnitTester(c) {
      set(0, 0)
      compare(0)
    })
  }

  // まとめて実施
  it should "(a, b)の組み合わせを入れた際にa && bの値が出力される" in {
    test(new SampleAnd).runPeekPoke(c => new SampleAndUnitTester(c) {
      for (a <- 0 to 1; b <- 0 to 1) {
        set(a, b)
        compare(a & b)
      }
    })
  }
}