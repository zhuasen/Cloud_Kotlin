package com.kotlin.cloudkotlin.usefulClass

import android.app.Activity

//使用exitApp一键退出app
object ActivityCollector {

    private val  activities = ArrayList<Activity>()

    fun addActivity(activity: Activity){
        activities.add(activity)
    }

    fun removeActivity(activity: Activity){
        activities.remove(activity)
    }

    fun exitApp(){
        for (activity in activities){
            if(!activity.isFinishing) activity.finish()
         }
        activities.clear()
    }
}