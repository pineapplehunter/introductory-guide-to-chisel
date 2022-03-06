// See README.md for license details.

package chapter6.uart

import chapter6.SimpleIOParams
import chiseltest.ChiselScalatestTester
import chiseltest.iotesters.PeekPokeTester
import chiseltest.simulator.WriteVcdAnnotation
import org.scalatest.flatspec.AnyFlatSpec

import scala.math.{floor, random}

/**
  * CSRモジュールのテスト制御クラス
  *
  * @param c CSR
  */
class CSRUnitTester(c: CSR) extends PeekPokeTester(c) {

  /**
    * アイドル
    *
    * @param cycle アイドルのサイクル数
    */
  def idle(cycle: Int = 1): Unit = {
    poke(c.csrIo.simpleIO.writeEnable, false)
    poke(c.csrIo.simpleIO.readEnable, false)
    step(cycle)
  }

  /**
    * レジスタライト
    *
    * @param addr レジスタのアドレス
    * @param data ライトデータ
    */
  def hwrite(addr: Int, data: Int): Unit = {
    poke(c.csrIo.simpleIO.address, addr)
    poke(c.csrIo.simpleIO.writeEnable, true)
    poke(c.csrIo.simpleIO.writeData, data)
    step(1)
  }

  /**
    * レジスタリード
    *
    * @param addr レジスタのアドレス
    * @param exp  リードの期待値
    */
  def hread(addr: Int, exp: Int): Unit = {
    poke(c.csrIo.simpleIO.address, addr)
    poke(c.csrIo.simpleIO.readEnable, true)
    step(1)
    expect(c.csrIo.simpleIO.readDataValid, true)
    expect(c.csrIo.simpleIO.readData, exp)
  }

  /**
    * 受信FIFOへのライト
    *
    * @param data register write data
    */
  def uwrite(data: Int): Unit = {
    poke(c.csrIo.csr2control.rx.enable, true)
    poke(c.csrIo.csr2control.rx.data, data)
    step(1)
    poke(c.csrIo.csr2control.rx.enable, false)
    step(1)
  }

  /**
    * 送信FIFOのenable発行
    */
  def txfifoAck(): Unit = {
    poke(c.csrIo.csr2control.tx.enable, true)
    step(1)
  }
}

/**
  * CSRのテストクラス
  */
class CSRTester extends AnyFlatSpec with ChiselScalatestTester {

  val dutName = "CSR"

  behavior of dutName

  val sp = SimpleIOParams()

  it should "送信FIFOにライトできる" in {
    test(new CSR(sp)(true)).runPeekPoke {
      c =>
        new CSRUnitTester(c) {
          val txData = Range(0, 10).map(_ => floor(random * 256).toInt)

          idle()
          for (d <- txData) {
            hwrite(RegInfo.txFifo, d)
            expect(c.csrIo.dbg.get.txFifo, d)
          }
        }
    }
  }

  it should "送信FIFOにライトするとtx_emptyビットが0に遷移する" in {
    test(new CSR(sp)(true)).withAnnotations(Seq(WriteVcdAnnotation)).runPeekPoke {
      c =>
        new CSRUnitTester(c) {
          val txData = 0xff

          idle()
          expect(c.csrIo.csr2control.tx.empty, true)
          hwrite(RegInfo.txFifo, txData)
          expect(c.csrIo.csr2control.tx.empty, false)
        }
    }
  }

  it should "be able to read Stat register from Host" in {
    test(new CSR(sp)(true)).withAnnotations(Seq(WriteVcdAnnotation)).runPeekPoke {
      c =>
        new CSRUnitTester(c) {
          val txData = 0xff

          idle()
          expect(c.csrIo.csr2control.tx.empty, true)
          hwrite(RegInfo.txFifo, txData)
          expect(c.csrIo.csr2control.tx.empty, false)
        }
    }
  }

  // Ctrlレジスタは使用していないのでテスト実行対象から除外
  // その場合は次のように"ignore"を指定する
  ignore should "be able to read Ctrl register from Host" in {
  }

  // ここからは受信FIFOのテストなので"behaviour"を変更している
  behavior of "RxFifo"

  it should "UARTの制御ブロック側から受信FIFOにライトできる" in {
    test(new CSR(sp)(true)).withAnnotations(Seq(WriteVcdAnnotation)).runPeekPoke {
      c =>
        new CSRUnitTester(c) {
          val txData = Range(0, 10).map(_ => floor(random * 256).toInt)

          idle()
          for (d <- txData) {
            uwrite(d)
            expect(c.csrIo.dbg.get.rxFifo, txData(0))
          }
        }
    }
  }

  it should "ホスト側からは受信FIFOをリードできる" in {
    test(new CSR(sp)(true)).runPeekPoke {
      c =>
        new CSRUnitTester(c) {
          val txData = Range(0, 10).map(_ => floor(random * 256).toInt)

          idle()
          for (d <- txData) {
            hwrite(RegInfo.txFifo, d)
            expect(c.csrIo.dbg.get.txFifo, d)
          }
        }
    }
  }
}
