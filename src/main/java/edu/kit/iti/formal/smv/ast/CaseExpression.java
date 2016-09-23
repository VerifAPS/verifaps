// --------------------------------------------------------
// Code generated by Papyrus Java
// --------------------------------------------------------

package edu.kit.iti.formal.smv.ast;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import edu.kit.iti.formal.smv.Visitor;
import edu.kit.iti.formal.smv.ast.Expression;

/************************************************************/
/**
 * 
 */
public class CaseExpression extends Expression {
	public List<Case> cases = new LinkedList<Case>();

	/**
	 * 
	 */
	public static class Case {
		/**
		 * 
		 */
		public Expression condition;
		/**
		 * 
		 */
		public Expression then;
	}
	


	public <T> T visit(Visitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public DataType getDataType() {
		List<DataType> list = cases.stream()
				.map((Case a) -> {return a.then.getDataType();})
				.collect(Collectors.toList());
		
		return DataType.infer(list);
	};
};
