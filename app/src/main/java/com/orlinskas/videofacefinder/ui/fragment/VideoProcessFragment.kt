package com.orlinskas.videofacefinder.ui.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.ObservableBoolean
import com.example.videofacefinder.R
import com.example.videofacefinder.databinding.FragmentVideoProcessBinding
import com.orlinskas.videofacefinder.core.BaseFragment
import com.orlinskas.videofacefinder.data.enums.FileSystemState
import com.orlinskas.videofacefinder.data.enums.Settings
import com.orlinskas.videofacefinder.data.enums.VideoMimeType
import com.orlinskas.videofacefinder.extensions.convertToStringRepresentation
import com.orlinskas.videofacefinder.extensions.singleObserve
import com.orlinskas.videofacefinder.extensions.toast
import com.orlinskas.videofacefinder.service.VideoProcessService
import com.orlinskas.videofacefinder.systems.FileSystem
import com.orlinskas.videofacefinder.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


@AndroidEntryPoint
class VideoProcessFragment : BaseFragment() {

    override val layoutResId: Int
        get() = R.layout.fragment_video_process

    private val binding: FragmentVideoProcessBinding
        get() = fragmentDataBinding as FragmentVideoProcessBinding

    private val viewModel: MainViewModel by lazy {
        obtainViewModel(requireActivity(), MainViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.isProgress = ObservableBoolean(false)

        if (viewModel.state.file != null) {
            setFileThumbnail()
        }

        binding.imageButton.setOnClickListener {
            viewModel.onFileManagerRequest?.invoke()
        }

        binding.imageVideo.setOnClickListener {
            viewModel.onFileManagerRequest?.invoke()
        }

        binding.btnStart.setOnClickListener {
            if (viewModel.state.file == null) {
                viewModel.onFileManagerRequest?.invoke()
            } else {
                viewModel.runVideoProcessing()
            }
        }

        viewModel.onFileManagerRequest = {
            requestPermission()
        }

        viewModel.onFileReceived = {
            setFileThumbnail()
        }

        with(binding.fpsSlider) {
            value = Settings.Fps.getSliderValue(viewModel.state.fps)
            setLabelFormatter {
                viewModel.state.fps.float.toString()
            }
            addOnChangeListener { _, value, _ ->
                viewModel.state.fps = Settings.Fps.fromValue(value)
            }
        }

        with(binding.compressSlider) {
            value = Settings.Compress.getSliderValue(viewModel.state.compress)
            setLabelFormatter {
                viewModel.state.compress.readable
            }
            addOnChangeListener { _, value, _ ->
                viewModel.state.compress = Settings.Compress.fromValue(value)
            }
        }

        with(binding.scaleSlider) {
            value = Settings.Scale.getSliderValue(viewModel.state.scale)
            setLabelFormatter {
                viewModel.state.scale.int.toString()
            }
            addOnChangeListener { _, value, _ ->
                viewModel.state.scale = Settings.Scale.fromValue(value)
            }
        }

        viewModel.videoProcessLiveData.singleObserve(viewLifecycleOwner) { state ->
            if (state != null) {
                when(state) {
                    VideoProcessService.State.LOADING -> {
                        showProgress()
                    }
                    VideoProcessService.State.SUCCESS -> {
                        hideProgress()
                    }
                    VideoProcessService.State.FAIL -> {
                        hideProgress()
                        requireContext().applicationContext.toast("Video processing error")
                    }
                }
            }
        }
    }

    private fun setFileThumbnail() {
        val file = viewModel.state.file
        val thumbnail = FileSystem.createVideoThumbnail(file)
        binding.imageVideo.setImageBitmap(thumbnail)
        binding.imageButton.visibility = View.INVISIBLE
        binding.chipType.text = file?.type
        binding.chipType.visibility = View.VISIBLE
        binding.chipSize.text = file?.size?.convertToStringRepresentation()
        binding.chipSize.visibility = View.VISIBLE
        binding.videoFileName.text = file?.name
        binding.videoFileName.visibility = View.VISIBLE
    }

    private fun requestPermission() {
        if (checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
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
                viewModel.fileLiveData(uri).singleObserve(this) {
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

    private fun showProgress() {
        viewModel.state.isProgress = true
        binding.isProgress?.set(true)
    }

    private fun hideProgress() {
        viewModel.state.isProgress = false
        binding.isProgress?.set(false)
    }

    companion object {
        const val GALLERY_REQUEST_CODE = 101
        const val PERMISSION_REQUEST_CODE = 102
    }
}