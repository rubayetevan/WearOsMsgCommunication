package live.ebox.myapplication.communication


import android.app.Activity
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WearableMessageManger private constructor(
    private val activity: Activity,
    private val capabilityName: String
) :
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    data class Builder(
        private var activity: Activity,
        private var capabilityName: String
    ) {
        fun setWearableMessageListener(wearableMessageListener: WearableMessageListener) =
            apply { Companion.wearableMessageListener = wearableMessageListener }

        fun build() = getInstance(activity, capabilityName)
    }

    companion object {

        private var wearableMessageListener: WearableMessageListener? = null

        @Volatile
        private var INSTANCE: WearableMessageManger? = null
        private fun getInstance(activity: Activity, capabilityName: String): WearableMessageManger {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = WearableMessageManger(activity, capabilityName)
                }
            }
            return INSTANCE!!
        }
    }

    private var nodeId: String? = null
    private val TAG = "WearableMessageManger"
    private var listenersAttached: Boolean = false

    init {
        onResume()
    }


    fun onResume() {
        if (!listenersAttached) {
            Wearable.getDataClient(activity).addListener(this)
            Wearable.getMessageClient(activity).addListener(this)
            Wearable.getCapabilityClient(activity)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
            listenersAttached = true
            requestCapability(capabilityName)
        }
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

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun requestCapability(capabilityName: String) {
        GlobalScope.launch {
            val capabilityInfo: CapabilityInfo = Tasks.await(
                Wearable.getCapabilityClient(activity)
                    .getCapability(
                        capabilityName,
                        CapabilityClient.FILTER_REACHABLE
                    )
            )
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
        listenersAttached = false
    }

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        Log.d(TAG, "onDataChanged-> $dataEventBuffer")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(
            TAG,
            "onMessageReceived-> data =${String(messageEvent.data)} | path =${messageEvent.path} | sourceNodeId =${messageEvent.sourceNodeId} | requestId =${messageEvent.requestId}"
        )
        wearableMessageListener?.onMessageReceived(
            data = messageEvent.data,
            path = messageEvent.path,
            sourceNodeId = messageEvent.sourceNodeId,
            requestId = messageEvent.requestId
        )
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged-> ${capabilityInfo}")
        updateNodeId(capabilityInfo)
    }

}