package com.orlinskas.videofacefinder.core

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.orlinskas.videofacefinder.exception.HandledException
import com.orlinskas.videofacefinder.extensions.singleObserve
import com.orlinskas.videofacefinder.ui.dialog.Dialogs
import com.orlinskas.videofacefinder.ui.dialog.ProgressDialogFragment

abstract class BaseActivity : AppCompatActivity() {

    private var progressDialog: ProgressDialogFragment? = null
    private val viewModelList = mutableSetOf<BaseViewModel>()

    inline fun <reified T : BaseViewModel> getViewModel(): T {
        return viewModelOf(T::class.java, defaultViewModelProviderFactory)
    }

    fun <T : BaseViewModel> viewModelOf(modelClass: Class<T>, viewmodelFactory: ViewModelProvider.Factory): T {
        val viewModel = ViewModelProvider(this, viewmodelFactory).get(modelClass)
        viewModel.exceptionLiveData.singleObserve(this) { handleException(it) }
        viewModel.attachLifecycle(this)
        viewModelList.removeAll { baseViewModel -> baseViewModel::class.java.name == viewModel::class.java.name }
        viewModelList.add(viewModel)
        return viewModel
    }

    open fun <T : ViewModel> obtainViewModel(owner: ViewModelStoreOwner,
                                             viewModelClass: Class<T>,
                                             viewmodelFactory: ViewModelProvider.Factory
    ) = ViewModelProvider(owner, viewmodelFactory).get(viewModelClass)

    /**
     * Shows fullscreen not cancelable dialog with spinner
     */
    fun showProgressDialog() {
        if (supportFragmentManager.findFragmentByTag(PROGRESS_LOAD_DIALOG_TAG) == null) {
            supportFragmentManager.beginTransaction().add(ProgressDialogFragment(), PROGRESS_LOAD_DIALOG_TAG).addToBackStack(PROGRESS_LOAD_DIALOG_TAG).commit()
        }
    }

    /**
     * Hides progress dialog
     */
    fun hideProgressDialog() {
        supportFragmentManager.findFragmentByTag(PROGRESS_LOAD_DIALOG_TAG)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    fun <T : ViewDataBinding> bindContentView(@LayoutRes layoutRes: Int): T =
        DataBindingUtil.setContentView(this, layoutRes)

    /**
     * Removes progress dialog if it was shown and shows default dialog with error message from [HandledException.getText]
     */
    protected open fun handleException(exception: HandledException) {
        hideProgressDialog()
        Dialogs.exceptionDialog(this, exception).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModelList.forEach { it.onSaveState(outState) }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModelList.forEach { it.onRestoreState(savedInstanceState) }
    }

    companion object {
        const val PROGRESS_LOAD_DIALOG_TAG = "progress-dialog"
    }
}
