// See README.md for license details.

package chapter6.sequencer

import chapter6.{SimpleIO, SimpleIOParams}
import chisel3._
import test_util.{BaseSimDTM, BaseSimDTMBundle}

class SimDTMSequencer(p: SimpleIOParams)(limit: Int) extends BaseSimDTM(limit) {
  val io = IO(new BaseSimDTMBundle {
    val t = new SimpleIO(p)
    val debug_stm = Output(State())
  })

  val dut = Module(new Sequencer(p)(true))

  io.t <> dut.io.sio
  io.debug_stm := dut.io.debug_stm.get

  connect(false.B)
}
