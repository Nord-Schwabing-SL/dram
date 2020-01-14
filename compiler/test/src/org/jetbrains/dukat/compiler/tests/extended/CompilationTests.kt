package org.jetbrains.dukat.compiler.tests.extended

import org.jetbrains.dukat.compiler.tests.CliTranslator
import org.jetbrains.dukat.compiler.tests.CompileMessageCollector
import org.jetbrains.dukat.compiler.tests.createStandardCliTranslator
import org.jetbrains.dukat.compiler.tests.httpService.CliHttpClient
import org.jetbrains.dukat.compiler.tests.httpService.CliHttpService
import org.jetbrains.dukat.compiler.tests.toFileUriScheme
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.config.Services
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.io.PrintStream
import kotlin.test.assertEquals

private var CLI_PROCESS: Process? = null
private val PORT = "8090"

class CliTestsStarted : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext?) {
        CLI_PROCESS = CliHttpService().startService(PORT)
        CliHttpClient(PORT).waitForServer()
        println("cli http process creation: ${CLI_PROCESS?.isAlive}")
    }
}

class CliTestsEnded : AfterAllCallback {
    override fun afterAll(context: ExtensionContext?) {
        CLI_PROCESS?.destroy()
        println("shutting down cli http process")
    }
}

private class TestsEnded : AfterAllCallback {
    override fun afterAll(context: ExtensionContext?) {
        CompilationTests.report("build/reports/compilation_${context?.displayName}.txt")
    }
}

private data class ReportData(var errorCount: Int, var translationTime: Long, var compilationTime: Long)
private fun MutableMap<String, ReportData>.getReportFor(reportName: String): ReportData {
    return getOrPut(reportName) { ReportData(0, 0, 0) }
}

@ExtendWith(CliTestsStarted::class, CliTestsEnded::class, TestsEnded::class)
abstract class CompilationTests {

    protected fun getTranslator(): CliTranslator = createStandardCliTranslator()

    companion object {
        val COMPILATION_ERROR_ASSERTION = "COMPILATION ERROR"
        val FILE_NOT_FIND_ASSERTION = "FILE NOT FOUND"
        val START_TIMESTAMP = System.currentTimeMillis()

        private val reportDataMap: MutableMap<String, ReportData> = mutableMapOf()

        fun report(fileName: String?) {
            val printStream = if (fileName == null) { System.out } else { PrintStream(fileName) }
            val formatString = "%-24s\t%6s\t%7s\t%5d"
            printStream.println("COMPILATION REPORT")
            printStream.println(java.lang.String.format("%-24s\t%-6s\t%-7s\t%-5s", "name", "trans.", "comp.", "error"))
            reportDataMap.toList().sortedByDescending { it.second.errorCount }.forEach { (key, reportData) ->
                val errorCount = reportData.errorCount
                printStream.println(java.lang.String.format(formatString, key, "${reportData.translationTime}ms", "${reportData.compilationTime}ms", errorCount))
            }
            printStream.println("")
            printStream.println("ERRORS: ${reportDataMap.values.map { it.errorCount }.sum()}")
            val translationTimes     = reportDataMap.values.map { it.translationTime }
            printStream.println("AVG TRANSLATION TIME: ${translationTimes.average()}ms")
            val compilationTimes = reportDataMap.values.map { it.compilationTime }
            printStream.println("AVG COMPILATION TIME: ${compilationTimes.average()}ms")
        }
    }

    abstract fun runTests(
            descriptor: String,
            sourcePath: String
    )

    protected fun compile(descriptor: String, sources: List<String>, targetPath: String): ExitCode {

        val options =
                K2JSCompilerArguments().apply {
                    outputFile = targetPath
                    metaInfo = false
                    sourceMap = false
                    noStdlib = true
                    moduleKind = "commonjs"
                    libraries = listOf(
                            "./build/kotlinHome/kotlin-stdlib-js.jar"
                    ).joinToString(File.pathSeparator)
                }

        options.freeArgs = sources

        val messageCollector = CompileMessageCollector { _, _, _ ->
            reportDataMap.getReportFor(descriptor).errorCount += 1
        }

        return K2JSCompiler().exec(
                messageCollector,
                Services.EMPTY,
                options
        )
    }

    protected fun assertContentCompiles(
            descriptor: String, sourcePath: String
    ) {
        println(sourcePath.toFileUriScheme())
        val targetPath = "./build/tests/compiled/$START_TIMESTAMP/$descriptor"
        val targetDir = File(targetPath)
        println(targetDir.normalize().absolutePath.toFileUriScheme())

        targetDir.deleteRecursively()

        val translationStarted = System.currentTimeMillis()
        getTranslator().translate(sourcePath, targetPath)
        reportDataMap.getReportFor(descriptor).translationTime = System.currentTimeMillis() - translationStarted

        val outSource = "${targetPath}/$START_TIMESTAMP/${descriptor}.js"

        val sources = targetDir.walk().map { it.normalize().absolutePath }.toList()

        assert(sources.isNotEmpty()) { "$FILE_NOT_FIND_ASSERTION: $targetPath" }

        val compilationErrorMessage = "$COMPILATION_ERROR_ASSERTION:\n" + sources.joinToString("\n") { source -> source.toFileUriScheme() }

        val compilationStarted = System.currentTimeMillis()
        val compilationResult = compile(
                descriptor,
                sources,
                outSource
        )
        reportDataMap.getReportFor(descriptor).compilationTime = System.currentTimeMillis() - compilationStarted

        assertEquals(
                ExitCode.OK,
                compilationResult,
                compilationErrorMessage
        )
    }

}