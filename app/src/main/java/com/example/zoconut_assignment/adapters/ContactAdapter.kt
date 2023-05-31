package com.example.zoconut_assignment.adapters

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.zoconut_assignment.R
import com.example.zoconut_assignment.data.UserModel
import com.example.zoconut_assignment.databinding.ContactCardBinding

class ContactAdapter(private val context: Context, private val items: ArrayList<UserModel?>) :
    RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val profileText: TextView = itemView.findViewById(R.id.profileName)
        val profileGithub: TextView = itemView.findViewById(R.id.profileGithub)
        val profileContact: TextView = itemView.findViewById(R.id.profileContact)
        val profileSkills: TextView = itemView.findViewById(R.id.profileSkills)
        val profileCall: ImageButton = itemView.findViewById(R.id.callBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ContactCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding.root)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.profileImage.loadImage(items[position]?.userPicture)
        holder.profileText.text = items[position]?.name
        holder.profileGithub.text = items[position]?.githubHandle
        holder.profileContact.text = items[position]?.contact
        holder.profileSkills.text = items[position]?.skills

        if (items[position]?.country?.isNotEmpty() == true) {
            holder.profileText.text = items[position]?.name + ", " + items[position]?.country
        }

        holder.profileCall.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    context as Activity,
                    Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    2
                )
            }
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:" + items[position]?.contact)
            }
            context.startActivity(intent)
        }
    }

    fun setUsers(users: ArrayList<UserModel>) { // setting adapter after get all the User data
        this.items.clear()
        this.items.addAll(users)
        notifyDataSetChanged()
    }

    private fun ImageView.loadImage(url: String?) {
        Glide.with(this)
            .load(url)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.error)
            .error(R.drawable.error)
            .into(this)
    }
}
