package teksturepako.pakkuDesktop.actions

import teksturepako.pakku.api.data.workingPath
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

fun String.runCommand(): String?
{
    try
    {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(File(workingPath))
            .start()

        println(this)

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    }
    catch(e: IOException)
    {
        return null
    }
}