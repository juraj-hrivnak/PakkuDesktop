package teksturepako.pakkuDesktop.app.actions

import kotlinx.coroutines.*
import teksturepako.pakkuDesktop.app.ui.viewmodel.ModpackViewModel
import java.util.concurrent.Executors

private val actionRunnerThreadPool = Executors.newSingleThreadExecutor { thread ->
    Thread(thread, "action-runner-background-thread").apply {
        // Set to daemon to prevent hanging
        isDaemon = true
    }
}.asCoroutineDispatcher()

fun runAction(actionName: String, action: CoroutineScope.() -> Job)
{
    val coroutineScope = CoroutineScope(actionRunnerThreadPool + SupervisorJob() + Dispatchers.IO)

    try
    {
        val job = action(coroutineScope)

        ModpackViewModel.runActionWithJob(actionName, job)

        job.invokeOnCompletion {
            coroutineScope.launch {
                ModpackViewModel.terminateAction()
            }
        }
    }
    catch (_: Exception)
    {
        coroutineScope.launch {
            ModpackViewModel.terminateAction()
        }
    }
}
