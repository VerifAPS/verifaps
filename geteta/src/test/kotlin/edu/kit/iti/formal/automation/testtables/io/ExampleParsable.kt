package edu.kit.iti.formal.automation.testtables.io

import edu.kit.iti.formal.automation.testtables.GetetaFacade
import org.antlr.v4.runtime.CharStreams
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream

object ExampleParsable {
    @ParameterizedTest
    @MethodSource("findExamples")
    fun parsable(file : File): Unit {
        val p = GetetaFacade.createParser(CharStreams.fromFileName(file.absolutePath))
        p.errorReporter.throwException()
    }

    @ParameterizedTest
    @MethodSource("findExamples")
    fun loadable(file : File): Unit {
        GetetaFacade.parseTableDSL(file)
    }


    @JvmStatic
    fun findExamples() : List<File> {
        val walker = File("examples").walkTopDown()
        return walker.filter {
            it.isFile && (it.name.endsWith(".tt.txt") || it.name.endsWith(".gtt") || it.name.endsWith(".rtt"))
        }.toList()
    }
}