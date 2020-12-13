package com.orlinskas.videofacefinder.core

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.orlinskas.videofacefinder.exception.HandledException
import com.orlinskas.videofacefinder.extensions.singleObserve
import com.orlinskas.videofacefinder.ui.dialog.Dialogs
import com.orlinskas.videofacefinder.ui.dialog.ProgressDialogFragment

abstract class BaseFragment : Fragment() {

    private var progressDialog: ProgressDialogFragment? = null
    protected val viewModelList = mutableSetOf<BaseViewModel>()
    protected var menu: Menu? = null

    protected open var optionsMenuRes: Int = 0
    protected open lateinit var fragmentDataBinding: ViewDataBinding

    abstract val layoutResId: Int

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
                                             viewModelClass: Class<T>
    ) = ViewModelProvider(owner, defaultViewModelProviderFactory).get(viewModelClass)

    protected inline fun <reified T : ViewDataBinding> fragmentBinding(): BindingLazy<T> = BindingLazy()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setHasOptionsMenu(layoutResId != 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentDataBinding = DataBindingUtil.inflate(layoutInflater, layoutResId, container, false)
        return fragmentDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModelList.forEach { it.onRestoreState(savedInstanceState) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModelList.forEach { it.onSaveState(outState) }
    }

    /**
     * Removes progress dialog if it was shown and shows default dialog with error message from [HandledException.getText]
     */
    open fun handleException(exception: HandledException) {
        hideProgressDialog()
        Dialogs.exceptionDialog(requireContext(), exception)
    }

    /**
     * Shows fullscreen not cancelable dialog with spinner
     */
    fun showProgressDialog() {
        if (childFragmentManager.findFragmentByTag(PROGRESS_LOAD_DIALOG_TAG) == null) {
            childFragmentManager.beginTransaction().add(
                ProgressDialogFragment(),
                PROGRESS_LOAD_DIALOG_TAG
            ).commit()
        }
    }

    /**
     * Hides progress dialog
     */
    fun hideProgressDialog() {
        childFragmentManager.findFragmentByTag(PROGRESS_LOAD_DIALOG_TAG)?.let {
            childFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected inner class BindingLazy<out T : ViewDataBinding> : Lazy<T> {

        override val value: T
            get() = fragmentDataBinding as T

        override fun isInitialized() = ::fragmentDataBinding.isInitialized
    }

    companion object {
        const val PROGRESS_LOAD_DIALOG_TAG = "progress-dialog-on-fragment"
    }
}
