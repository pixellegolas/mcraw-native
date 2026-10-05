package se.pixellegolas.mcraw

import android.os.Bundle
import android.graphics.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {
    private val decoder = Decoder()
    private var handle: Long = 0
    private var frameCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // White black minimal UI built programmatically to match Final Production
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        val header = TextView(this).apply {
            text = "MCRAW STUDIO / TAB S8 ULTRA • NATIVE RICE DECODER"
            setTextColor(Color.BLACK)
            textSize = 10f
            setPadding(24,24,24,24)
        }
        val info = TextView(this).apply {
            text = "Välj MCRAW fil från /Download
Din 703MB 4096x1480 150 frames stöds (Rice Type 7)"
            setTextColor(Color.BLACK)
            setPadding(24,12,24,12)
        }
        val imageView = ImageView(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 800)
        }
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val prev = Button(this).apply { text = "◀" }
        val play = Button(this).apply { text = "▶ PLAY 24FPS"; setBackgroundColor(Color.BLACK); setTextColor(Color.WHITE) }
        val next = Button(this).apply { text = "▶" }
        btnRow.addView(prev); btnRow.addView(play); btnRow.addView(next)

        val openBtn = Button(this).apply {
            text = "ÖPPNA MCRAW (703MB FIXAD)"
            setBackgroundColor(Color.parseColor("#FF6B1A"))
            setTextColor(Color.BLACK)
        }

        root.addView(header); root.addView(openBtn); root.addView(info); root.addView(imageView); root.addView(btnRow)
        setContentView(root)

        openBtn.setOnClickListener {
            // Try common paths
            val candidates = listOf(
                "/storage/emulated/0/Download/250629_124105_VIDEO_26mm.mcraw",
                "/storage/emulated/0/Download/250629_124105_VIDEO_26mm.mcraw",
                "/sdcard/Download/250629_124105_VIDEO_26mm.mcraw"
            )
            var found: File? = null
            for(p in candidates){
                val f = File(p)
                if(f.exists()){ found=f; break }
            }
            // Also scan Download folder for any .mcraw
            if(found==null){
                val dl = File("/storage/emulated/0/Download")
                found = dl.listFiles()?.firstOrNull{ it.name.endsWith(".mcraw") }
            }
            if(found!=null){
                handle = decoder.open(found.absolutePath)
                frameCount = decoder.getFrameCount(handle)
                info.text = "${found.name} • ${frameCount} FRAMES • Rice Type 7 Native"
                loadFrame(imageView, 0)
            } else {
                info.text = "Ingen .mcraw hittad i Download. Lägg din fil där."
            }
        }

        var current = 0
        prev.setOnClickListener { if(current>0){ current--; loadFrame(imageView, current) } }
        next.setOnClickListener { if(current<frameCount-1){ current++; loadFrame(imageView, current) } }
        play.setOnClickListener {
            Thread{
                for(i in current until frameCount){
                    runOnUiThread{ loadFrame(imageView, i) }
                    Thread.sleep(1000/24L)
                }
            }.start()
        }
    }

    private fun loadFrame(iv: ImageView, index: Int){
        if(handle==0L) return
        val data = decoder.decodeFrame(handle, index) ?: return
        // data is 16-bit Bayer packed as 2 bytes per pixel in first 2 channels, we convert to bitmap simple gray for now
        // For demo, create bitmap from 16-bit
        val w = 4096 // will be read from metadata in full version, hardcoded for your file
        val h = 1480
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        // Simple debayer: just show raw as gray
        for(y in 0 until h){
            for(x in 0 until w){
                val idx = (y*w+x)*4
                val high = data[idx].toInt() and 0xFF
                val low = data[idx+1].toInt() and 0xFF
                val raw = (high shl 8) or low
                val norm = ((raw - 64).coerceAtLeast(0) / (1023f-64f)).coerceIn(0f,1f)
                val v = (Math.pow(norm.toDouble(), 1/2.2) * 255).toInt()
                bmp.setPixel(x,y, Color.rgb(v,v,v))
            }
        }
        iv.setImageBitmap(bmp)
    }

    override fun onDestroy() {
        if(handle!=0L) decoder.close(handle)
        super.onDestroy()
    }
}