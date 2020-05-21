

package sifive.blocks.i3cmaster

import chisel3._
import chisel3.util._

import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.diplomaticobjectmodel.logicaltree._
import freechips.rocketchip.diplomaticobjectmodel.ConstructOM
import freechips.rocketchip.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.system._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.devices.debug.Debug._
import freechips.rocketchip.tilelink.{TLError,DevNullParams}

import sifive.skeleton._

class I3CMasterConfig0 extends Config((site,here,up) => {
 case I3CMasterKey => I3CMasterParams(
 
	address		= 0x10000000
	beatbytes	= 4
)
}) 



class TestSocDUT(harness: LazyScope)(implicit p: Parameters) extends SkeletonDUT(harness)
{
  val I3CMasterParams = p(I3CMasterKey)
  val i3cmaster = LazyModule(new I3CMaster(I3CMasterParams)) 
  pbus.coupleTo("i3cmaster"){ i3cmaster.controlNode := TLWidthWidget(pbus) :=_ }
  LogicalModuleTree.add(attachParams.parentNode, i3cmaster.ltnode)
}

class TestSocHarness()(implicit p: Parameters) extends LazyModule with LazyScope
{
  val dut = LazyModule(new TestSocDUT(this))
  lazy val module = new LazyModuleImp(this) {
    ConstructOM.constructOM()
    Debug.tieoffDebug(dut.module.debug)
  }
}