package com.baro.adapters

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.ArraySet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.baro.R
import com.baro.models.Category
import java.lang.ref.WeakReference

class CategoryAdapter internal constructor(var context: WeakReference<Context>, var categories: ArrayList<Category>) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context.get())
    private val selectedCategories = HashSet<Category>()
    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.category_cell, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onBindViewHolder(@NonNull holder: ViewHolder, position: Int) {

        val category = categories[position]
        holder.categoryText.text = categories[position].emoji + " " + categories[position].name

        holder.categoryText.setOnClickListener(View.OnClickListener {
            category.isSelected = !category.isSelected

            if (category.isSelected) {
                selectedCategories.add(category)
                holder.categoryText.setBackgroundResource(R.color.light_green)
            } else {
                selectedCategories.remove(category)
                holder.categoryText.setBackgroundResource(R.color.white)
            }


        })
    }
    fun getSelectedCategories(): HashSet<Category> {
        return selectedCategories
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    fun getItem(id: Int): Category {
        return categories[id]
    }




    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var categoryText: TextView = itemView.findViewById(R.id.category_cell)
    }

}