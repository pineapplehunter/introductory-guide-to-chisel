// See README.md for license details.

package chapter6

import chisel3._

/**
  * SimpleIOクラスのパラメータ用クラス
  *
  * @param addressBits アドレスのビット幅
  * @param dataBits データのビット幅
  */
case class SimpleIOParams
(
  addressBits: Int = 4,
  dataBits: Int = 8
)

/**
  * SimpleIO
  *
  * @param params IOパラメータ
  */
class SimpleIO(params: SimpleIOParams) extends Bundle {
  val address = Output(UInt(params.addressBits.W))
  val writeEnable = Output(Bool())
  val readEnable = Output(Bool())
  val writeData = Output(UInt(params.dataBits.W))
  val readDataValid = Input(Bool())
  val readData = Input(UInt(params.dataBits.W))
}
