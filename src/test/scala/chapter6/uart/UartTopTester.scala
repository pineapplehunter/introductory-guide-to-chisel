// See README.md for license details.

package chapter6.uart

import chiseltest.ChiselScalatestTester
import chiseltest.iotesters.PeekPokeTester
import chiseltest.simulator.WriteVcdAnnotation
import org.scalatest.flatspec.AnyFlatSpec

import scala.math.{floor, random, round}

/**
  * UartTopのテスト制御用クラス
  *
  * @param c         テスト対象モジュール (UartTop)
  * @param baudrate  ボーレート
  * @param clockFreq クロックの周波数
  */
class UartTopUnitTester(c: UartTop, baudrate: Int, clockFreq: Int)(implicit debug: Boolean = false)
  extends PeekPokeTester(c) {

  val memAccLimit = 10
  val timeOutCycle = 1000
  val duration = round(clockFreq.toDouble / baudrate).toInt

  println(s"duration = $duration")

  var i = 0

  /**
    * アイドル
    */
  def idle(): Unit = {
    poke(c.io.simpleIo.writeEnable, false)
    poke(c.io.simpleIo.readEnable, false)
    poke(c.io.uartIO.rx, true)
    step(1)
  }

  /**
    * レジスタライト
    *
    * @param addr レジスタアドレス
    * @param data ライトデータ
    */
  def write(addr: Int, data: Int): Unit = {
    if (debug) {
      println(f"[HOST] write(0x$addr%02x) : 0x$data%08x")
    }
    issueWrite(addr, data)
    poke(c.io.simpleIo.writeEnable, false)
    step(1)
  }

  /**
    * Uart register write
    *
    * @param addr destination address
    * @param data data to write
    */
  def issueWrite(addr: Int, data: Int): Unit = {
    poke(c.io.simpleIo.address, addr)
    poke(c.io.simpleIo.writeEnable, true)
    poke(c.io.simpleIo.writeData, data)
    step(1)
  }

  /**
    * レジスタリード
    *
    * @param addr レジスタアドレス
    * @param exp  期待値
    * @param cmp  期待値比較を実行するかどうかのフラグ
    * @return リードデータ
    */
  def read(addr: Int, exp: BigInt, cmp: Boolean = true): BigInt = {
    poke(c.io.simpleIo.address, addr)
    poke(c.io.simpleIo.readEnable, true)
    step(1)
    poke(c.io.simpleIo.readEnable, false)
    val data = peek(c.io.simpleIo.readData)
    if (debug) {
      println(f"[HOST] read (0x$addr%02x) : 0x$data%08x")
    }
    if (cmp) {
      expect(c.io.simpleIo.readDataValid, true)
      expect(c.io.simpleIo.readData, exp)
    }
    data
  }

  /**
    * UART送信
    *
    * @param data 送信するデータ
    */
  def send(data: Int): Unit = {
    println(f"[UART] send data   : 0x$data%02x")
    // send start bit
    poke(c.io.uartIO.rx, false)
    for (_ <- Range(0, duration)) {
      step(1)
    }

    // send bit data
    for (idx <- Range(0, 8)) {
      val rxBit = (data >> idx) & 0x1
      //println(s"peri.uart bit= $rxBit")
      poke(c.io.uartIO.rx, rxBit)
      for (_ <- Range(0, duration)) {
        step(1)
      }
    }

    // send stop bits
    poke(c.io.uartIO.rx, true)
    for (_ <- Range(0, duration)) {
      step(1)
    }

  }

  /**
    * UART受信
    *
    * @param exp 期待値
    */
  def receive(exp: Int): Unit = {

    // detect start
    while (peek(c.io.uartIO.tx) == 0x1) {
      step(1)
    }

    // shift half period
    for (_ <- Range(0, duration / 2)) {
      step(1)
    }

    expect(c.io.uartIO.tx, false, "detect bit must be low")

    for (idx <- Range(0, 8)) {
      val expTxBit = (exp >> idx) & 0x1
      for (_ <- Range(0, duration)) {
        step(1)
      }
      expect(c.io.uartIO.tx, expTxBit, s"don't match exp value bit($idx) : exp = $expTxBit")
    }

    // stop bits
    for (_ <- Range(0, duration)) {
      step(1)
    }

    // check stop bit value
    expect(c.io.uartIO.tx, true, s"stop bit must be high")
  }
}

/**
  * UartTopの送信動作のテストクラス
  */
class UartTopTester extends AnyFlatSpec with ChiselScalatestTester {

  import RegInfo._

  val dutName = "chapter6.uart.UartTop"

  behavior of dutName

  it should s"ホストからTxFIFOにライトが発生すると、UARTの送信が行われる [$dutName-tx-000]" in {
    val baudrate = 500000
    val clockFreq = 1_000_000

    test(new UartTop(baudrate, clockFreq)).runPeekPoke {
      c =>
        new UartTopUnitTester(c, baudrate, clockFreq) {

          val txData = Range(0, 100).map(_ => floor(random * 256).toInt)

          idle()

          step(10)
          for (data <- txData) {
            while ((read(status, 0x4, cmp = false) & 0x4) == 0x0) {
              step(1)
            }
            read(status, 0x4) // TxFifoEmpty
            write(txFifo, data)
            receive(data)
          }
        }
    }
  }

  it should "送信FIFOにデータをライトすると、TxEmptyビットが0x0に遷移する" +
    s" [$dutName-tx-001]" in {
    val baudrate = 500000
    val clockFreq = 1_000_000

    test(new UartTop(baudrate, clockFreq)).runPeekPoke {
      c =>
        new UartTopUnitTester(c, baudrate, clockFreq) {

          val txData = Range(0, 100).map(_ => floor(random * 256).toInt)

          idle()

          for (data <- txData) {
            while ((read(status, 0x4, cmp = false) & 0x4) == 0x0) {
              step(1)
            }
            read(status, 0x4) // TxFifoEmpty
            write(txFifo, data)
            read(status, 0x0) // TxFifoEmpty 1 -> 0
          }
        }
    }
  }

  it should "送信FIFOにデータを連続で16個ライトすると、TxFullビットが0x1に遷移する" +
    s" [$dutName-tx-002]" in {

    val baudrate = 9600
    val clockFreq = 100_000_000

    test(new UartTop(baudrate, clockFreq)).runPeekPoke {
      c =>
        new UartTopUnitTester(c, baudrate, clockFreq) {

          val txData = Range(0, 16).map(_ => floor(random * 256).toInt)

          idle()

          // TxEmptyを確認
          while ((read(status, 0x4, cmp = false) & 0x4) == 0x0) {
            step(1)
          }

          // データを連続でライト
          for (data <- txData) {
            write(txFifo, data)
          }
          read(status, 0x8) // TxFifoFull 0 -> 1
        }
    }
  }

  it should s"ボーレート／クロック周波数を変更しても適切に送信できる [$dutName-tx-100]" in {
    val baudrate = 9600
    val clockFreq = 100_000_000

    test(new UartTop(baudrate, clockFreq)).runPeekPoke {
      c =>
        new UartTopUnitTester(c, baudrate, clockFreq) {

          val b = new scala.util.control.Breaks
          val txData = Range(0, 3).map(_ => floor(random * 256).toInt)

          idle()

          for (data <- txData) {
            while ((read(status, 0x4, cmp = false) & 0x4) == 0x0) {
              step(1)
            }
            read(status, 0x4) // TxFifoEmpty
            write(txFifo, data)
            receive(data)
          }
        }
    }
  }
}

/**
  * UartTopの受信動作のテストクラス
  */
class UartRxTester extends AnyFlatSpec with ChiselScalatestTester {

  import RegInfo._

  val dutName = "peri.uart-Top"

  behavior of dutName

  it should s"対向からUART送信を行うと、受信FIFOにデータが格納され、RxDataValidが0x1に遷移する [$dutName-rx-000]" in {
    val baudrate = 500_000
    val clockFreq = 1_000_000

    test(new UartTop(baudrate, clockFreq)).runPeekPoke {
      c =>
        new UartTopUnitTester(c, baudrate, clockFreq) {

          //val rxData = Range(0, 100).map(_ => floor(random * 256).toInt)
          val rxData = Range(0, 25)

          idle()

          for (data <- rxData) {
            send(data)

            // wait data receive
            while ((read(status, 0x4, cmp = false) & 0x1) == 0x0) {
              step(1)
            }
            read(status, 0x5) // TxFifoEmpty / RxDataValid
            read(rxFifo, data)
          }
        }
    }
  }

  it should s"ボーレート／クロック周波数を変更しても適切に受信できる [$dutName-rx-100]" in {
    val baudrate = 9600
    val clockFreq = 100_000_000

    test(new UartTop(baudrate, clockFreq))
      .withAnnotations(Seq(WriteVcdAnnotation))
      .runPeekPoke {
        c =>
          new UartTopUnitTester(c, baudrate, clockFreq) {

            //val rxData = Range(0, 100).map(_ => floor(random * 256).toInt)
            val rxData = Range(0, 2)

            idle()

            for (data <- rxData) {
              send(data)

              // wait data receive
              while ((read(status, 0x4, cmp = false) & 0x1) == 0x0) {
                step(1)
              }
              read(status, 0x5) // TxFifoEmpty / RxDataValid
              read(rxFifo, data)
            }
          }
      }
  }
}
