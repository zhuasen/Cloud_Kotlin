package com.kotlin.cloudkotlin.kotlinUtil

/**
 *
 * 对某个String类进行划分操作
 * 比如“1234.jpg”
 * “1234.jpg”.getStrByIndex('.'  , 0) = “1234”
 *“1234.jpg”.getStrByIndex('.'  , 1) = “jpg”
 * **/
fun String.getStrByIndex(index : Char,part: Int = 0) : String{
    val lastPosition = this.lastIndexOf(index)
    if (lastPosition == -1) return ""
    return when(part){
        0-> this.substring(0,lastPosition - 1)
        1-> this.substring(lastPosition + 1)
        else-> ""
    }
}