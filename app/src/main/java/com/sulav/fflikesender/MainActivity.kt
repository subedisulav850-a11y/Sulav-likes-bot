package com.sulav.fflikesender

import android.content.*
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sulav.fflikesender.databinding.ActivityMainBinding
import org.json.JSONObject
import java.net.*
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class MainActivity: AppCompatActivity() {
 private lateinit var b: ActivityMainBinding
 private var raw=""
 private val api="aHR0cHM6Ly9zdWxhdmxpa2VzZW5kZXIyLnZlcmNlbC5hcHAvbGlrZT91aWQ9e30mc2VydmVyX25hbWU9e30ma2V5PUpNTEI="
 private fun endpoint()=String(android.util.Base64.decode(api, android.util.Base64.DEFAULT), StandardCharsets.UTF_8)
 override fun onCreate(s:Bundle?){super.onCreate(s);b=ActivityMainBinding.inflate(layoutInflater);setContentView(b.root);b.sendButton.setOnClickListener{send()};b.copyResponseButton.setOnClickListener{copy()};b.clearButton.setOnClickListener{clear()}}
 private fun send(){val uid=b.uidInput.text?.toString()?.trim().orEmpty();val region=b.regionInput.text?.toString()?.trim().orEmpty();b.uidLayout.error=null;b.regionLayout.error=null;if(uid.isEmpty()){b.uidLayout.error="Enter UID";return};if(region.isEmpty()){b.regionLayout.error="Enter region";return};val u=endpoint().replaceFirst("{}",URLEncoder.encode(uid,"UTF-8")).replaceFirst("{}",URLEncoder.encode(region,"UTF-8"));loading(true);thread{try{val c=URL(u).openConnection() as HttpURLConnection;c.requestMethod="GET";c.connectTimeout=15000;c.readTimeout=20000;val code=c.responseCode;val body=(if(code<400)c.inputStream else c.errorStream).bufferedReader().use{it.readText()};raw="HTTP $code\n$body";runOnUiThread{b.responseText.text=raw;if(code in 200..299)show(body)else fail("Request failed")};c.disconnect()}catch(e:Exception){raw="ERROR\n${e.message?:"Network error"}";runOnUiThread{b.responseText.text=raw;fail("Connection error")}}}}
 private fun show(body:String){try{val j=JSONObject(body);b.nameValue.text=j.optString("PlayerNickname","Unknown");b.beforeValue.text="Before likes: ${j.optInt("LikesbeforeCommand",0)}";b.afterValue.text="After likes: ${j.optInt("LikesafterCommand",0)}";b.givenValue.text="Like given: ${j.optInt("LikesGivenByAPI",0)}";b.remainsValue.text="Remains: ${j.optString("remains","N/A")}";b.apiStatusValue.text="Status: ${if(j.optInt("status",0)==1)"SUCCESS" else "FAILED"}";b.resultCard.visibility=View.VISIBLE;b.statusText.text="LIKE SENT SUCCESSFULLY";b.statusText.setTextColor(getColor(android.R.color.holo_green_dark))}catch(_:Exception){fail("Invalid API response")};loading(false)}
 private fun fail(t:String){b.statusText.text=t;b.statusText.setTextColor(getColor(android.R.color.holo_red_dark));b.resultCard.visibility=View.VISIBLE;loading(false)}
 private fun loading(x:Boolean){b.progress.visibility=if(x)View.VISIBLE else View.GONE;b.sendButton.isEnabled=!x;b.sendButton.text=if(x)"SENDING…" else "SEND LIKE"}
 private fun copy(){if(raw.isEmpty()){Toast.makeText(this,"No response",Toast.LENGTH_SHORT).show();return};(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("API Response",raw));Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show()}
 private fun clear(){b.uidInput.text?.clear();b.responseText.text="No response yet";b.resultCard.visibility=View.GONE;b.statusText.text="Ready";raw=""}
}
