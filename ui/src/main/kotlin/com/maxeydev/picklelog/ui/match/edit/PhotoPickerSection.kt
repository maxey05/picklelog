package com.maxeydev.picklelog.ui.match.edit

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.common.PermissionPrompt
import com.maxeydev.picklelog.ui.common.PermissionRationale
import com.maxeydev.picklelog.ui.common.PermissionSettingsRedirect
import com.maxeydev.picklelog.ui.common.rememberPermissionState
import java.io.File

private val TILE_WIDTH = 144.dp
private val TILE_IMAGE_HEIGHT = 108.dp

class PhotoPickerActions(
    val newCaptureUri: () -> String,
    val onPhotosPicked: (List<String>) -> Unit,
    val onPhotoCaptured: (String) -> Unit,
    val onPhotoMoved: (String, Int) -> Unit,
    val onPhotoRemoved: (String) -> Unit,
    val onPhotoErrorDismissed: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoPickerSection(
    photos: List<PhotoUiState>,
    hasPhotoError: Boolean,
    actions: PhotoPickerActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val latestActions by rememberUpdatedState(actions)
    val hasCamera = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                latestActions.onPhotosPicked(uris.map { it.toString() })
            }
        }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val captured = pendingCapture
            pendingCapture = null
            if (saved && captured != null) {
                latestActions.onPhotoCaptured(captured)
            }
        }
    val (cameraPermission, prompt) =
        rememberPermissionState(Manifest.permission.CAMERA) {
            val uri = latestActions.newCaptureUri()
            pendingCapture = uri
            camera.launch(Uri.parse(uri))
        }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.label_photos), style = MaterialTheme.typography.titleSmall)
        if (photos.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                photos.forEachIndexed { index, photo ->
                    PhotoTile(
                        photo = photo,
                        position = index + 1,
                        count = photos.size,
                        onMoved = { offset -> actions.onPhotoMoved(photo.key, offset) },
                        onRemoved = { actions.onPhotoRemoved(photo.key) },
                    )
                }
            }
        }
        if (hasPhotoError) {
            PhotoErrorMessage(onDismiss = actions.onPhotoErrorDismissed)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.testTag(MatchEditTestTags.ADD_PHOTOS),
            ) {
                Icon(painter = painterResource(R.drawable.ic_image), contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.photos_add))
            }
            if (hasCamera) {
                OutlinedButton(
                    onClick = cameraPermission::request,
                    modifier = Modifier.testTag(MatchEditTestTags.TAKE_PHOTO),
                ) {
                    Icon(painter = painterResource(R.drawable.ic_camera), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.photos_take))
                }
            }
        }
    }

    when (prompt) {
        PermissionPrompt.RATIONALE ->
            PermissionRationale(
                title = stringResource(R.string.camera_rationale_title),
                body = stringResource(R.string.camera_rationale_body),
                onContinue = cameraPermission::continueFromRationale,
                onDismiss = cameraPermission::dismiss,
            )
        PermissionPrompt.SETTINGS ->
            PermissionSettingsRedirect(
                title = stringResource(R.string.camera_settings_title),
                body = stringResource(R.string.camera_settings_body),
                onOpenSettings = cameraPermission::openAppSettings,
                onDismiss = cameraPermission::dismiss,
            )
        PermissionPrompt.NONE -> Unit
    }
}

@Composable
private fun PhotoTile(
    photo: PhotoUiState,
    position: Int,
    count: Int,
    onMoved: (Int) -> Unit,
    onRemoved: () -> Unit,
) {
    val description =
        when {
            photo.isImporting -> stringResource(R.string.photo_position_importing, position, count)
            position == 1 -> stringResource(R.string.photo_position_cover, position, count)
            else -> stringResource(R.string.photo_position, position, count)
        }
    Column(modifier = Modifier.width(TILE_WIDTH).testTag(MatchEditTestTags.photo(photo.key))) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier =
                Modifier
                    .size(width = TILE_WIDTH, height = TILE_IMAGE_HEIGHT)
                    .clearAndSetSemantics { contentDescription = description },
        ) {
            Box(contentAlignment = Alignment.BottomStart) {
                val path = photo.filePath
                if (path == null) {
                    Box(
                        modifier = Modifier.size(width = TILE_WIDTH, height = TILE_IMAGE_HEIGHT),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    AsyncImage(
                        model = File(path),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(width = TILE_WIDTH, height = TILE_IMAGE_HEIGHT)
                                .clip(MaterialTheme.shapes.small),
                    )
                }
                if (position == 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = stringResource(R.string.photo_cover),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { onMoved(-1) },
                enabled = position > 1,
                modifier = Modifier.testTag(MatchEditTestTags.photoMoveEarlier(photo.key)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.photo_move_earlier, position),
                )
            }
            IconButton(
                onClick = { onMoved(1) },
                enabled = position < count,
                modifier = Modifier.testTag(MatchEditTestTags.photoMoveLater(photo.key)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.photo_move_later, position),
                )
            }
            IconButton(
                onClick = onRemoved,
                modifier = Modifier.testTag(MatchEditTestTags.photoRemove(photo.key)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.photo_remove, position),
                )
            }
        }
    }
}

@Composable
private fun PhotoErrorMessage(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(MatchEditTestTags.PHOTO_ERROR),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_unreadable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
        )
        TextButton(onClick = onDismiss, modifier = Modifier.testTag(MatchEditTestTags.PHOTO_ERROR_DISMISS)) {
            Text(stringResource(R.string.photo_error_dismiss))
        }
    }
}
