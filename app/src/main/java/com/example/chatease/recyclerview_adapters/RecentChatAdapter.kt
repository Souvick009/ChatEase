package com.example.chatease.recyclerview_adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.activities.ChatActivity
import com.example.chatease.databinding.RecentChatUserLayoutBinding
import com.example.chatease.dataclass.RecentChatData

class RecentChatAdapter(
    val context: Context, // Context for starting activities
    private val recentChatDataList: MutableList<RecentChatData> // List of recent chat data
) : RecyclerView.Adapter<RecentChatAdapter.RecentChatViewHolder>() {

    // ViewHolder class for binding recent chat layout
    class RecentChatViewHolder(val binding: RecentChatUserLayoutBinding) : RecyclerView.ViewHolder(binding.root) {}

    // Create new ViewHolder instances
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentChatViewHolder {
        val view = RecentChatUserLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentChatViewHolder(view)
    }

    // Get the total number of items in the list
    override fun getItemCount(): Int {
        return recentChatDataList.size
    }

    // Bind data to the ViewHolder
    override fun onBindViewHolder(holder: RecentChatViewHolder, position: Int) {
        holder.binding.apply {
            // Set the username and last message timestamp
            userName.text = recentChatDataList[position].userName
            userLastMessageTimeStamp.text = recentChatDataList[position].lastMessageTimeStamp

            // Create a formatted string for the last message
            val lastMessage =
                recentChatDataList[position].lastMessageSender + ": " + recentChatDataList[position].lastMessage
            // If the message is longer than 35 characters, truncate it
            if (lastMessage.length > 35) {
                val subStr = lastMessage.substring(0, 35) + "..."
                userLastMessage.text = subStr
            } else {
                userLastMessage.text = lastMessage
            }

            Glide.with(holder.binding.userAvatar.context)
                .load(recentChatDataList[position].avatar)
                .placeholder(R.drawable.vector_default_user_avatar)
                .into(holder.binding.userAvatar)

//            Picasso.get().load(recentChatDataList[position].avatar).into(holder.binding.userAvatar)

            // Set click listener to open the chat activity with the selected user
            root.setOnClickListener {
                val intent = Intent(context, ChatActivity::class.java)
                intent.apply {
                    putExtra("id", recentChatDataList[position].id)
                    putExtra("username", recentChatDataList[position].userName)
                    putExtra("displayname", recentChatDataList[position].displayName)
                    putExtra("avatar", recentChatDataList[position].avatar)
                }
                context.startActivity(intent) // Start the chat activity
            }
        }
    }
}