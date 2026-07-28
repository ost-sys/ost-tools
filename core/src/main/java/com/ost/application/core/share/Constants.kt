package com.ost.application.core.share
import java.net.URLDecoder
import java.net.URLEncoder
object Constants {
    const val SERVICE_TYPE = "_ost_filetransfer._tcp."
    const val SERVICE_NAME_PREFIX = "OST_Share_"
    const val KEY_DEVICE_TYPE = "devtype"
    const val VALUE_DEVICE_PHONE = "phone"
    const val VALUE_DEVICE_WATCH = "watch"
    const val VALUE_DEVICE_UNKNOWN = "unknown"
    const val TRANSFER_PORT = 55667
    const val CMD_SEPARATOR = "|"
    const val CMD_REQUEST_SEND = "REQ_SEND"
    const val CMD_REQUEST_SEND_MULTI = "SEND_MULTI_REQUEST"
    const val FILE_META = "FILE_META"
    const val CMD_ACCEPT = "ACCEPT"
    const val CMD_REJECT = "REJECT"
    const val FILES_DIR = "OST"
    const val FILE_PROVIDER_AUTHORITY = "com.ost.application.fileprovider"
    const val NOTIFICATION_CHANNEL_ID = "ost_file_transfer_channel"
    const val NOTIFICATION_CHANNEL_ID_LIVE_UPDATES = "ost_file_transfer_live_updates"
    const val NOTIFICATION_CHANNEL_ID_COMPLETION = "ost_file_transfer_completion_channel"
    const val NOTIFICATION_CHANNEL_ID_INCOMING = "ost_file_transfer_incoming_channel"
    const val NOTIFICATION_ID_TRANSFER = 11223
    const val NOTIFICATION_ID_FOREGROUND_SERVICE = 11224
    const val NOTIFICATION_ID_SERVICE_FOREGROUND = 11224
    const val NOTIFICATION_ID_INCOMING_FILE = 11225
    const val ACTION_LAUNCH_SERVICE = "com.ost.application.share.ACTION_LAUNCH_SERVICE"
    const val ACTION_SHUTDOWN_SERVICE = "com.ost.application.share.ACTION_SHUTDOWN_SERVICE"
    const val ACTION_START_SERVICE = "com.ost.application.share.ACTION_START_SERVICE"
    const val ACTION_STOP_SERVICE = "com.ost.application.share.ACTION_STOP_SERVICE"
    const val ACTION_START_RECEIVING = "com.ost.application.share.ACTION_START_RECEIVING"
    const val ACTION_STOP_RECEIVING = "com.ost.application.share.ACTION_STOP_RECEIVING"
    const val ACTION_START_DISCOVERY = "com.ost.application.share.ACTION_START_DISCOVERY"
    const val ACTION_STOP_DISCOVERY = "com.ost.application.share.ACTION_STOP_DISCOVERY"
    const val ACTION_SEND_FILES = "com.ost.application.share.ACTION_SEND_FILES"
    const val ACTION_CANCEL_TRANSFER = "com.ost.application.share.ACTION_CANCEL_TRANSFER"
    const val ACTION_ACCEPT_RECEIVE = "com.ost.application.share.ACTION_ACCEPT_RECEIVE"
    const val ACTION_REJECT_RECEIVE = "com.ost.application.share.ACTION_REJECT_RECEIVE"
    const val EXTRA_FILE_URIS = "extra_file_uris"
    const val EXTRA_TARGET_DEVICE = "extra_target_device"
    const val EXTRA_REQUEST_ID = "extra_request_id"
    const val SENT_PREFIX = "SENT"
    const val RECEIVED_PREFIX = "RECEIVED"
    const val RECEIVING_PREFIX = "RECEIVING"
    const val CANCELLED_KEYWORD = "CANCELED"
    const val ERROR_PREFIX = "ERROR"
    const val TAG = "OST_Share"
    const val CONFIRMATION_TIMEOUT_MILLIS = 3000L
    const val INCOMING_REQUEST_RESPONSE_TIMEOUT_MILLIS = 30000L
    const val SENDER_RESPONSE_TIMEOUT_MILLIS = 40000L
    const val INCOMING_REQUEST_TIMEOUT_MS = 30000L
}
fun String.encodeForURL(): String {
    return runCatching { URLEncoder.encode(this, Charsets.UTF_8.name()) }.getOrDefault(this)
}
fun String.decodeFromURL(): String {
    return runCatching { URLDecoder.decode(this, Charsets.UTF_8.name()) }.getOrDefault(this)
}
