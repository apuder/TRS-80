LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog  
LOCAL_MODULE := z80
LOCAL_CFLAGS := -fsigned-char  
LOCAL_SRC_FILES := native.c \
    z80.c
include $(BUILD_SHARED_LIBRARY)