// See README.md for license details.

package chapter6.uart

import chapter5.{FIFO, FIFOReadIO, FIFOWriteIO}
import chapter6.{SimpleIO, SimpleIOParams}
import chisel3._
import chisel3.util._

/**
  * CSRのレジスタアドレス
  */
object RegInfo {
  val rxFifo = 0x0 // RX FIFO
  val txFifo = 0x4 // TX FIFO
  val status = 0x8 // Status
  val control = 0xc // Control
}

/**
  * CSRのレジスタ用クラス
  */
abstract class UartReg extends Bundle {
  def write(data: UInt): Unit

  def read(): UInt
}

/**
  * ステータス・レジスタ
  */
class StatReg extends UartReg with IgnoreSeqInBundle {
  val txFifoFull = Bool()
  val txFifoEmpty = Bool()
  val rxFifoFull = Bool()
  val rxFifoValid = Bool()

  // ビット配置
  val bitOrder = Seq(
    txFifoFull, txFifoEmpty,
    rxFifoFull, rxFifoValid)

  //
  def write(v: UInt): Unit = {
    bitOrder.zip(v.asBools).foreach { case (r, b) => r := b }
  }

  def read(): UInt = Cat(bitOrder)
}

/**
  * デバッグ用のBundle
  */
class CSRDebugIO extends Bundle {
  val rxFifo = Output(UInt(8.W))
  val txFifo = Output(UInt(8.W))
  val status = Output(UInt(8.W))
}

/**
  * CSRとCtrl間のI/F
  */
class CSR2CtrlIO extends Bundle {
  val tx = new FIFOReadIO(UInt(8.W))
  val rx = new FIFOWriteIO(UInt(8.W))
}

/**
  * CSR部のIOクラス
  *
  * @param params     CSRブロックのパラメータ
  * @param debug Trueでデバッグ用のIOが有効になる
  */
class CSRIO(params: SimpleIOParams)(implicit debug: Boolean = false) extends Bundle {
  val simpleIO = Flipped(new SimpleIO(params))
  val csr2control = new CSR2CtrlIO()
  val dbg = if (debug) Some(new CSRDebugIO) else None
}

/**
  * レジスタブロック
  *
  * @param params    IOの設定
  * @param debug デバッグ出力のON/OFF
  */
class CSR(params: SimpleIOParams)(implicit debug: Boolean = false) extends Module {

  val csrIo = IO(new CSRIO(params))

  // FIFOの段数
  val fifoDepth = 16

  val rxFifo = Module(new FIFO(UInt(8.W), fifoDepth))
  val txFifo = Module(new FIFO(UInt(8.W), fifoDepth))
  val writeStatus = WireInit(0.U.asTypeOf(new StatReg))

  // レジスタのアクセス制御信号
  val selectRx = (csrIo.simpleIO.address === RegInfo.rxFifo.U) && csrIo.simpleIO.readEnable
  val selectTx = (csrIo.simpleIO.address === RegInfo.txFifo.U) && csrIo.simpleIO.writeEnable
  val selectStatus = (csrIo.simpleIO.address === RegInfo.status.U) && csrIo.simpleIO.readEnable

  // statusレジスタの接続
  writeStatus.txFifoEmpty := txFifo.io.read.empty
  writeStatus.txFifoFull := txFifo.io.write.full
  writeStatus.rxFifoFull := rxFifo.io.write.full
  writeStatus.rxFifoValid := !rxFifo.io.read.empty

  // リードの制御
  csrIo.simpleIO.readDataValid := RegNext(selectRx || selectStatus, false.B)
  csrIo.simpleIO.readData := RegNext(MuxCase(0.U, Seq(
    selectRx -> rxFifo.io.read.data,
    selectStatus -> writeStatus.read())), 0.U)

  // Ctrl部との接続
  // Tx
  csrIo.csr2control.tx <> txFifo.io.read
  txFifo.io.write.enable := selectTx
  txFifo.io.write.data := csrIo.simpleIO.writeData
  // Rx
  csrIo.csr2control.rx <> rxFifo.io.write
  rxFifo.io.read.enable := selectRx

  // debug
  if (debug) {
    val dbg = csrIo.dbg.get
    dbg.rxFifo := rxFifo.io.read.data
    dbg.txFifo := txFifo.io.write.data
    dbg.status := writeStatus.read()
  }
}
