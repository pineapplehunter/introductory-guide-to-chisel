// See README.md for license details.

package chapter6.uart

import chapter6.{SimpleIO, SimpleIOParams}
import chisel3._

/**
  * UARTのIOクラス
  */
class UartIO extends Bundle {
  val tx = Output(UInt(1.W))
  val rx = Input(UInt(1.W))
}

class UartTopIO(p: SimpleIOParams)
               (implicit debug: Boolean = false) extends Bundle {
  val simpleIo = Flipped(new SimpleIO(p))
  val uartIO = new UartIO
  val dbg = if (debug) Some(new CSRDebugIO) else None
}

/**
  * Uartのトップモジュール
  *
  * @param baudrate  ボーレート
  * @param clockFreq クロックの周波数(MHz)
  */
class UartTop(baudrate: Int, clockFreq: Int) extends Module {

  val p = SimpleIOParams()

  val io = IO(new UartTopIO(p))

  val m_reg = Module(new CSR(p))
  val m_ctrl = Module(new TxRxControl(baudrate, clockFreq))

  io.uartIO <> m_ctrl.io.uart

  io.simpleIo <> m_reg.csrIo.simpleIO
  m_reg.csrIo.csr2control <> m_ctrl.io.csr2control
}
