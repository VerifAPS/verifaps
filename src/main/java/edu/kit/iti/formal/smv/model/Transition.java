package edu.kit.iti.formal.smv.model;

/*-
 * #%L
 * smv-model
 * %%
 * Copyright (C) 2016 Alexander Weigl
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

import edu.kit.iti.formal.smv.ast.SMVExpr;
import edu.kit.iti.formal.smv.ast.SLiteral;

public class Transition {
	public State outgoing;
	public State incoming;

	public SMVExpr guard = SLiteral.TRUE;

	public Transition from(State s) {
		outgoing = s;
		return this;
	}

	public Transition to(State n) {
		incoming = n;
		return this;
	}

	public Transition on(SMVExpr g) {
		guard = g;
		return this;
	}

}
