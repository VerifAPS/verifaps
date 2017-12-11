/*-
 * #%L
 * aps-rvt
 * %%
 * Copyright (C) 2017 Alexander Weigl
 * %%
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
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package edu.kit.iti.formal.automation.rvt

import org.apache.commons.io.FileUtils
import org.junit.Test

import org.junit.Assert.*

/**
 * @author Alexander Weigl
 * @version 1 (14.09.17)
 */
class McKtTest {
    @Test
    fun testParseOutput() {
        val resource = javaClass.getResource("/cex.xml")
        val xml = resource.readText()
        val out =parseOutput(xml)
        assertFalse(out.hasErrors)
        assertEquals(0, out.errors.size)
        assertNotNull(out.counterExample)

        assertEquals(false, out.isVerified)
        println(out.counterExample)
    }

}
