//// See README.md for license details.
//
//package chapter6
//
//import chiseltest.iotesters.PeekPokeTester
//import test.util.BaseTester
//
//import scala.math.{abs, pow, round}
//import scala.util.Random
//
///**
//  * EchoBackTopのテストモデル
//  *
//  * @param c SimCTMEchoBackTopのインスタンス
//  */
//class EchoBackerUnitTester(c: SimDTMEchoBackTop,
//                           baudrate: Int, clockFreq: Int)
//  extends PeekPokeTester(c) {
//
//  val memAccLimit = 10
//  val timeOutCycle = 1000
//  val duration = round(clockFreq * pow(10, 6) / baudrate).toInt
//
//  var i = 0
//
//  val rx = c.io.uart_rx
//
//  /**
//    * Uartのデータ受信
//    *
//    * @param exp 期待値
//    */
//  def receive(exp: Int): Unit = {
//
//    // uart_rxが下がるまでウェイト
//    while (peek(rx) == 0x1) {
//      step(1)
//    }
//
//    // 半周期ずらすためにduration / 2サイクルでループ
//    for (_ <- Range(0, duration / 2)) {
//      step(1)
//    }
//
//    expect(rx, false, "detect bit must be low")
//
//    for (idx <- Range(0, 8)) {
//      val expTxBit = (exp >> idx) & 0x1
//      for (_ <- Range(0, duration)) {
//        step(1)
//      }
//      expect(rx, expTxBit, s"don't match exp value bit($idx) : exp = $expTxBit")
//    }
//
//    // ストップビットの受信
//    for (_ <- Range(0, duration)) {
//      step(1)
//    }
//
//    // check stop bit value
//    expect(rx, true, s"stop bit must be high")
//  }
//}
//
///**
//  * EchoBackTopのテストクラス
//  */
//class EchoBackTopTester extends BaseTester {
//  val p = SimpleIOParams()
//  val limit = 1000000
//  implicit val debug = true
//
//  override def dutName:String = "EchoBackTopTester"
//
//  it should "UARTに送ったデータがループバックして受信できる。" +
//    s" [$dutName-000]" in {
//    val outDir = dutName + "-000"
//    val args = getArgs(Map(
//      "--top-name" -> dutName,
//      "--target-dir" -> s"test_run_dir/$outDir"
//    ))
//
//    val test_seq = Seq(0xa5, 0x2a, 0x35)
//    val baudrate = 960000
//    val clockFreq = 100
//
//    info(s"baudtate    = $baudrate Hz")
//    info(s"clock freq. = $clockFreq MHz")
//
//    test(new SimDTMEchoBackTop(p, baudrate, clockFreq)(test_seq, limit = limit)).runPeekPoke {
//      c =>
//        new EchoBackerUnitTester(c, baudrate, clockFreq) {
//          for (d <- test_seq) {
//            receive(d)
//          }
//          step(100)
//        }
//    }
//  }
//
//  it should "UARTに送ったデータがループバックして受信できる。" +
//    s" [$dutName-001]" in {
//    val outDir = dutName + "-000"
//    val args = getArgs(Map(
//      "--top-name" -> dutName,
//      "--target-dir" -> s"test_run_dir/$outDir"
//    ))
//
//    val test_seq = Seq(0xa5, 0x2a, 0x35)
//
//    // 周波数が1MHzの場合設定値上は666666がMaxだが
//    // 実際には500000で動くことになる(四捨五入でduration=2になるから）
//    // 理論上のbaurateの上限は周波数の1/2
//    val baudrate: Int = 500000
//    val clockFreq: Int = 1
//
//    info(s"baudtate    = $baudrate Hz")
//    info(s"clock freq. = $clockFreq MHz")
//
//    test(new SimDTMEchoBackTop(p, baudrate, clockFreq)(test_seq, limit = limit)).runPeekPoke {
//      c =>
//        new EchoBackerUnitTester(c, baudrate, clockFreq) {
//          for (d <- test_seq) {
//            receive(d)
//          }
//          step(100)
//        }
//    }
//  }
//}
