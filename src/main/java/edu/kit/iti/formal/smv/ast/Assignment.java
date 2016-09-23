// --------------------------------------------------------
// Code generated by Papyrus Java
// --------------------------------------------------------

package edu.kit.iti.formal.smv.ast;

import edu.kit.iti.formal.smv.Visitor;
import edu.kit.iti.formal.smv.ast.Expression;
import edu.kit.iti.formal.smv.ast.Statement;
import edu.kit.iti.formal.smv.ast.Variable;

/************************************************************/
/**
 * 
 */
public class Assignment extends Statement {
	/**
	 * 
	 */
	public Variable target;
	/**
	 * 
	 */
	public Expression expression;
	
	

	<T> T visit(Visitor<T> visitor) {
		return visitor.visit(this);
	}

};
