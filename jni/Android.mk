LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := osslcrypto
LOCAL_SRC_FILES := libcrypto.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opensslmodule
LOCAL_SRC_FILES := libssl.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := libjpeg
LOCAL_LDLIBS := -llog
LOCAL_MODULE := libjpg
LOCAL_SRC_FILES := jpge.cpp wrapper.cpp stb_image.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_SHARED_LIBRARIES := osslcrypto opensslmodule
include $(BUILD_SHARED_LIBRARY)