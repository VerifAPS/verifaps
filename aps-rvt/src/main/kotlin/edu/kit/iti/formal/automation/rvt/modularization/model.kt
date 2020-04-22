package edu.kit.iti.formal.automation.rvt.modularization

import edu.kit.iti.formal.automation.IEC61131Facade
import edu.kit.iti.formal.automation.SymbExFacade
import edu.kit.iti.formal.automation.rvt.ModuleBuilder
import edu.kit.iti.formal.automation.rvt.SymbolicExecutioner
import edu.kit.iti.formal.automation.rvt.SymbolicState
import edu.kit.iti.formal.automation.scope.Scope
import edu.kit.iti.formal.automation.st.ast.*
import edu.kit.iti.formal.automation.st.util.AstVisitorWithScope
import edu.kit.iti.formal.automation.st.util.UsageFinder
import edu.kit.iti.formal.smv.SMVFacade
import edu.kit.iti.formal.smv.SMVPrinter
import edu.kit.iti.formal.smv.ast.*
import edu.kit.iti.formal.util.CodeWriter
import edu.kit.iti.formal.util.info
import java.io.File
import java.util.*
import kotlin.collections.HashMap

/** Get the name of the callee */
val Invoked?.name: String?
    get() = when (this) {
        is Invoked.Program -> program.name
        is Invoked.FunctionBlock -> fb.name
        is Invoked.Function -> function.name
        is Invoked.Method -> method.name
        is Invoked.Action -> action.name
        null -> null
    }

typealias CallSiteMapping = List<Pair<BlockStatement, BlockStatement>>

object ModFacade {
    fun createModularProgram(entry: PouExecutable,
                             outputFolder: File,
                             prefix: String = ""): ModularProgram {

        val complete = SymbExFacade.simplify(entry)
        val simplifiedFile = File(outputFolder, "${prefix}_${entry.name}_simplified.st")
        info("Write simplified version of '$prefix' to $simplifiedFile")
        simplifiedFile.bufferedWriter().use { IEC61131Facade.printTo(it, complete) }

        info("Maintain frames in $prefix")
        val callSites = updateBlockStatements(complete)

        val (symbex, frameContext) = evaluateProgram(complete)
        val smvFile = File(outputFolder, "${prefix}_${entry.name}_simplified.smv")
        info("Write complete SMV file of '$prefix' to $smvFile")
        smvFile.bufferedWriter().use { symbex.accept(SMVPrinter(CodeWriter(it))) }

        return ModularProgram(entry, complete, symbex, callSites, frameContext)
    }

    private fun evaluateProgram(complete: PouExecutable): Pair<SMVModule, HashMap<BlockStatement, SymbolicState>> {
        class SymbolicExecutionerFC(topLevel: Scope) : SymbolicExecutioner(topLevel) {
            val catch = HashMap<BlockStatement, SymbolicState>()
            override fun visit(blockStatement: BlockStatement): SMVExpr? {
                catch[blockStatement] = SymbolicState(peek())
                return super.visit(blockStatement)
            }
        }

        val se = SymbolicExecutionerFC(complete.scope.topLevel)
        complete.accept(se)
        val moduleBuilder = ModuleBuilder(complete, se.peek())
        moduleBuilder.run()
        return moduleBuilder.module to se.catch
    }

    fun findCallSitePair(old: String, new: String,
                         oldProgram: ModularProgram, newProgram: ModularProgram)
            : Pair<BlockStatement, BlockStatement> {
        val x = oldProgram.findCallSite(old)
                ?: error("Could not find $old")
        val y = newProgram.findCallSite(new)
                ?: error("Could not find $new")
        return x to y
    }

    fun createReveContextsBySpecification(seq: List<String>, oldProgram: ModularProgram, newProgram: ModularProgram): ReveContextManager {
        val cm = ReveContextManager()
        seq.map { createReveContextsBySpecification(it, oldProgram, newProgram, cm) }
        return cm
    }

    fun createReveContextsBySpecification(it: String, oldProgram: ModularProgram, newProgram: ModularProgram,
                                          ctxManager: ReveContextManager) {
        // it == "A.f=A.f#cond#relations"
        val sharp = it.count { it == '#' }
        val (sitemap, cond, relations) = when (sharp) {
            2 -> it.split("#")
            //1 -> it.split("#")
            else -> error("Contract format violated: $it")
        }
        val (left, right) = if ("=" in sitemap) sitemap.split("=") else listOf(sitemap, sitemap)
        val (bA, bB) = findCallSitePair(left, right, oldProgram, newProgram)

        val ctx = TopReveContext()
        ctx.condition = if (cond.isBlank()) SLiteral.TRUE else SMVFacade.expr(cond)
        ctx.relation = if (relations.isBlank()) arrayListOf() else
            relations.split(",").map(::parseRelation).toMutableList()
        ctxManager.add(bA, bB, ctx)
    }

    fun parseRelation(it: String): RelatedVariables {
        val b = SMVFacade.expr(it) as SBinaryExpression
        val left = b.left as SVariable
        val right = b.right as SVariable
        return RelatedVariables(left, b.operator, right)
    }


    fun createFrame(cNew: BlockStatement, scope: Scope): Frame {
        return Frame(cNew, scope)
    }

    fun inferBlockAssignable(scope: Scope, block: BlockStatement) {
        val a = UsageFinder.investigate(block)
        val known = a.knownVariables
        val written = a.writtenReferences.map { it.identifier }
        val read = a.readReference.map { it.identifier }
        for (it in known) {
            val w = it.identifier in written
            val r = it.identifier in read
            when {
                w && r -> block.state.add(it)
                r -> block.input.add(it)
                w -> block.output.add(it)
            }
        }

    }

    fun updateBlockStatements(p: PouExecutable) = MaintainBlocks(p).run()

    fun slice(block: BlockStatement, program: ModularProgram): PouExecutable {
        val origin = program.complete.scope
        val scope = Scope()

        val inputs = origin.getAll(block.input, VariableDeclaration.INPUT)
        val state = origin.getAll(block.state, VariableDeclaration.LOCAL)
        val outputs = origin.getAll(block.output, VariableDeclaration.OUTPUT)

        // TODO check assignable
        scope.variables.addAll(inputs + state + outputs)
        val fbd = FunctionBlockDeclaration(
                block.originalInvoked?.name ?: block.name, scope, block.statements)

        return fbd
    }
}

class ModularProgram(val entry: PouExecutable,
                     val complete: PouExecutable,
                     val symbex: SMVModule,
                     val callSites: List<BlockStatement>,
                     val frameContext: HashMap<BlockStatement, SymbolicState>) {

    fun findCallSite(aa: String): BlockStatement? {
        return callSites.find { it.repr() == aa }
    }

    val frame: Frame
        get() {
            val blockStatement = BlockStatement(complete.name, complete.stBody!!)
            ModFacade.inferBlockAssignable(complete.scope, blockStatement)
            return ModFacade.createFrame(blockStatement, complete.scope)
        }
}

data class CallSiteSpec(val contextPath: List<String>, val number: Int = 0) {
    var statement: BlockStatement? = null

    val inferedContext = HashMap<String, SMVExpr>()
    var specifiedContext: SMVExpr? = null

    fun repr(): String = contextPath.joinToString(".") + ".$number"
    fun correspond(other: CallSiteSpec) =
            contextPath.subList(1, contextPath.lastIndex) == other.contextPath.subList(1, other.contextPath.lastIndex)
                    && other.number == number

    fun isPrefix(ids: List<String>) = ids.size <= contextPath.size && ids.zip(contextPath).all { (a, b) -> a == b }
}


class MaintainBlocks(val entry: PouExecutable) {
    val blocks: MutableList<BlockStatement> = arrayListOf()

    fun run(): MutableList<BlockStatement> {
        val visitor = CallSiteFinderSearcher()
        visitor.vars += entry.name
        entry.accept(visitor)
        return blocks
    }

    private inner class CallSiteFinderSearcher : AstVisitorWithScope<Unit>() {
        val vars: Stack<String> = Stack()
        var invocationCounter = HashMap<String, Int>()

        val fqName
            get() = vars.joinToString(".")

        private fun endCall() {
            vars.pop()
        }

        private fun startCall(c: BlockStatement): CallSiteSpec {
            vars.push(c.name)
            val n = fqName
            val currentNo = invocationCounter.computeIfAbsent(n) { 0 }
            val cs = CallSiteSpec(vars.toList(), currentNo)
            c.fqName = n
            c.number = currentNo
            invocationCounter[n] = 1 + currentNo

            if (c.state.isEmpty() && c.input.isEmpty() && c.output.isEmpty()) {
                info("Trigger inference of assignable for callsite ${c.repr()}")
                ModFacade.inferBlockAssignable(entry.scope, c)
            }
            return cs
        }

        override fun defaultVisit(obj: Any) {}

        override fun visit(blockStatement: BlockStatement) {
            val ctx = startCall(blockStatement)
            blocks.add(blockStatement)
            visit(blockStatement.statements)
            endCall()
        }
    }
}

private fun Scope.getAll(vars: MutableList<SymbolicReference>, newType: Int = 0) =
        vars.map { reference -> this.getVariable(reference).clone().also { it.type = newType } }
