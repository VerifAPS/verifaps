package edu.kit.iti.formal.automation.run

import edu.kit.iti.formal.automation.datatypes.Any
import edu.kit.iti.formal.automation.datatypes.values.Values
import edu.kit.iti.formal.automation.operators.Operator
import edu.kit.iti.formal.automation.st.ast.Literal

class BinaryEvaluator(operator: Operator, leftType: Any, rightType: Any, eval: (ExpressionValue, ExpressionValue) -> ExpressionValue)

object OperationEvaluator {

     fun add(leftValue: ExpressionValue, rightValue: ExpressionValue): ExpressionValue {
        if (leftValue is Values.VAnyInt && rightValue is Values.VAnyInt) {
            return add(leftValue, rightValue)
        }

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

     fun add(leftValue: Values.VAnyInt, rightValue: Values.VAnyInt) : Values.VAnyInt {
        return Values.VAnyInt(leftValue.dataType, leftValue.value.add(rightValue.value) )
        TODO("choose correct datatype and modulo the addition")
    }


     fun not(expressionValue: ExpressionValue) : ExpressionValue {
        if (expressionValue is Values.VBool) {
            return Values.VBool(expressionValue.dataType, expressionValue.value.not())
        } else {
            throw TypeMissmatchException("must be bool")
        }
    }




     fun negate(expressionValue: ExpressionValue) : ExpressionValue {
        return when(expressionValue) {
            is Values.VAnyInt -> Values.VAnyInt(expressionValue.dataType.asSigned(), expressionValue.value.negate())
            is Values.VAnyReal -> Values.VAnyReal(expressionValue.dataType, expressionValue.value.negate())
            else -> throw TypeMissmatchException("must be a number")
        }
    }
}