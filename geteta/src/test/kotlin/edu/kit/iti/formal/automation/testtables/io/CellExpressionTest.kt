/**
 * geteta
 *
 * Copyright (C) 2016-2018 -- Alexander Weigl <weigl></weigl>@kit.edu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http:></http:>//www.gnu.org/licenses/gpl-3.0.html>.
 */
package edu.kit.iti.formal.automation.testtables.io


import edu.kit.iti.formal.automation.datatypes.INT
import edu.kit.iti.formal.automation.testtables.GetetaFacade
import edu.kit.iti.formal.automation.testtables.model.GeneralizedTestTable
import edu.kit.iti.formal.automation.testtables.model.IoVariableType
import edu.kit.iti.formal.automation.testtables.model.ParseContext
import edu.kit.iti.formal.automation.testtables.model.ProgramVariable
import edu.kit.iti.formal.smv.SMVTypes
import edu.kit.iti.formal.smv.ast.SVariable
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Created by weigl on 15.12.16.
 */
object CellExpressionTest {
    @TestFactory
    fun createExpressionTests(): List<DynamicTest> {
        return CASES.map {
            DynamicTest.dynamicTest(it) { parse(it) }
        }
    }

    private val gtt: GeneralizedTestTable
    private val pc: ParseContext

    init {
        this.gtt = defaultTestTable()
        pc = gtt.parseContext
    }

    fun parse(expr: String) {
        val v = SVariable.create("Q").withSigned(16)
        val e = GetetaFacade.exprToSMV(expr, v, 0, pc)
        println(e)
    }

    internal fun defaultTestTable(run: Int = 0): GeneralizedTestTable {
        val gtt = GeneralizedTestTable()
        gtt.add(iovar("a", "input", run))
        gtt.add(iovar("a", "input", 1))
        gtt.add(iovar("b", "input", run))
        gtt.add(iovar("c", "input", run))
        gtt.add(iovar("d", "input", run))
        gtt.ensureProgramRuns()
        return gtt
    }

    private fun iovar(name: String, io: String, run: Int) =
            ProgramVariable(name, INT, SMVTypes.signed(16),
                    if (io == "input") IoVariableType.INPUT else IoVariableType.OUTPUT,
                    run)

    val CASES = arrayOf(">2",
            "<52152343243214234", "!=6", "<>-16134", "-243261", "a",
            "a+b", "(a)+(((b+c)+d))/2", "convert(a,2)", "TRUE", "true", "false",
            "FALSE", "a[-5]", "[2+2, 6]", "[-61+2, -61]",
            "0|>a", "0\$a + |>a", "·"
    )
}
