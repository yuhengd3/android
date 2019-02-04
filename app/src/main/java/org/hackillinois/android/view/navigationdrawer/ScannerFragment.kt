package org.hackillinois.android.view.navigationdrawer

import android.Manifest
import android.R
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import kotlinx.android.synthetic.main.fragment_scanner.*
import kotlinx.android.synthetic.main.fragment_scanner.view.*
import org.hackillinois.android.model.CheckIn.CheckIn
import org.hackillinois.android.model.Event.UserEventPair
import org.hackillinois.android.viewmodel.ScannerViewModel


class ScannerFragment : Fragment() {
    var lastSuccessfullyScannedUser: String = ""
    val PERMISSIONS_REQUEST_ACCESS_CAMERA = 0
    lateinit var viewModel: ScannerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ScannerViewModel::class.java)
        viewModel.init()
        viewModel.eventsListLiveData.observe(this, Observer { updateEventList(it) })

        // We will only observe the change in lastScanWasSuccessful, as if that changes,
        // so will the scanned userId
        viewModel.lastScanWasSuccessful.observe(this, Observer { processLastScan(it) })

        viewModel.shouldDisplayOverrideSwitch.observe(this, Observer { updateOverrideSwitchVisibility(it) })
    }

    /**
     * Make the switch invisible when not relevant to the event.
     */
    private fun updateOverrideSwitchVisibility(it: Boolean?) {
        staffOverrideSwitch.visibility = when (it) {
            true -> AdapterView.VISIBLE
            false -> AdapterView.GONE
            null -> AdapterView.GONE
        }
    }

    /**
     * Called when scanner scans something.
     */
    private var onQrCodeScan: BarcodeCallback = object : BarcodeCallback {
        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
        }

        override fun barcodeResult(result: BarcodeResult) {
            // Prevent duplicate scans
            if (result.text == null || result.text == lastSuccessfullyScannedUser) {
                return
            }

            if (lastSuccessfullyScannedUser == "") {
                lastSuccessfullyScannedUser = result.text
            }

            Log.d("ScanEvent", "Scanned text: ${result.text}")
            Toast.makeText(context, lastSuccessfullyScannedUser, Toast.LENGTH_LONG).show()

            // Event that user is being scanned in for
            var eventName: String = eventListView.selectedItem.toString()

            // User's ID
            var userId: String = getUserIdFromQrString(lastSuccessfullyScannedUser)


            Log.d("ScanEvent", "User ID: ${userId}")
            Log.d("ScanEvent", "Event: ${eventName}")

            // Check-in is a special event in the HackIllinois API
            if (eventName == "Check In") {
                var override = staffOverrideSwitch.isChecked
                var hasCheckedIn = true
                var hasPickedUpSwag = true
                var checkIn = CheckIn(userId, override, hasCheckedIn, hasPickedUpSwag)

                viewModel.checkInUser(checkIn)

            } else {
                var userEventPair = UserEventPair(eventName, userId)

                viewModel.markUserAsAttendingEvent(userEventPair)
            }
        }
    }

    /**
     * A callback that is called when the status of the last scan request changes.
     */
    fun processLastScan(lastScanWasSuccessful: Boolean?) {
        when (lastScanWasSuccessful) {
            false -> {
                Toast.makeText(context, "Try again!", Toast.LENGTH_LONG).show()
            }
            true -> {
                lastSuccessfullyScannedUser = viewModel.lastUserIdScannedIn.value.toString()
                Toast.makeText(context, "Success: ${lastSuccessfullyScannedUser}",
                        Toast.LENGTH_LONG).show()
                staffOverrideSwitch.isChecked = false
            }
            null -> {
                return
            }
        }

    }

    /**
     * Extracts the userId from the HackIllinois QR String URI.
     * Given an example URI "hackillinois://user?userid=github0000001"
     * it is enough to grab everything after the last (and only) equals sign.
     *
     * If the URI becomes more complex, we can use actual URI decoders.
     * @return the userId from a QR String
     */
    private fun getUserIdFromQrString(qrString: String): String {
        var splitOnEquals = qrString.split("=")
        return splitOnEquals.last()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_ACCESS_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                qrScanner.decodeContinuous(onQrCodeScan)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(org.hackillinois.android.R.layout.fragment_scanner, container, false)

        view.eventListView.onItemSelectedListener = viewModel.onEventSelected

        // Ensure the Camera permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && activity!!.checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA),
                    PERMISSIONS_REQUEST_ACCESS_CAMERA)
        } else {
            view.qrScanner.decodeContinuous(onQrCodeScan)
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    /**
     * Takes a list of events, grabs all of the name fields, creates an adapter with the list of
     * names, and uses that to populate the Spinner.
     * @param eventList the list of events from the latest publish event (local DB / API call)
     */
    private fun updateEventList(eventList: List<org.hackillinois.android.database.entity.Event>?) {
        var eventNameList: List<String> = eventList!!.map { it.name }

        var spinnerAdapter: ArrayAdapter<String> = ArrayAdapter(context, R.layout.simple_dropdown_item_1line, eventNameList)

        eventListView.adapter = spinnerAdapter
    }

    override fun onResume() {
        super.onResume()
        qrScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        qrScanner.pause()
    }

}
