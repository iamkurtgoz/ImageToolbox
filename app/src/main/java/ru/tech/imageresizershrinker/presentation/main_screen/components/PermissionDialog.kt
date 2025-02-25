package ru.tech.imageresizershrinker.presentation.main_screen.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import com.t8rin.dynamic.theme.observeAsState
import ru.tech.imageresizershrinker.R
import ru.tech.imageresizershrinker.presentation.root.utils.helper.ContextUtils.findActivity
import ru.tech.imageresizershrinker.presentation.root.utils.helper.ContextUtils.needToShowStoragePermissionRequest
import ru.tech.imageresizershrinker.presentation.root.utils.helper.ContextUtils.requestStoragePermission
import ru.tech.imageresizershrinker.presentation.root.utils.modifier.alertDialogBorder
import ru.tech.imageresizershrinker.presentation.root.widget.controls.EnhancedButton
import ru.tech.imageresizershrinker.presentation.root.widget.utils.LocalSettingsState

@Composable
fun PermissionDialog() {
    val context = LocalContext.current.findActivity()
    val settingsState = LocalSettingsState.current

    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(
        showDialog,
        context,
        settingsState,
        LocalLifecycleOwner.current.lifecycle.observeAsState().value
    ) {
        showDialog = context?.needToShowStoragePermissionRequest() == true
        while (showDialog) {
            showDialog = context?.needToShowStoragePermissionRequest() == true
            kotlinx.coroutines.delay(100)
        }
    }

    if (showDialog) {
        AlertDialog(
            modifier = Modifier.alertDialogBorder(),
            onDismissRequest = { },
            icon = {
                Icon(Icons.Rounded.Storage, null)
            },
            title = { Text(stringResource(R.string.permission)) },
            text = {
                Text(stringResource(R.string.permission_sub))
            },
            confirmButton = {
                EnhancedButton(
                    onClick = {
                        context!!.requestStoragePermission()
                    }
                ) {
                    Text(stringResource(id = R.string.grant))
                }
            }
        )
    }
}