#include <jni.h>
#include <algorithm>

#include "datagenerator.h"

class JListener {
    JNIEnv *m_pEnv;
    jobject m_jRef;
    jclass m_jClass;
    jmethodID m_jMethod;
public:
    JListener(JNIEnv *env, jobject jref) : m_pEnv(env)
            , m_jRef(m_pEnv->NewGlobalRef(jref))
            , m_jClass((jclass)m_pEnv->NewGlobalRef(m_pEnv->GetObjectClass(m_jRef)))
            , m_jMethod(m_pEnv->GetMethodID(m_jClass, "onNewValue", "(D)V"))
    {}

    JListener(const JListener& l) : m_pEnv(l.m_pEnv), m_jRef(m_pEnv->NewGlobalRef(l.m_jRef)), m_jClass((jclass)m_pEnv->NewGlobalRef(l.m_jClass)), m_jMethod(l.m_jMethod) {}
    JListener(JListener&& l) noexcept : m_pEnv(l.m_pEnv), m_jRef(l.m_jRef), m_jClass(l.m_jClass), m_jMethod(l.m_jMethod)
    {
        l.m_jRef = NULL;
    }

    ~JListener()
    {
        if (m_jRef != NULL)
        {
            m_pEnv->DeleteGlobalRef(m_jRef);
            m_pEnv->DeleteGlobalRef(m_jClass);
        }
    }

    void operator() (JNIEnv* env, double val) {
        if (m_jRef != NULL) {
            env->CallVoidMethod(m_jRef, m_jMethod, (jdouble)val);
        }
    }
};

static const std::size_t MAX_CHANNELS = 2;

static JavaVM* g_jvm = NULL;
static engine::DataGenerator* g_generators[MAX_CHANNELS] = {NULL, NULL};

extern "C" JNIEXPORT JNICALL
jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    g_jvm = vm;
    jint jversion = JNI_VERSION_1_6;
    JNIEnv *env;
    if (vm->GetEnv((void**)&env, jversion) != JNI_OK)
        return -1;

    return jversion;
}

extern "C" JNIEXPORT JNICALL
void Java_com_lexis_speedometer_DataCollectionService_nativeStart(JNIEnv *env, jclass, jobjectArray jListeners) {

    jsize count = env->GetArrayLength(jListeners);
    if (count > MAX_CHANNELS)
    {
        jclass jcre = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(jcre, "Too many data listeners were passed to start call");
        return;
    }

    for (jsize channel = 0; channel < count; ++channel)
    {
        jobject jListener = env->GetObjectArrayElement(jListeners, channel);
        if (g_generators[channel] == NULL)
        {
            if (channel % MAX_CHANNELS == 0)
            {
                g_generators[channel] = new engine::SpeedGenerator(g_jvm, JListener(env, jListener));
            } else {
                g_generators[channel] = new engine::TachoGenerator(g_jvm, JListener(env, jListener));
            }

        }
        g_generators[channel]->start();
    }
}

extern "C" JNIEXPORT JNICALL
void Java_com_lexis_speedometer_DataCollectionService_nativeCleanup(JNIEnv*, jclass )
{
    for (jsize channel = 0; channel < MAX_CHANNELS; ++channel) {
        delete(g_generators[channel]);
        g_generators[channel] = NULL;
    }
}

