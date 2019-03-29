package edu.kit.iti.formal.automation.ide

import edu.kit.iti.formal.automation.IEC61131Facade
import edu.kit.iti.formal.automation.datatypes.values.Value
import edu.kit.iti.formal.automation.run.ExecutionFacade
import edu.kit.iti.formal.automation.run.State
import edu.kit.iti.formal.automation.st.ast.Literal
import edu.kit.iti.formal.automation.st.ast.PouElements
import edu.kit.iti.formal.automation.st.ast.PouExecutable
import edu.kit.iti.formal.automation.st.ast.VariableDeclaration
import edu.kit.iti.formal.automation.testtables.GetetaFacade
import edu.kit.iti.formal.automation.testtables.print.HTMLTablePrinter
import edu.kit.iti.formal.automation.visitors.Utils
import edu.kit.iti.formal.util.CodeWriter
import net.miginfocom.layout.CC
import net.miginfocom.swing.MigLayout
import org.antlr.v4.runtime.CharStreams
import java.awt.BorderLayout
import java.awt.Component
import java.awt.LayoutManager
import java.io.File
import java.io.StringWriter
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

abstract class ToolPane(layout: LayoutManager = BorderLayout()) : TabbedPanel(layout)

/**
 *
 * @author Alexander Weigl
 * @version 1 (11.03.19)
 */
class RunnerWindow(val lookup: Lookup,
                   val stEditor: STEditor) : ToolPane(BorderLayout()) {
    override fun close() {}

    val toolBar = JToolBar()

    var elements: PouElements = PouElements()

    var inputVars: List<VariableDeclaration> = listOf()
    var outputVars: List<VariableDeclaration> = listOf()

    val tableModel = RunTableModel()
    val table = JTable(tableModel)

    val actionLoad = createAction("Load", "") { load() }
    val actionSaveAs = createAction("Save", "") { saveAs() }
    val actionRecalculate = createAction("Rebuild", "") { updateModel(); }

    init {
        title = "Run: ${stEditor.title}"

        toolBar.add(actionLoad)
        toolBar.add(actionSaveAs)
        toolBar.add(actionRecalculate)
        add(toolBar, BorderLayout.NORTH)
        add(JScrollPane(table), BorderLayout.CENTER)

        table.setDefaultRenderer(Value::class.java, Renderer())
        table.setDefaultEditor(Value::class.java, Editor())
        table.addPropertyChangeListener("tableCellEditor") {
            if (!table.isEditing) calculateOutput()
        }

        updateModel()
        fillModel()
        calculateOutput()
    }

    private fun updateModel() {
        try {
            elements = IEC61131Facade.file(CharStreams.fromString(stEditor.code))
            IEC61131Facade.resolveDataTypes(elements)
            val program = Utils.findProgram(elements)
            if (program != null) {
                inputVars = program.scope.variables
                        .filter { it.isInput }
                        .sortedBy { it.name }

                outputVars = program.scope.variables
                        .filter { it.isOutput || it.isLocal }
                        .sortedBy { it.name }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tableModel.fireTableStructureChanged()
        tableModel.ensureCells()
    }

    fun load(file: File? = null) {
        if (file == null) {
            val fc = lookup.get<GetFileChooser>().fileChooser
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                load(fc.selectedFile)
            }
        } else {
            tableModel.fromText(file.readText())
        }
    }

    fun saveAs(file: File? = null) {
        if (file == null) {
            val fc = lookup.get<GetFileChooser>().fileChooser
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                saveAs(fc.selectedFile)
            }
        } else {
            file.writeText(tableModel.toText())
        }
    }

    private fun fillModel() {
        inputVars.forEachIndexed { col, input ->
            val defaultValue = input.initValue ?: ExecutionFacade.getDefaultValue(input.dataType!!)
            (0 until tableModel.rowCount).forEach { row ->
                tableModel.setValueAt(defaultValue, row, col)
            }
        }
    }

    fun calculateOutput() {
        val exec = ExecutionFacade.createExecutionContext(elements)
        (0 until tableModel.rowCount).forEach { row ->
            val inputs = tableModel.getInputs(row)
            exec.executeCycle(input = inputs)
        }
        exec.states.forEachIndexed { rowIndex, state ->
            outputVars.forEachIndexed { columnIndex, outvar ->
                val value = state[outvar.name]
                tableModel.setValueAt(value, rowIndex, columnIndex + inputVars.size)
                println("${outvar.name}: $value, $rowIndex, $columnIndex")
            }
        }
    }

    class Editor(textfield: JTextField = JTextField())
        : DefaultCellEditor(textfield) {
        override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
            val value = value as? Value<*, *>
            val text = when (value) {
                null -> ""
                else -> {
                    val (dt, v) = value; dt.repr(v)
                }
            }
            return super.getTableCellEditorComponent(table, text, isSelected, row, column)
        }

        override fun getCellEditorValue(): Any? {
            val input = super.getCellEditorValue().toString()
            val expr = IEC61131Facade.expr(input) as? Literal
            return expr?.asValue()
        }


    }

    class Renderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component? {
            val value = value as? Value<*, *>
            val text = when (value) {
                null -> ""
                else -> {
                    val (dt, v) = value; dt.repr(v)
                }
            }
            val lbl: JLabel = super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column) as JLabel
            return lbl
        }
    }

    inner class RunTableModel : AbstractTableModel() {
        override fun getRowCount() = 50
        override fun getColumnCount(): Int = inputVars.size + outputVars.size
        val values = arrayListOf<ArrayList<Value<*, *>?>>()

        init {
            ensureCells()
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            if (rowIndex > rowCount) return Any()
            if (columnIndex > columnCount) return Any()
            return values[rowIndex][columnIndex]
        }

        fun ensureCells() {
            values.ensureCapacity(rowCount)
            while (values.size < rowCount) {
                values.add(ArrayList(columnCount))
            }
            values.forEach { row -> while (row.size < columnCount) row.add(null) }
        }

        override fun getColumnName(column: Int): String {
            return when {
                column > columnCount -> ""
                column < inputVars.size -> inputVars[column].name
                else -> outputVars[column - inputVars.size].name
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex < inputVars.size
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            try {
                values[rowIndex][columnIndex] = aValue as Value<*, *>?
                fireTableCellUpdated(rowIndex, columnIndex)
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            } catch (e: ClassCastException) {
                e.printStackTrace()
            }
        }

        override fun findColumn(columnName: String?): Int {
            return super.findColumn(columnName)
        }

        override fun getColumnClass(columnIndex: Int): Class<*> = Value::class.java

        fun getInputs(rowIndex: Int): State {
            val row = values[rowIndex].zip(inputVars)
            val state = State()
            row.forEach { (value, variable) ->
                if (value != null)
                    state[variable.name] = value
            }
            return state
        }

        fun toText(): String {
            val sb = StringBuffer()
            values.forEach { row ->
                row.joinTo(sb, "|") { cell ->
                    cell?.dataType?.repr(cell.value) ?: ""
                }
                sb.append('\n')
            }
            return sb.toString()
        }

        fun fromText(readText: String) {
            readText.splitToSequence('\n')
                    .forEachIndexed { row, line ->
                        line.splitToSequence('|')
                                .forEachIndexed { col, cell ->
                                    val lit = (IEC61131Facade.expr(cell) as? Literal)?.asValue()
                                    setValueAt(lit, row, col)
                                }
                    }
        }
    }
}

class GetetaWindow(lookup: Lookup) : ToolPane() {
    override fun close() {}

    val cboStEditor = JComboBox<STEditor>()
    val cboPou = JComboBox<PouExecutable>()
    val cboTable = JComboBox<TTEditor>()
    val lookup = Lookup(lookup)

    init {
        title = "Geteta"
        icon = IconFontSwing.buildIcon(FontAwesomeSolid.TABLE, 16f)
        layout = MigLayout()

        val lblProgram = JLabel("Program: ")
        lblProgram.labelFor = cboStEditor
        val lblTable = JLabel("Table: ")
        lblTable.labelFor = cboTable

        add(lblProgram)
        add(cboStEditor)
        add(cboPou, CC().wrap())

        add(lblTable)
        add(cboTable)
        lookup.addChangeListener(STEditor::class.java, this::updateData)
        lookup.addChangeListener(TTEditor::class.java, this::updateData)

        cboTable.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                val a = (value as TabbedPanel?)?.title
                return super.getListCellRendererComponent(list, a, index, isSelected, cellHasFocus)
            }
        }

        cboStEditor.renderer = cboTable.renderer as ListCellRenderer<in STEditor>?

        cboStEditor.addActionListener {
            updatePouElements()
        }

        updateData()
    }

    private fun updatePouElements() {
        cboPou.removeAllItems()


    }

    private fun updateData() {
        val stEditor = lookup.getAll<STEditor>()
        val ttEditor = lookup.getAll<TTEditor>()

        cboStEditor.removeAllItems()
        cboTable.removeAllItems()

        stEditor.forEach { cboStEditor.addItem(it) }
        ttEditor.forEach { cboTable.addItem(it) }

    }
}

class GetetaPreview(lookup: Lookup) : ToolPane() {
    override fun close() {}

    val lookup = Lookup(lookup)
    var swingbox = JEditorPane()
    val viewRender = JScrollPane(swingbox)

    init {
        title = "Test Table Preview"
        _dockKey.isCloseEnabled = false
        EVENT_BUS.listenTo(this::render)
    }

    fun render(event: EventGetetaUpdate) {
        try {
            val gtt = GetetaFacade.parseTableDSL(event.text)
            val sw = StringWriter()
            val pp = HTMLTablePrinter(gtt, CodeWriter(sw))
            pp.printPreamble()
            pp.print()
            pp.printPostamble()
            print(sw.toString())
            swingbox.text = sw.toString()
            /*swingbox.setDocumentFromString(sw.toString(),
                    File(".").toURI().toString(),
                    XhtmlNamespaceHandler())
                    */
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

class EventGetetaUpdate(val text: String)