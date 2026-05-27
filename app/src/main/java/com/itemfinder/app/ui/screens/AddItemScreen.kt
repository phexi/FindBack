package com.itemfinder.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.itemfinder.app.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    onSave: (String, String, String, List<String>, () -> Unit, (String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var itemPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var itemPhotoFile by remember { mutableStateOf<File?>(null) }
    var locationPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var locationPhotoFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var pendingLocationPhotoFile by remember { mutableStateOf<File?>(null) }
    var saving by remember { mutableStateOf(false) }
    var fullScreenImage by remember { mutableStateOf<Any?>(null) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    fun createTempImageUri(): Pair<Uri, File> {
        val tempDir = File(context.cacheDir, "temp_photos")
        if (!tempDir.exists()) tempDir.mkdirs()
        val timestamp = System.currentTimeMillis()
        val random = (0..9999).random()
        val file = File(tempDir, "IMG_" + timestamp + "_" + random + ".jpg")
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        return uri to file
    }

    // Camera launcher for item photo
    val itemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            itemPhotoUri = itemPhotoFile?.let { Uri.fromFile(it) }
        }
    }

    // Camera launcher for location photo
    val locationCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingLocationPhotoFile != null) {
            val newFile = pendingLocationPhotoFile!!
            val newUri = Uri.fromFile(newFile)
            locationPhotoUris = locationPhotoUris + newUri
            locationPhotoFiles = locationPhotoFiles + newFile
            pendingLocationPhotoFile = null
        }
    }

    // Permission launcher for photo taking
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (uri, file) = createTempImageUri()
            if (itemPhotoUri == null) {
                itemPhotoFile = file
                itemCameraLauncher.launch(uri)
            } else {
                pendingLocationPhotoFile = file
                locationCameraLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, context.getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for barcode scanning
    val barcodePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showBarcodeScanner = true
        } else {
            Toast.makeText(context, context.getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    fun takeItemPhoto() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val (uri, file) = createTempImageUri()
            itemPhotoFile = file
            itemCameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun takeLocationPhoto() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val (uri, file) = createTempImageUri()
            pendingLocationPhotoFile = file
            locationCameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun startBarcodeScan() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            showBarcodeScanner = true
        } else {
            barcodePermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun removeLocationPhoto(index: Int) {
        locationPhotoUris = locationPhotoUris.toMutableList().apply { removeAt(index) }
        locationPhotoFiles = locationPhotoFiles.toMutableList().apply { removeAt(index) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_item_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Item name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.item_name_label)) },
                placeholder = { Text(stringResource(R.string.item_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { startBarcodeScan() }) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.scan_barcode)
                        )
                    }
                }
            )

            // Location description
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(stringResource(R.string.location_label)) },
                placeholder = { Text(stringResource(R.string.location_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Item photo
            Text(stringResource(R.string.section_item_photo), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 2.dp,
                        color = if (itemPhotoUri != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        if (itemPhotoUri != null) {
                            fullScreenImage = itemPhotoUri
                        } else {
                            takeItemPhoto()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (itemPhotoUri != null) {
                    AsyncImage(
                        model = itemPhotoUri,
                        contentDescription = stringResource(R.string.item_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = {
                            itemPhotoUri = null
                            itemPhotoFile = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.tap_to_take_photo), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Location photos (multiple)
            Text(stringResource(R.string.section_location_photo), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.location_photo_hint_multi),
                    style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                locationPhotoUris.forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(140.dp, 180.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { fullScreenImage = uri },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = stringResource(R.string.location_photo) + " " + (index + 1),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "" + (index + 1),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { removeLocationPhoto(index) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .offset(x = (-4).dp, y = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = stringResource(R.string.delete),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Add button
                Box(
                    modifier = Modifier
                        .size(140.dp, 180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { takeLocationPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (locationPhotoUris.isEmpty()) stringResource(R.string.tap_to_take_photo)
                                   else stringResource(R.string.add_more_photo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.error_name_required), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val itemFile = itemPhotoFile
                    if (itemPhotoUri == null || itemFile == null) {
                        Toast.makeText(context, context.getString(R.string.error_item_photo_required), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (locationPhotoFiles.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.error_location_photo_required), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    saving = true
                    onSave(
                        name.trim(),
                        location.trim(),
                        itemFile.absolutePath,
                        locationPhotoFiles.map { it.absolutePath },
                        { saving = false },
                        { msg ->
                            saving = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !saving,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save_item), style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Full screen image dialog
    fullScreenImage?.let { model ->
        FullScreenImageDialog(
            imageModel = model,
            onDismiss = { fullScreenImage = null }
        )
    }

    // Barcode scanner dialog
    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { result ->
                name = result
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}