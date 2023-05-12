package com.example.nextface_android.fragment

import android.annotation.SuppressLint

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.nextface_android.CameraManager
import com.example.nextface_android.Constants
import com.example.nextface_android.R
import com.example.nextface_android.databinding.FragmentRegisterFaceBinding
import com.example.nextface_android.model.StaffInfo
import com.example.nextface_android.viewmodel.NextFaceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class RegisterFaceFragment : Fragment(), CoroutineScope {
    private lateinit var mJob: Job
    private lateinit var cameraManager: CameraManager
    private lateinit var binding: FragmentRegisterFaceBinding
    private val sharedViewModel: NextFaceViewModel by activityViewModels()
    private val handler = Handler(Looper.myLooper()!!)

    private val TAG = "RegisterFace"

    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        (activity as AppCompatActivity?)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding = FragmentRegisterFaceBinding.inflate(inflater)
        return inflater.inflate(R.layout.fragment_register_face, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.setContentView(binding.root)
        mJob = Job()

        createCameraManager()

        val mPic = context?.let { AppCompatResources.getDrawable(it, R.drawable.ic_app_logo) }
        binding.overlayContainer.setLoggoPic(mPic!!)
    }

    override fun onStart() {
        super.onStart()
        startCamera()
        initData()
        addListener()
    }

    override fun onStop() {
        super.onStop()
        cameraManager.stopCamera()
        removeListener()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mJob.cancel()
    }

    private fun initData() {
        binding.topText.text = ""
        binding.bottomText.text = getString(R.string.default_description)
        binding.overlayContainer.setImageFlip(true)

        setButtonVisible("None")
        sharedViewModel.setStaff(null)
    }

    private  fun startCamera() {
        if (allPermissionsGranted()){
            cameraManager.startCamera(::cameraCallback)
        } else {
            requestPermissionLauncher.launch(Constants.CAMERA_REQUIRED_PERMISSIONS)
        }
    }

    private fun setButtonVisible(key: String) {
        when(key) {
            "All" -> {
                binding.homeBtn.visibility = View.VISIBLE
                binding.space30.visibility = View.VISIBLE
                binding.voiceRegBtn.visibility = View.VISIBLE
                return
            }
            "Home" -> {
                binding.homeBtn.visibility = View.VISIBLE
                binding.space30.visibility = View.GONE
                binding.voiceRegBtn.visibility = View.GONE
                return
            }
            "Voice" -> {
                binding.homeBtn.visibility = View.GONE
                binding.space30.visibility = View.GONE
                binding.voiceRegBtn.visibility = View.VISIBLE
                return
            }
            "None" -> {
                binding.homeBtn.visibility = View.GONE
                binding.space30.visibility = View.GONE
                binding.voiceRegBtn.visibility = View.GONE
                return
            }
        }
    }

    private fun addListener() {
        binding.homeBtn.setOnClickListener { backToHome() }
        binding.voiceRegBtn.setOnClickListener { goToVoiceRegister() }
        handler.postDelayed ( {
            if(sharedViewModel.getStaff() == null)
                onFaceSearchFailure()
        }, 30000)
        // fake data for testing
//        val mPic = context?.let { AppCompatResources.getDrawable(it, R.drawable.ic_app_logo) }
//        handler.postDelayed( {
//            onFaceSearchSuccess(StaffInfo(
//                "Nguyen Van Boi",
////                BitmapFactory.decodeResource(resources)
//                null,
//                "123456"))
//        }, 100)
    }

    private fun removeListener() {
        handler.removeCallbacks({onFaceSearchFailure()})
        binding.homeBtn.removeCallbacks { backToHome() }
        binding.voiceRegBtn.removeCallbacks { goToVoiceRegister() }
    }

    private fun backToHome() {
        findNavController().navigate(R.id.action_registerFaceFragment_to_homeActivity)
        activity?.finish()
    }

    private fun goToVoiceRegister() {
        context?.let {
            findNavController().navigate(R.id.action_registerFaceFragment_to_registerVoiceFragment)
        }
    }

    private fun createCameraManager() {
        context?.let {
            cameraManager = CameraManager(
                it,
                binding.viewFinder,
                viewLifecycleOwner,
                binding.overlayContainer,
                mJob, coroutineContext
            )
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->
        run {
            if (isGranted.containsValue(false)) {
                Toast.makeText(
                    context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                cameraManager.startCamera(::cameraCallback)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun cameraCallback(staff: StaffInfo) {
        Log.d(TAG, "====> camera callback")
        if(sharedViewModel.getStaff() == null) {
            staff.code?.let {
                sharedViewModel.setStaff(staff)
                onFaceSearchSuccess(staff)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onFaceSearchSuccess(staff: StaffInfo) {
        sharedViewModel.setStaff(staff)
        activity?.runOnUiThread {
            binding.topText.text = getString(R.string.staff) + staff.name
            binding.bottomText.text = getString(R.string.msgPleaseRegisterVoice)
            setButtonVisible("Voice")
        }
        cameraManager.stopCamera()
    }

    @SuppressLint("SetTextI18n")
    private fun onFaceSearchFailure() {
        activity?.runOnUiThread {
            binding.camField.visibility = View.GONE
            binding.topText.text = getString(R.string.identificationFailed)
            binding.bottomText.text = getString(R.string.msgFaceUnregistered)
            setButtonVisible("Home")
        }
        cameraManager.stopCamera()
    }

    private fun allPermissionsGranted() = Constants.CAMERA_REQUIRED_PERMISSIONS.all {
        context?.let { baseContext ->
            ContextCompat.checkSelfPermission(
                baseContext, it)
        } == PackageManager.PERMISSION_GRANTED
    }
}