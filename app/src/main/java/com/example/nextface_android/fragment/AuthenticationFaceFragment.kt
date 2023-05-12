package com.example.nextface_android.fragment

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nextface_android.*
import com.example.nextface_android.api.NFCallback
import com.example.nextface_android.databinding.FragmentAuthenticationFaceBinding
import com.example.nextface_android.model.StaffInfo
import com.example.nextface_android.viewmodel.NextFaceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class AuthenticationFaceFragment : Fragment(), CoroutineScope {
    private lateinit var mJob: Job
    private lateinit var cameraManager: CameraManager
    private lateinit var binding: FragmentAuthenticationFaceBinding
    private lateinit var handler: Handler
    private lateinit var navController: NavController

    private val staffListViewAdapter = StaffListViewAdapter()
    private val sharedViewModel: NextFaceViewModel by activityViewModels()

    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    private val logService = LogService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        return inflater.inflate(R.layout.fragment_authentication_face, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAuthenticationFaceBinding.inflate(layoutInflater)
        activity?.setContentView(binding.root)
        mJob = Job()

        binding.staffList.apply {
            setHasFixedSize(true)
            itemAnimator = null
            adapter = staffListViewAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        handler = Handler(Looper.myLooper()!!)
        navController = findNavController()

        createCameraManager()
        startCamera()

        val mPic = context?.let { AppCompatResources.getDrawable(it, R.drawable.ic_app_logo) }!!
        binding.overlayContainer.setLoggoPic(mPic)
    }

    override fun onStart() {
        super.onStart()
        initData()
        addListener()
    }

    override fun onStop() {
        super.onStop()
        cameraManager.stopCamera()
        removeListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mJob.cancel()
    }

    private fun initData() {
        binding.bottomText.text = getString(R.string.default_description)
        binding.overlayContainer.setImageFlip(true)
        binding.camField.visibility = View.VISIBLE
        binding.timer.visibility = View.VISIBLE

        staffListViewAdapter.clear()
        sharedViewModel.reset()
        setButtonVisible("None")
        initCountDown()
    }

    @SuppressLint("SetTextI18n")
    private fun initCountDown() {
        // time in seconds
        var count = 60
        binding.timer.isCountDown = true
        binding.timer.base = SystemClock.elapsedRealtime()
        binding.timer.text = getString(R.string.timeVerify)
        binding.timer.onChronometerTickListener = Chronometer.OnChronometerTickListener {
            if(count < 0) {
                if(staffListViewAdapter.itemCount == 3) {
                    onFaceSearchSuccess()
                } else {
                    onFaceSearchFailure()
                }
                count = 60
            } else {
                val m = count / 60
                val s = count % 60
                val mm = if(m < 10) "0$m" else "$m"
                val ss = if(s < 10) "0$s" else "$s"
                binding.timer.text = "$mm:$ss"
                count--
//                count -= 30
            }
        }
        binding.timer.start()
    }

    private  fun startCamera() {
        if(allPermissionsGranted()){
            cameraManager.startCamera(::cameraCallback)
        }else{
            requestPermissionLauncher.launch(Constants.CAMERA_REQUIRED_PERMISSIONS)
        }
    }

    private fun setButtonVisible(key: String) {
        when(key) {
            "Home" ->
                binding.homeBtn.visibility = View.VISIBLE
            else ->
                binding.homeBtn.visibility = View.GONE
        }
    }

    private fun addListener() {
        binding.homeBtn.setOnClickListener { backToHome() }
    }

    private fun removeListener() {
        binding.homeBtn.removeCallbacks { backToHome() }
    }

    private fun backToHome() {
        navController.navigate(R.id.action_authenticationFaceFragment_to_homeActivity)
        activity?.finish()
    }

    private fun goToVoiceRegister() {
        navController.navigate(R.id.action_authenticationFaceFragment_to_authenticationVoiceFragment)
    }

    private fun createCameraManager() {
        context?.let {
            cameraManager = CameraManager(
                it,
                binding.viewFinder,
                this,
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

    private fun allPermissionsGranted() = Constants.CAMERA_REQUIRED_PERMISSIONS.all {
        context?.let { baseContext ->
            ContextCompat.checkSelfPermission(
                baseContext, it)
        } == PackageManager.PERMISSION_GRANTED
    }

    private fun cameraCallback(staff: StaffInfo) {
        if(sharedViewModel.getStaff() != null) {
            return
        }
        if(staffListViewAdapter.itemCount < Constants.KEY_FACE_NUMB && !staff.code.isNullOrEmpty()) {
            cameraManager.faceSearch.verifyUser(staff.code!!, object: NFCallback<String> {
                override fun onSuccess(response: String) {
                    staffListViewAdapter.add(staff)
                    if(staffListViewAdapter.itemCount == Constants.KEY_FACE_NUMB) {
                        onFaceSearchSuccess()
                    }
                    return
                }

                override fun onFailure(message: String) {
                    onFaceSearchFailure()
                }
            })

            cameraManager.faceSearch.verifyWorkingSession(object: NFCallback<String> {
                override fun onSuccess(response: String) {
                    staffListViewAdapter.add(staff)
                    if(!response.toBoolean()) {
                        Log.d("verifyWorkingSession", "============callback = ${response}")
                        activity?.runOnUiThread {
                            Toast.makeText(
                                activity?.applicationContext,
                                "WARN: Out of working session!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    return
                }

                override fun onFailure(message: String) {

                }
            })
        }
    }

    private fun onFaceSearchSuccess() {
        val random = (0 until staffListViewAdapter.itemCount).random()
        val staff = staffListViewAdapter.getItem(random)
        logService.appendLog("RANDOM STAFF ${staff?.name} ${staff?.code}")
        sharedViewModel.setStaff(staff)
        binding.timer.stop()
        cameraManager.stopCamera()

        activity?.runOnUiThread {
            binding.camField.visibility = View.GONE
            binding.timer.visibility = View.GONE
            binding.bottomText.text = getString(R.string.identificationSuccessfully)
            handler.postDelayed({ goToVoiceRegister() }, 1000)
        }
    }

    private fun onFaceSearchFailure() {
        binding.timer.stop()
        cameraManager.stopCamera()

        activity?.runOnUiThread {
            binding.staffList.visibility = View.GONE
            binding.camField.visibility = View.GONE
            binding.timer.visibility = View.GONE
            binding.bottomText.text = getString(R.string.identificationFailedWithCause)
            setButtonVisible("Home")
        }
    }
}