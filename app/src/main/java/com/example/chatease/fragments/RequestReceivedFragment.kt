package com.example.chatease.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.example.chatease.R
import com.example.chatease.adapters_recyclerview.FrndRequestResponseAdapter
import com.example.chatease.databinding.FragmentRequestReceivedBinding
import com.example.chatease.dataclass.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RequestReceivedFragment : Fragment() {
    private lateinit var binding: FragmentRequestReceivedBinding
    private val rtDB = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var listenerObject: ChildEventListener
    private val userDataList = mutableListOf<UserData>()
    private lateinit var adapter: FrndRequestResponseAdapter
    private lateinit var adapterDataObserver: AdapterDataObserver
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRequestReceivedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewReceived.layoutManager = LinearLayoutManager(requireContext())

        adapter = FrndRequestResponseAdapter(context = requireContext(), userDataList = userDataList, usage = "Received")
        binding.recyclerViewReceived.adapter = adapter
        val drawable = ContextCompat.getDrawable(requireContext(),R.drawable.vector_icon_single_tick)
        drawable?.setTint(Color.WHITE)
        var addedBackground = false

        adapterDataObserver = object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateBackground()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updateBackground()
            }

            private fun updateBackground() {
                val hasItems = adapter.itemCount > 0
                if (hasItems && !addedBackground) {
                    addedBackground = true
                    binding.recyclerViewReceived.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.card_view_design)
                } else if (!hasItems) {
                    addedBackground = false
                    binding.recyclerViewReceived.background = null
                }
            }
        }

        adapter.registerAdapterDataObserver(adapterDataObserver)



        auth.currentUser?.let { currentUser ->

            listenerObject = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (snapshot.exists()) {
                        fetchUserData(userID = snapshot.key!!)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (index in 0..<userDataList.size) {
                            if (userDataList[index].userID == snapshot.key) {
                                userDataList.removeAt(index)
                                adapter.notifyItemRemoved(index)
                                break
                            }
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {}

            }

            listenerObject.let {
                rtDB.getReference("users/${currentUser.uid}/friends/requestReceived")
                    .addChildEventListener(listenerObject)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        auth.currentUser?.let { currentUser ->
            listenerObject.let {
                rtDB.getReference("users/${currentUser.uid}/friends/requestReceived")
                    .removeEventListener(listenerObject)
            }
        }

        adapterDataObserver?.let {
            adapter.unregisterAdapterDataObserver(adapterDataObserver)
        }
    }

    private fun fetchUserData(userID: String) {
        CoroutineScope(Dispatchers.IO).launch {
            rtDB.getReference("users/$userID").get().addOnSuccessListener { snapshot ->
                val userData = UserData(
                    userName = snapshot.child("userName").getValue(String::class.java) ?: "",
                    displayName = snapshot.child("displayName").getValue(String::class.java) ?: "",
                    userID = userID,
                    userAvatar = snapshot.child("avatar").getValue(String::class.java) ?: ""
                )
                userDataList.add(userData)
                adapter.notifyItemInserted(userDataList.size - 1)
            }
        }
    }
}
