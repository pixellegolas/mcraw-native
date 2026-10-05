package se.pixellegolas.mcraw
class Decoder {
    companion object { init { System.loadLibrary("mcraw_native") } }
    external fun open(path: String): Long
    external fun getFrameCount(handle: Long): Int
    external fun decodeFrame(handle: Long, index: Int): ByteArray?
    external fun close(handle: Long)
}