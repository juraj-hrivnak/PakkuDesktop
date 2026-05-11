/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git

import org.eclipse.jgit.errors.UnsupportedCredentialItem
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.ChainingCredentialsProvider
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.NetRCCredentialsProvider
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * JGit does not read `credential.helper` or the OS keychain by itself. This chains:
 * 1. Optional [UsernamePasswordCredentialsProvider] from env (automation / PAT).
 * 2. [GitCredentialFillCredentialsProvider] — same as `git credential fill` for the repo.
 * 3. [NetRCCredentialsProvider] — `~/.netrc` if present.
 *
 * Env (optional): `PAKKU_GIT_USERNAME` + `PAKKU_GIT_PASSWORD`, or `GITHUB_TOKEN` (uses user name `git`).
 */
internal fun jgitTransportCredentialsProvider(repository: Repository): CredentialsProvider {
    val env = envCredentialsProvider()
    val fill = GitCredentialFillCredentialsProvider(repository)
    val netRc = NetRCCredentialsProvider()
    return if (env != null) {
        ChainingCredentialsProvider(env, fill, netRc)
    } else {
        ChainingCredentialsProvider(fill, netRc)
    }
}

private fun envCredentialsProvider(): CredentialsProvider? {
    val user = System.getenv("PAKKU_GIT_USERNAME")
    val pass = System.getenv("PAKKU_GIT_PASSWORD")
    if (!user.isNullOrBlank() && !pass.isNullOrBlank()) {
        return UsernamePasswordCredentialsProvider(user, pass)
    }
    val gh = System.getenv("GITHUB_TOKEN")
    if (!gh.isNullOrBlank()) {
        return UsernamePasswordCredentialsProvider("git", gh)
    }
    return null
}

/**
 * Runs `git -C <repo> credential fill` with a [URIish]-derived request so helpers (store, osxkeychain, manager, etc.)
 * match CLI behaviour.
 */
private class GitCredentialFillCredentialsProvider(
    private val repository: Repository,
) : CredentialsProvider() {

    override fun isInteractive(): Boolean = false

    override fun supports(vararg items: CredentialItem): Boolean {
        for (i in items) {
            when (i) {
                is CredentialItem.InformationalMessage -> continue
                is CredentialItem.Username -> continue
                is CredentialItem.Password -> continue
                is CredentialItem.StringType ->
                    if (i.promptText == "Password: ") continue else return false
                else -> return false
            }
        }
        return true
    }

    override fun get(uri: URIish, vararg items: CredentialItem): Boolean {
        if (!supports(*items)) {
            throw UnsupportedCredentialItem(uri, items.joinToString { it.javaClass.name })
        }
        val host = uri.host ?: return false
        val protocol = uri.scheme?.ifBlank { null } ?: "https"
        val path = uri.path?.trim('/')?.takeIf { it.isNotEmpty() }
        val workDir: File = repository.workTree ?: repository.directory ?: return false
        val input = buildString {
            append("protocol=").append(protocol).append('\n')
            append("host=").append(host).append('\n')
            if (path != null) append("path=").append(path).append('\n')
            append('\n')
        }
        val proc = ProcessBuilder("git", "-C", workDir.absolutePath, "credential", "fill")
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        proc.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { it.write(input) }
        val output = proc.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        if (!proc.waitFor(120, TimeUnit.SECONDS)) {
            proc.destroyForcibly()
            return false
        }
        if (proc.exitValue() != 0) return false
        val map = LinkedHashMap<String, String>()
        for (line in output.lineSequence()) {
            val eq = line.indexOf('=')
            if (eq <= 0) continue
            map[line.substring(0, eq)] = line.substring(eq + 1)
        }
        val username = map["username"] ?: return false
        val password = map["password"] ?: ""
        val passChars = password.toCharArray()
        try {
            for (i in items) {
                when (i) {
                    is CredentialItem.InformationalMessage -> continue
                    is CredentialItem.Username -> i.setValue(username)
                    is CredentialItem.Password -> i.setValue(passChars)
                    is CredentialItem.StringType ->
                        if (i.promptText == "Password: ") i.setValue(password)
                    else -> Unit
                }
            }
            return username.isNotEmpty()
        } finally {
            passChars.fill('\u0000')
        }
    }
}
