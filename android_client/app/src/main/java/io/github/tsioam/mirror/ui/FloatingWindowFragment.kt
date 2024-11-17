package io.github.tsioam.mirror.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.tsioam.mirror.R
import io.github.tsioam.mirror.core.MirrorContent
import kotlinx.coroutines.launch

class FloatingWindowFragment(
    private val floatView: FloatView,
    private val mirror: MirrorContent
) : Fragment(R.layout.fragment_floating_window) {

    private lateinit var expandIcon: View
    private var extending = false

    private val hideCallback: Runnable = Runnable {
        if (!extending) {
            view?.alpha = 0.3f
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        expandIcon = view.findViewById(R.id.icon_expand)
        expandIcon.setOnClickListener {
            showBottomSheet()
        }
        view.postDelayed(hideCallback, 2000)
        floatView.moveListener = {
            view.removeCallbacks(hideCallback)
            view.alpha = 1.0f
            view.postDelayed(hideCallback, 2000)
        }

    }

    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.dialog_menu_bottom_sheet)
        bottomSheetDialog.findViewById<View>(R.id.close_bottom)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            mirror.attachActivity.finish()
        }

        bottomSheetDialog.findViewById<View>(R.id.device_wakeup)?.setOnClickListener {
            bottomSheetDialog.lifecycleScope.launch {
                mirror.control?.sendScreenOn()
                bottomSheetDialog.dismiss()
            }
        }

        val switchAppBtn = bottomSheetDialog.findViewById<View>(R.id.switch_app)
        if (mirror.isAppVirtualMirror()) {
            switchAppBtn?.visibility = View.GONE
        } else {
            switchAppBtn?.setOnClickListener {
                bottomSheetDialog.lifecycleScope.launch {
                    mirror.control?.sendAppSwitchKey()
                    bottomSheetDialog.dismiss()
                }
            }
        }

        bottomSheetDialog.setOnDismissListener {
            view?.removeCallbacks(hideCallback)
            view?.alpha = 1.0f
            view?.postDelayed(hideCallback, 2000)
        }
        bottomSheetDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        view?.removeCallbacks(hideCallback)
    }
}