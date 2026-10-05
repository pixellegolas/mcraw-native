#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "Decoder.hpp"
#include "RawData.hpp"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "MCRAW", __VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_se_pixellegolas_mcraw_Decoder_open(JNIEnv* env, jobject, jstring path) {
    const char* cpath = env->GetStringUTFChars(path, nullptr);
    try {
        auto* decoder = new motioncam::Decoder(std::string(cpath));
        env->ReleaseStringUTFChars(path, cpath);
        return reinterpret_cast<jlong>(decoder);
    } catch(...) {
        env->ReleaseStringUTFChars(path, cpath);
        return 0;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_se_pixellegolas_mcraw_Decoder_getFrameCount(JNIEnv* env, jobject, jlong handle) {
    auto* decoder = reinterpret_cast<motioncam::Decoder*>(handle);
    if(!decoder) return 0;
    return decoder->getNumFrames();
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_se_pixellegolas_mcraw_Decoder_decodeFrame(JNIEnv* env, jobject, jlong handle, jint index) {
    auto* decoder = reinterpret_cast<motioncam::Decoder*>(handle);
    if(!decoder) return nullptr;
    try {
        motioncam::RawData raw;
        motioncam::FrameMetadata meta;
        // load frame - this internally calls RawData::Decode for Rice Type 7
        decoder->loadFrame(index, raw, meta);
        int w = raw.width;
        int h = raw.height;
        // raw.data is vector<uint16_t> Bayer RGGB 16-bit
        // Convert to RGBA8 with simple debayer for preview (black level 64, white 1023)
        std::vector<uint8_t> rgba(w*h*4);
        // Simple bilinear debayer in C++ for speed
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                int i=y*w+x;
                uint16_t v = raw.data[i];
                float norm = (v - 64.f) / (1023.f - 64.f);
                if(norm<0) norm=0; if(norm>1) norm=1;
                // very simple: put raw as gray for now, Kotlin will do color
                // store as 16-bit in RGBA for transfer
                rgba[i*4+0] = (v>>8) & 0xFF;
                rgba[i*4+1] = v & 0xFF;
                rgba[i*4+2] = 0;
                rgba[i*4+3] = 255;
            }
        }
        jbyteArray arr = env->NewByteArray(rgba.size());
        env->SetByteArrayRegion(arr, 0, rgba.size(), reinterpret_cast<jbyte*>(rgba.data()));
        return arr;
    } catch(std::exception& e){
        LOGI("decode error %s", e.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_se_pixellegolas_mcraw_Decoder_close(JNIEnv* env, jobject, jlong handle) {
    auto* decoder = reinterpret_cast<motioncam::Decoder*>(handle);
    if(decoder) delete decoder;
}