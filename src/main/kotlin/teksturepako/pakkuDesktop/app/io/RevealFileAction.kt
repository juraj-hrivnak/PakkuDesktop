/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.io

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

object RevealFileAction
{
    private val isWindows = System.getProperty("os.name").lowercase().contains("win")
    private val isMac = System.getProperty("os.name").lowercase().contains("mac")

    /**
     * Opens a system file manager with the given file's parent directory loaded and the file highlighted in it
     * (note that some platforms may not support the file highlighting).
     */
    fun openFile(file: Path)
    {
        val parent = canonicalize(file).parent
        if (parent != null)
        {
            doOpen(parent, file)
        }
        else
        {
            doOpen(file, null)
        }
    }

    /**
     * Opens a system file manager with the given directory loaded in it.
     */
    fun openDirectory(directory: Path)
    {
        doOpen(directory, null)
    }

    private fun doOpen(dir: Path, toSelect: Path?)
    {
        val normalizedDir = canonicalize(dir).normalize().toString()
        val normalizedToSelect = toSelect?.let { canonicalize(it).normalize().toString() }

        when
        {
            isWindows ->
            {
                openViaExplorerCall(normalizedDir, normalizedToSelect)
            }

            isMac     ->
            {
                if (normalizedToSelect != null)
                {
                    spawn("open", "-R", normalizedToSelect)
                }
                else
                {
                    spawn("open", normalizedDir)
                }
            }

            // Linux and others
            else      ->
            {
                if (hasXdgOpen())
                {
                    spawn("xdg-open", normalizedDir)
                }
                else
                {
                    // Try common Linux file managers
                    val fileManagers = listOf("nautilus", "dolphin", "nemo", "thunar", "pcmanfm", "dde-file-manager")
                    val foundManager = fileManagers.firstOrNull { Path("/usr/bin/$it").exists() }

                    // Special handling for specific file managers
                    when (foundManager)
                    {
                        "dolphin"          ->
                        {
                            if (toSelect != null)
                            {
                                ProcessBuilder(foundManager, "--select", toSelect.absolutePathString())
                            }
                            else
                            {
                                ProcessBuilder(foundManager, dir.absolutePathString())
                            }
                        }

                        "dde-file-manager" ->
                        {
                            if (toSelect != null)
                            {
                                ProcessBuilder(foundManager, "--show-item", toSelect.absolutePathString())
                            }
                            else
                            {
                                ProcessBuilder(foundManager, dir.absolutePathString())
                            }
                        }

                        else               -> ProcessBuilder(foundManager, dir.absolutePathString())
                    }
                }
            }
        }
    }

    private fun canonicalize(path: Path): Path
    {
        return try
        {
            path.toRealPath()
        }
        catch (e: IOException)
        {
            println("Could not convert $path to canonical path")
            path.toAbsolutePath()
        }
    }

    private fun openViaExplorerCall(dir: String, toSelect: String?)
    {
        val command = if (toSelect != null)
        {
            "explorer /select,\"$toSelect\""
        }
        else
        {
            "explorer /root,\"$dir\""
        }
        spawn(command)
    }

    private fun spawn(vararg command: String)
    {
        println(command.contentToString())

        try
        {
            val process = if (isWindows)
            {
                Runtime.getRuntime().exec(command[0])
            }
            else
            {
                ProcessBuilder(*command).redirectError(ProcessBuilder.Redirect.PIPE).start()
            }

            // Wait a bit to check for immediate failure
            if (!process.waitFor(10, TimeUnit.SECONDS))
            {
                process.destroy()
            }

            // Check exit code
            val exitCode = process.exitValue()
            if (exitCode != 0)
            {
                println("Command ${command.contentToString()} failed with exit code $exitCode")
            }
        }
        catch (e: Exception)
        {
            println(e)
        }
    }

    private fun hasXdgOpen(): Boolean
    {
        return Files.exists(Path.of("/usr/bin/xdg-open"))
    }
}
