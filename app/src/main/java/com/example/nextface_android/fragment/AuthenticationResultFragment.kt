package com.example.nextface_android.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.nextface_android.*
import com.example.nextface_android.databinding.FragmentAuthenticationResultBinding
import com.example.nextface_android.viewmodel.NextFaceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class AuthenticationResultFragment : Fragment(), CoroutineScope {
    private lateinit var mJob: Job
    private lateinit var binding: FragmentAuthenticationResultBinding
    private val sharedViewModel: NextFaceViewModel by activityViewModels()
    private lateinit var usbManager2 : UsbManager

    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAuthenticationResultBinding.inflate(inflater)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_authentication_result, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setContentView(binding.root)
        mJob = Job()
        sharedViewModel.getStaff()?.let {
            binding.staffAvatar.setImageBitmap(it.avatar)
            binding.staffName.text = getString(R.string.staff) + it.name
        }
        sharedViewModel.getResult()?.let {
            binding.status.text = it.text
            binding.status.setTextColor(it.color!!)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.homeBtn.setOnClickListener { goToHome() }
    }

    override fun onStop() {
        super.onStop()
        binding.homeBtn.removeCallbacks { goToHome() }
    }

    private fun goToHome() {
        findNavController().navigate(R.id.action_authenticationResultFragment_to_homeActivity)
//        usbManager2 = requireActivity().getSystemService(Context.USB_SERVICE) as UsbManager
//        GateController.connect(Constants.REQUEST_GATE_CLOSE, usbManager2)
        activity?.finish()
    }
}