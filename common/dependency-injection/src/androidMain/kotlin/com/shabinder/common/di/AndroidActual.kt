package com.shabinder.common.di

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.github.kiulian.downloader.model.YoutubeVideo
import com.github.kiulian.downloader.model.formats.Format
import com.github.kiulian.downloader.model.quality.AudioQuality
import com.razorpay.Checkout
import com.shabinder.common.database.activityContext
import com.shabinder.common.di.worker.ForegroundService
import com.shabinder.common.models.TrackDetails
import com.shabinder.common.ui.R
import org.json.JSONObject

actual fun openPlatform(packageID:String, platformLink:String){
    val manager: PackageManager = activityContext.packageManager
    try {
        val intent = manager.getLaunchIntentForPackage(packageID)
            ?: throw PackageManager.NameNotFoundException()
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        activityContext.startActivity(intent)
    } catch (e: PackageManager.NameNotFoundException) {
        val uri: Uri =
            Uri.parse(platformLink)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        activityContext.startActivity(intent)
    }
}

actual fun shareApp(){
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Hey, checkout this excellent Music Downloader http://github.com/Shabinder/SpotiFlyer")
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    activityContext.startActivity(shareIntent)
}

actual fun giveDonation() = startPayment()

private fun startPayment(mainActivity: Activity = activityContext as Activity) {
    /*
    *  You need to pass current activity in order to let Razorpay create CheckoutActivity
    * */
    val co = Checkout().apply {
        setKeyID("rzp_live_3ZQeoFYOxjmXye")
        setImage(R.drawable.ic_spotiflyer_logo)
    }

    try {
        val preFill = JSONObject()

        val options = JSONObject().apply {
            put("name","SpotiFlyer")
            put("description","Thanks For the Donation!")
            //You can omit the image option to fetch the image from dashboard
            //put("image","https://github.com/Shabinder/SpotiFlyer/raw/master/app/SpotifyDownload.png")
            put("currency","INR")
            put("amount","4900")
            put("prefill",preFill)
        }

        co.open(mainActivity,options)
    }catch (e: Exception){
        //showPop("Error in payment: "+ e.message)
        e.printStackTrace()
    }
}
actual fun queryActiveTracks() {
    val serviceIntent = Intent(activityContext, ForegroundService::class.java).apply {
        action = "query"
    }
    ContextCompat.startForegroundService(activityContext, serviceIntent)
}

actual suspend fun downloadTracks(
    list: List<TrackDetails>,
    getYTIDBestMatch:suspend (String,TrackDetails)->String?,
    saveFileWithMetaData:suspend (mp3ByteArray:ByteArray, trackDetails: TrackDetails) -> Unit
){
    if(!list.isNullOrEmpty()){
        val serviceIntent = Intent(activityContext, ForegroundService::class.java)
        serviceIntent.putParcelableArrayListExtra("object",ArrayList<TrackDetails>(list))
        activityContext.let { ContextCompat.startForegroundService(it, serviceIntent) }
    }
}

fun YoutubeVideo.getData(): Format?{
    return try {
        findAudioWithQuality(AudioQuality.medium)?.get(0) as Format
    } catch (e: java.lang.IndexOutOfBoundsException) {
        try {
            findAudioWithQuality(AudioQuality.high)?.get(0) as Format
        } catch (e: java.lang.IndexOutOfBoundsException) {
            try {
                findAudioWithQuality(AudioQuality.low)?.get(0) as Format
            } catch (e: java.lang.IndexOutOfBoundsException) {
                null
            }
        }
    }
}