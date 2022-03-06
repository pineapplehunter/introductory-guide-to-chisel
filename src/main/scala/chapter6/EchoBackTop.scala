// See README.md for license details.

package chapter6

import chapter6.sequencer._
import chapter6.uart._
import chisel3._

/**
  * EchoBackTop
  *
  * @param p         SimpleIOのパラメータ
  * @param baudrate  ボーレート
  * @param clockFreq クロック周波数(MHz)
  */
class EchoBackTop(p: SimpleIOParams, baudrate: Int = 9600, clockFreq: Int = 100) extends Module {
  val io = IO(new UartIO)

  io.tx := io.rx

  val sequencerModule = Module(new Sequencer(p))
  val uartModule = Module(new UartTop(baudrate, clockFreq))

  uartModule.io.simpleIo <> sequencerModule.io.simpleIo
  io <> uartModule.io.uartIO
}