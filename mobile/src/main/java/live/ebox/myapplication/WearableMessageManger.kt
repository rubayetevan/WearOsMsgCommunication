package live.ebox.myapplication

import android.app.Activity
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WearableMessageManger private constructor(private val activity: Activity) :
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    companion object {
        @Volatile
        private var INSTANCE: WearableMessageManger? = null
        fun getInstance(activity: Activity): WearableMessageManger {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = WearableMessageManger(activity)
                }
            }
            return INSTANCE!!
        }
    }


    private val CAPABILITY_1_NAME = "capability_1"
    private val CAPABILITY_2_NAME = "capability_2"
    private var nodeId: String? = null

    private val TAG = "WearableMessageManger"
    private val wearableMessageListener = activity as WearableMessageListener

    fun onResume() {
        Wearable.getDataClient(activity).addListener(this)
        Wearable.getMessageClient(activity).addListener(this)
        Wearable.getCapabilityClient(activity)
            .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)

        requestCapability(CAPABILITY_1_NAME)

        /*btn?.setOnClickListener {

        }*/
    }

    fun sendMessage(path: String, message: String) {
        nodeId?.let { nid ->
            val sendMessageTask =
                Wearable.getMessageClient(activity)
                    .sendMessage(nid, path, message.toByteArray()).addOnCompleteListener {
                        Log.d(TAG, "msg sent : ${it.isSuccessful}")
                    }
        }
    }

    private fun requestCapability(capabilityName: String) {

        GlobalScope.launch {
            val capabilityInfo: CapabilityInfo = Tasks.await(
                Wearable.getCapabilityClient(activity)
                    .getCapability(
                        capabilityName,
                        CapabilityClient.FILTER_REACHABLE
                    )
            )
            // capabilityInfo has the reachable nodes with the capability
            updateNodeId(capabilityInfo)
        }
    }

    private fun updateNodeId(capabilityInfo: CapabilityInfo) {
        nodeId = pickBestNodeId(capabilityInfo.nodes)
        Log.d(TAG, "phone got wear node id = $nodeId")
    }

    private fun pickBestNodeId(nodes: Set<Node>): String? {
        // Find a nearby node or pick one arbitrarily
        return nodes.firstOrNull { it.isNearby }?.id ?: nodes.firstOrNull()?.id
    }


    fun onPause() {
        Wearable.getDataClient(activity).removeListener(this)
        Wearable.getMessageClient(activity).removeListener(this)
        Wearable.getCapabilityClient(activity).removeListener(this)
    }

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        Log.d(TAG, "onDataChanged-> $dataEventBuffer")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(
            TAG,
            "onMessageReceived-> data =${String(messageEvent.data)} | path =${messageEvent.path}"
        )
        wearableMessageListener.onMessageReceived(String(messageEvent.data))
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged-> ${capabilityInfo}")
        updateNodeId(capabilityInfo)
    }

}