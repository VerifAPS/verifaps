package edu.kit.iti.formal.automation

/*-
 * #%L
 * iec-symbex
 * %%
 * Copyright (C) 2018 Alexander Weigl
 * %%
 * This program isType free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program isType distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a clone of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.antlr.v4.runtime.CharStreams
import org.junit.Test

import java.io.IOException

/**
 * @author Alexander Weigl
 * @version 1 (18.02.18)
 */
class ProgramWithActionsTest {
    @Test
    @Throws(IOException::class)
    fun test() {
        val tle = IEC61131Facade.file(CharStreams.fromStream(
                javaClass.getResourceAsStream("program_with_actions.st")
        ))
        val newTle = SymbExFacade.simplify(tle)
        println(IEC61131Facade.print(newTle))
    }
}