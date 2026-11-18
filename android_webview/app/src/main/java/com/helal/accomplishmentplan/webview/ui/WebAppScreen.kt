package com.helal.accomplishmentplan.webview.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.helal.accomplishmentplan.webview.R
import com.helal.accomplishmentplan.webview.config.AppConfig
import com.helal.accomplishmentplan.webview.webview.BlobDownloadHookScript
import com.helal.accomplishmentplan.webview.webview.DownloadBridge
import com.helal.accomplishmentplan.webview.webview.DownloadUtils
import com.helal.accomplishmentplan.webview.webview.FileUploadHelper
import com.helal.accomplishmentplan.webview.webview.UrlNavigationPolicy
import com.helal.accomplishmentplan.webview.webview.WebViewConfigurator
import kotlinx.coroutines.launch

private data class PendingFileUpload(
    val callback: ValueCallback<Array<Uri>>,
    val acceptTypes: Array<String>?,
    val allowMultiple: Boolean,
    val isCaptureEnabled: Boolean
)

@Composable
fun WebAppScreen(
    initialUrl: String = AppConfig.LIVE_APP_URL,
    onExitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var hasCompletedFirstLoad by remember { mutableStateOf(false) }
    var mainFrameError by remember { mutableStateOf<String?>(null) }
    var isOfflineError by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var popupProgress by remember { mutableIntStateOf(0) }

    var pendingUpload by remember { mutableStateOf<PendingFileUpload?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val openSingleDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val callback = pendingUpload?.callback
        pendingUpload = null
        callback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
    }

    val openMultiDocsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val callback = pendingUpload?.callback
        pendingUpload = null
        callback?.onReceiveValue(if (uris.isNotEmpty()) uris.toTypedArray() else null)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val callback = pendingUpload?.callback
        val captureUri = pendingCameraUri
        pendingUpload = null
        pendingCameraUri = null
        callback?.onReceiveValue(if (success && captureUri != null) arrayOf(captureUri) else null)
    }

    val launchCameraCapture = {
        try {
            val photoUri = FileUploadHelper.createCameraPhotoUri(context)
            pendingCameraUri = photoUri
            takePictureLauncher.launch(photoUri)
        } catch (_: Throwable) {
            pendingUpload?.callback?.onReceiveValue(null)
            pendingUpload = null
            pendingCameraUri = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            pendingUpload?.callback?.onReceiveValue(null)
            pendingUpload = null
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.camera_permission_denied)
                )
            }
        }
    }

    val downloadBridge = remember(context) {
        DownloadBridge(
            context = context,
            onSaved = { saved ->
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.download_saved, saved.fileName),
                        actionLabel = context.getString(R.string.action_open),
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        try {
                            context.startActivity(DownloadUtils.buildOpenFileIntent(saved))
                        } catch (_: ActivityNotFoundException) {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.no_app_to_open, saved.fileName)
                            )
                        }
                    }
                }
            },
            onError = { fileName ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.download_failed, fileName)
                    )
                }
            }
        )
    }

    val closeOAuthPopup: () -> Unit = {
        popupWebView?.apply {
            stopLoading()
            destroy()
        }
        popupWebView = null
        popupProgress = 0
    }

    BackHandler(enabled = true) {
        val activePopup = popupWebView
        when {
            activePopup != null -> {
                if (activePopup.canGoBack()) {
                    activePopup.goBack()
                } else {
                    closeOAuthPopup()
                }
            }
            webViewRef?.canGoBack() == true -> {
                webViewRef?.goBack()
            }
            else -> {
                showExitConfirm = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
            closeOAuthPopup()
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        WebViewConfigurator.configure(this, downloadBridge)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val uri = request?.url ?: return false
                                return when (UrlNavigationPolicy.evaluateNavigation(uri, isPopupWindow = false)) {
                                    UrlNavigationPolicy.Action.LOAD_IN_WEBVIEW -> false
                                    UrlNavigationPolicy.Action.OPEN_EXTERNAL -> {
                                        openExternally(ctx, uri)
                                        true
                                    }
                                    UrlNavigationPolicy.Action.BLOCK -> true
                                }
                            }

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                mainFrameError = null
                                isOfflineError = false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                hasCompletedFirstLoad = true
                                CookieManager.getInstance().flush()
                                view?.evaluateJavascript(BlobDownloadHookScript.SCRIPT, null)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    hasCompletedFirstLoad = true
                                    isOfflineError = !isNetworkConnected(ctx)
                                    mainFrameError = error?.description?.toString()
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                val status = errorResponse?.statusCode ?: return
                                if (request?.isForMainFrame == true && status >= 500) {
                                    hasCompletedFirstLoad = true
                                    isOfflineError = false
                                    mainFrameError = "Server returned HTTP $status"
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                if (filePathCallback == null) return false
                                pendingUpload?.callback?.onReceiveValue(null)
                                pendingUpload = PendingFileUpload(
                                    callback = filePathCallback,
                                    acceptTypes = fileChooserParams?.acceptTypes,
                                    allowMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE,
                                    isCaptureEnabled = fileChooserParams?.isCaptureEnabled == true
                                )
                                return true
                            }

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                closeOAuthPopup()
                                val child = WebView(ctx).apply {
                                    WebViewConfigurator.configure(this, downloadBridge = null)
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            childView: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val uri = request?.url ?: return false
                                            return when (UrlNavigationPolicy.evaluateNavigation(uri, isPopupWindow = true)) {
                                                UrlNavigationPolicy.Action.LOAD_IN_WEBVIEW -> false
                                                UrlNavigationPolicy.Action.OPEN_EXTERNAL -> {
                                                    openExternally(ctx, uri)
                                                    true
                                                }
                                                UrlNavigationPolicy.Action.BLOCK -> true
                                            }
                                        }

                                        override fun onPageFinished(childView: WebView?, url: String?) {
                                            super.onPageFinished(childView, url)
                                            CookieManager.getInstance().flush()
                                        }
                                    }
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(childView: WebView?, newProgress: Int) {
                                            popupProgress = newProgress
                                        }

                                        override fun onCloseWindow(window: WebView?) {
                                            closeOAuthPopup()
                                        }
                                    }
                                }
                                popupWebView = child
                                transport.webView = child
                                resultMsg.sendToTarget()
                                return true
                            }

                            override fun onCloseWindow(window: WebView?) {
                                closeOAuthPopup()
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                            if (url.startsWith("blob:", ignoreCase = true)) {
                                evaluateJavascript(BlobDownloadHookScript.SCRIPT, null)
                                val safeUrl = url.replace("'", "\\'")
                                evaluateJavascript("window.__androidFetchBlobDownload && window.__androidFetchBlobDownload('$safeUrl', '');", null)
                            } else {
                                downloadBridge.handleStandardDownload(
                                    url = url,
                                    userAgent = userAgent,
                                    contentDisposition = contentDisposition,
                                    mimetype = mimetype
                                )
                            }
                        }

                        webViewRef = this
                        loadUrl(initialUrl)
                    }
                }
            )

            if (loadProgress in 1..99 && mainFrameError == null) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .testTag("top_loading_indicator")
                )
            }

            if (!hasCompletedFirstLoad && mainFrameError == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .testTag("initial_loading_overlay"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_art),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.loading_message),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (mainFrameError != null) {
                NetworkErrorView(
                    isOffline = isOfflineError,
                    errorDetail = mainFrameError,
                    onRetry = {
                        mainFrameError = null
                        isOfflineError = false
                        val target = webViewRef
                        if (target != null && !target.url.isNullOrBlank()) {
                            target.reload()
                        } else {
                            target?.loadUrl(initialUrl)
                        }
                    },
                    modifier = Modifier.testTag("network_error_view")
                )
            }

            val currentPopup = popupWebView
            if (currentPopup != null) {
                OAuthPopupDialog(
                    popupWebView = currentPopup,
                    progress = popupProgress,
                    onClose = closeOAuthPopup
                )
            }

            val currentUpload = pendingUpload
            if (currentUpload != null) {
                UploadSourceDialog(
                    showCameraOption = FileUploadHelper.shouldOfferCamera(
                        currentUpload.acceptTypes,
                        currentUpload.isCaptureEnabled
                    ),
                    onChooseStorage = {
                        val mimeTypes = FileUploadHelper.parseAcceptMimeTypes(currentUpload.acceptTypes)
                        if (currentUpload.allowMultiple) {
                            openMultiDocsLauncher.launch(mimeTypes)
                        } else {
                            openSingleDocLauncher.launch(mimeTypes)
                        }
                    },
                    onTakePhoto = {
                        val hasCamPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCamPermission) {
                            launchCameraCapture()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onDismiss = {
                        currentUpload.callback.onReceiveValue(null)
                        pendingUpload = null
                    }
                )
            }

            if (showExitConfirm) {
                ExitConfirmationDialog(
                    onConfirmExit = {
                        showExitConfirm = false
                        onExitApp()
                    },
                    onDismiss = { showExitConfirm = false }
                )
            }
        }
    }
}

private fun openExternally(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Ignore if no handler exists on the device
    }
}

private fun isNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
