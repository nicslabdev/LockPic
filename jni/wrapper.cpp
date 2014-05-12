// Copyright (C) 2014  Carlos Parés: carlosparespulido (at) gmail (dot) com
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

#include <jni.h>
#include <string.h>
#include <android/log.h>

#include "jpge.h"
#include "stb_image.h"

#define DEBUG_TAG "NDK_JPGCompr"

extern "C" {
	jboolean Java_es_uma_lcc_lockpic_MainActivity_encodeWrapperRegions(
					JNIEnv * env, jobject obj, jstring source,
					jstring dest, jint squareNum, jintArray horizStart,
					jintArray horizEnd, jintArray vertStart, jintArray vertEnd,
					jobjectArray keysArray, jstring picId)  {

				jboolean isCopy, ret;
			    const char * src_filename = env->GetStringUTFChars(source, &isCopy);
			    const char * dst_filename = env->GetStringUTFChars(dest, &isCopy);
			    const char * pic_id = env->GetStringUTFChars(picId, &isCopy);
			    unsigned char *keys[squareNum];
			    int i;
			    for(i=0; i<squareNum; i++)  {
			    	keys[i] = (unsigned char*) env->GetStringUTFChars(
			    			(jstring)(env->GetObjectArrayElement(keysArray, i)), &isCopy);
			    }
			    int hStart[squareNum], hEnd[squareNum], vStart[squareNum], vEnd[squareNum];
			    env->GetIntArrayRegion(horizStart, 0, squareNum, hStart);
			    env->GetIntArrayRegion(horizEnd, 0, squareNum, hEnd);
			    env->GetIntArrayRegion(vertStart, 0, squareNum, vStart);
			    env->GetIntArrayRegion(vertEnd, 0, squareNum, vEnd);
			    int w = 0;
			    int h = 0;
			    int channels = 0;
			    int req_channels = 3;
			    int actual_channels = 0;

			    int x,y,n;
			    unsigned char *pImage_data = stbi_load_dec(0, src_filename, &x, &y, &n, 0, squareNum, hStart, hEnd, vStart, vEnd, keys, pic_id);
			    if(pImage_data == NULL)  {
			    	__android_log_print(ANDROID_LOG_ERROR, DEBUG_TAG, "%s", stbi_failure_reason());
			    }
			    ret = jpge::compress_image_to_jpeg_file(dst_filename, x, y, n, pImage_data, jpge::params(false, squareNum, hStart, hEnd, vStart, vEnd, keys, pic_id));

			    stbi_image_free(pImage_data);
			    env->ReleaseStringUTFChars(source, src_filename);
			    env->ReleaseStringUTFChars(dest, dst_filename);
			    env->ReleaseStringUTFChars(picId, pic_id);

			    return ret;
			}

	jbyteArray Java_es_uma_lcc_lockpic_MainActivity_decodeWrapperRegions(
					JNIEnv * env, jobject obj, jstring source,
					jint squareNum, jintArray horizStart, jintArray horizEnd,
					jintArray vertStart, jintArray vertEnd, jobjectArray keysArray,
					jstring picId)  {

					jboolean isCopy, ret;
				    const char * src_filename = env->GetStringUTFChars(source, &isCopy);
				    const char * pic_id = env->GetStringUTFChars(picId, &isCopy);
				    unsigned char *keys[squareNum];
				    for(int i=0; i<squareNum; i++)  {
				    	keys[i] = (unsigned char*) env->GetStringUTFChars(
				    			(jstring)(env->GetObjectArrayElement(keysArray, i)), &isCopy);
				    }

				    int hStart[squareNum], hEnd[squareNum], vStart[squareNum], vEnd[squareNum];
				    env->GetIntArrayRegion(horizStart, 0, squareNum, hStart);
				    env->GetIntArrayRegion(horizEnd, 0, squareNum, hEnd);
				    env->GetIntArrayRegion(vertStart, 0, squareNum, vStart);
				    env->GetIntArrayRegion(vertEnd, 0, squareNum, vEnd);
				    int w = 0;
				    int h = 0;
				    int channels = 0;
				    int req_channels = 3;
				    int actual_channels = 0;

				    int x,y,n;
				    unsigned char *pImage_data = stbi_load_dec(true, src_filename, &x, &y, &n, 4, squareNum, hStart, hEnd, vStart, vEnd, keys, pic_id);
				    if(pImage_data == NULL)
				    {
				    	__android_log_print(ANDROID_LOG_ERROR, DEBUG_TAG, "%s", stbi_failure_reason());
				    }
				    env->ReleaseStringUTFChars(source, src_filename);

				    jbyteArray result = env->NewByteArray(x*y*4);
				    env->SetByteArrayRegion(result, 0, x*y*4, (jbyte*)pImage_data);

				    stbi_image_free(pImage_data);
				    return result;
				}
}
