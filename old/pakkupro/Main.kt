package pakkupro

import ModpackView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import com.github.michaelbull.result.Err
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.*
import org.jetbrains.jewel.intui.window.decoratedWindow
import org.jetbrains.jewel.intui.window.styling.dark
import org.jetbrains.jewel.intui.window.styling.light
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.window.DecoratedWindow
import org.jetbrains.jewel.window.styling.DecoratedWindowStyle
import org.jetbrains.jewel.window.styling.TitleBarStyle
import teksturepako.pakkupro.ui.application.theme.IntUiThemes
import pakkupro.actions.export
import pakkupro.view.TitleBarView
import pakkupro.viewmodel.MainViewModel
import pakkupro.viewmodel.MainViewModel.profileData
import pakkupro.viewmodel.MainViewModel.theme
import pakkupro.viewmodel.WindowType
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakkupro.ui.application.appName
import java.awt.*
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main()
{
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        Dialog(Frame(), e.message ?: "Error").apply {
            layout = FlowLayout()
            val label = Label(e.message)
            add(label)
            val button = Button("OK").apply {
                addActionListener { dispose() }
            }
            add(button)
            setSize(300, 300)
            isVisible = true
        }
    }

    application {
        val textStyle = JewelTheme.createDefaultTextStyle(fontFamily = FontFamily.Default)

        val themeDefinition = if (theme.isDark())
        {
            JewelTheme.darkThemeDefinition(defaultTextStyle = textStyle)
        }
        else
        {
            JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle)
        }

        IntUiTheme(
            themeDefinition, ComponentStyling.default().decoratedWindow(
                titleBarStyle = when (theme)
                {
                    IntUiThemes.Light                -> TitleBarStyle.light()
                    IntUiThemes.Dark                 -> TitleBarStyle.dark()
                    IntUiThemes.System               -> if (theme.isDark())
                    {
                        TitleBarStyle.dark()
                    } else
                    {
                        TitleBarStyle.light()
                    }
                },
            ), swingCompatMode = false
        ) {
            val windowType = remember { mutableStateOf<WindowType>(WindowType.APP) }
            val coroutineScope = rememberCoroutineScope()

            when (windowType.value)
            {
                WindowType.APP     ->
                {
                    DecoratedWindow(
                        state = WindowType.APP.getState(),
                        onCloseRequest = {
                            exitApplication()
                            exitProcess(0)
                        },
                        title = appName(),
                        icon = painterResource("icons/pakku.svg"),
                        style = DecoratedWindowStyle.light()
                    ) {
                        window.minimumSize = Dimension(600, 100)

                        val isExporting = remember { mutableStateOf(false) }

                        val pickerLauncher: PickerResultLauncher = rememberDirectoryPickerLauncher(
                            title = "Open modpack directory"
                        ) { directory ->
                            if (directory?.path == null) return@rememberDirectoryPickerLauncher

                            MainViewModel.updatePakkuApi()
                            windowType.value = WindowType.LOADING

                            profileData.setCurrentProfile(Path(directory.path!!))
                            profileData.write()
                        }

                        val toaster = rememberToasterState()

                        TitleBarView(
                            enabled = true,
                            filePicker = pickerLauncher,
                            windowType = windowType,
                            exporting = isExporting
                        )

                        Row(Modifier.fillMaxSize().offset(y = 33.dp)) {
                            Toaster(
                                state = toaster,
                                alignment = Alignment.BottomEnd,
                                darkTheme = theme.isDark(),
                                expanded = true,
                                richColors = true,
                                showCloseButton = true,
                                maxVisibleToasts = 6
                            )
                        }

                        Box(
                            Modifier.fillMaxSize().background(JewelTheme.globalColors.panelBackground),
                        ) {
                            ModpackView(coroutineScope, onSideSelect = {
                                Err(ActionError.ProjNotFound())
                            })

                            if (isExporting.value) export(isExporting, toaster)
                        }
                    }
                }

                WindowType.LOADING ->
                {
                    coroutineScope.launch {
                        windowType.value = WindowType.APP
                    }
                }
            }
        }
    }
}
