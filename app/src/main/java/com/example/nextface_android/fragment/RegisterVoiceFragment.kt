package com.example.nextface_android.fragment

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.nextface_android.Constants
import com.example.nextface_android.LogService
import com.example.nextface_android.R
import com.example.nextface_android.Utils
import com.example.nextface_android.api.NFCallback
import com.example.nextface_android.databinding.FragmentRegisterVoiceBinding
import com.example.nextface_android.model.VoiceModel
import com.example.nextface_android.viewmodel.NextFaceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import okhttp3.OkHttpClient
import java.text.DecimalFormat
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class RegisterVoiceFragment : Fragment(), CoroutineScope {
    private lateinit var mJob: Job

    private lateinit var binding: FragmentRegisterVoiceBinding
    private lateinit var voiceModel: VoiceModel
    private lateinit var navController: NavController
    private var logService = LogService
    private val handler = Handler(Looper.myLooper()!!)

    private lateinit var httpClient: OkHttpClient
    private val sharedViewModel: NextFaceViewModel by activityViewModels()
    private val util = Utils()
    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>>
        get() = registerForActivityResult(
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
                        _startRecord()
                    }
                }
            }

    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        httpClient = OkHttpClient()
        binding = FragmentRegisterVoiceBinding.inflate(inflater)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register_voice, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setContentView(binding.root)
        mJob = Job()
        setButtonVisible("Start")

        voiceModel = VoiceModel(requireContext(), logService, httpClient)
        voiceModel.loginVoiceService()

        navController = findNavController()
        stopRunnable.parent = this

        sharedViewModel.getStaff()?.let {
            binding.staffAvatar.setImageBitmap(it.avatar)
            binding.staffName.text = getString(R.string.staff) + it.name
        }
        binding.dialogue.text = "${getString(R.string.default_reg_dialogue)} ${util.generateOTP()}"
    }

    override fun onStart() {
        super.onStart()
        enrollResultDispatcher.navCtrl = findNavController()
        binding.startRecordBtn.setOnClickListener { startRecord() }
        binding.stopRecordBtn.setOnClickListener { stopRecord() }
    }

    override fun onStop() {
        super.onStop()
        binding.startRecordBtn.removeCallbacks { startRecord() }
        binding.stopRecordBtn.removeCallbacks { stopRecord() }
        handler.removeCallbacks(stopRunnable)
    }

    private fun startRecord() {
        if(allPermissionsGranted()) {
            _startRecord()
        } else {
            requestPermissionLauncher.launch(Constants.RECORD_AUDIO_REQUIRED_PERMISSIONS)
        }
    }

    private fun _startRecord() {
        voiceModel.startRecord()
        context?.let {
            setButtonVisible("Stop")
        }
        binding.timeRecord.base = SystemClock.elapsedRealtime()
        binding.timeRecord.start()
        handler.postDelayed(stopRunnable, 10000)
    }

    private object stopRunnable: Runnable {
        lateinit var parent: RegisterVoiceFragment
        override fun run() {
            parent.stopRecord()
        }
    }

    private fun stopRecord() {
        logService.appendLog("[LINHNT63] ====> ")
        binding.timeRecord.stop()
        activity?.runOnUiThread{ binding.timeRecord.text = getString(R.string.zeroMinutes) }
        voiceModel.stopRecord()
        val enrollData = arrayOf<String>()
        voiceModel.queryRecordedData()?.let {
            voiceModel.enrollVoice(
                enrollData.plusElement(it),
                sharedViewModel.getStaff()?.code!!,
                object: NFCallback<String> {
                    override fun onSuccess(response: String) {
                        Log.e("RegisterVoice", "response")
                        sharedViewModel.setResult(response)
                        handler.post {
                            navController.navigate(R.id.action_registerVoiceFragment_to_registerResultFragment)
                        }
                    }

                    override fun onFailure(message: String) {
                        Log.e("RegisterVoice", "enroll error")
                        sharedViewModel.setResult(message)
                        handler.post(enrollResultDispatcher)
                    }
                }
            )
        }
    }

    private fun setButtonVisible(name: String) {
        when(name) {
            "Start" -> {
                binding.startRecordBtn.visibility = View.VISIBLE
                binding.stopRecordBtn.visibility = View.GONE
                return
            }
            "Stop" -> {
                binding.startRecordBtn.visibility = View.GONE
                binding.stopRecordBtn.visibility = View.VISIBLE
                return
            }
        }
    }

    private fun allPermissionsGranted() = Constants.RECORD_AUDIO_REQUIRED_PERMISSIONS.all {
        requireContext().checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    private object enrollResultDispatcher: Runnable {
        lateinit var navCtrl: NavController
        override fun run() {
            navCtrl.navigate(R.id.action_registerVoiceFragment_to_registerResultFragment)
        }

    }

    companion object {

    }
}