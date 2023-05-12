package com.example.nextface_android.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.nextface_android.R
import com.example.nextface_android.databinding.FragmentRegisterResultBinding
import com.example.nextface_android.databinding.FragmentRegisterVoiceBinding
import com.example.nextface_android.viewmodel.NextFaceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class RegisterResultFragment : Fragment(), CoroutineScope {
    private lateinit var mJob: Job
    private lateinit var binding: FragmentRegisterResultBinding
    private val sharedViewModel: NextFaceViewModel by activityViewModels()

    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterResultBinding.inflate(inflater)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register_result, container, false)
    }

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
        }
        setButtonVisible("All")
    }

    override fun onStart() {
        super.onStart()
        binding.regVoiceAgainBtn.setOnClickListener { registerVoiceAgain() }
        binding.homeBtn.setOnClickListener { goToHome() }
    }

    override fun onStop() {
        super.onStop()
        binding.regVoiceAgainBtn.removeCallbacks { registerVoiceAgain() }
        binding.homeBtn.removeCallbacks { goToHome() }
    }

    private fun registerVoiceAgain() {
        findNavController().popBackStack(R.id.registerVoiceFragment, false)
    }

    private fun goToHome() {
        findNavController().navigate(R.id.action_registerResultFragment_to_homeActivity)
        activity?.finish()
    }

    private fun setButtonVisible(name: String) {
        when(name) {
            "All" -> {
                binding.regVoiceAgainBtn.visibility = View.VISIBLE
                binding.homeBtn.visibility = View.VISIBLE
                return
            }
            "RegisterVoiceAgain" -> {
                binding.regVoiceAgainBtn.visibility = View.VISIBLE
                binding.homeBtn.visibility = View.GONE
                return
            }
            "Home" -> {
                binding.regVoiceAgainBtn.visibility = View.GONE
                binding.homeBtn.visibility = View.VISIBLE
                return
            }
        }
    }
}