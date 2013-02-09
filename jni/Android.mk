LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE	:= chook
LOCAL_SRC_FILES	:= chook.c

include $(BUILD_SHARED_LIBRARY)