// See README.md for license details.

package chapter6.sequencer

import chapter6.SimpleIOParams
import chisel3._
import chiseltest.ChiselScalatestTester
import chiseltest.iotesters.PeekPokeTester
import chiseltest.simulator.WriteVcdAnnotation
import org.scalatest.flatspec.AnyFlatSpec

import scala.math.abs
import scala.util.Random

/**
  * Unit tester for TxRxCtrl module.
  *
  * @param c dut module (instance of TxRXCtrl)
  */
class SequencerUnitTester(c: SimDTMSequencer) extends PeekPokeTester(c) {

  import chapter6.uart._

  val memAccLimit = 10
  val timeOutCycle = 1000

  var i = 0

  val simpleIO = c.io.simpleIO

  def waitRead(): Unit = {
    while (peek(simpleIO.readEnable) != 0x1) {
      step(1)
    }
  }

  def retRead(data: Int): Unit = {
    poke(simpleIO.readDataValid, true)
    poke(simpleIO.readData, data)
    step(1)
    poke(simpleIO.readDataValid, false)
  }

  /**
    *
    */
  def idleTest(): Unit = {
    val statData = Seq(0x80, 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01)

    for (stat <- statData) {
      expect(c.io.debug_stm, State.sIdle)

      // ステータスレジスタのリード待ち
      waitRead()

      // アイドル時はずっとステータスレジスタをリード
      expect(simpleIO.address, RegInfo.status)
      poke(simpleIO.readDataValid, true)
      // statDataの中身を順にリード値として返却
      poke(simpleIO.readData, stat)
      step(1)
      poke(simpleIO.readDataValid, false)
    }

    // 最後のリードでステートが遷移することを確認
    expect(c.io.debug_stm, State.sRX)
  }

  /**
    *
    * @param rxData
    */
  def rxTest(rxData: Int): Unit = {
    expect(c.io.debug_stm, State.sIdle, "state wrong")

    // ステータスレジスタのリード待ち
    waitRead()
    step(1)
    retRead(0x1)

    // ステートの遷移を確認
    expect(c.io.debug_stm, State.sRX, "state wrong")
    waitRead()
    expect(simpleIO.address, RegInfo.rxFifo, "address wrong")
    step(1)
    retRead(rxData)
    expect(c.io.debug_stm, State.sTX, "state wrong")
  }

  /**
    *
    * @param txData
    * @param statData
    */
  def txTest(txData: Int, statData: Seq[Int]): Unit = {

    rxTest(txData)

    // ステートの遷移を確認
    expect(c.io.debug_stm, State.sTX)

    for (stat <- statData) {
      waitRead()
      expect(simpleIO.address, RegInfo.status)
      step(1)
      retRead(stat)
    }

    expect(c.io.simpleIO.writeEnable, true)
    expect(c.io.simpleIO.writeData, txData)
    step(1)
    expect(c.io.simpleIO.writeEnable, false)

    expect(c.io.debug_stm, State.sIdle)
  }
}

class SequencerTester extends AnyFlatSpec with ChiselScalatestTester {

  val dutName = "chapter6.sequencer.Sequencer"

  behavior of dutName

  val p = SimpleIOParams()
  val limit = 1000
//  implicit val debug = true

  it should s"データを受信するまではアイドルステートに留まる. [$dutName-000]" in {
    test(new SimDTMSequencer(p)(limit)).withAnnotations(Seq(WriteVcdAnnotation)).runPeekPoke { c =>
      new SequencerUnitTester(c) {
        idleTest()
        step(5)
      }
    }
  }

  it should s"データの受信ステータスを確認後、rx_fifoをリードしデータを受信する. [$dutName-001]" in {
    test(new SimDTMSequencer(p)(limit)).withAnnotations(Seq(WriteVcdAnnotation)).runPeekPoke { c =>
      new SequencerUnitTester(c) {
        rxTest(0xa5)
        step(5)
      }
    }
  }

  it should s"データを受信後、送信FIFOに受信データをライトする. [$dutName-002]" in {
    test(new SimDTMSequencer(p)(limit)).withAnnotations(Seq(WriteVcdAnnotation)).runPeekPoke(c =>
      new SequencerUnitTester(c) {
        txTest(0xff, Seq(0x0)) // 送信FIFOに空きあり
        step(5)
      })
  }

  it should s"データを受信後に送信FIFOがフルの場合は、空くまでステータスレジスタをポーリングする. [$dutName-003]" in {
    test(new SimDTMSequencer(p)(limit)).withAnnotations(Seq(WriteVcdAnnotation)).runPeekPoke {
      c =>
        new SequencerUnitTester(c) {
          txTest(0xff, Seq.fill(100)(0xc) :+ 0x4) // 送信FIFOに空きなし→ありのパターン
          step(5)
        }
    }
  }

  it should s"連続で処理をしてもステートが正常に遷移する. [$dutName-004]" in {
    val r = new Random(1)

    test(new SimDTMSequencer(p)(limit * 10)).withAnnotations(Seq(WriteVcdAnnotation)).runPeekPoke(
      c =>
        new SequencerUnitTester(c) {

          for (_ <- 0 until 10) {
            val numOfStat = abs(r.nextInt(31)) + 1
            val statData = Range(0, numOfStat).map(_ => r.nextInt() & 0xf7 | 0x8) :+ 0x4
            txTest(r.nextInt() & 0xff, statData) // 送信FIFOに空きなし→ありのパターン
            step(5)
          }
        }
    )
  }
}
