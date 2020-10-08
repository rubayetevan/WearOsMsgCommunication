package live.ebox.myapplication.communication

interface WearableMessageListener {
    fun onMessageReceived(data: ByteArray, path: String, sourceNodeId: String, requestId: Int)
}