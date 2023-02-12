package me.xx2bab.caliper.sample.customproxy

import me.xx2bab.caliper.anno.ASMOpcodes
import me.xx2bab.caliper.anno.CaliperMethodProxy

object CustomProxy {

    @CaliperMethodProxy(
        className = "LibrarySampleClass",
        methodName = "commonMethodReturnString",
        opcode = ASMOpcodes.INVOKEVIRTUAL
    )
    @JvmStatic
    fun commonMethodReturnsString() = "CustomProxy"

}