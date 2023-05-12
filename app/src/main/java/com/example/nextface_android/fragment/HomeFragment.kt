package com.example.nextface_android.fragment

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.nextface_android.Constants
import com.example.nextface_android.GateController
import com.example.nextface_android.R
import com.example.nextface_android.databinding.FragmentHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

class HomeFragment : Fragment(), CoroutineScope {
    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    private lateinit var binding: FragmentHomeBinding
    private val navCtrl: NavController
        get() = findNavController()

    private lateinit var usbManager1 : UsbManager
    private lateinit var usbManager2 : UsbManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentHomeBinding.inflate(inflater)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    var foo:Boolean? by Delegates.observable(false) { property, oldValue, newValue ->
        Log.d("gate_check_distance","gggol: $newValue")
        if ((newValue == true) and (oldValue != newValue)){
//            Log.d("gate_check_distance","leuleu:")
//            gateCtl.connect(Constants.REQUEST_GATE_CLOSE, usbManager)
//            delayhandler.removeCallbacks(runCheckDistance)
//            closed = true
        }
    }
    private val delayhandler = Handler()
    private fun loopCheckDistance(){
            delayhandler.postDelayed(runable, 500)
    }
    private val runable = Runnable {
        if (foo == true){
//            GateController.connect(Constants.REQUEST_GATE_CLOSE, usbManager2)
            return@Runnable
        }
        Log.d("gate_check_distance", "read status")
        val distance = GateController.read_status(Constants.REQUEST_GATE_STATUS, usbManager1)
        try {
            Log.d("gate_check_distance", "distance: $distance")
            if (distance > Constants.DISTANCE_TO_CLOSE_GATE || distance==0) {
                foo = true
                Log.d("gate_check_distance", "close gate")
                GateController.connect(Constants.REQUEST_GATE_CLOSE, usbManager2)
            }
        }catch (e: Exception){
            Log.d("gate_check_distance", "CheckDistance exception:  ${e.toString()}")
        }

        loopCheckDistance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setContentView(binding.root)

        mJob = Job()

        usbManager2 = requireActivity().getSystemService(Context.USB_SERVICE) as UsbManager
        usbManager1 = requireActivity().getSystemService(Context.USB_SERVICE) as UsbManager
//        GateController.connect(Constants.REQUEST_GATE_CLOSE, usbManager2)
//        for (i in 1..5){
//            val dis = GateController.read_status(Constants.REQUEST_GATE_STATUS, usbManager1)
//            Log.d("distance_", "distance: $dis")
//            Log.d("dissss", "distance: $i")
////            if (dis > 50){
////                GateController.connect(Constants.REQUEST_GATE_CLOSE, usbManager)
////                break
////            }
//            if (i==5){GateController.connect(Constants.REQUEST_GATE_CLOSE, usbManager2) }
//            Thread.sleep(1000)
//        }


    }

    override fun onStart() {
        super.onStart()
        foo = false
        delayhandler.postDelayed(runable, 1000)

        binding.regBtn.setOnClickListener { register() }
        binding.authBtn.setOnClickListener { authenticate() }
    }

    override fun onStop() {
        super.onStop()
//        GateController.connect(Constants.REQUEST_GATE_OPEN, usbManager2)  // -------> FOR TEST
        delayhandler.removeCallbacks(runable)
        binding.regBtn.removeCallbacks { register() }
        binding.authBtn.removeCallbacks { authenticate() }
    }

    private fun register() {
        navCtrl.navigate(R.id.action_homeFragment_to_registerActivity)
    }

    private fun authenticate() {
        navCtrl.navigate(R.id.action_homeFragment_to_authenticationActivity)
//        GateController.connect(Constants.REQUEST_GATE_OPEN, usbManager2)
    }
}