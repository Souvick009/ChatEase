package com.example.chatease.activities

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.adapters_recyclerview.GroupChatAdapter
import com.example.chatease.databinding.ActivityGroupChatBinding
import com.example.chatease.dataclass.GroupMessageData
import com.example.chatease.trackers.TrackerSingletonObject
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class GroupChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupChatBinding
    private val rtDB = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var groupChatRef: DatabaseReference
    private var groupChatListenerObject: ChildEventListener? = null
    private lateinit var adapter: GroupChatAdapter
    private var unReadMessagesSet = mutableSetOf<String>()
    private val typingDBRef = rtDB.getReference("groups/metadata/typingReceipt")
    private var typingListener : ChildEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val groupID = intent.getStringExtra("groupID")!!

        TrackerSingletonObject.groupChatID = groupID

        // Setting up the RecyclerView for displaying messages
        val layoutManager = LinearLayoutManager(this@GroupChatActivity)
        layoutManager.stackFromEnd = true
        binding.recyclerViewCurrentChat.layoutManager = layoutManager// Setting layout manager

        val currentUserId = auth.currentUser?.uid

        adapter = GroupChatAdapter(messageList = messagesList, currentUserId = currentUserId!!)
        binding.recyclerViewCurrentChat.adapter = adapter


        if (currentUserId.isNullOrEmpty()) {
            Toast.makeText(this@GroupChatActivity, "Unable to get your userID, Please SignIn Again", Toast.LENGTH_LONG).show()
        }


//        typingListener = object : ChildEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.child("typing_of_$otherUserId").getValue(Boolean::class.java) == true) {
//                    if (lastSeenAndOnlinePersonalSetting && lastSeenAndOnlineOtherUserSetting) {
//                        binding.textViewUserPresenceStatus.visibility = View.INVISIBLE
//                    }
//
//                    binding.textViewTypingStatus.visibility = View.VISIBLE
//                } else {
//
//                    if (lastSeenAndOnlinePersonalSetting && lastSeenAndOnlineOtherUserSetting) {
//                        binding.textViewUserPresenceStatus.visibility = View.VISIBLE
//                    }
//
//                    binding.textViewTypingStatus.visibility = View.INVISIBLE
//                }
//            }
//
//            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onChildRemoved(snapshot: DataSnapshot) {}
//
//            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
//
//            override fun onCancelled(error: DatabaseError) {}
//        }
//
//        typingListener?.let {
//            typingDBRef.addChildEventListener(it)
//        }
//
//        var previousLineCount = 2
//        var minLines = 1
//        var lineCount = 0
//        var baseSdp = 50
//        val screenDensity = resources.displayMetrics.densityDpi / 160f
//        binding.editTextMessage.addTextChangedListener(object : TextWatcher {
//            // Typing Indicator SETUP below
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if (!TrackerSingletonObject.isTyping.get()) {
//                    TrackerSingletonObject.isTyping.set(true)
//                    val dataMap = hashMapOf<String, Any>(
//                        "typing_of_$currentUserId" to true
//                    )
//                    userDbRef.updateChildren(dataMap)
//                }
//
//                typingStatusHandler.removeCallbacksAndMessages(null)
//
//                typingStatusHandler.postDelayed({
//                    if (TrackerSingletonObject.isAppForeground.get()) {
//                        TrackerSingletonObject.isTyping.set(false)
//                        val dataMap = hashMapOf<String, Any>(
//                            "typing_of_$currentUserId" to false
//                        )
//                        userDbRef.updateChildren(dataMap)
//                    }
//                }, 2000L)
//
//                lineCount = binding.editTextMessage.lineCount
//                if(previousLineCount != lineCount) {
//                    if(lineCount > minLines) {
//                        Log.d("LINECOUNT",lineCount.toString())
//                        val adjustedLineCount = lineCount.coerceIn(minLines, 5)
//                        val newSdp = baseSdp + (20 * (adjustedLineCount - 2))
//                        val params = binding.chatInputCard.layoutParams
//                        params.height = (newSdp * screenDensity).toInt()
//                        binding.chatInputCard.layoutParams = params
//                    }
//
//                    previousLineCount = lineCount
//                }
//            }
//
//            override fun afterTextChanged(s: Editable?) {}
//
//        })

        binding.buttonSend.setOnClickListener {
            sendMessage(groupID = groupID, currentUserID = currentUserId!!)
        }

        fetchGroupMetaData(groupID = groupID, currentUserID = currentUserId!!)
    }

    private fun sendMessage(groupID: String, currentUserID: String) {
        if (binding.editTextMessage.text.isNullOrEmpty()) return

        // Setting up the send button click listener
        binding.buttonSend.setOnClickListener {

            // Check if the message input is not empty
            if (binding.editTextMessage.text.toString().trim().isNotEmpty()) {
                // Reference to the metadata document for the chat
                val metaRef = rtDB.getReference("groups/$groupID/metadata")

                // Timestamp for the message
                val timestamp = ServerValue.TIMESTAMP

                // Last message content
                val lastMessage = binding.editTextMessage.text.toString().trim()
                binding.editTextMessage.text.clear()
                // Reference to the messages collection within the chat document
                val messageRef = rtDB.getReference("groups/$groupID/messages")
                // Generating a new message ID
                val newMessageId = messageRef.push().key // Firebase generates a random ID

                // Creating a map to store metadata of the chat
                val groupMetaData = hashMapOf(
                    "lastMessage" to lastMessage,
                    "lastMessageID" to newMessageId,
                    // setting the the last message read for the other user (with whom im chatting with) haven't read
                    "readReceipt" to mapOf(
                        currentUserID to true
                    ),
                    "lastMessageTimestamp" to timestamp,
                    "lastMessageSender" to currentUserID
                )

                // Setting the metadata document in Realtime Database
                metaRef.updateChildren(groupMetaData)

                // Creating a map for the message data
                val messageData = hashMapOf(
                    "sender" to currentUserID,
                    "content" to lastMessage,
                    "timestamp" to timestamp,
                    "readReceipt" to mapOf(
                        currentUserID to true
                    ),
                    "lastReadTimestamp" to ""
                )

                // Adding the new message to Realtime Database
                if (newMessageId != null) {
                    messageRef.child(newMessageId).setValue(messageData)
                        .addOnCompleteListener { task ->
                            // Clearing the message input field on successful send
                            if (task.isSuccessful) {
//                                showOfflineNotification(
//                                    lastMessage = lastMessage,
//                                    senderID = auth.currentUser!!.uid,
//                                    messageID = newMessageId,
//                                    groupID = groupID
//                                )
                            }
                        }
                        .addOnFailureListener { exception ->
                            // Displaying an error message if sending fails
                            Log.d("buttonSend", exception.toString())
                            Toast.makeText(this@GroupChatActivity, exception.toString(), Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(
                        this@GroupChatActivity,
                        "Failed to generate new message id for this message",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        }
    }

    private var totalParticipants = 0

    private fun fetchGroupMetaData(groupID: String?, currentUserID: String) {
        if (groupID == null) return

        CoroutineScope(Dispatchers.IO).launch {
            rtDB.getReference("groups/$groupID/metadata").get()
                .addOnSuccessListener { snapshot ->

                    CoroutineScope(Dispatchers.Main).launch {
                        binding.textViewGroupName.text = snapshot.child("groupName").getValue(String::class.java) ?: ""

                        val groupIcon = snapshot.child("groupIcon").getValue(String::class.java) ?: ""
                        if (groupIcon.isNotEmpty()) {
                            Glide.with(this@GroupChatActivity)
                                .load(groupIcon)
                                .placeholder(R.drawable.vector_icon_group)
                                .into(binding.groupIcon)

                            val params = binding.groupIcon.layoutParams
                            params.height = ViewGroup.LayoutParams.MATCH_PARENT
                            params.width = ViewGroup.LayoutParams.MATCH_PARENT
                            binding.groupIcon.layoutParams = params
                        }
                    }

                    val participantsList = mutableListOf<String>()
                    for (participant in snapshot.child("participants").children) {
                        participant.key?.let {
                            participantsList.add(it)
                        }
                    }
                    totalParticipants = participantsList.size
                    fetchGroupChats(groupID = groupID, totalParticipants = totalParticipants, currentUserID = currentUserID)
                }
        }
    }

    var participantsName = mutableMapOf<String, String>()

    private fun fetchGroupChats(groupID: String, totalParticipants: Int, currentUserID: String) {
        CoroutineScope(Dispatchers.IO).launch {
            groupChatRef = rtDB.getReference("groups/$groupID/messages")
            groupChatListenerObject = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val senderID = snapshot.child("sender").getValue(String::class.java) ?: ""

                    Log.d("content", snapshot.child("content").getValue(String::class.java) ?: "")

                    if (participantsName[senderID] == null) {
                        rtDB.getReference("users/$senderID/displayName").get()
                            .addOnSuccessListener { snapshot1 ->
                                participantsName[senderID] = snapshot1.value as? String? ?: ""
                                groupChatManagement(
                                    snapshot = snapshot,
                                    senderName = participantsName[senderID] ?: "",
                                    senderID = senderID,
                                    totalParticipants = totalParticipants,
                                    groupID = groupID,
                                    currentUserID = currentUserID
                                )
                            }
                    } else {
                        groupChatManagement(
                            snapshot = snapshot,
                            senderName = participantsName[senderID] ?: "",
                            senderID = senderID,
                            totalParticipants = totalParticipants,
                            groupID = groupID,
                            currentUserID = currentUserID
                        )
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val id = snapshot.key ?: ""
                    val senderID = snapshot.child("sender").getValue(String::class.java) ?: ""
                    if (senderID == currentUserID) {
                        val index = messagesList.indexOfFirst { it.id == id }

                        val readReceipt = snapshot.child("readReceipt").children
                        val messageReadByUsers = mutableMapOf<String, Boolean>()
                        for (participant in readReceipt) {
                            participant.key?.let { participantID ->
                                messageReadByUsers.put(participantID, true)
                            }
                        }

                        var everyoneReadChanged = false
                        if (messageReadByUsers.size == totalParticipants) {
                            everyoneReadChanged = true
                        }

                        if (index != -1 &&
                            messagesList[index].everyoneRead != everyoneReadChanged
                        ) {
                            val timestampLong = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                            // Convert Long timestamp to Date
                            val timestamp = Date(timestampLong)

                            // Formatting the timestamp to a readable string
                            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            formatter.timeZone = TimeZone.getDefault()
                            val formattedTimeStamp = formatter.format(timestamp)

                            messagesList[index] = GroupMessageData(
                                id = id,
                                senderName = participantsName[senderID] ?: "",
                                senderID = senderID,
                                content = snapshot.child("content").getValue(String::class.java) ?: "",
                                formattedTimestamp = formattedTimeStamp,
                                timestamp = timestampLong,
                                everyoneRead = everyoneReadChanged
                            )

                            adapter.notifyItemChanged(index)
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {}

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {}
            }

            groupChatListenerObject?.let {
                groupChatRef.orderByChild("timestamp")
                    .addChildEventListener(it)
            }
        }
    }

    private val currentTimestamp = System.currentTimeMillis()
    private var unReadIndicatorPosition = 0
    private val messagesList = mutableListOf<GroupMessageData>()
    private val readReceiptUsers = mutableMapOf<String, Boolean>()
    private var gotUnreadMessages = false

    private val newMessagesBuffer = mutableListOf<GroupMessageData>()
    private var messageQueueRunnable: Runnable? = null
    private var isInitialLoad = true

    private fun groupChatManagement(
        snapshot: DataSnapshot,
        senderName: String,
        senderID: String,
        totalParticipants: Int,
        groupID: String,
        currentUserID: String
    ) {
        // Convert Long timestamp to Date

        Log.e("1content", snapshot.child("content").getValue(String::class.java) ?: "")

        val timestampLong = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
        val readReceipt = snapshot.child("readReceipt").children
        val formattedTimeStamp =
            getRelativeTime(Timestamp((timestampLong / 1000), 0)) // Format the timestamp for display

        readReceiptUsers.clear()

        for (participant in readReceipt) {
            participant.key?.let { participantID ->
                readReceiptUsers.put(participantID, true)
            }
        }

        var everyoneRead = false
        // agar sabne padhliya
        if (readReceiptUsers.size == totalParticipants) {
            everyoneRead = true
        }

        // Creating a MessageUserData object
        val messageObject = GroupMessageData(
            id = snapshot.key ?: "",
            senderName = senderName,
            senderID = senderID,
            content = snapshot.child("content").getValue(String::class.java) ?: "",
            formattedTimestamp = formattedTimeStamp,
            timestamp = timestampLong,
            everyoneRead = everyoneRead
        )

        // Adding the message to the list and notifying the adapter
        if (senderID != auth.currentUser?.uid) {
            // This message wasn't sent by this user
            Log.d("runs Sender", "They Have Sent")
            if (!everyoneRead && (timestampLong < currentTimestamp)) {
                Log.d("runs read", "executes")
                // Mark it as the first unread message and add an indicator
                if (!gotUnreadMessages) {
                    newMessagesBuffer.add(GroupMessageData("", "", "", "", "", 0L, everyoneRead = false))
                    unReadIndicatorPosition = messagesList.size - 1
                    gotUnreadMessages = true
                }

                // Update read status and timestamp in the database
                if (TrackerSingletonObject.isAppForeground.get()) {
                    // Update metadata to reflect that the user has read the last message
                    everyoneRead = markAsRead(
                        messageReadByUsers = readReceiptUsers,
                        currentUserID = currentUserID,
                        groupID = groupID,
                        messageID = snapshot.key!!,
                        totalParticipants = totalParticipants
                    )
                    messageObject.everyoneRead = everyoneRead
                    newMessagesBuffer.add(messageObject)
                    updateMetaData(
                        groupID = groupID,
                        currentUserID = currentUserID,
                        readReceiptMap = readReceiptUsers
                    )
                } else {
                    newMessagesBuffer.add(messageObject)
                    unReadMessagesSet.add(snapshot.key!!)
                }

            } else if (!everyoneRead && (timestampLong > currentTimestamp)) {
                Log.d("runs read", "executes2")
                // Update read status and timestamp in the database
                if (TrackerSingletonObject.isAppForeground.get()) {
                    // Update metadata to reflect that the user has read the last message
                    everyoneRead = markAsRead(
                        messageReadByUsers = readReceiptUsers,
                        currentUserID = currentUserID,
                        groupID = groupID,
                        messageID = snapshot.key!!,
                        totalParticipants = totalParticipants
                    )
                    messageObject.everyoneRead = everyoneRead
                    newMessagesBuffer.add(messageObject)
                    updateMetaData(
                        groupID = groupID,
                        currentUserID = currentUserID,
                        readReceiptMap = readReceiptUsers
                    )
                } else {
                    newMessagesBuffer.add(messageObject)
                    unReadMessagesSet.add(snapshot.key!!)
                }
            } else if (everyoneRead) {
                Log.d("runs read", "executes everyone")
                // Add the message without marking it as truly unread
                newMessagesBuffer.add(messageObject)
                if (TrackerSingletonObject.isAppForeground.get()) {
                    // Update metadata to reflect that the user has read the last message
                    updateMetaData(
                        groupID = groupID,
                        currentUserID = currentUserID,
                        readReceiptMap = readReceiptUsers
                    )
                } else {
                    unReadMessagesSet.add(snapshot.key!!)
                }
            }
        } else {
            if (messagesList.size > 0) {
                if (unReadIndicatorPosition != -1) {
                    if (!messagesList[unReadIndicatorPosition].everyoneRead && messagesList[unReadIndicatorPosition].senderID == "") {
                        // Removing the unread indicator container "NEW"
                        // when this user replies/sent some message after those unread messages
                        messagesList.removeAt(unReadIndicatorPosition)
                        adapter.notifyItemRemoved(unReadIndicatorPosition)
                    }
                }
            }
            // This message was sent by this user
            newMessagesBuffer.add(messageObject)

//            updateMetaData(
//                groupID = groupID,
//                readReceiptMap = readReceiptUsers
//            )
        }
        messageQueueRunnable?.let { delayHandler.removeCallbacksAndMessages(it) }

        if (isInitialLoad) {
            messageQueueRunnable = Runnable {
                isInitialLoad = false
                messagesList.addAll(newMessagesBuffer)
                newMessagesBuffer.clear()
                messagesList.sortBy { it.timestamp }
                adapter.notifyDataSetChanged()
                binding.recyclerViewCurrentChat.scrollToPosition(messagesList.size - 1)
            }
            delayHandler.postDelayed(messageQueueRunnable!!, 200L)
        } else {
            messageQueueRunnable = Runnable {
                isInitialLoad = false
                messagesList.addAll(newMessagesBuffer)
                messagesList.sortBy { it.timestamp }
                val startIndex = messagesList.size - newMessagesBuffer.size
                val endIndex = messagesList.size - 1
                if (newMessagesBuffer.size == 1) {
                    adapter.notifyItemChanged(endIndex)
                } else {
                    adapter.notifyItemRangeChanged(startIndex, newMessagesBuffer.size)
                }
                newMessagesBuffer.clear()
                binding.recyclerViewCurrentChat.scrollToPosition(messagesList.size - 1)
            }
            delayHandler.postDelayed(messageQueueRunnable!!, 500L)
        }

//        adapter.notifyItemChanged(messagesList.size - 1) // Notify adapter of data change

        // Scrolling to the bottom to show the latest message
    }

    private fun markAsRead(
        messageReadByUsers: MutableMap<String, Boolean>,
        currentUserID: String,
        groupID: String,
        messageID: String,
        totalParticipants: Int
    ): Boolean {
        if (messageReadByUsers[currentUserID] == null) {
            // agar maine nahi padha
            messageReadByUsers[currentUserID] = true
            Log.d("runs mark as read", "yes marks")
            rtDB.getReference("groups/$groupID/messages/$messageID/readReceipt").updateChildren(
                mapOf(
                    currentUserID to true
                )
            )
            return messageReadByUsers.size == totalParticipants
            // Will return TRUE if readReceiptMembers size equals to Total group members, which means everybody has read the message.
            // otherwise will return false
        } else {
            Log.d("runs mark as read", "doesnt marks")
            return false
        }
    }

    private var delayHandler = Handler(Looper.getMainLooper())

    private fun updateMetaData(groupID: String, currentUserID: String, readReceiptMap: MutableMap<String, Boolean>) {
        delayHandler.removeCallbacksAndMessages(null)

        delayHandler.postDelayed({
            rtDB.getReference("groups/$groupID/metadata/readReceipt").updateChildren(
                mapOf(
                    currentUserID to true
                )
            )
        }, 1000L)
    }

    // Get a relative time string based on the timestamp for display
    private fun getRelativeTime(timestamp: Timestamp): String {
        // Create calendar instance from the timestamp
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp.seconds * 1000 }
        val today = Calendar.getInstance() // Get current date

        // Formatters for time display
        val dateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())
        val dateFormatterYear = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        return when {
            calendar.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> {
                // Not in the current year
                dateFormatterYear.format(calendar.time)
            }

            calendar.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR) -> {
                // Earlier this year
                dateFormatter.format(calendar.time)
            }

            else -> {
                // Today
                timeFormatter.format(calendar.time)
            }
        }
    }

    private fun getGoogleCredential(context: Context): GoogleCredentials {
        val oAuthJsonFile = context.assets.open("FCM_Server_OAuthKey.json")
        return GoogleCredentials.fromStream(oAuthJsonFile)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
    }

    private suspend fun getAccessToken(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val credentials = getGoogleCredential(this@GroupChatActivity) as ServiceAccountCredentials
                credentials.refreshIfExpired()
                credentials.accessToken.tokenValue
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun showOfflineNotification(
        lastMessage: String,
        senderID: String,
        messageID: String,
        groupID: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val accessToken = getAccessToken() ?: return@launch
            val url = "https://fcm.googleapis.com/v1/projects/chatease2024-aa2e1/messages:send"
            val payload = """
                {
                    "message" : {
                        "topic" : "$groupID",
                        "data" : {
                        "senderID" : "$senderID",
                        "messageID" : "$messageID",
                        "lastMessage" : "$lastMessage"
                        },
                        "android" : {
                        "priority" : "high"
                        }
                    }
                }
            """.trimIndent()

            val request = Request.Builder().apply {
                url(url)
                addHeader("Authorization", "Bearer $accessToken")
                addHeader("Content-Type", "application/json")
                post(payload.toRequestBody("application/json".toMediaType()))
            }

            val client = OkHttpClient()
            client.newCall(request.build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {

                    } else {
                        Log.e("FCM", "Error sending message: ${response.body?.string()}")
                    }
                }

            })
        }
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//        if (intent.getBooleanExtra("fromNotification", false)) {
//            startActivity(Intent(this@GroupChatActivity, MainActivity::class.java))
//            finish()
//        }
//    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences("CurrentUserMetaData", MODE_PRIVATE).edit().apply {
            putString("groupChatID", intent.getStringExtra("id"))
            commit()
        }

        if (unReadMessagesSet.isNotEmpty()) {
            auth.currentUser?.let { currentUser ->
                TrackerSingletonObject.groupChatID?.let { groupID ->
                    for (messageId in unReadMessagesSet) {
                        markAsRead(
                            messageReadByUsers = readReceiptUsers,
                            currentUserID = currentUser.uid,
                            groupID = groupID,
                            messageID = messageId,
                            totalParticipants = totalParticipants
                        )
                        updateMetaData(
                            groupID = groupID,
                            currentUserID = currentUser.uid,
                            readReceiptMap = readReceiptUsers
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences("CurrentUserMetaData", MODE_PRIVATE).edit().apply {
            remove("groupChatID")
            commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        groupChatListenerObject?.let {
            groupChatRef.removeEventListener(it)
            groupChatListenerObject = null
        }

//        typingListener?.let {
//            convoDBRef.removeEventListener(it)
//            typingListener = null
//        }
    }
}