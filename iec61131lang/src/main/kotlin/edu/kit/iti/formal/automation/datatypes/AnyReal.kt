package edu.kit.iti.formal.automation.datatypes

/*-
 * #%L
 * iec61131lang
 * %%
 * Copyright (C) 2016 Alexander Weigl
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

/**
 *
 * Abstract AnyReal class.
 *
 * @author weigl
 * @version $Id: $Id
 */
abstract class AnyReal : AnyNum() {
    override fun repr(obj: Any): String {
        val d = obj as Double
        return javaClass.getName().toUpperCase() + "#" + d
    }

    override fun <T> accept(visitor: DataTypeVisitorNN<T>) = visitor.visit(this)


    object REAL : AnyReal() {
        init {
            name = "REAL"
        }

        override fun <T> accept(visitor: DataTypeVisitorNN<T>) = visitor.visit(this)
    }

    object LREAL : AnyReal() {
        init {
            name = "LREAL"
        }

        override fun <T> accept(visitor: DataTypeVisitorNN<T>) = visitor.visit(this)
    }
}