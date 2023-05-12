package com.example.nextface_android

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.util.*


object GateController {
    fun connect(request: String, usbManager: UsbManager){
        val availableDrivers: List<UsbSerialDriver> =
            UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            return;
        }
        val driver = availableDrivers[0]
        val connection: UsbDeviceConnection = usbManager.openDevice(driver.device)
        val port = driver.ports[0] // Most devices have just one port (port 0)
        port.open(connection)
        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        val data: ByteArray = request.toByteArray()
        port.write(data, Constants.WRITE_WAIT_MILLIS)
        val buffer = ByteArray(8192)
        val len: Int = port.read(
            buffer,
            Constants.READ_WAIT_MILLIS
        )
        Log.d("gate_connect", "request: $request")
        port.close()
        connection.close()
    }

    fun read_status(request: String, usbManager: UsbManager): Int{
        val availableDrivers: List<UsbSerialDriver> =
            UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            return -1;
        }
        val driver = availableDrivers[0]
        val connection: UsbDeviceConnection = usbManager.openDevice(driver.device)
        val port = driver.ports[0] // Most devices have just one port (port 0)
        port.open(connection)
        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

        var distance = -1


        try {
            val data: ByteArray = request.toByteArray()
            port.write(data, Constants.WRITE_WAIT_MILLIS)
            val buffer = ByteArray(8192)
            val len: Int = port.read(
                buffer,
                Constants.READ_WAIT_MILLIS
            )
            Log.d("check_len_", "$len")

            distance = receive(Arrays.copyOf(buffer, len))

        } catch (e: Exception){
            Log.d("gate_check_distance_err", "read_status exception: ${e.toString()}")
        }

        port.close()
        connection.close()

        return distance
    }

    private fun receive(data: ByteArray): Int {
        var msg = String(data)

        try{
            msg = msg.split("\"d\":")[1].split(",")[0]
            Log.d("gate_check_distance: ", msg)
            return msg.toInt()
        }catch (e: Exception){
            Log.d("gate_check_distance: ",  "receive exception: ${e.toString()}")
        }

        return -1
    }
}