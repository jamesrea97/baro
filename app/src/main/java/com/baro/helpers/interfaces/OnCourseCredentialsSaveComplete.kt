package com.baro.helpers.interfaces

import com.baro.models.Course

interface OnCourseCredentialsSaveComplete {
    fun onCourseDataReturned(result: Boolean?)
}