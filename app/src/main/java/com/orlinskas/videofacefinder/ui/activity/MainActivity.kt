package com.orlinskas.videofacefinder.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toFile
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe
import com.arthenica.mobileffmpeg.MediaInformation
import com.example.videofacefinder.R
import com.example.videofacefinder.databinding.ActivityMainBinding
import com.orlinskas.videofacefinder.core.BaseActivity
import com.orlinskas.videofacefinder.data.enums.FileSystemState
import com.orlinskas.videofacefinder.data.enums.VideoMimeType
import com.orlinskas.videofacefinder.extensions.launchActivity
import com.orlinskas.videofacefinder.extensions.singleObserve
import com.orlinskas.videofacefinder.util.io
import com.orlinskas.videofacefinder.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getViewModel()
        binding = bindContentView(R.layout.activity_main)

        binding.btnStart.setOnClickListener {
            viewModel.onFileManagerRequest?.invoke()
        }

        viewModel.onFileManagerRequest = {
            requestPermission()
        }

        viewModel.onFileReceived = {
            io {
                val file = viewModel.state.file ?: error("File is null")
                val filePath = getVideoPath(file.uri) ?: error("Error convert to file from uri")
                val rc = FFmpeg.execute("-i $filePath")
                getMediaInfo(file.uri)
            }
        }

    }

    private fun requestPermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openFileManager()
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults.all { it ==  PackageManager.PERMISSION_GRANTED}) {
            openFileManager()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == GALLERY_REQUEST_CODE) {
            showProgressDialog()

            data?.data?.let { uri ->
                viewModel.processFile(uri).singleObserve(this) {
                    hideProgressDialog()

                    when (it) {
                        FileSystemState.OK -> {
                            viewModel.onFileReceived?.invoke()
                        }
                        FileSystemState.EXCEPTION -> {
                            Timber.e("Error")
                        }
                        else -> {
                            Timber.e("Not found")
                        }
                    }
                }
            }
        }
    }

    private fun openFileManager() {

        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            val intent = Intent("com.sec.android.app.myfiles.PICK_DATA")
            intent.putExtra("CONTENT_TYPE", "*/*")
            intent.addCategory(Intent.CATEGORY_DEFAULT)

            startActivityForResult(intent, GALLERY_REQUEST_CODE)
        } else {

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, VideoMimeType.values().map { it.value }.toTypedArray())
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)

            startActivityForResult(intent, GALLERY_REQUEST_CODE)
        }
    }

    fun logExecution(code: Int) {
        if (code == RETURN_CODE_SUCCESS) {
            Timber.d("Command execution completed successfully.");
        } else {
            Timber.d("Command execution failed with rc=$code.")
            Config.printLastCommandOutput(Log.DEBUG)
        }
    }

    fun getMediaInfo(uri: Uri?): MediaInformation? {
        if (uri == null) {
            Timber.e("Uri is null")
            return null
        }

        val info = FFprobe.getMediaInformation(getVideoPath(uri))
        Timber.d(info.allProperties.toString(2))
        return info
    }

    fun getVideoPath(uri: Uri?): String? {
        if (uri == null) {
            Timber.e("Uri is null")
            return null
        }

        var cursor = contentResolver.query(uri, null, null, null, null)

        if (cursor == null) {
            Timber.e("Cursor is null")
            return null
        }

        cursor.moveToFirst()
        var documentId: String = cursor.getString(0)
        documentId = documentId.substring(documentId.lastIndexOf(":") + 1)
        cursor.close()

        cursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Video.Media._ID + " = ? ", arrayOf(documentId), null)

        if (cursor == null) {
            Timber.e("Cursor is null")
            return null
        }

        cursor.moveToFirst()
        val path: String = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        cursor.close()

        return path
    }

    companion object {

        const val GALLERY_REQUEST_CODE = 101
        const val PERMISSION_REQUEST_CODE = 102

        fun start(context: Context) = context.launchActivity(MainActivity::class, null) {}
    }
}