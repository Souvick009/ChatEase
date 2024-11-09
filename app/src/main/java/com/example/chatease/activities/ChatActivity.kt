package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.databinding.ActivityChatBinding
import com.example.chatease.dataclass.MessageUserData
import com.example.chatease.recyclerview_adapters.ChatAdapter
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ChatActivity handles the chat screen functionality
class ChatActivity : AppCompatActivity() {

    // Firestore database instance
    private val db = Firebase.firestore

    private val rtDB = FirebaseDatabase.getInstance()

    // Firebase Authentication instance to handle user authentication
    private val auth = FirebaseAuth.getInstance()

    // View binding for the activity layout
    private lateinit var binding: ActivityChatBinding

    // RecyclerView for displaying messages
    private lateinit var recyclerView: RecyclerView

    // List to hold message data
    private var messagesList = mutableListOf<MessageUserData>()

    private val token = 1 // Token for activity result
    private var otherUserId: String? = "" // ID of the other user in the chat

    private lateinit var rtDbChatListener: DatabaseReference

    private var messageListener: ChildEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Adjusting the padding for the main view to avoid overlap with system UI elements
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            // Getting the insets for the system bars (status bar, navigation bar)
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Setting padding based on the insets to avoid content overlap
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Taking Custom Toolbar from view binding
        val toolbar = binding.chatActivityToolbar
        // Setting the custom toolbar as the ActionBar
        setSupportActionBar(toolbar)
        // Enables back button on toolbar to navigate to parent activity
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Disabling the default app name display on the ActionBar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Get the current user's ID
        val currentUserId = auth.currentUser?.uid

        // Retrieve the other user's ID from the intent
        val otherUserId = intent.getStringExtra("id")

        if (otherUserId == null) {
            // Show a Toast message if the user ID is missing
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show()
            finish() // Close the activity if ID is missing
            return
        }

        var avatarAlreadyLoadedForTheFirstTime = false
        var previousAvatarUrl: String? = null

        rtDB.getReference("users").child(otherUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Setting the display name from intent extra
                    binding.textViewDisplayName.text = snapshot.child("displayName").getValue(String::class.java) ?: ""

                    if (snapshot.child("status").getValue(String::class.java) == "Offline") {
                        val lastHeartBeatTime = (snapshot.child("lastHeartBeat").getValue(Long::class.java) ?: 0L) / 1000

                        binding.textViewUserPresenceStatus.text = "Last Seen at " +
                                getRelativeTime(Timestamp(lastHeartBeatTime, 0)) // Format the timestamp for display
                    } else {
                        binding.textViewUserPresenceStatus.text = snapshot.child("status").getValue(String::class.java)
                    }

                    // Loading the user's avatar image using Glide library
                    val avatar = snapshot.child("avatar").getValue(String::class.java)

                    if (!avatarAlreadyLoadedForTheFirstTime || avatar != previousAvatarUrl) {
                        avatarAlreadyLoadedForTheFirstTime = true
                        previousAvatarUrl = avatar

                        if (!isFinishing && !isDestroyed) {
                            Glide.with(this@ChatActivity)
                                .load(snapshot.child("avatar").getValue(String::class.java))
                                .placeholder(R.drawable.vector_default_user_avatar)
                                .into(binding.roundedImageViewDisplayImage)
                        }
                    }

                    if (otherUserId != currentUserId) {
                        if (snapshot.child("typing").getValue(Boolean::class.java) == true) {
                            binding.textViewUserPresenceStatus.visibility = View.INVISIBLE
                            binding.textViewTypingStatus.visibility = View.VISIBLE
                        } else {
                            binding.textViewUserPresenceStatus.visibility = View.VISIBLE
                            binding.textViewTypingStatus.visibility = View.INVISIBLE
                        }

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Failed to read user data: ${error.message}")
            }

        })


        // Generate a unique conversation ID using the current user ID and the other user's ID
        val conversationID = generateConversationID(currentUserId!!, otherUserId)

        // Typing Indicator SETUP below
        var typing = false
        var handler = Handler(Looper.getMainLooper())
        var userDbRef = rtDB.getReference("users/${currentUserId}")

        binding.editTextMessage.addTextChangedListener(object : TextWatcher {
            // Typing Indicator SETUP below
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!typing) {
                    typing = true
                    val dataMap = hashMapOf<String, Any>(
                        "typing" to typing
                    )
                    userDbRef.updateChildren(dataMap)
                }

                handler.removeCallbacksAndMessages(null)

                handler.postDelayed({
                    typing = false
                    val dataMap = hashMapOf<String, Any>(
                        "typing" to typing
                    )
                    userDbRef.updateChildren(dataMap)
                }, 2000L)

            }

            override fun afterTextChanged(s: Editable?) {}

        })

        // Setting up the send button click listener
        binding.buttonSend.setOnClickListener {
            // Check if the message input is not empty
            if (binding.editTextMessage.text.toString().trim().isNotEmpty()) {
                // Reference to the metadata document for the chat
                val metaRef = rtDB.getReference("chats/$conversationID")
//                    db.collection("chats").document(conversationID)

                // List of participants sorted (current user and the other user)
//                val participants = listOf(auth.currentUser!!.uid, otherUserId).sorted()
                val participants = mapOf(
                    currentUserId to true,
                    otherUserId to true
                )

                // Timestamp for the message
                val timestamp = ServerValue.TIMESTAMP

                // Last message content
                val lastMessage = binding.editTextMessage.text.toString().trim()

                // Creating a map to store metadata of the chat
                val userMetaData = hashMapOf(
                    "participants" to participants,
                    "lastMessage" to lastMessage,
                    // setting the the last message read for the other user (with whom im chatting with)
                    // hasn't read or read to false
                    "unRead_By_$otherUserId" to false,
                    "lastMessageTimestamp" to timestamp,
                    "lastMessageSender" to auth.currentUser!!.uid
                )

                // Setting the metadata document in Firestore
                metaRef.updateChildren(userMetaData)

                // Reference to the messages collection within the chat document
                val messageRef = rtDB.getReference("chats").child(conversationID).child("messages")
                // Generating a new message ID
                val newMessageId = messageRef.push().key // Firebase generates a random ID

                // Creating a map for the message data
                val messageData = hashMapOf(
                    "sender" to auth.currentUser?.uid,
                    "content" to binding.editTextMessage.text.toString().trim(),
                    "timestamp" to timestamp,
                    "isRead" to false,
                    "lastReadTimestamp" to ""
                )

                // Adding the new message to Firestore
                if (newMessageId != null) {
                    messageRef.child(newMessageId).setValue(messageData)
                        .addOnCompleteListener { task ->
                            // Clearing the message input field on successful send
                            if (task.isSuccessful) {
                                binding.editTextMessage.text.clear()
                            }
                        }
                        .addOnFailureListener { exception ->
                            // Displaying an error message if sending fails
                            Toast.makeText(this@ChatActivity, exception.toString(), Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(
                        this@ChatActivity,
                        "Failed to generate new message id for this message",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        }

        // Setting up the RecyclerView for displaying messages
        recyclerView = binding.recyclerViewCurrentChat
        val layoutManager = LinearLayoutManager(this@ChatActivity)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager// Setting layout manager
        val adapter = ChatAdapter(messagesList, currentUserId) // Creating adapter for messages
        recyclerView.adapter = adapter // Setting the adapter to the RecyclerView

        var hasRead = false
        val currentTimestamp = System.currentTimeMillis()
        // Listening for changes in the messages collection
        rtDbChatListener = rtDB.getReference("chats/$conversationID/messages")

        var unReadIndicatorPosition = 0
        messageListener = object : ChildEventListener {
            // Message Listeners

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val sender = snapshot.child("sender").getValue(String::class.java) ?: ""
                val timestampLong = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                val isRead = snapshot.child("isRead").getValue(Boolean::class.java) ?: false

                // Convert Long timestamp to Date
//                val timestamp = Date(timestampLong)
//
//                // Formatting the timestamp to a readable string
//                val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
//                formatter.timeZone = TimeZone.getDefault()
//                val formattedTimeStamp = formatter.format(timestamp)
                val formattedTimeStamp = getRelativeTime(Timestamp((timestampLong/1000), 0)) // Format the timestamp for display
                // Creating a MessageUserData object
                val messageObject = MessageUserData(
                    id = snapshot.key ?: "",
                    sender = sender,
                    content = snapshot.child("content").getValue(String::class.java) ?: "",
                    timestamp = formattedTimeStamp,
                    hasRead = isRead
                )
                // Adding the message to the list and notifying the adapter

                if (sender != auth.currentUser?.uid) {

                    // This message wasn't sent by this user
                    if (!isRead && !hasRead && (timestampLong < currentTimestamp)) {

                        // If the message was sent before the user opened the chat and
                        // is unread both in the database and locally:

                        // (which indicates that this is the first message among all,
                        // which also indicates that the remaining message after this aren't read by this user)
                        // This ensures that the message is really an unread message

                        // Mark it as the first unread message and add an indicator
                        messagesList.add(MessageUserData("", "", "", "", false))
                        unReadIndicatorPosition = messagesList.size - 1
                        messagesList.add(messageObject)
                        hasRead = true
                        // Update read status and timestamp in the database
                        rtDB.getReference("chats/$conversationID/messages/${snapshot.key}").updateChildren(
                            mapOf(
                                "isRead" to true,
                                "lastReadTimestamp" to ServerValue.TIMESTAMP
                            )
                        )
                        // Update metadata to reflect that the user has read the last message
                        rtDB.getReference("chats/$conversationID").updateChildren(
                            mapOf(
                                "unRead_By_$currentUserId" to true,
                            )
                        )
                    } else if (!isRead && !hasRead && (timestampLong > currentTimestamp)) {
                        // If the message is unread but was sent after the user opened the chat:

                        // (which indicates that this is the first message among all,
                        // which also indicates that the remaining message after this aren't read by this user) &&
                        // BUT the time of the message sent is after the user opened the chat activity

                        // This ensures that the message isn't really an unread message

                        // Add the message without marking it as truly unread
                        messagesList.add(messageObject)
                        hasRead = true
                        // Update read status and timestamp in the database
                        rtDB.getReference("chats/$conversationID/messages/${snapshot.key}").updateChildren(
                            mapOf(
                                "isRead" to true,
                                "lastReadTimestamp" to ServerValue.TIMESTAMP
                            )
                        )
                        // Update metadata to reflect that the user has read the last message
                        rtDB.getReference("chats/$conversationID").updateChildren(
                            mapOf(
                                "unRead_By_$currentUserId" to true,
                            )
                        )
                    } else if (!isRead && hasRead) {
                        // If the message is unread in the database but is not the first unread message:

                        // (which indicates that this message is not the first unread message but
                        // maybe 2nd or 3rd continuous unread message,
                        // so if among 4 unread message, first message is read, then other 3 message will also get read by the user)
                        // BUT the time of the message sent is after the user opened the chat activity

                        // Add the message without marking it as truly unread
                        messagesList.add(messageObject)
                        rtDB.getReference("chats/$conversationID/messages/${snapshot.key}").updateChildren(
                            mapOf(
                                "isRead" to true,
                                "lastReadTimestamp" to ServerValue.TIMESTAMP
                            )
                        )
                    } else if (isRead) {
                        // If the message is already marked as read:
                        messagesList.add(messageObject)
                    }
                } else {

                    if (messagesList.size > 0) {
                        if (!messagesList[unReadIndicatorPosition].hasRead && messagesList[unReadIndicatorPosition].sender == "") {
                            // Removing the unread indicator containter "NEW"
                            // when this user replies/sent some message after those unread messages
                            messagesList.removeAt(unReadIndicatorPosition)
                            adapter.notifyItemRemoved(unReadIndicatorPosition)
                        }
                    }
                    // This message was sent by this user
                    messagesList.add(messageObject)
                }

                adapter.notifyDataSetChanged() // Notify adapter of data change
                // Scrolling to the bottom to show the latest message
                recyclerView.scrollToPosition(messagesList.size - 1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: ""
                if (snapshot.child("sender").getValue(String::class.java) == currentUserId) {
//                    Log.d("CHECK_CHANGED", snapshot.child("isRead").getValue(Boolean::class.java).toString())
//                    Log.d("CHECK_CHANGED", messagesList.toString())

                    val index = messagesList.indexOfFirst { it.id == id }
                    if (index != -1 &&
                        messagesList[index].hasRead != snapshot.child("isRead").getValue(Boolean::class.java)
                    ) {
                        val timestampLong = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                        // Convert Long timestamp to Date
                        val timestamp = Date(timestampLong)

                        // Formatting the timestamp to a readable string
                        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        formatter.timeZone = TimeZone.getDefault()
                        val formattedTimeStamp = formatter.format(timestamp)

                        messagesList[index] = MessageUserData(
                            id = id,
                            sender = snapshot.child("sender").getValue(String::class.java) ?: "",
                            content = snapshot.child("content").getValue(String::class.java) ?: "",
                            timestamp = formattedTimeStamp,
                            hasRead = snapshot.child("isRead").getValue(Boolean::class.java) ?: false,
                        )

                        adapter.notifyItemChanged(index)
                    }
                }

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        messageListener?.let {
            rtDbChatListener.orderByChild("timestamp")
                .addChildEventListener(it)
        }


        // Click listener for user profile frame
        binding.frameUserProfile.setOnClickListener {
            val intent = Intent(this@ChatActivity, UserProfileActivity::class.java)
            intent.apply {
                putExtra("id", otherUserId) // Pass the other user's ID to the profile activity
                putExtra("userFromChatActivity", true) // Flag indicating the source of the intent
            }
            startActivityForResult(intent, token) // Start UserProfileActivity with a request code
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        messageListener?.let {
            rtDbChatListener.removeEventListener(it)
        }
        messageListener = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) // Call superclass method
        if (requestCode == token && resultCode == RESULT_OK) {
            otherUserId = data?.getStringExtra("id") // Get the updated user ID from the result
        }
    }

    // Function to generate a unique conversation ID based on the participants
    private fun generateConversationID(user1: String, user2: String): String {
        val convoId = listOf(user1, user2).sorted() // Sorting the user IDs to maintain consistency
        return convoId.joinToString("_") // Joining the sorted IDs to create a unique ID
    }

    // Function to create options menu for the chat layout
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu resource
        menuInflater.inflate(R.menu.menu_chat_options, menu)
        return true
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
}
