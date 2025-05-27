/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import kotlinx.coroutines.*
import teksturepako.pakkuDesktop.app.ui.viewmodel.ModpackViewModel

val dispatcher = Dispatchers.IO.limitedParallelism(1, "action-runner")

fun actionRunner(actionName: String, action: CoroutineScope.() -> Job)
{
    val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

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
