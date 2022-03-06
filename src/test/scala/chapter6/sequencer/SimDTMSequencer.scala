// See README.md for license details.

package chapter6.sequencer

import chapter6.{SimpleIO, SimpleIOParams}
import chisel3._
import chisel3.stage.ChiselStage
import test_util.{BaseSimDTM, BaseSimDTMBundle}

class SequencerBundle(params:SimpleIOParams) extends BaseSimDTMBundle {
  val simpleIO = new SimpleIO(params)
  val debug_stm = Output(State())
}

class SimDTMSequencer(params: SimpleIOParams)(limit: Int) extends BaseSimDTM(limit) {
  val io = IO(new SequencerBundle(params))

  val dut = Module(new Sequencer(params)(true))

  io.simpleIO <> dut.io.simpleIo
  io.debug_stm := dut.io.debug_stm.get

  connect(false.B)
}

object A extends App {
  val p = SimpleIOParams()
  (new ChiselStage).emitVerilog(new SimDTMSequencer(p)(10))
}