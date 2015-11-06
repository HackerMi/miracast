LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := framework

LOCAL_PACKAGE_NAME := TVMiracast

LOCAL_CERTIFICATE := platform

#LOCAL_JNI_SHARED_LIBRARIES := libctsverifier_jni

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)
