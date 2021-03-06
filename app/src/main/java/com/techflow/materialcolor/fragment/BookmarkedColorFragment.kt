package com.techflow.materialcolor.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.techflow.materialcolor.MaterialColor

import com.techflow.materialcolor.R
import com.techflow.materialcolor.adapter.AdapterColor
import com.techflow.materialcolor.db.AppDatabase
import com.techflow.materialcolor.databinding.FragmentBookmarkedColorBinding
import com.techflow.materialcolor.helpers.AnalyticsHelper
import com.techflow.materialcolor.helpers.AppExecutorHelper
import com.techflow.materialcolor.helpers.displayToast
import com.techflow.materialcolor.helpers.isTablet
import com.techflow.materialcolor.model.Color
import com.techflow.materialcolor.utils.AnimUtils
import com.techflow.materialcolor.utils.ColorUtils
import com.techflow.materialcolor.utils.Tools
import com.techflow.materialcolor.viewmodel.BookmarkedColorViewModel
import com.techflow.materialcolor.viewmodel.BookmarkedColorViewModelFactory
/**
 * Modified by Dilip on 03/12/19
 */
class BookmarkedColorFragment : Fragment(), AdapterColor.OnItemClickListener {

    companion object {
        private var bookmarkedColorFragment: BookmarkedColorFragment? = null
        fun getInstance(): BookmarkedColorFragment {
            if (bookmarkedColorFragment == null)
                bookmarkedColorFragment = BookmarkedColorFragment()

            return bookmarkedColorFragment as BookmarkedColorFragment
        }
    }

    private var mDb: AppDatabase? = null

    private lateinit var binding: FragmentBookmarkedColorBinding
    private lateinit var adapter: AdapterColor
    private var toast: Toast? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_bookmarked_color, container, false)
        mDb = AppDatabase.getInstance(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager =
            if (context?.isTablet()!!) GridLayoutManager(context, 2) else LinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)
        adapter = AdapterColor(requireContext(), requireActivity(), this, mDb)
        binding.recyclerView.adapter = adapter

        initViewModel()
    }

    /**
     * @func init view model and their subscribers
     */
    private fun initViewModel() {
        val factory = BookmarkedColorViewModelFactory(mDb!!)
        val viewModel = ViewModelProviders
            .of(this, factory)
            .get(BookmarkedColorViewModel::class.java)

        viewModel.getBookmarkedColors()?.observe(viewLifecycleOwner, Observer {
            if (it == null || it.isEmpty()) {
                Tools.visibleViews(binding.layoutNoItems)
                Tools.inVisibleViews(binding.recyclerView, type = Tools.InvisibilityType.GONE)
            } else {
                Tools.visibleViews(binding.recyclerView)
                Tools.inVisibleViews(binding.layoutNoItems, type = Tools.InvisibilityType.GONE)
            }

            adapter.setColors(it)
        })
    }

    /**
     * @inherited on item click callback
     * @param view view
     * @param color color object
     * @param position recycler view item position
     */
    override fun onItemClick(view: View, color: Color, position: Int) {
        AnalyticsHelper.getInstance()?.logEvent(MaterialColor.FIREBASE_EVENT_COPY_HEX_CODE, null)
        Tools.copyToClipboard(requireContext(), color.colorCode, "HEX code ${color.colorCode}")
    }

    /**
     * @inherited on item long click callback
     * @param view view
     * @param color color object
     * @param position recycler view item position
     */
    override fun onItemLongClick(view: View, color: Color, position: Int) {
        ColorUtils.executeColorCodePopupMenu(requireContext(), color.colorCode, view)
    }

    /**
     * @inherited on bookmark button click callback
     * @param view view
     * @param color color object
     * @param position recycler view item position
     */
    override fun onBookmarkButtonClick(view: View, color: Color, position: Int) {
        AnimUtils.bounceAnim(view)

        AppExecutorHelper.getInstance()?.diskIO()?.execute {
            mDb?.colorDao()?.removeColor(adapter.getColor(position))

            AppExecutorHelper.getInstance()?.mainThread()?.execute {
                toast?.cancel()
                toast =  context?.displayToast("${color.colorCode} removed from bookmark")
            }
        }
    }

}
