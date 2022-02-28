// See README.md for license details.

import math.pow
import chapter5.{RX, TX}
import chapter6.SimpleIOParams
import chisel3._
import chisel3.stage.ChiselStage

/**
  * Stringでもらったクラスのパスからインスタンスを生成する
  */
object GetInstance {
  /**
    * apply
    *
    * @param modPath インスタンスを生成したいChiselモジュールのクラスパス
    * @return modPathで指定したChiselモジュールのインスタンス
    * @note このapplyで作れるのはクラスパラメータを持たないクラスのみ
    */
  def apply(modPath: String): Module = {
    Class.forName(modPath)
      .getConstructor()
      .newInstance() match {
      case m: Module => m
      case _ => throw new ClassNotFoundException("The class must be inherited chisel3.Module")
    }
  }
}

/**
  * RTLを生成するジェネレータ
  * runMainのプログラム引数でRTLを生成したいChiselモジュールの
  * パスを渡すと、その情報を元にクラスのインスタンスを
  * chisel3.Driver.executeに渡してRTLを生成する。
  */
object RTLGenerator extends App {

  val genArgs = Array(
    s"-td=rtl"
  )

  args(0) match {
    // chapter1
    case "chapter1.FIFO" =>
      (new ChiselStage).emitVerilog(new chapter1.FIFO(16))
    // chapter4
    case "chapter4.HelloChisel" =>
      val freq = 100 * pow(10, 6).toInt // 50MHz
      val interval = 500 // 500msec
      (new ChiselStage).emitVerilog(new chapter4.HelloChisel(freq, interval))
    // chapter5
    case "chapter5.SampleTop" =>
      (new ChiselStage).emitVerilog(new chapter5.SampleTop(4, 8))
    case "chapter5.SampleDontTouch" =>
      val modName = "SampleDontTouch"
      (new ChiselStage).emitVerilog(new chapter5.SampleDontTouch(true))
      (new ChiselStage).emitVerilog(new chapter5.SampleDontTouch(false))
    case "chapter5.SampleDelayParameterizeTop" =>
      (new ChiselStage).emitVerilog(new chapter5.SampleDelayParameterizeTop(true, false))
    case "chapter5.SampleNDelayParameterizeTop" =>
      (new ChiselStage).emitVerilog(new chapter5.SampleNDelayParameterizeTop(5, 10))
    case "chapter5.ParameterizeEachPorts" =>
      val portParams = Range(8, 25, 8) // 8, 16, 24
      (new ChiselStage).emitVerilog(new chapter5.ParameterizeEachPorts(portParams))
    case "chapter5.IOPortParameterize" =>
      (new ChiselStage).emitVerilog(new chapter5.IOPortParameterize(true, false))
    case "chapter5.IOParameterize" =>
      (new ChiselStage).emitVerilog(new chapter5.IOParameterize(TX))
      (new ChiselStage).emitVerilog(new chapter5.IOParameterize(RX))
    case "chapter5.SampleSuggestName2" =>
      (new ChiselStage).emitVerilog(new chapter5.SampleSuggestName2(Seq("A", "B")))
    case "chapter6.EchoBackTop" =>
      val p = SimpleIOParams()
      (new ChiselStage).emitVerilog(new chapter6.EchoBackTop(p))
    case _ => (new ChiselStage).emitVerilog(GetInstance(args(0)))
  }
}
