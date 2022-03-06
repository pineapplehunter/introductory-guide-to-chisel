// See README.md for license details.

package chapter5

import chisel3._
import chisel3.util._

/**
  * FIFO リード側 I/O
  */
class FIFOReadIO[T <: Data](gen: T) extends Bundle {
  val enable = Input(Bool())
  val empty = Output(Bool())
  val data = Output(gen)
}

/**
  * FIFO ライト側 I/O
  */
class FIFOWriteIO[T <: Data](gen: T) extends Bundle {
  val enable = Input(Bool())
  val full = Output(Bool())
  val data = Input(gen)
}

/**
  * FIFO I/O
  *
  * @param gen   格納したいデータ型のインスタンス
  * @param depth FIFOの段数
  * @param debug trueでデバッグモード
  */
class FIFOIO[T <: Data](gen: T, depth: Int = 16, debug: Boolean = false) extends Bundle {

  val depthBits = log2Ceil(depth)

  val write = new FIFOWriteIO(gen)
  val read = new FIFOReadIO(gen)

  val dbg = if (debug) {
    Some(Output(new Bundle {
      val r_wrptr = Output(UInt(depthBits.W))
      val r_rdptr = Output(UInt(depthBits.W))
      val r_data_ctr = Output(UInt((depthBits + 1).W))
    }))
  } else {
    None
  }
}

/**
  * 単純なFIFO
  *
  * @param gen   格納したいデータ型のインスタンス
  * @param depth FIFOの段数
  * @param debug trueでデバッグモード
  */
class FIFO[T <: Data](gen: T, depth: Int = 16, debug: Boolean = false) extends Module {

  // parameter
  val depthBits = log2Ceil(depth)
  val io = IO(new FIFOIO(gen, depth, debug))
  val r_fifo = RegInit(VecInit(Seq.fill(depth)(0.U.asTypeOf(gen))))
  val r_rdptr = RegInit(0.U(depthBits.W))
  val r_wrptr = RegInit(0.U(depthBits.W))
  val r_data_ctr = RegInit(0.U((depthBits + 1).W))

  def ptrWrap(ptr: UInt): Bool = ptr === (depth - 1).U

  // リードポインタ
  when(io.read.enable) {
    r_rdptr := Mux(ptrWrap(r_rdptr), 0.U, r_rdptr + 1.U)
  }

  // ライトポインタ
  when(io.write.enable) {
    r_fifo(r_wrptr) := io.write.data
    r_wrptr := Mux(ptrWrap(r_wrptr), 0.U, r_wrptr + 1.U)
  }

  // データカウント
  when(io.write.enable && io.read.enable) {
    r_data_ctr := r_data_ctr
  }.otherwise {
    when(io.write.enable) {
      r_data_ctr := r_data_ctr + 1.U
    }
    when(io.read.enable) {
      r_data_ctr := r_data_ctr - 1.U
    }
  }

  // IOとの接続
  io.write.full := r_data_ctr === depth.U
  io.read.empty := r_data_ctr === 0.U
  io.read.data := r_fifo(r_rdptr)

  // テスト用のデバッグ端子の接続
  if (debug) {
    io.dbg.get.r_wrptr := r_wrptr
    io.dbg.get.r_rdptr := r_rdptr
    io.dbg.get.r_data_ctr := r_data_ctr
  }
}