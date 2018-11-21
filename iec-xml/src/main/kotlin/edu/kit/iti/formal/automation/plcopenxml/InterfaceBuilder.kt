package edu.kit.iti.formal.automation.plcopenxml

import edu.kit.iti.formal.util.CodeWriter
import org.jdom2.Element

/**
 * Construct from an &lt;interface&gt; and [edu.kit.iti.formal.automation.scope.Scope]
 *
 * @author Alexander Weigl
 * @version 1 (20.02.18)
 */
object InterfaceBuilder : XMLTranslatorFind {
    override fun find(e: Element): Element? = when {
        e.name == "interface" -> e
        e.getChild("interface") != null -> e.getChild("interface")
        else -> null
    }

    override fun translate(e: Element, writer: CodeWriter) {
        val interfaze = find(e)
        translateVars(interfaze?.getChild("inputVars"), writer, "_INPUT")
        translateVars(interfaze?.getChild("outputVars"), writer, "_OUTPUT")
        translateVars(interfaze?.getChild("localVars"), writer)
        //TODO Test for IN_OUT and others
    }

    private fun translateVars(vars: Element?, writer: CodeWriter, suffix: String = "") {
        if (vars == null) return
        writer.nl().nl().printf("VAR$suffix").block {
            for (e in vars.getChildren("variable")) {
                val name = e.getAttributeValue("name")
                val datatype = VariableDeclXML.getDatatype(e.getChild("type"))
                val initValue = VariableDeclXML.getInitialValue(e)
                nl()
                printf("$name : $datatype")
                if (initValue != null)
                    printf(":= $initValue")
                printf(";")
            }
        }
        writer.nl().printf("END_VAR")
    }

}


object VariableDeclXML {
    fun getDatatype(e: Element): String {
        val derived = e.getChild("derived")
        return if (derived != null) {
            derived.getAttributeValue("name")
        } else {
            e.children[0].name
        }
    }

    fun getInitialValue(variable: Element?): String? {
        if (variable == null)
            return null
        return variable.getChild("initialValue")
                ?.getChild("simpleValue")
                ?.getAttributeValue("value")
    }
}