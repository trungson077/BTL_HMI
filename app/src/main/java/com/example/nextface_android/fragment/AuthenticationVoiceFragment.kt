package com.example.nextface_android.fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.nextface_android.*
import com.example.nextface_android.api.NFCallback
import com.example.nextface_android.databinding.FragmentAuthenticationVoiceBinding
import com.example.nextface_android.model.VoiceModel
import com.example.nextface_android.viewmodel.NextFaceViewModel
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import okhttp3.OkHttpClient
import kotlin.coroutines.CoroutineContext


class AuthenticationVoiceFragment : Fragment(), CoroutineScope {
    private lateinit var mJob: Job
    private lateinit var binding: FragmentAuthenticationVoiceBinding
    private lateinit var voiceModel: VoiceModel
    private lateinit var httpClient: OkHttpClient
    private lateinit var navController: NavController
    private lateinit var handler: Handler

    private val sharedViewModel: NextFaceViewModel by activityViewModels()
    private val util = Utils()

    private lateinit var usbManager : UsbManager

    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAuthenticationVoiceBinding.inflate(inflater)
        mJob = Job()
        httpClient = OkHttpClient()
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_authentication_voice, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setContentView(binding.root)
        voiceModel = VoiceModel(requireContext(), LogService, httpClient)

        sharedViewModel.getStaff()?.let {
            binding.staffAvatar.setImageBitmap(it.avatar)
            binding.staffName.text = getString(R.string.staff) + it.name }

        navController = findNavController()
        handler = Handler(Looper.myLooper()!!)

        voiceModel.loginVoiceService()
        binding.dialogue.text = "${getString(R.string.default_auth_dialogue)} ${util.generateOTP()}"
        stopRecordRunnable.parent = this


        usbManager = requireActivity().getSystemService(Context.USB_SERVICE) as UsbManager
    }

    override fun onStart() {
        super.onStart()
        setButtonVisible("Start")
        binding.startRecordBtn.setOnClickListener { startRecord() }
        binding.stopRecordBtn.setOnClickListener { stopRecord() }
    }

    override fun onStop() {
        super.onStop()
        binding.startRecordBtn.removeCallbacks { startRecord() }
        binding.stopRecordBtn.removeCallbacks { stopRecord() }
        handler.removeCallbacks(stopRecordRunnable)
    }

    private fun startRecord() {
        if(allPermissionsGranted()) {
            _startRecord()
        } else {
            requestPermissionLauncher.launch(Constants.RECORD_AUDIO_REQUIRED_PERMISSIONS)
        }
    }

    private fun stopRecord() {
        context?.let {
            binding.timeRecord.stop()
            binding.timeRecord.text = getString(R.string.zeroMinutes)
            voiceModel.stopRecord()
            verifyVoice()
        }
    }

    private fun _startRecord() {
        voiceModel.startRecord()
        context?.let {
            setButtonVisible("Stop")
        }
        binding.timeRecord.base = SystemClock.elapsedRealtime()
        binding.timeRecord.start()
        handler.postDelayed(stopRecordRunnable, 10000)
    }

    private object stopRecordRunnable: Runnable {
        lateinit var parent: AuthenticationVoiceFragment
        override fun run() {
            parent.stopRecord()
        }
    }

    private fun verifyVoice() {
        val audio = voiceModel.queryRecordedData()
        val code = sharedViewModel.getStaff()?.code
        val desc = "Voice Record"
        if (code != null && audio != null) {
            voiceModel.verifyVoice(code, audio, desc, object: NFCallback<String> {
                override fun onSuccess(response: String) {
                    if(response == "2")
                        onVerifyVoiceResult("Short speech", Color.RED)
                    else if(response == "3"){
                        GateController.connect(Constants.REQUEST_GATE_OPEN, usbManager)
                        onVerifyVoiceResult("Match", Color.GREEN)
                    }
                    else
                        onVerifyVoiceResult("Not Match", Color.RED)
                }
                override fun onFailure(message: String) {
                    onVerifyVoiceResult(message, Color.RED)
                }
            })
        }
    }

    private fun onVerifyVoiceResult(msg: String, color: Int)  {
        sharedViewModel.setResult(msg, color)
        handler.post {
            navController.navigate(R.id.action_authenticationVoiceFragment_to_authenticationResultFragment)
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
}