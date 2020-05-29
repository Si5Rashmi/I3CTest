package sifive.blocks.i3cmaster


import chisel3._
import chisel3.util._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.diplomaticobjectmodel._
import freechips.rocketchip.diplomaticobjectmodel.model.{OMDevice,OMMemoryRegion,OMComponent,OMRegister}
import freechips.rocketchip.diplomaticobjectmodel.model._
import freechips.rocketchip.diplomaticobjectmodel.logicaltree._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.util._

case object I3CMasterKey extends Field[Seq[I3CMasterParams]]

case class I3CMasterParams(
 
	beatBytes:	  Int = 4,
	address:          BigInt
)

case class OMI3CMasterParams(
	i3cmaster: I3CMasterParams,
	memoryRegions: Seq[OMMemoryRegion],
	interrupts : Seq[OMInterrupt],
	_types: Seq[String] = Seq("OMI3CMaster","OMDevice","OMComponent","OMCompoundType") 
)extends OMDevice

class I3CMaster(params: I3CMasterParams)(implicit p: Parameters) extends LazyModule
{
	
  val device = new SimpleDevice("i3cmaster", Seq("sifive,i3cmaster0")) 

  val controlNode = TLRegisterNode(
		address = Seq(AddressSet(params.address, 0xffff)),
		device = device,
		beatBytes = params.beatBytes)


lazy val module = new LazyModuleImp(this){



val r1 = RegInit(0.U(32.W))

val r2 = RegInit(0.U(32.W))


val field = Seq (
	0x0 -> RegFieldGroup("Register1",Some("First Register"),
	Seq(RegField(32,r1))),

	0x4 -> RegFieldGroup("Register2",Some("Second Register"),
	Seq(RegField(32,r2)))
)

controlNode.regmap(field : _*)
}

  lazy val ltnode = new LogicalTreeNode(() => Some(device)) {
    def getOMComponents(resourceBindings: ResourceBindings, children: Seq[OMComponent] = Nil): Seq[OMComponent] = {
      val compname = device.describe(resourceBindings).name
      val regions = DiplomaticObjectModelAddressing.getOMMemoryRegions(compname, resourceBindings, Some(OMRegister.convert(module.field: _*)))
      val intr = DiplomaticObjectModelAddressing.describeGlobalInterrupts(compname, resourceBindings)
	Seq(OMI3CMasterParams(i3cmaster = params, memoryRegions = regions,interrupts = intr))
    }
  }

}

case class I3CMasterAttachParams(
 i3cmaster : I3CMasterParams,
 controlBus : TLBusWrapper
 
) 

object I3CMaster {

 val nextId = { var i = -1; () => { i += 1; i}}

 def attach(params : I3CMasterAttachParams)(implicit p: Parameters): I3CMaster = {
	val name = s"i3cmaster_${nextId()}"	
	val i3cmaster = LazyModule(new I3CMaster(params.i3cmaster.copy(beatBytes = params.controlBus.beatBytes)))
	i3cmaster.suggestName(name)


//params.controlBus.coupleTo(name) { i3cmaster.controlNode := TLFragmenter(params.controlBus) :=  TLWidthWidget(params.controlBus) := _ }
 
 	 params.controlBus.coupleTo(name) { i3cmaster.controlNode := TLWidthWidget(params.controlBus):= _ }
	i3cmaster

}
}
