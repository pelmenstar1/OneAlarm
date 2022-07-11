package io.pelmenstar.onealarm.shared.time

const val MILLIS_IN_MINUTE = 60000
const val MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE
const val MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR

const val SECONDS_IN_HOUR = 60 * 60
const val SECONDS_IN_DAY = 24 * SECONDS_IN_HOUR
const val SECONDS_IN_WEEK = 7 * SECONDS_IN_DAY

const val MINUTES_IN_DAY = 24 * 60

const val DAYS_PER_CYCLE = 146097
const val DAYS_0000_TO_1970 = DAYS_PER_CYCLE * 5 - (30 * 365 + 7)